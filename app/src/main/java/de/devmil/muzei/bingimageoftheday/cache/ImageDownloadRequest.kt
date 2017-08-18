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
package de.devmil.muzei.bingimageoftheday.cache

import android.content.Context
import android.net.Uri
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley

/**
* Created by devmil on 24.02.14.
*/
class ImageDownloadRequest(private val onFinished: (content: ByteArray?, tag: Any) -> Unit, private val _Tag: Any) {

    var numberOfRetries: Int = 0
        private set
    private var dead = false
    private var queue: RequestQueue? = null
    private var downloadingUri: String? = null

    init {
        numberOfRetries = 0
    }


    @Synchronized fun download(context: Context, uri: Uri) {
        numberOfRetries++
        dead = false
        downloadingUri = uri.toString()
        if (queue == null)
            queue = Volley.newRequestQueue(context)
        val listener = Response.Listener<ByteArray> { response ->
            if (dead)
                return@Listener
            onFinished(response, _Tag)
        }
        val errorListener = Response.ErrorListener {
            if (dead)
                return@ErrorListener
            onFinished(null, _Tag)
        }
        val fileRequest = FileRequest(
                uri.toString(),
                listener,
                errorListener
        )
        fileRequest.tag = downloadingUri
        queue?.add(fileRequest)
    }

    @Synchronized fun cancel() {
        dead = true
        queue?.cancelAll(downloadingUri)
    }
}
