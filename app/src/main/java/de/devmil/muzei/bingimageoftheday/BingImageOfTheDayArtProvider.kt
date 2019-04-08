package de.devmil.muzei.bingimageoftheday

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.apps.muzei.api.UserCommand
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
        private val COMMAND_ID_OPEN = 3

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

    override fun getCommands(artwork: Artwork): MutableList<UserCommand> {
        val result = super.getCommands(artwork)

        result.add(UserCommand(COMMAND_ID_SHARE, context?.getString(R.string.command_share_title) ?: "Share"))
        result.add(UserCommand(COMMAND_ID_OPEN, context?.getString(R.string.command_open_title) ?: "Open"))

        return result;
    }

    override fun onCommand(artwork: Artwork, id: Int) {
        super.onCommand(artwork, id)
        if(id == COMMAND_ID_SHARE) {
            shareCurrentImage()
        } else if(id == COMMAND_ID_OPEN) {
            openCurrentImage()
        }
    }

    override fun onLoadRequested(initial: Boolean) {
        isActive = true
        BingImageOfTheDayWorker.enqueueLoad()
    }

    override fun openFile(artwork: Artwork): InputStream {
        Log.d(TAG, "Loading artwork: ${artwork.title} (${artwork.persistentUri})")
        return super.openFile(artwork);
    }

    private fun shareCurrentImage() {
        LogUtil.LOGD(TAG, "got share request")
        lastAddedArtwork?.let {
            var shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            val shareMessage = context?.getString(R.string.command_share_message, it.byline)
            shareIntent.putExtra(Intent.EXTRA_TEXT, "$shareMessage - ${it.webUri.toString()}")
            shareIntent.type = "text/plain"
            //shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            LogUtil.LOGD(TAG, "Sharing ${it.webUri}")

            shareIntent = Intent.createChooser(shareIntent, context?.getString(R.string.command_share_title) ?: "")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context?.startActivity(shareIntent)
        }
    }

    private fun openCurrentImage() {
        LogUtil.LOGD(TAG, "got open request")
        lastAddedArtwork?.let {
            var openIntent = Intent(Intent.ACTION_VIEW)

            LogUtil.LOGD(TAG, "Opening ${it.webUri}")

            openIntent.data = it.webUri
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context?.startActivity(openIntent)
        }
    }
}