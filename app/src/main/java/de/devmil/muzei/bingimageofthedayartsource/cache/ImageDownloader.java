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

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static de.devmil.common.utils.LogUtil.LOGD;

import de.devmil.common.utils.LogUtil;

/**
 * Created by devmil on 03.03.14.
 *
 * Handles the sequential download of a set of Uris pointing to images.
 * Fires an event each time an image has been downloaded
 */
public class ImageDownloader implements ImageDownloadRequest.OnDownloadFinishedListener {

    private static final int MAX_RETRIES = 3;
    private static final String TAG = LogUtil.makeLogTag(ImageDownloader.class);

    private Queue<Uri> _ActiveUris;
    private Context _Context;
    private ImageDownloadRequest _Request;
    private WeakReference<ImageDownloadRequest.OnDownloadFinishedListener> _Listener;

    public ImageDownloader(Context context, ImageDownloadRequest.OnDownloadFinishedListener listener)
    {
        _Context = context;
        _Listener = new WeakReference<ImageDownloadRequest.OnDownloadFinishedListener>(listener);
    }

    public synchronized void download(List<Uri> uris)
    {
        if(_Request != null)
        {
            LOGD(TAG, "cancelling running download to start a new one");
            cancel();
        }
        _ActiveUris = new ArrayDeque<Uri>(uris);
        processNextQueueItem();
    }

    public synchronized void cancel()
    {
        if(_Request != null)
        {
            _Request.cancel(_Context);
            _Request = null;
        }
    }

    private synchronized void processNextQueueItem()
    {
        LOGD(TAG, "Processing next download queue item");
        if(_ActiveUris.isEmpty())
        {
            LOGD(TAG, "no items to download => stop");
            _Request = null;
            return;
        }
        Uri uri = _ActiveUris.peek();
        _Request = new ImageDownloadRequest(this, uri);
        LOGD(TAG, String.format("starting download of %s", uri));
        _Request.download(_Context, uri);
    }

    @Override
    public synchronized void downloadFinished(byte[] content, Object tag)
    {
        Uri uri = (Uri)tag;
        LOGD(TAG, String.format("download finished: %s", uri));
        if(content != null)
        {
            LOGD(TAG, "passing downloaded image to the listener and proceeding");
            ImageDownloadRequest.OnDownloadFinishedListener listener = _Listener.get();
            if(listener != null)
                listener.downloadFinished(content, tag);
            _ActiveUris.poll();
            processNextQueueItem();
        }
        else
        {
            LOGD(TAG, "download didn't finish correctly");
            boolean retry = false;
            if(_Request != null)
            {
                if(_Request.getNumberOfRetries() <= MAX_RETRIES)
                    retry = true;
            }
            if(retry)
            {
                LOGD(TAG, "retrying...");
                _Request.download(_Context, uri);
            }
            else
            {
                LOGD(TAG, "enough retries, proceeding with the next item");
                processNextQueueItem();
            }
        }
    }
}
