package com.replog.util

import com.replog.BuildConfig

/** Single accessor for the runtime app version (used by legal acceptance records). */
object AppInfo {
    val versionName: String get() = BuildConfig.VERSION_NAME
    val versionCode: Int get() = BuildConfig.VERSION_CODE
}
