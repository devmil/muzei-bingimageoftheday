/*
 * Copyright 2014 Devmil Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.devmil.muzei.bingimageofthedayartsource;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.IBinder;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.MuzeiArtSource;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.devmil.muzei.bingimageofthedayartsource.cache.BingImageCache;
import de.devmil.muzei.bingimageofthedayartsource.events.RequestMarketSettingChangedEvent;
import de.devmil.muzei.bingimageofthedayartsource.events.RequestPortraitSettingChangedEvent;
import de.devmil.common.utils.LogUtil;
import de.greenrobot.event.EventBus;

import static de.devmil.common.utils.LogUtil.LOGD;

/**
 * Muzei Art source for Bing images of the day.
 * This class is the plugin entry point for Muzei
 */
public class BingImageOfTheDayArtSource extends RemoteMuzeiArtSource {

    private static EventCatcher _CatcherInstance;

    static
    {
        //instantiate the EventCatcher when BingImageOfTheDayArtSource is loaded
        _CatcherInstance = new EventCatcher();
    }

    private static final String TAG = LogUtil.makeLogTag(BingImageOfTheDayArtSource.class);

    private static final String SOURCE_NAME = "BingImageOfTheDayArtSource";

    public static final String PREF_MARKET_CODE = "art_source_settings_market_code";
    public static final String PREF_ORIENTATION_PORTRAIT = "art_source_settings_orientation_portrait";
    private static final String PREF_CURRENT_IMAGE_NUM = "art_source_runtime_current_image_number";
    private static final String PREF_CURRENT_MARKET = "art_source_runtime_current_market";
    private static final String PREF_CURRENT_ORIENTATION_PORTRAIT = "art_source_runtime_current_orientation_portrait";

    public static final BingMarket DEFAULT_MARKET = BingMarket.EN_US;

    private static final String ACTION_REQUESTUPDATE = "de.devmil.muzei.bingimageofthedayartsource.ACTION_REQUESTUPDATE";
    private static final String ACTION_ENSUREINITIALIZED = "de.devmil.muzei.bingimageofthedayartsource.ACTION_ENSURE_INITIALIZED";

    private static final int FAST_RETRY_MINUTES = 15;

    //this is static so that the asynchronous download keeps running even if this service instance gets destroyed
    private static BingImageCache _Cache;

