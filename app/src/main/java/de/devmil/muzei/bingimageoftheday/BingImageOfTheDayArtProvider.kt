package de.devmil.muzei.bingimageoftheday

import android.app.PendingIntent
import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.RemoteActionCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import de.devmil.common.utils.LogUtil
import de.devmil.muzei.bingimageoftheday.events.RequestMarketSettingChangedEvent
import de.devmil.muzei.bingimageoftheday.events.RequestPortraitSettingChangedEvent
import de.devmil.muzei.bingimageoftheday.worker.BingImageOfTheDayWorker
import de.greenrobot.event.EventBus
import java.io.InputStream

class BingImageOfTheDayArtProvider : MuzeiArtProvider() {

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
            doUpdate()
        }

    }

    companion object {
        private const val TAG = "BingImageOfTheDayArtPro"

        private val COMMAND_ID_SHARE = 2

        private var CatcherInstance: BingImageOfTheDayArtProvider.EventCatcher? = null

        init {
            //instantiate the EventCatcher when BingImageOfTheDayArtSource is loaded
            CatcherInstance = BingImageOfTheDayArtProvider.EventCatcher()
        }

        private var _isActive: Boolean? = null
        var isActive: Boolean?
            get() = _isActive
            private set(value) {
                _isActive = value
            }

        fun doUpdate() {
            BingImageOfTheDayWorker.enqueueLoad()
        }
    }

    /* kept for backward compatibility with Muzei 3.3 */
    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun getCommands(artwork: Artwork) = listOf(
            com.google.android.apps.muzei.api.UserCommand(COMMAND_ID_SHARE,
                    context?.getString(R.string.command_share_title) ?: "Share"))

    /* kept for backward compatibility with Muzei 3.3 */
    @Suppress("OverridingDeprecatedMember")
    override fun onCommand(artwork: Artwork, id: Int) {
        val context = context ?: return
        if(id == COMMAND_ID_SHARE) {
            shareCurrentImage(context, artwork)
        }
    }

    override fun onLoadRequested(initial: Boolean) {
        isActive = true
        BingImageOfTheDayWorker.enqueueLoad()
    }

    override fun openFile(artwork: Artwork): InputStream {
        Log.d(TAG, "Loading artwork: ${artwork.title} (${artwork.persistentUri})")
        return super.openFile(artwork)
    }

    private fun createShareIntent(context: Context, artwork: Artwork): Intent {
        LogUtil.LOGD(TAG, "got share request")
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            val shareMessage = context.getString(R.string.command_share_message, artwork.byline)
            putExtra(Intent.EXTRA_TEXT, "$shareMessage - ${artwork.webUri.toString()}")
            val contentUri = Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority("de.devmil.muzei.bingimageoftheday.provider.BingImages")
                    .build()
            val uri = ContentUris.withAppendedId(contentUri, artwork.id)
            putExtra(Intent.EXTRA_TITLE, artwork.byline)
            putExtra(Intent.EXTRA_STREAM, uri)
            type = context.contentResolver.getType(uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver,
                    artwork.byline, uri)
        }
        return Intent.createChooser(shareIntent,
                context.getString(R.string.command_share_title))
    }

    private fun shareCurrentImage(context: Context, artwork: Artwork) {
        LogUtil.LOGD(TAG, "Sharing ${artwork.webUri}")

        val shareIntent = createShareIntent(context, artwork)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(shareIntent)
    }

    /* Used by Muzei 3.4+ */
    override fun getCommandActions(artwork: Artwork): List<RemoteActionCompat> {
        val context = context ?: return super.getCommandActions(artwork)
        return listOf(
                RemoteActionCompat(
                        IconCompat.createWithResource(context, R.drawable.ic_share),
                        context.getString(R.string.command_share_title),
                        context.getString(R.string.command_share_title),
                        PendingIntent.getActivity(context, artwork.id.toInt(),
                                createShareIntent(context, artwork),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE))
        )
    }

    override fun getArtworkInfo(artwork: Artwork): PendingIntent? {
        LogUtil.LOGD(TAG, "Opening ${artwork.webUri}")
        return super.getArtworkInfo(artwork)
    }
}