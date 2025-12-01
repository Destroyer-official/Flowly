package com.ledger.app

import android.app.Application
import com.ledger.app.data.local.SeedDataInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.sqlcipher.database.SQLiteDatabase
import javax.inject.Inject

/**
 * Main Application class for the Offline Ledger app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class LedgerApplication : Application() {

    @Inject
    lateinit var seedDataInitializer: SeedDataInitializer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        // Load SQLCipher native libraries
        SQLiteDatabase.loadLibs(this)
        
        // Initialize seed data on first launch
        applicationScope.launch {
            seedDataInitializer.initializeIfNeeded()
        }
    }
}
