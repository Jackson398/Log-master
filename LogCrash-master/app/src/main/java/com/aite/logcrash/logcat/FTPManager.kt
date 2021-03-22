package com.aite.logcrash.logcat

import com.aite.logcrash.utils.logger
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.regex.Pattern

class FTPManager {
    companion object {
        var hostIp = "www.server.com"
        var account = "admin"
        var pwd = "123456"
        var port = 21
        private var mFtpClient: FTPClient? = null
        var mFtpBackCallResult = object : IFtpProgressReport {
            override fun uploadProgressReport(process: Int) {
                logger("upload progress=$process")
            }

            override fun downProgressReport(process: Int) {
                logger("down progress=$process")
            }
        }

        private fun getServerIp():String? {
            try {
                val pattern = Pattern.compile("^(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])$")
                return if (pattern.matcher(hostIp).matches()) {
                    hostIp
                } else {
                    InetAddress.getByName(hostIp).hostAddress
                }
            }  catch (exception: UnknownHostException) {
                logger("${exception.message}")
            }
            return null
        }

        fun init() {
            mFtpClient = FTPClient()
        }

        fun upload(localDir: String?, fileName: String?, serverPath: String?): Boolean {
            try {
                if (connect()) {
                    logger("filename=$fileName,serverPath=$serverPath")
                    if (uploadFile("$localDir$fileName", serverPath!!)) {
                        closeFTP()
                        logger("upload success")
                        if (mFtpBackCallResult != null) {
                            mFtpBackCallResult!!.uploadProgressReport(100)
                        }
                        return true
                    } else {
                        logger("upload fail")
                        if (mFtpBackCallResult != null) {
                            mFtpBackCallResult!!.uploadProgressReport(-1)
                        }
                    }
                }
            } catch (exception: Exception) {
            }
            return false
        }

        @Synchronized
        private fun connect(): Boolean {
            var bool = false
            try {
                if (mFtpClient == null) {
                    mFtpClient = FTPClient()
                }
                if (mFtpClient!!.isConnected) {
                    mFtpClient!!.disconnect()
                }
                val tempIp = getServerIp() ?: return false

                logger("connect==$tempIp")
                mFtpClient!!.setDataTimeout(20000)
                mFtpClient!!.controlEncoding = "utf-8"
                mFtpClient!!.connect(tempIp, port)
                if (FTPReply.isPositiveCompletion(mFtpClient!!.replyCode) && mFtpClient!!.login(account, pwd) ) {
                    bool = true
                }
                logger("login==$bool, account=$account, pwd=$pwd")
            } catch (var1: Exception) {
                logger("login, error=${var1.message}")
                var1.printStackTrace()
            }
            return bool
        }

        @Synchronized
        private fun uploadFile(localPath: String, serverPath: String):Boolean {
            try {
                val localFile = File(localPath)
                if (!localFile.exists()) {
                    logger("local file not existed=$localPath")
                    return false
                }

                if (createDirectory(serverPath)) {
                    val fileName = localFile.name
                    val localSize = localFile.length()
                    var files = mFtpClient!!.listFiles(fileName)
                   var  serverSize = if (files.isEmpty()) {
                        logger("server file not existed")
                        0L
                    } else {
                        files[0].size
                    }

                    if (mFtpClient!!.deleteFile(fileName)) {
                        serverSize = 0L
                    }

                    val randomAccessFile = RandomAccessFile(localFile, "r")
                    val step = localSize / 100L
                    var process = 0L
                    var currentSize = 0L
                    mFtpClient!!.enterLocalPassiveMode()
                    mFtpClient!!.setFileType(2)
                    mFtpClient!!.restartOffset = serverSize
                    randomAccessFile.seek(serverSize)
                    var output = mFtpClient!!.storeFileStream(fileName)
                    var buf = ByteArray(1024)
                    var length: Int? = null

                    while (randomAccessFile.read(buf).also { length = it } != -1) {
                        output.write(buf, 0, length!!)
                        currentSize += length as Long
                        if (currentSize / step != process) {
                            process = currentSize / step
                            if (process % 2L == 0L && mFtpBackCallResult != null) {
                                mFtpBackCallResult!!.uploadProgressReport(process.toInt())
                            }
                        }
                    }
                    randomAccessFile.close()
                    output.flush()
                    output.close()
                    if (mFtpClient!!.completePendingCommand()) {
                        return true
                    }
                    return false
                }
                logger("server create dir muff")
            } catch (var2:Exception) {
                var2.printStackTrace()
            }
            return false
        }

        private fun download(localDir: String, remoteDir: String, fileName: String) {
            try {
                if (connect()) {
                    if (downloadFile(localDir, remoteDir, fileName)) {
                        closeFTP()
                        mFtpBackCallResult!!.downProgressReport(100)
                        logger("download success")
                    } else {
                        mFtpBackCallResult!!.downProgressReport(-1)
                        logger("download fail")
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }

        private fun downloadFile(localDir: String, remoteDir: String, serverFileName: String):Boolean {
            var files = mFtpClient!!.listFiles("$remoteDir$serverFileName")
            if (files.isEmpty()) {
                logger("server file not existed")
                return false
            } else {
                if (!File("$localDir").exists()) {
                    File("$localDir").mkdir()
                }

                var serverSize = files[0].size
                var localFile = File("$localDir${files[0].name}")
                var localSize = 0L
                if (localFile.exists()) {
                    localFile.delete()
                    logger("local file existed, delete existed file going")
                }

                var step = serverSize / 100L
                var process = 0L
                var currentSize = 0L
                mFtpClient!!.enterLocalActiveMode()
                mFtpClient!!.setFileType(2)
                var output = FileOutputStream(localFile, true)
                mFtpClient!!.restartOffset = localSize
                var input = mFtpClient!!.retrieveFileStream("$remoteDir$serverFileName")
                var buf = ByteArray(4096)
                var length: Int
                while (input.read(buf).also { length = it } != -1) {
                    output.write(buf, 0, length)
                    currentSize += length.toLong()
                    if (currentSize / step != process) {
                        process = currentSize / step
                        logger("process===========$process")
                        if (process % 2L == 0L && mFtpBackCallResult != null) {
                            mFtpBackCallResult!!.downProgressReport(process.toInt())
                        }
                    }
                }
                output.flush()
                output.close()
                output.close()
                return mFtpClient!!.completePendingCommand()
            }
        }

        private fun createDirectory(remote: String): Boolean {
            try {
                val directory = remote.substring(0, remote.lastIndexOf("/") + 1)
                if (directory != "/" && !mFtpClient!!.changeWorkingDirectory(String(directory.toByteArray(charset("GBK")), charset("iso-8859-1")))) {
                   var  start = if (directory.startsWith("/")) {
                        1
                    } else {
                        0
                    }

                    var end = directory.indexOf("/", start)
                    do {
                        val subDirectory = String(remote.substring(start, end).toByteArray(charset("GBK")), charset("iso-8859-1"))
                        if (!mFtpClient!!.changeWorkingDirectory(subDirectory)) {
                            if (!mFtpClient!!.makeDirectory(subDirectory)) {
                                logger("create dir fai")
                                return false
                            }

                            mFtpClient!!.changeWorkingDirectory(subDirectory)
                        }
                        start = end + 1
                        end = directory.indexOf("/", start)
                    } while (end  > start)
                }
                return true
            } catch (var5: Exception) {
                var5.printStackTrace()
                return false
            }
        }

        private fun closeFTP() {
            try {
                if (mFtpClient!!.isConnected) {
                    mFtpClient!!.disconnect()
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }
}