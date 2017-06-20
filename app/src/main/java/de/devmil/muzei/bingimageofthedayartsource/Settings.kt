package de.devmil.muzei.bingimageofthedayartsource

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration

/**
* Created by michaellamers on 05.05.15.
*/
class Settings(private val _Context: Context, private val _Preferences: SharedPreferences) {

    @Suppress("DEPRECATION")
            // Try find best match based on local
    // no best match? Default!
    var bingMarket: BingMarket
        get() {
            var marketCode = _Preferences.getString(PREF_MARKET_CODE, null)

            if (marketCode == null) {
                val currentLocale = _Context.resources.configuration.locale
                if (currentLocale != null) {
                    val isoCode = currentLocale.toString().replace("_", "-")
                    val market = BingMarket.fromMarketCode(isoCode)
                    if (market !== BingMarket.Unknown) {
                        marketCode = market.marketCode
                    }
                }
                if (marketCode == null) {
                    return DEFAULT_MARKET
                }
            }
            return BingMarket.fromMarketCode(marketCode)
        }
        set(bingMarket) {
            _Preferences.edit().putString(PREF_MARKET_CODE, bingMarket.marketCode).apply()
        }

    var currentBingMarket: BingMarket
        get() = BingMarket.fromMarketCode(_Preferences.getString(PREF_CURRENT_MARKET, BingMarket.Unknown.marketCode)!!)
        set(currentBingMarket) {
            _Preferences
                    .edit()
                    .putString(PREF_CURRENT_MARKET, currentBingMarket.marketCode)
                    .apply()
        }

    var isOrientationPortrait: Boolean
        get() = _Preferences.getBoolean(PREF_ORIENTATION_PORTRAIT, isPortraitDefault(_Context))
        set(isOrientationPortrait) {
            _Preferences.edit().putBoolean(PREF_ORIENTATION_PORTRAIT, isOrientationPortrait).apply()
        }

    var isCurrentOrientationPortrait: Boolean
        get() = _Preferences.getBoolean(PREF_CURRENT_ORIENTATION_PORTRAIT, isOrientationPortrait)
        set(isCurrentOrientationPortrait) {
            _Preferences
                    .edit()
                    .putBoolean(PREF_CURRENT_ORIENTATION_PORTRAIT, isCurrentOrientationPortrait)
                    .apply()
        }

    var currentImageNumber: Int
        get() = _Preferences.getInt(PREF_CURRENT_IMAGE_NUM, 0)
        set(currentImageNumber) {
            _Preferences
                    .edit()
                    .putInt(PREF_CURRENT_IMAGE_NUM, currentImageNumber)
                    .apply()
        }

    companion object {
        private val PREF_MARKET_CODE = "art_source_settings_market_code"
        private val PREF_ORIENTATION_PORTRAIT = "art_source_settings_orientation_portrait"
        private val PREF_CURRENT_IMAGE_NUM = "art_source_runtime_current_image_number"
        private val PREF_CURRENT_MARKET = "art_source_runtime_current_market"
        private val PREF_CURRENT_ORIENTATION_PORTRAIT = "art_source_runtime_current_orientation_portrait"

        private val DEFAULT_MARKET = BingMarket.EN_US

        private fun isPortraitDefault(context: Context): Boolean {
            val xlarge = context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE
            val large = context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE
            return !(xlarge or large)
        }
    }
}
