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

import android.content.Context;
import android.net.Uri;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.lang.ref.WeakReference;

/**
 * Created by devmil on 24.02.14.
 */
public class ImageDownloadRequest {

    private WeakReference<OnDownloadFinishedListener> _Listener;
    private Object _Tag;
    private int _NumberOfRetries;
    private boolean _Dead = false;
    private RequestQueue _Queue;
    private String _DownloadingUri;

    public ImageDownloadRequest(OnDownloadFinishedListener listener, Object tag)
    {
        _Listener = new WeakReference<OnDownloadFinishedListener>(listener);
        _Tag = tag;
        _NumberOfRetries = 0;
    }

    public int getNumberOfRetries()
    {
        return _NumberOfRetries;
    }


    public synchronized void download(Context context, Uri uri)
    {
        _NumberOfRetries++;
        _Dead = false;
        _DownloadingUri = uri.toString();
        if(_Queue == null)
            _Queue = Volley.newRequestQueue(context);
        Response.Listener<byte[]> listener = new Response.Listener<byte[]>() {
            @Override
            public void onResponse(byte[] response) {
                if(_Dead)
                    return;
                OnDownloadFinishedListener listener = _Listener.get();
                if(listener != null)
                {
                    listener.downloadFinished(response, _Tag);
                }
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(_Dead)
                    return;
                OnDownloadFinishedListener listener = _Listener.get();
                if(listener != null)
                {
                    listener.downloadFinished(null, _Tag);
                }
            }
        };
        FileRequest fileRequest = new FileRequest(
                uri.toString(),
                listener,
                errorListener
        );
        fileRequest.setTag(_DownloadingUri);
        _Queue.add(fileRequest);
    }

    public synchronized void cancel(Context context)
    {
        _Dead = true;
        if(_Queue != null)
            _Queue.cancelAll(_DownloadingUri);
    }

    public static interface OnDownloadFinishedListener
    {
        void downloadFinished(byte[] content, Object tag);
    }
}