    public BingImageOfTheDayArtSource() {
        super("de.devmil.muzei.Bing");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        if (intent != null &&
                (ACTION_REQUESTUPDATE.equals(intent.getAction())
                || ACTION_ENSUREINITIALIZED.equals(intent.getAction())))
            refreshImage();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    protected void onTryUpdate(int updateReason) throws RetryException {
        SharedPreferences prefs = getSharedPreferences(getApplicationContext(), SOURCE_NAME); // getSharedPreferences();

        if(_Cache == null)
            _Cache = new BingImageCache(getApplicationContext());

        LOGD(TAG, "Update try received");

        BingMarket market = BingMarket.fromMarketCode(prefs.getString(PREF_MARKET_CODE, DEFAULT_MARKET.getMarketCode()));
        BingMarket currentMarket = BingMarket.fromMarketCode(prefs.getString(PREF_CURRENT_MARKET, BingMarket.Unknown.getMarketCode()));
        boolean marketChanged = market != currentMarket;
        LOGD(TAG, String.format("Market changed: %b", marketChanged));

        boolean requestPortraitMode = prefs.getBoolean(BingImageOfTheDayArtSource.PREF_ORIENTATION_PORTRAIT, isPortraitDefault(getApplicationContext()));
        boolean currentRequestPortraitMode = prefs.getBoolean(PREF_CURRENT_ORIENTATION_PORTRAIT, requestPortraitMode);
        LOGD(TAG, String.format("request portrait mode: %b", requestPortraitMode));
        boolean requestPortraitModeChanged = requestPortraitMode != currentRequestPortraitMode;

        //check if a "Bing day" has passed
        boolean newestImageChanged = false;
        if(_Cache.getMetadata() != null && _Cache.getMetadata().getValidThru().before(Calendar.getInstance()))
            newestImageChanged = true;
        LOGD(TAG, String.format("newest image changed: %b", newestImageChanged));

        //when anything changed the cache has to be reloaded
        boolean reloadCache = marketChanged || requestPortraitModeChanged || _Cache.getMetadata() == null || newestImageChanged;

        if(reloadCache)
        {
            LOGD(TAG, "we have to reload the cache metadata");
            BingImageOfTheDayMetadataRetriever retriever = new BingImageOfTheDayMetadataRetriever(market, BingImageDimension.HD, requestPortraitMode);
            List<BingImageMetadata> metadata = retriever.getBingImageOfTheDayMetadata();

            //if there is a result
            if (metadata != null && metadata.size() >= 1)
            {
                //build the cache metadata
                LOGD(TAG, "metadata received");
                List<BingImageCache.CacheEntry> entries = new ArrayList<BingImageCache.CacheEntry>();
                for(int i=0; i<metadata.size(); i++)
                {
                    entries.add(
                            new BingImageCache.CacheEntry(
                                    metadata.get(i).getUri(),
                                    getImageTitle(metadata.get(i).getStartDate()),
                                    metadata.get(i).getCopyrightOrEmpty()));
                }
                LOGD(TAG, "setting the metadata to the cache");
                _Cache.setMetadata(new BingImageCache.CacheMetadata(entries.toArray(new BingImageCache.CacheEntry[0]), getNextUpdate(metadata.get(0).getStartDate())));
            }
        }
        _Cache.ensureMissingImages();

        int lastNumber = prefs.getInt(PREF_CURRENT_IMAGE_NUM, 0);

        boolean userNext = updateReason == UPDATE_REASON_USER_NEXT;
        boolean hasBeenScheduled = updateReason == UPDATE_REASON_SCHEDULED;

        LOGD(TAG, String.format("userNext: %b", userNext));
        LOGD(TAG, String.format("scheduled: %b", hasBeenScheduled));

        if(_Cache.getMetadata() != null)
        {
            LOGD(TAG, "Cache has metadata => proceeding");

            int imageNumberToUse = lastNumber;
            if (userNext) {
                imageNumberToUse++;
                //this way next would mean current because there is a new image
                if (newestImageChanged)
                    imageNumberToUse++;
            }

            //this is the moment we change the current Bing of the day image => move to the current day
            //TODO: preference to decide if the currently active image should stay active as long as possible or if the new image should be activated (as it currently happens)
            if ((hasBeenScheduled && newestImageChanged) || reloadCache) {
                LOGD(TAG, "setting the current image to today");
                imageNumberToUse = 0;
            }

            //overflow
            if (imageNumberToUse >= _Cache.getMetadata().getEntries().length)
                imageNumberToUse = 0;

            //calculate up to what point of time this image is (or has been) active on Bing
            Calendar imageOnBingPresentThru = Calendar.getInstance();
            imageOnBingPresentThru.setTimeInMillis(_Cache.getMetadata().getValidThru().getTimeInMillis());
            imageOnBingPresentThru.add(Calendar.DAY_OF_MONTH, -1);
            imageOnBingPresentThru.add(Calendar.MINUTE, -1);
            imageOnBingPresentThru.add(Calendar.DAY_OF_MONTH, -1 * imageNumberToUse);

            String imgToken = createToken(imageOnBingPresentThru.getTime(), market, requestPortraitMode);
            String currentToken = "";
            if (getCurrentArtwork() != null)
                currentToken = getCurrentArtwork().getToken();
            prefs
                    .edit()
                    .putInt(PREF_CURRENT_IMAGE_NUM, imageNumberToUse)
                    .putString(PREF_CURRENT_MARKET, market.getMarketCode())
                    .putBoolean(PREF_CURRENT_ORIENTATION_PORTRAIT, requestPortraitMode)
                    .commit();
            boolean newImage = false;

            if (!imgToken.equals(currentToken)) {
                LOGD(TAG, "image changed => notifying Muzei");
                newImage = true;
                BingImageCache.CacheEntry entry = _Cache.getMetadata().getEntries()[imageNumberToUse];

                //this is the web Uri of the image
                Uri uri = entry.getUri();

                //when our cache already has the image => get it from there
                if(_Cache.hasImage(imageNumberToUse))
                    uri = BingImageContentProvider.getContentUri(_Cache.getFileName(imageNumberToUse), true);

                //tell Muzei to use the new image
                publishArtwork(
                        new Artwork.Builder()
                                .title(entry.getDescription())
                                .byline(entry.getCopyright())
                                .imageUri(uri)
                                .viewIntent(new Intent(Intent.ACTION_VIEW, uri))
                                .token(imgToken)
                                .build());
            }
            //if this update has been scheduled but didn't result in a new image => retry in FAST_RETRY_MINUTES minutes
            boolean scheduleFastRetry = !newImage && hasBeenScheduled;
            if (scheduleFastRetry)
                requestFastRetryUpdate();
            else
                requestNextImageUpdate(imageOnBingPresentThru.getTime());
        }
        else
        {
            throw new RetryException();
        }
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return MuzeiArtSource.getSharedPreferences(context, SOURCE_NAME);
    }

    public static boolean isPortraitDefault(Context context)
    {
        boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE);
        boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return !(xlarge | large);
    }

