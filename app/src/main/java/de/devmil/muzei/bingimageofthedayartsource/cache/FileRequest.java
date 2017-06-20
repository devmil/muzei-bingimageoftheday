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
package de.devmil.muzei.bingimageofthedayartsource.cache;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

/**
 * Simple FileRequest that handles the download of a file
 *
 * Created by devmil on 06.03.14.
 */
public class FileRequest extends Request<byte[]> {

    /** Socket timeout in milliseconds for image file requests */
    private static final int FILE_TIMEOUT_MS = 1000;

    /** Default number of retries for image file requests */
    private static final int FILE_MAX_RETRIES = 2;

    /** Default backoff multiplier for image file requests */
    private static final float FILE_BACKOFF_MULT = 2f;

    private Response.Listener<byte[]> _Listener;

    public FileRequest(String url, Response.Listener<byte[]> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        setRetryPolicy(
                new DefaultRetryPolicy(FILE_TIMEOUT_MS, FILE_MAX_RETRIES, FILE_BACKOFF_MULT));
        _Listener = listener;
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(byte[] response) {
        _Listener.onResponse(response);
    }
}
