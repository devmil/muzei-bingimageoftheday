package de.devmil.muzei.bingimageoftheday.worker

import android.app.AlarmManager
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.work.*
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import de.devmil.common.utils.LogUtil
import de.devmil.muzei.bingimageoftheday.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class BingImageOfTheDayWorker(
        val context: Context,
        workerParams: WorkerParameters
) : Worker(context, workerParams) {
    companion object {
        private const val TAG = "BingImageIfTheDayWorker"

        private const val SETTINGS_NAME = "BingImageOfTheDayArtSource"

        internal fun enqueueLoad() {
            Log.d(TAG, "Loading enqued")
            val workManager = WorkManager.getInstance()
            workManager.enqueue(OneTimeWorkRequestBuilder<BingImageOfTheDayWorker>()
                    .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .build())
        }

        private val lockObject = Object()
        private var lastArtworkUpdate: Calendar? = null
    }

    private fun getSharedPreferences(): SharedPreferences {
        return applicationContext.getSharedPreferences("muzeiartsource_$SETTINGS_NAME", 0)
    }

    private fun getImageTitle(imageDate: Date): String {
        val df = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        return "Bing: " + df.format(imageDate.time)
    }

    override fun doWork(): Result {
        LogUtil.LOGD(TAG, "Request: Loading new Bing images")

        synchronized(lockObject) {

            val now = Calendar.getInstance()

            val settings = Settings(applicationContext, getSharedPreferences())
            val isPortrait = settings.isOrientationPortrait
            val isCurrentArtworkPortrait = settings.isCurrentOrientationPortrait

            val market = settings.bingMarket
            val currentArtworkMarket = settings.currentBingMarket

            //when there are settings changes then the request for new images
            //gets overwritten
            val requestOverride = market != currentArtworkMarket
                    || isPortrait != isCurrentArtworkPortrait

            //Check if the last update is more than 2 minutes away.
            //If not then return early (exception: when settings changed that lead to an update)
            lastArtworkUpdate?.let {
                val millisDiff = now.timeInMillis - it.timeInMillis
                val minDiff = 1000 /* seconds */ * 60 /* minutes */ * 2
                if (!requestOverride && millisDiff < minDiff) {
                    LogUtil.LOGD(TAG, "Last update was less than 2 minutes ago => ignoring")
                    return Result.success()
                }
            }
            lastArtworkUpdate = now

            //Default = request the image list from Bing
            var requestNewImages = true
            val lastArtwork = ProviderContract.getProviderClient<BingImageOfTheDayArtProvider>(applicationContext)
                    .lastAddedArtwork
            if (lastArtwork != null) {
                LogUtil.LOGD(TAG, "Found last artwork")
                val timeInMillis = lastArtwork.metadata?.toLongOrNull()
                if (timeInMillis != null) {
                    val token = getToken(Date(timeInMillis), market, isPortrait)
                    LogUtil.LOGD(TAG, "Metadata is correct")
                    if (token == lastArtwork.token && isNewestBingImage(Date(timeInMillis))) {
                        //when the current artwork matches the settings and is the newest, then don't load that Bing list
                        LogUtil.LOGD(TAG, "We have the latest image => do nothing")
                        requestNewImages = false
                        requestNextImageUpdate(Date(timeInMillis))
                    }
                }
            }

            if (requestOverride) {
                LogUtil.LOGD(TAG, "Settings changed! reloading anyways!")
                requestNewImages = true
            }
            if (!requestNewImages) {
                return Result.success()
            }

            LogUtil.LOGD(TAG, "Reloading Bing images")

            val retriever = BingImageOfTheDayMetadataRetriever(
                    market,
                    BingImageDimension.UHD,
                    isPortrait
            )

            val photosMetadata = try {
                retriever.bingImageOfTheDayMetadata ?: listOf()
            } catch (e: IOException) {
                Log.w(TAG, "Error reading Bing response", e)
                return Result.retry()
            }

            if (photosMetadata.isEmpty()) {
                Log.w(TAG, "No photos returned from Bing API.")
                return Result.failure()
            }

            photosMetadata.asSequence().map { metadata ->
                Artwork(
                        token = getToken(metadata.startDate, market, isPortrait),
                        title = metadata.startDate?.let { getImageTitle(it) } ?: "",
                        byline = metadata.copyright ?: "",
                        persistentUri = metadata.uri,
                        webUri = metadata.uri,
                        metadata = metadata.startDate?.time.toString()
                )
            }.sortedByDescending { aw ->
                aw.metadata?.toLongOrNull() ?: 0
            }.firstOrNull()
                    ?.let { artwork ->
                        Log.d(TAG, "Got artworks. Selected this one: ${artwork.title} valid on: ${Date(artwork.metadata!!.toLong())}")
                        requestNextImageUpdate(Date(artwork.metadata!!.toLong()))
                        setArtwork(artwork)
                        settings.isCurrentOrientationPortrait = isPortrait
                        settings.currentBingMarket = market
                        if (settings.isStoreImages) {
                            downloadImage(artwork, isPortrait)
                        }
                    }
            return Result.success()
        }
    }

    private fun downloadImage(artwork: Artwork, isPortrait: Boolean) {
        val downloadManager = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = artwork.webUri.toString()
        val downloadUriAlternative = artwork.webUri.toString().replace(BingImageDimension.UHD.getStringRepresentation(isPortrait), BingImageDimension.UHD.getStringRepresentation(!isPortrait))

        val fileNameIndex = max(downloadUri.lastIndexOf("/"), downloadUri.lastIndexOf("="))
        val fileNameAlternativeIndex = max(downloadUriAlternative.lastIndexOf("/"), downloadUriAlternative.lastIndexOf("="))

        val fileName = artwork.metadata + "_" + downloadUri.substring(fileNameIndex + 1)
        val fileNameAlternative = artwork.metadata + "_" + downloadUriAlternative.substring(fileNameAlternativeIndex + 1)

        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + File.separator + "Bing Image of the Day" + File.separator + fileName
        val filePathAlternative = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + File.separator + "Bing Image of the Day" + File.separator + fileNameAlternative

        LogUtil.LOGD(TAG, "Downloading: $fileName to $filePath")

        if (!File(filePath).exists()) {
            val requestCurrent = DownloadManager.Request(Uri.parse(downloadUri)).apply {
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                        .setAllowedOverRoaming(false)
                        .setTitle("Downloading current Bing Image ($fileName)")
                        .setDescription("")
                        .setMimeType("image/jpeg")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
                        .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_PICTURES,
                                File.separator + "Bing Image of the Day" + File.separator + fileName
                        )
            }

            downloadManager.enqueue(requestCurrent)
        }

        if (!File(filePathAlternative).exists()) {
            val requestAlternative = DownloadManager.Request(Uri.parse(downloadUriAlternative)).apply {
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                        .setAllowedOverRoaming(false)
                        .setTitle("Downloading alternative Bing Image ($fileNameAlternative)")
                        .setDescription("")
                        .setMimeType("image/jpeg")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_PICTURES,
                                File.separator + "Bing Image of the Day" + File.separator + fileNameAlternative
                        )
            }

            downloadManager.enqueue(requestAlternative)
        }
    }

    private fun getToken(startDate: Date?, market: BingMarket, isPortrait: Boolean): String {
        val result = "$startDate-$market-${if (isPortrait) "portrait" else "landscape"}"
        LogUtil.LOGD(TAG, "Token: $result")
        return result
    }

    private fun isNewestBingImage(newestBingImageDate: Date): Boolean {
        val now = Calendar.getInstance().time
        val nextBingImageDate = getNextBingImageDate(newestBingImageDate)

        return now < nextBingImageDate
    }

    private fun getNextBingImageDate(newestBingImageDate: Date): Date {
        val nextBingImageDate = Calendar.getInstance()
        nextBingImageDate.timeInMillis = newestBingImageDate.time
        nextBingImageDate.add(Calendar.DAY_OF_YEAR, 1)
        return nextBingImageDate.time
    }

    private fun setArtwork(artwork: Artwork) {
        LogUtil.LOGD(TAG, "Setting artwork: ${artwork.metadata?.toLongOrNull()?.let { Date(it) }}, ${artwork.title}")
        ProviderContract.getProviderClient<BingImageOfTheDayArtProvider>(applicationContext)
                .setArtwork(artwork)
    }

    private fun requestNextImageUpdate(currentImageDate: Date): Calendar {
        val nextBingImageDate = getNextBingImageDate(currentImageDate)
        val nextUpdate = Calendar.getInstance()
        nextUpdate.time = nextBingImageDate
        nextUpdate.add(Calendar.MINUTE, 1)

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US)

        LogUtil.LOGD(TAG, String.format("next update: %s", sdf.format(nextUpdate.time)))

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        val updateIntent = Intent(context, UpdateReceiver::class.java)
        val pendingUpdateIntent = PendingIntent.getBroadcast(context, 1, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        alarmManager?.set(AlarmManager.RTC_WAKEUP, nextUpdate.timeInMillis, pendingUpdateIntent)

        return nextUpdate
    }
}