    private String createToken(Date date, BingMarket market, boolean portrait) {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");

        return String.format("%s|%s|%b", market.getMarketCode(), df.format(date), portrait);
    }

    private String getImageTitle(Date imageDate) {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");

        return "Bing: " + df.format(imageDate.getTime());
    }

    /**
     * this method schedules a normal update request
     * @param imageStartDate the valid thru date of one of the images. Only the time of the day portion gets used to calculate the next update
     * @return
     */
    private Calendar requestNextImageUpdate(Date imageStartDate) {
        Calendar nextUpdate = getNextUpdate(imageStartDate);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        LOGD(TAG, String.format("next update: %s", sdf.format(nextUpdate.getTime())));
        scheduleUpdate(nextUpdate.getTimeInMillis());

        return nextUpdate;
    }

    /**
     * This method calculates the next update point of time based on the
     * time of the day information provided in the "newestImageStartDate" parameter
     * @param newestImageStartDate
     * @return
     */
    private Calendar getNextUpdate(Date newestImageStartDate)
    {
        Calendar nextUpdate = Calendar.getInstance();

        Calendar imageStart = Calendar.getInstance();
        imageStart.setTime(newestImageStartDate);

        boolean addDay = true;

        if (nextUpdate.get(Calendar.HOUR_OF_DAY) < imageStart.get(Calendar.HOUR_OF_DAY))
            addDay = false;
        else if (nextUpdate.get(Calendar.HOUR_OF_DAY) == imageStart.get(Calendar.HOUR_OF_DAY)) {
            if (nextUpdate.get(Calendar.MINUTE) < imageStart.get(Calendar.MINUTE))
                addDay = false;
            else if (nextUpdate.get(Calendar.MINUTE) == imageStart.get(Calendar.MINUTE)) {
                if (nextUpdate.get(Calendar.SECOND) < imageStart.get(Calendar.SECOND))
                    addDay = false;
            }
        }

        nextUpdate.set(Calendar.HOUR_OF_DAY, imageStart.get(Calendar.HOUR_OF_DAY));
        nextUpdate.set(Calendar.MINUTE, imageStart.get(Calendar.MINUTE));
        nextUpdate.set(Calendar.SECOND, imageStart.get(Calendar.SECOND));
        nextUpdate.set(Calendar.MILLISECOND, imageStart.get(Calendar.MILLISECOND));

        nextUpdate.add(Calendar.MINUTE, 1); //wait 1 minute extra

        if (addDay)
            nextUpdate.add(Calendar.DAY_OF_MONTH, 1);

        return nextUpdate;
    }

    /**
     * Schedules a fast retry update (one hour in the future)
     * @return
     */
    private Calendar requestFastRetryUpdate() {
        Calendar nextUpdate = Calendar.getInstance();
        nextUpdate.add(Calendar.MINUTE, FAST_RETRY_MINUTES);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        LOGD(TAG, String.format("next update: %s", sdf.format(nextUpdate.getTime())));

        scheduleUpdate(nextUpdate.getTimeInMillis());

        return nextUpdate;
    }

    private void refreshImage() {
        Calendar nextUpdate = Calendar.getInstance();
        nextUpdate.add(Calendar.MILLISECOND, 500);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        LOGD(TAG, String.format("next update: %s", sdf.format(nextUpdate.getTime())));
        //this one doesn't get stored so that it isn't a daily schedule (that would reset the image number)
        scheduleUpdate(nextUpdate.getTimeInMillis());
    }

    public static void ensureInitialized(Context context)
    {
        Intent thisServiceIntent = new Intent();
        thisServiceIntent.setClass(context, BingImageOfTheDayArtSource.class);
        thisServiceIntent.setAction(ACTION_ENSUREINITIALIZED);
        context.startService(thisServiceIntent);
    }

    /**
     * This class is used to get EventBus events even if there is no instance of BingImageOfTheDayArtSource
     */
    public static class EventCatcher
    {
        public EventCatcher()
        {
            EventBus.getDefault().register(this);
        }

        public void onEventBackgroundThread(RequestPortraitSettingChangedEvent e) {
            requestUpdate(e.getContext());
        }

        public void onEventBackgroundThread(RequestMarketSettingChangedEvent e) {
            requestUpdate(e.getContext());
        }

        private void requestUpdate(Context context)
        {
            //this transition (source thread -> here -> Service intent) is needed because the service doesn't have a context
            //when this event is fired from the settings UI
            Intent thisServiceIntent = new Intent();
            thisServiceIntent.setClass(context, BingImageOfTheDayArtSource.class);
            thisServiceIntent.setAction(ACTION_REQUESTUPDATE);
            context.startService(thisServiceIntent);
        }

    }
}