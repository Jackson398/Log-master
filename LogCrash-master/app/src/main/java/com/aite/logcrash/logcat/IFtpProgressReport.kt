package com.aite.logcrash.logcat

interface IFtpProgressReport {
    fun uploadProgressReport(process: Int)
    fun downProgressReport(process: Int)
}