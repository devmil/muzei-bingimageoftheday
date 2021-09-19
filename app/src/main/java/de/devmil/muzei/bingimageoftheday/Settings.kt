package de.devmil.muzei.bingimageoftheday

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration

/**
 * Created by michaellamers on 05.05.15.
 */
class Settings(private val context: Context, private val preferences: SharedPreferences) {

    @Suppress("DEPRECATION")
    // Try find best match based on local
    // no best match? Default!
    var bingMarket: BingMarket
        get() {
            var marketCode = preferences.getString(PREF_MARKET_CODE, null)

            if (marketCode == null) {
                val currentLocale = context.resources.configuration.locale
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
            preferences.edit().putString(PREF_MARKET_CODE, bingMarket.marketCode).apply()
        }

    var currentBingMarket: BingMarket
        get() = BingMarket.fromMarketCode(preferences.getString(PREF_CURRENT_MARKET, BingMarket.Unknown.marketCode)!!)
        set(currentBingMarket) {
            preferences
                    .edit()
                    .putString(PREF_CURRENT_MARKET, currentBingMarket.marketCode)
                    .apply()
        }

    var isOrientationPortrait: Boolean
        get() = preferences.getBoolean(PREF_ORIENTATION_PORTRAIT, isPortraitDefault(context))
        set(isOrientationPortrait) {
            preferences.edit().putBoolean(PREF_ORIENTATION_PORTRAIT, isOrientationPortrait).apply()
        }

    var isStoreImages: Boolean
        get() = preferences.getBoolean(PREF_STORE_IMAGES, false)
        set(isStoreImages) {
            preferences.edit().putBoolean(PREF_STORE_IMAGES, isStoreImages).apply()
        }

    var isCurrentOrientationPortrait: Boolean
        get() = preferences.getBoolean(PREF_CURRENT_ORIENTATION_PORTRAIT, isOrientationPortrait)
        set(isCurrentOrientationPortrait) {
            preferences
                    .edit()
                    .putBoolean(PREF_CURRENT_ORIENTATION_PORTRAIT, isCurrentOrientationPortrait)
                    .apply()
        }


    var currentImageNumber: Int
        get() = preferences.getInt(PREF_CURRENT_IMAGE_NUM, 0)
        set(currentImageNumber) {
            preferences
                    .edit()
                    .putInt(PREF_CURRENT_IMAGE_NUM, currentImageNumber)
                    .apply()
        }

    companion object {
        private val PREF_MARKET_CODE = "art_source_settings_market_code"
        private val PREF_ORIENTATION_PORTRAIT = "art_source_settings_orientation_portrait"
        private val PREF_STORE_IMAGES = "art_source_settings_store_images"
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
