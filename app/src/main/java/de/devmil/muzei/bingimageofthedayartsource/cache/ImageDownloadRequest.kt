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
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import java.lang.ref.WeakReference

/**
* Created by devmil on 24.02.14.
*/
class ImageDownloadRequest(listener: ImageDownloadRequest.OnDownloadFinishedListener, private val _Tag: Any) {

    private val _Listener: WeakReference<OnDownloadFinishedListener> = WeakReference(listener)
    var numberOfRetries: Int = 0
        private set
    private var _Dead = false
    private var _Queue: RequestQueue? = null
    private var _DownloadingUri: String? = null

    init {
        numberOfRetries = 0
    }


    @Synchronized fun download(context: Context, uri: Uri) {
        numberOfRetries++
        _Dead = false
        _DownloadingUri = uri.toString()
        if (_Queue == null)
            _Queue = Volley.newRequestQueue(context)
        val listener = Response.Listener<ByteArray> { response ->
            if (_Dead)
                return@Listener
            val listener = _Listener.get()
            listener?.downloadFinished(response, _Tag)
        }
        val errorListener = Response.ErrorListener {
            if (_Dead)
                return@ErrorListener
            val rawListener = _Listener.get()
            rawListener?.downloadFinished(null, _Tag)
        }
        val fileRequest = FileRequest(
                uri.toString(),
                listener,
                errorListener
        )
        fileRequest.tag = _DownloadingUri
        _Queue!!.add(fileRequest)
    }

    @Synchronized fun cancel() {
        _Dead = true
        if (_Queue != null)
            _Queue!!.cancelAll(_DownloadingUri!!)
    }

    interface OnDownloadFinishedListener {
        fun downloadFinished(content: ByteArray?, tag: Any)
    }
}
