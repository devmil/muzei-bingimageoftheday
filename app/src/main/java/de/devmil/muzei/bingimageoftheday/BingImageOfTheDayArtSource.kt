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
package de.devmil.muzei.bingimageoftheday

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.IBinder
import com.google.android.apps.muzei.api.Artwork
import com.google.android.apps.muzei.api.MuzeiArtSource
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource
import com.google.android.apps.muzei.api.UserCommand
import de.devmil.common.utils.LogUtil
import de.devmil.muzei.bingimageoftheday.cache.BingImageCache
import de.devmil.muzei.bingimageoftheday.events.RequestMarketSettingChangedEvent
import de.devmil.muzei.bingimageoftheday.events.RequestPortraitSettingChangedEvent
import de.greenrobot.event.EventBus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Muzei Art source for Bing images of the day.
 * This class is the plugin entry point for Muzei
 */
class BingImageOfTheDayArtSource : RemoteMuzeiArtSource("de.devmil.muzei.Bing") {

    private val COMMAND_ID_SHARE = MuzeiArtSource.MAX_CUSTOM_COMMAND_ID - 1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        if (intent != null && (ACTION_REQUESTUPDATE == intent.action || ACTION_ENSUREINITIALIZED == intent.action))
            refreshImage()
        return result
    }

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        setUserCommands(UserCommand(MuzeiArtSource.BUILTIN_COMMAND_ID_NEXT_ARTWORK),
                UserCommand(COMMAND_ID_SHARE, getString(R.string.command_title_share)))
    }

    override fun onCustomCommand(id: Int) {
        super.onCustomCommand(id)
        val settings = Settings(this, MuzeiArtSource.getSharedPreferences(applicationContext, SOURCE_NAME))

        if (_Cache == null)
            _Cache = BingImageCache(applicationContext)

        if(id == COMMAND_ID_SHARE) {
            shareCurrentImage(settings);
        }
    }

    @Throws(RemoteMuzeiArtSource.RetryException::class)
    override fun onTryUpdate(updateReason: Int) {
        val settings = Settings(this, MuzeiArtSource.getSharedPreferences(applicationContext, SOURCE_NAME))

        if (_Cache == null)
            _Cache = BingImageCache(applicationContext)

        LogUtil.LOGD(TAG, "Update try received")

        val market = settings.bingMarket
        val currentMarket = settings.currentBingMarket
        val marketChanged = market != currentMarket
        LogUtil.LOGD(TAG, String.format("Market changed: %b", marketChanged))

        val requestPortraitMode = settings.isOrientationPortrait
        val currentRequestPortraitMode = settings.isCurrentOrientationPortrait
        LogUtil.LOGD(TAG, String.format("request portrait mode: %b", requestPortraitMode))
        val requestPortraitModeChanged = requestPortraitMode != currentRequestPortraitMode

        //check if a "Bing day" has passed
        var newestImageChanged = false
        if (_Cache!!.metadata != null && _Cache!!.metadata!!.validThru!!.before(Calendar.getInstance()))
            newestImageChanged = true
        LogUtil.LOGD(TAG, String.format("newest image changed: %b", newestImageChanged))

        //when anything changed the cache has to be reloaded
        val reloadCache = marketChanged || requestPortraitModeChanged || _Cache!!.metadata == null || newestImageChanged

        if (reloadCache) {
            LogUtil.LOGD(TAG, "we have to reload the cache metadata")
            val retriever = BingImageOfTheDayMetadataRetriever(market, BingImageDimension.HD, requestPortraitMode)
            val metadata = retriever.bingImageOfTheDayMetadata

            //if there is a result
            if (metadata != null && metadata.isNotEmpty()) {
                //build the cache metadata
                LogUtil.LOGD(TAG, "metadata received")
                val entries = ArrayList<BingImageCache.CacheEntry>()
                for (i in metadata.indices) {
                    entries.add(
                            BingImageCache.CacheEntry(
                                    metadata[i].uri!!,
                                    getImageTitle(metadata[i].startDate!!),
                                    metadata[i].copyrightOrEmpty))
                }
                LogUtil.LOGD(TAG, "setting the metadata to the cache")
                _Cache!!.metadata = BingImageCache.CacheMetadata(entries.toTypedArray(), getNextUpdate(metadata[0].startDate!!))
            }
        }
        _Cache!!.ensureMissingImages()

        val lastNumber = settings.currentImageNumber

        val userNext = updateReason == MuzeiArtSource.UPDATE_REASON_USER_NEXT
        val hasBeenScheduled = updateReason == MuzeiArtSource.UPDATE_REASON_SCHEDULED

        LogUtil.LOGD(TAG, String.format("userNext: %b", userNext))
        LogUtil.LOGD(TAG, String.format("scheduled: %b", hasBeenScheduled))

        if (_Cache!!.metadata != null) {
            LogUtil.LOGD(TAG, "Cache has metadata => proceeding")

            var imageNumberToUse = lastNumber
            if (userNext) {
                imageNumberToUse++
                //this way next would mean current because there is a new image
                if (newestImageChanged)
                    imageNumberToUse++
            }

            //this is the moment we change the current Bing of the day image => move to the current day
            //TODO: preference to decide if the currently active image should stay active as long as possible or if the new image should be activated (as it currently happens)
            if (hasBeenScheduled && newestImageChanged || reloadCache) {
                LogUtil.LOGD(TAG, "setting the current image to today")
                imageNumberToUse = 0
            }

            //overflow
            if (imageNumberToUse >= _Cache!!.metadata!!.entries!!.size)
                imageNumberToUse = 0

            //calculate up to what point of time this image is (or has been) active on Bing
            val imageOnBingPresentThru = Calendar.getInstance()
            imageOnBingPresentThru.timeInMillis = _Cache!!.metadata!!.validThru!!.timeInMillis
            imageOnBingPresentThru.add(Calendar.DAY_OF_MONTH, -1)
            imageOnBingPresentThru.add(Calendar.MINUTE, -1)
            imageOnBingPresentThru.add(Calendar.DAY_OF_MONTH, -1 * imageNumberToUse)

            val imgToken = createToken(imageOnBingPresentThru.time, market, requestPortraitMode)
            var currentToken = ""
            if (currentArtwork != null)
                currentToken = currentArtwork.token

            settings.currentImageNumber = imageNumberToUse
            settings.currentBingMarket = market
            settings.isCurrentOrientationPortrait = requestPortraitMode

            var newImage = false

            if (imgToken != currentToken) {
                LogUtil.LOGD(TAG, "image changed => notifying Muzei")
                newImage = true
                val entry = _Cache!!.metadata!!.entries!![imageNumberToUse]

                //this is the web Uri of the image
                var uri: Uri = entry.uri!!

                //when our cache already has the image => get it from there
                if (_Cache!!.hasImage(imageNumberToUse))
                    uri = BingImageContentProvider.getContentUri(_Cache!!.getFileName(imageNumberToUse)!!, true)

                //tell Muzei to use the new image
                publishArtwork(
                        Artwork.Builder()
                                .title(entry.description)
                                .byline(entry.copyright)
                                .imageUri(uri)
                                .viewIntent(Intent(Intent.ACTION_VIEW, uri))
                                .token(imgToken)
                                .build())
            }
            //if this update has been scheduled but didn't result in a new image => retry in FAST_RETRY_MINUTES minutes
            val scheduleFastRetry = !newImage && hasBeenScheduled
            if (scheduleFastRetry)
                requestFastRetryUpdate()
            else
                requestNextImageUpdate(imageOnBingPresentThru.time)
        } else {
            throw RemoteMuzeiArtSource.RetryException()
        }
    }

    private fun shareCurrentImage(settings: Settings) {
        LogUtil.LOGD(TAG, String.format("got share request, current image: $settings.currentImageNumber"))
        if (_Cache!!.hasImage(settings.currentImageNumber)) {
            val uri = BingImageContentProvider.getContentUri(_Cache!!.getFileName(settings.currentImageNumber)!!, false)

            LogUtil.LOGD(TAG, String.format("Share URI: $uri"))

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "image/jpeg"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(shareIntent)
        }
    }

    private fun createToken(date: Date, market: BingMarket, portrait: Boolean): String {
        val df = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        return String.format("%s|%s|%b", market.marketCode, df.format(date), portrait)
    }

    private fun getImageTitle(imageDate: Date): String {
        val df = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        return "Bing: " + df.format(imageDate.time)
    }

    /**
     * this method schedules a normal update request
     * @param imageStartDate the valid thru date of one of the images. Only the time of the day portion gets used to calculate the next update
     * *
     * @return
     */
    private fun requestNextImageUpdate(imageStartDate: Date): Calendar {
        val nextUpdate = getNextUpdate(imageStartDate)

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US)

        LogUtil.LOGD(TAG, String.format("next update: %s", sdf.format(nextUpdate.time)))
        scheduleUpdate(nextUpdate.timeInMillis)

        return nextUpdate
    }

    /**
     * This method calculates the next update point of time based on the
     * time of the day information provided in the "newestImageStartDate" parameter
     * @param newestImageStartDate
     * *
     * @return
     */
    private fun getNextUpdate(newestImageStartDate: Date): Calendar {
        val nextUpdate = Calendar.getInstance()

        val imageStart = Calendar.getInstance()
        imageStart.time = newestImageStartDate

        var addDay = true

        if (nextUpdate.get(Calendar.HOUR_OF_DAY) < imageStart.get(Calendar.HOUR_OF_DAY))
            addDay = false
        else if (nextUpdate.get(Calendar.HOUR_OF_DAY) == imageStart.get(Calendar.HOUR_OF_DAY)) {
            if (nextUpdate.get(Calendar.MINUTE) < imageStart.get(Calendar.MINUTE))
                addDay = false
            else if (nextUpdate.get(Calendar.MINUTE) == imageStart.get(Calendar.MINUTE)) {
                if (nextUpdate.get(Calendar.SECOND) < imageStart.get(Calendar.SECOND))
                    addDay = false
            }
        }

        nextUpdate.set(Calendar.HOUR_OF_DAY, imageStart.get(Calendar.HOUR_OF_DAY))
        nextUpdate.set(Calendar.MINUTE, imageStart.get(Calendar.MINUTE))
        nextUpdate.set(Calendar.SECOND, imageStart.get(Calendar.SECOND))
        nextUpdate.set(Calendar.MILLISECOND, imageStart.get(Calendar.MILLISECOND))

        nextUpdate.add(Calendar.MINUTE, 1) //wait 1 minute extra

        if (addDay)
            nextUpdate.add(Calendar.DAY_OF_MONTH, 1)

        return nextUpdate
    }

    /**
     * Schedules a fast retry update (one hour in the future)
     * @return
     */
    private fun requestFastRetryUpdate(): Calendar {
        val nextUpdate = Calendar.getInstance()
        nextUpdate.add(Calendar.MINUTE, FAST_RETRY_MINUTES)

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US)
        LogUtil.LOGD(TAG, String.format("next update: %s", sdf.format(nextUpdate.time)))

        scheduleUpdate(nextUpdate.timeInMillis)

        return nextUpdate
    }

    private fun refreshImage() {
        val nextUpdate = Calendar.getInstance()
        nextUpdate.add(Calendar.MILLISECOND, 500)

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US)
        LogUtil.LOGD(TAG, String.format("next update: %s", sdf.format(nextUpdate.time)))
        //this one doesn't get stored so that it isn't a daily schedule (that would reset the image number)
        scheduleUpdate(nextUpdate.timeInMillis)
    }

    /**
     * This class is used to get EventBus events even if there is no instance of BingImageOfTheDayArtSource
     */
    class EventCatcher {
        init {
            EventBus.getDefault().register(this)
        }

        fun onEventBackgroundThread(e: RequestPortraitSettingChangedEvent) {
            requestUpdate(e.context)
        }

        fun onEventBackgroundThread(e: RequestMarketSettingChangedEvent) {
            requestUpdate(e.context)
        }

        private fun requestUpdate(context: Context) {
            //this transition (source thread -> here -> Service intent) is needed because the service doesn't have a context
            //when this event is fired from the settings UI
            val thisServiceIntent = Intent()
            thisServiceIntent.setClass(context, BingImageOfTheDayArtSource::class.java)
            thisServiceIntent.action = ACTION_REQUESTUPDATE
            context.startService(thisServiceIntent)
        }

    }

    companion object {

        private var _CatcherInstance: EventCatcher? = null

        init {
            //instantiate the EventCatcher when BingImageOfTheDayArtSource is loaded
            _CatcherInstance = EventCatcher()
        }

        private val TAG = LogUtil.makeLogTag(BingImageOfTheDayArtSource::class.java)

        private val SOURCE_NAME = "BingImageOfTheDayArtSource"

        private val ACTION_REQUESTUPDATE = "de.devmil.muzei.bingimageoftheday.ACTION_REQUESTUPDATE"
        private val ACTION_ENSUREINITIALIZED = "de.devmil.muzei.bingimageoftheday.ACTION_ENSURE_INITIALIZED"

        private val FAST_RETRY_MINUTES = 15

        //this is static so that the asynchronous download keeps running even if this service instance gets destroyed
        private var _Cache: BingImageCache? = null

        fun getSharedPreferences(context: Context): SharedPreferences {
            return MuzeiArtSource.getSharedPreferences(context, SOURCE_NAME)
        }

        fun ensureInitialized(context: Context) {
            val thisServiceIntent = Intent()
            thisServiceIntent.setClass(context, BingImageOfTheDayArtSource::class.java)
            thisServiceIntent.action = ACTION_ENSUREINITIALIZED
            context.startService(thisServiceIntent)
        }
    }
}