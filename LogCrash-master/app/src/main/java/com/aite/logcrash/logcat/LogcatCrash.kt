package com.aite.logcrash.logcat

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.aite.logcrash.logcat.FTPManager.Companion.mFtpBackCallResult
import com.aite.logcrash.utils.logger
import java.io.*
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.*

class LogcatCrash: Thread.UncaughtExceptionHandler {
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var mContext: Context? = null
    private var mLogInfo = HashMap<String, String>()
    private val mSimpleDataFormat = SimpleDateFormat("yyyyMMdd_HH-mm-ss")
    private class Holder {
        companion object {
            val INSTANCE = LogcatCrash()
        }
    }
    companion object {
        private var isDelLocalLogs = true
        private const val account = "admin"
        private const val password ="123456"
        private const val serverIp = "www.server.com"
        private const val port = 21

        fun initCrash(context: Context, isDelLocalLogs: Boolean) {
            this.isDelLocalLogs = isDelLocalLogs
           Holder.INSTANCE.init(context)
        }
    }

    private fun init(paramContext:Context) {
        this.mContext = paramContext
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
    override fun uncaughtException(t: Thread, e: Throwable) {
        val result = handleException(e)
        object : Thread() {
            override fun run() {
                if (this@LogcatCrash.upload(result!!) && isDelLocalLogs) {
                    File("/sdcard/logcatCrash/errorLogs/$result").delete()
                }
            }
        }.start()
    }

    private fun handleException(throwable: Throwable): String? {
        if (throwable == null) return null
        var packageName = getDeviceInfo(mContext)
        return saveCrashLog(packageName!!, throwable)
    }

    private fun upload(logFileName: String): Boolean {
        if (logFileName == null) return false
        FTPManager.account = account
        FTPManager.hostIp = serverIp
        FTPManager.pwd = password
        FTPManager.port = port
        FTPManager.init()
        return FTPManager.upload("/sdcard/logcatCrash/errorLogs/", logFileName, "/logcatCrash/" + mLogInfo["app_name"] + "/")
    }

    private fun saveCrashLog(packageName:String, paramThrowable: Throwable) :String? {
        var stringBuffer = StringBuffer()
        for (entry in this.mLogInfo.entries) {
            val key = entry.key
            val value = entry.value
            stringBuffer.append("$key=$value\r\n")
        }
        var writer = StringWriter()
        var printWriter = PrintWriter(writer)
        paramThrowable.printStackTrace(printWriter)
        paramThrowable.printStackTrace()
        var throwable = paramThrowable.cause
        while (throwable != null) {
            throwable.printStackTrace(printWriter)
            printWriter.append("\r\n")
            throwable = throwable.cause
        }
        printWriter.flush()
        printWriter.close()
        val result = writer.toString()
        stringBuffer.append(result)
        val time = this.mSimpleDataFormat.format(Date())
        val fileName = "$packageName$time.log"
        try {
            var directory = File( "/sdcard/logcatCrash/errorLogs")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            logger("saveCrashLog, fileName=$fileName")
            val fileOutputStream = FileOutputStream("$directory/$fileName" )
            fileOutputStream.write(stringBuffer.toString().toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()
            return fileName
        } catch (var1: FileNotFoundException) {
            var1.printStackTrace()
        } catch (var2: IOException) {
            var2.printStackTrace()
        }
        return null
    }

    private fun getDeviceInfo(paramContext: Context?): String? {
        var packageName: String? = null
        try {
            val packageManager = paramContext!!.packageManager
            val packageInfo = packageManager.getPackageInfo(paramContext.packageName, PackageManager.GET_SHARED_LIBRARY_FILES)
            if (packageInfo != null) {
                this.mLogInfo["app_name"] = packageInfo.packageName
                this.mLogInfo["versionName"] = if (packageInfo.versionName == null) "null" else packageInfo.versionName
                this.mLogInfo["versionCode"] =  (StringBuilder(packageInfo.versionCode.toString())).toString()
                packageName = packageInfo.packageName
            }
        } catch (var1: PackageManager.NameNotFoundException) {
            var1.printStackTrace()
        }
        val fields = Build::class.java.declaredFields
        var buf: Byte = 0
        var arrayOfField: Array<Field?> = fields
        var index =arrayOfField.size
            while (buf < index) {
                val field = arrayOfField[buf.toInt()]
                try {
                    field!!.isAccessible = true
                    mLogInfo[field.name] = field.toString()
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
                buf++
        }
        return packageName
    }
}