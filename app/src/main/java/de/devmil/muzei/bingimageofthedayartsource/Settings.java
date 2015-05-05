package de.devmil.muzei.bingimageofthedayartsource;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Created by michaellamers on 05.05.15.
 */
public class Settings {
    private static final String PREF_MARKET_CODE = "art_source_settings_market_code";
    private static final String PREF_ORIENTATION_PORTRAIT = "art_source_settings_orientation_portrait";
    private static final String PREF_CURRENT_IMAGE_NUM = "art_source_runtime_current_image_number";
    private static final String PREF_CURRENT_MARKET = "art_source_runtime_current_market";
    private static final String PREF_CURRENT_ORIENTATION_PORTRAIT = "art_source_runtime_current_orientation_portrait";

    private static final BingMarket DEFAULT_MARKET = BingMarket.EN_US;

    private SharedPreferences _Preferences;
    private Context _Context;

    public Settings(Context context, SharedPreferences preferences)
    {
        _Context = context;
        _Preferences = preferences;
    }

    public BingMarket getBingMarket()
    {
        String marketCode = _Preferences.getString(PREF_MARKET_CODE, null);

        if(marketCode == null) {
            // Try find best match based on local
            Locale currentLocale = _Context.getResources().getConfiguration().locale;
            if (currentLocale != null) {
                String isoCode = currentLocale.toString().replace("_","-");
                BingMarket market = BingMarket.fromMarketCode(isoCode);
                if(market != BingMarket.Unknown){
                    marketCode = market.getMarketCode();
                }
            }

            // no best match? Default!
            if(marketCode == null) {
                return DEFAULT_MARKET;
            }
        }
        return BingMarket.fromMarketCode(marketCode);
    }

    public void setBingMarket(BingMarket bingMarket)
    {
        _Preferences.edit().putString(PREF_MARKET_CODE, bingMarket.getMarketCode()).commit();
    }

    public BingMarket getCurrentBingMarket()
    {
        return BingMarket.fromMarketCode(_Preferences.getString(PREF_CURRENT_MARKET, BingMarket.Unknown.getMarketCode()));
    }

    public void setCurrentBingMarket(BingMarket currentBingMarket)
    {
        _Preferences
                .edit()
                .putString(PREF_CURRENT_MARKET, currentBingMarket.getMarketCode())
                .commit();
    }

    public boolean isOrientationPortrait()
    {
        return  _Preferences.getBoolean(PREF_ORIENTATION_PORTRAIT, isPortraitDefault(_Context));
    }

    public void setIsOrientationPortrait(boolean isOrientationPortrait)
    {
        _Preferences.edit().putBoolean(PREF_ORIENTATION_PORTRAIT, isOrientationPortrait).commit();
    }

    private static boolean isPortraitDefault(Context context)
    {
        boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE);
        boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return !(xlarge | large);
    }

    public boolean isCurrentOrientationPortrait()
    {
        return _Preferences.getBoolean(PREF_CURRENT_ORIENTATION_PORTRAIT, isOrientationPortrait());
    }

    public void setIsCurrentOrientationPortrait(boolean isCurrentOrientationPortrait)
    {
        _Preferences
                .edit()
                .putBoolean(PREF_CURRENT_ORIENTATION_PORTRAIT, isCurrentOrientationPortrait)
                .commit();
    }

    public int getCurrentImageNumber()
    {
        return _Preferences.getInt(PREF_CURRENT_IMAGE_NUM, 0);
    }

    public void setCurrentImageNumber(int currentImageNumber)
    {
        _Preferences
                .edit()
                .putInt(PREF_CURRENT_IMAGE_NUM, currentImageNumber)
                .commit();
    }
}
