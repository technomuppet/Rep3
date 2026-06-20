package com.replog

import android.app.Application
import com.replog.util.DataSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RepLogApplication : Application() {
    @Inject lateinit var dataSeeder: DataSeeder
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        appScope.launch { dataSeeder.seedDataIfFirstLaunch() }
    }
}
