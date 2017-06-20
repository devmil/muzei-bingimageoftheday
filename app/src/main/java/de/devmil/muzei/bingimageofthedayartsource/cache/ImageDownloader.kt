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
package de.devmil.muzei.bingimageofthedayartsource.cache

import android.content.Context
import android.net.Uri

import java.lang.ref.WeakReference
import java.util.ArrayDeque
import java.util.Queue

import de.devmil.common.utils.LogUtil

/**
 * Created by devmil on 03.03.14.

 * Handles the sequential download of a set of Uris pointing to images.
 * Fires an event each time an image has been downloaded
 */
class ImageDownloader(private val _Context: Context, listener: ImageDownloadRequest.OnDownloadFinishedListener) : ImageDownloadRequest.OnDownloadFinishedListener {

    private var _ActiveUris: Queue<Uri>? = null
    private var _Request: ImageDownloadRequest? = null
    private val _Listener: WeakReference<ImageDownloadRequest.OnDownloadFinishedListener> = WeakReference(listener)

    @Synchronized fun download(uris: List<Uri>) {
        if (_Request != null) {
            LogUtil.LOGD(TAG, "cancelling running download to start a new one")
            cancel()
        }
        _ActiveUris = ArrayDeque(uris)
        processNextQueueItem()
    }

    @Synchronized fun cancel() {
        if (_Request != null) {
            _Request!!.cancel()
            _Request = null
        }
    }

    @Synchronized private fun processNextQueueItem() {
        LogUtil.LOGD(TAG, "Processing next download queue item")
        if (_ActiveUris!!.isEmpty()) {
            LogUtil.LOGD(TAG, "no items to download => stop")
            _Request = null
            return
        }
        val uri = _ActiveUris!!.peek()
        _Request = ImageDownloadRequest(this, uri)
        LogUtil.LOGD(TAG, String.format("starting download of %s", uri))
        _Request!!.download(_Context, uri)
    }

    @Synchronized override fun downloadFinished(content: ByteArray?, tag: Any) {
        val uri = tag as Uri
        LogUtil.LOGD(TAG, String.format("download finished: %s", uri))
        if (content != null) {
            LogUtil.LOGD(TAG, "passing downloaded image to the listener and proceeding")
            val listener = _Listener.get()
            listener?.downloadFinished(content, tag)
            _ActiveUris!!.poll()
            processNextQueueItem()
        } else {
            LogUtil.LOGD(TAG, "download didn't finish correctly")
            var retry = false
            if (_Request != null) {
                if (_Request!!.numberOfRetries <= MAX_RETRIES)
                    retry = true
            }
            if (retry) {
                LogUtil.LOGD(TAG, "retrying...")
                _Request!!.download(_Context, uri)
            } else {
                LogUtil.LOGD(TAG, "enough retries, proceeding with the next item")
                processNextQueueItem()
            }
        }
    }

    companion object {

        private val MAX_RETRIES = 3
        private val TAG = LogUtil.makeLogTag(ImageDownloader::class.java)
    }
}
