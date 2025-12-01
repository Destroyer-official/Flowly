package com.ledger.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.ledger.app.presentation.theme.DesignSkin
import com.ledger.app.presentation.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app preferences using SharedPreferences.
 * Provides reactive access to theme mode, design skin, and default account settings.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _designSkin = MutableStateFlow(getDesignSkin())
    val designSkin: StateFlow<DesignSkin> = _designSkin.asStateFlow()

    private val _defaultAccountId = MutableStateFlow(getDefaultAccountId())
    val defaultAccountId: StateFlow<Long?> = _defaultAccountId.asStateFlow()

    fun getThemeMode(): ThemeMode {
        val mode = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(mode)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun getDesignSkin(): DesignSkin {
        val skin = prefs.getString(KEY_DESIGN_SKIN, DesignSkin.NEO_MINIMAL.name) ?: DesignSkin.NEO_MINIMAL.name
        return try {
            DesignSkin.valueOf(skin)
        } catch (e: IllegalArgumentException) {
            DesignSkin.NEO_MINIMAL
        }
    }

    fun setDesignSkin(skin: DesignSkin) {
        prefs.edit().putString(KEY_DESIGN_SKIN, skin.name).apply()
        _designSkin.value = skin
    }

    fun getDefaultAccountId(): Long? {
        val id = prefs.getLong(KEY_DEFAULT_ACCOUNT_ID, -1L)
        return if (id == -1L) null else id
    }

    fun setDefaultAccountId(accountId: Long?) {
        if (accountId == null) {
            prefs.edit().remove(KEY_DEFAULT_ACCOUNT_ID).apply()
        } else {
            prefs.edit().putLong(KEY_DEFAULT_ACCOUNT_ID, accountId).apply()
        }
        _defaultAccountId.value = accountId
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    companion object {
        private const val PREFS_NAME = "ledger_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DESIGN_SKIN = "design_skin"
        private const val KEY_DEFAULT_ACCOUNT_ID = "default_account_id"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
}
