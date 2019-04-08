package de.devmil.muzei.bingimageoftheday

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.apps.muzei.api.UserCommand
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import de.devmil.common.utils.LogUtil
import de.devmil.muzei.bingimageoftheday.events.RequestMarketSettingChangedEvent
import de.devmil.muzei.bingimageoftheday.events.RequestPortraitSettingChangedEvent
import de.devmil.muzei.bingimageoftheday.worker.BingImageOfTheDayWorker
import de.greenrobot.event.EventBus
import java.io.InputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


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

    @TargetApi(26)
    fun getLocalBitmapUri(bmp: Bitmap?, context: Context?): Uri? {
        var bmpUri: Uri? = null
        bmp?.let { bitmap ->
            try {
                context?.let { ctx ->
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-kkmmss"))
                    val imagePath = File(context.cacheDir, "images")
                    imagePath.mkdirs()
                    val outputFile = File(imagePath, "output-$timestamp.png")
                    val out = FileOutputStream(outputFile)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                    out.close()
                    bmpUri = FileProvider.getUriForFile(context, "de.devmil.muzei.bingimageoftheday.ImageFileProvider", outputFile)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Error downloading the image to share", Toast.LENGTH_LONG).show()
            }
        }

        return bmpUri
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

            //For API > 26: download image and attach that
            if(Build.VERSION.SDK_INT >= 26) {
                val uiHandler = Handler(Looper.getMainLooper())
                uiHandler.post {
                    Picasso.with(context).load(it.webUri).into(object : Target {
                        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                            LogUtil.LOGD(TAG, "Downloading ${it.webUri}")
                        }

                        override fun onBitmapFailed(errorDrawable: Drawable?) {
                            Toast.makeText(context, "Error downloading the image to share", Toast.LENGTH_LONG).show()
                        }

                        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                            shareIntent.putExtra(Intent.EXTRA_STREAM, getLocalBitmapUri(bitmap, context))
                            shareIntent.type = "image/png"

                            executeIntentSharing(shareIntent)
                        }
                    })
                }
            } else { // SDK < 26 => directly share (the URL)
                executeIntentSharing(shareIntent)
            }
        }
    }

    private fun executeIntentSharing(intent: Intent) {
        var shareIntent = Intent.createChooser(intent, context?.getString(R.string.command_share_title) ?: "")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context?.startActivity(shareIntent)
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