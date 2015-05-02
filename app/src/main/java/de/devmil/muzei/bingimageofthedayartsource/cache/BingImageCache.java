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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.devmil.muzei.bingimageofthedayartsource.utils.FileUtils;
import de.devmil.common.utils.LogUtil;

import static de.devmil.common.utils.LogUtil.LOGD;

/**
 * Created by devmil on 25.02.14.
 *
 * This class represents the image cache. It controls the (serialized) metadata
 * and triggers the cleanup / image download depending on the metadata it gets
 */
public class BingImageCache implements ImageDownloadRequest.OnDownloadFinishedListener {

    public static class CacheEntry
    {
        private Uri _Uri;
        private String _Description;
        private String _Copyright;

        private CacheEntry()
        {

        }

        public CacheEntry(Uri uri, String description, String copyright)
        {
            _Uri = uri;
            _Description = description;
            _Copyright = copyright;
        }

        public Uri getUri()
        {
            return _Uri;
        }

        public String getDescription()
        {
            return _Description;
        }

        public String getCopyright()
        {
            return _Copyright;
        }

        public JSONObject toJSONObject() throws JSONException {
            JSONObject result = new JSONObject();
            result.put("uri", _Uri.toString());
            result.put("desc", _Description);
            result.put("copyright", _Copyright);
            return result;
        }

        private void readFromJSONObject(JSONObject json) throws JSONException {
            _Uri = Uri.parse(json.getString("uri"));
            _Description = json.getString("desc");
            _Copyright = json.getString("copyright");
        }

        public static CacheEntry fromJSONObject(JSONObject json) throws JSONException {
            CacheEntry entry = new CacheEntry();
            entry.readFromJSONObject(json);
            return entry;
        }
    }
    public static class CacheMetadata
    {
        private CacheMetadata()
        {
        }

        public CacheMetadata(CacheEntry[] entries, Calendar validThru)
        {
            _CacheEntries = entries;
            _ValidThru = validThru;
        }

        private CacheEntry[] _CacheEntries;
        private Calendar _ValidThru;

        public CacheEntry[] getEntries()
        {
            return _CacheEntries;
        }

        public Calendar getValidThru() {
            return _ValidThru;
        }

        public void setValidThru(Calendar validThru) {
            this._ValidThru = validThru;
        }

        public JSONObject toJSONObject() throws JSONException {
            JSONObject result = new JSONObject();
            JSONArray entriesArray = new JSONArray();
            for(int i=0; i< _CacheEntries.length; i++)
            {
                entriesArray.put(_CacheEntries[i].toJSONObject());
            }
            result.put("entries", entriesArray);
            result.put("validThru", _ValidThru.getTimeInMillis());
            return result;
        }

        private void readFromJSONObject(JSONObject json) throws JSONException {
            List<CacheEntry> entries = new ArrayList<CacheEntry>();
            JSONArray entriesArray = json.getJSONArray("entries");
            for(int i=0; i<entriesArray.length(); i++)
                entries.add(CacheEntry.fromJSONObject(entriesArray.getJSONObject(i)));
            _CacheEntries = entries.toArray(new CacheEntry[0]);
            _ValidThru = Calendar.getInstance();
            _ValidThru.setTimeInMillis(json.getLong("validThru"));
        }

        public static CacheMetadata fromJSONObject(JSONObject json) throws JSONException {
            CacheMetadata result = new CacheMetadata();
            result.readFromJSONObject(json);
            return result;
        }
    }

    private static final String TAG = LogUtil.makeLogTag(BingImageCache.class);
    private static final String METADATA_FILE_NAME = "metadata.json";

    private CacheMetadata _CacheMetadata;
    private WeakReference<Context> _Context;

    private ImageDownloader _ImageDownloader;

    public BingImageCache(Context context)
    {
        _Context = new WeakReference<Context>(context);
        _ImageDownloader = new ImageDownloader(context, this);
        loadMetadata();
    }

    public void setMetadata(CacheMetadata cacheMetadata)
    {
        _CacheMetadata = cacheMetadata;
        saveMetadata();
        cleanUpFiles();
    }

    public void ensureMissingImages()
    {
        LOGD(TAG, "ensuring missing items");
        List<Uri> missingImageUris = getMissingImageUris();
        if(!missingImageUris.isEmpty())
        {
            LOGD(TAG, "start downloading missing items");
            _ImageDownloader.download(missingImageUris);
        }
        else
            LOGD(TAG, "we have all needed items => nothing to download");
    }

    private List<Uri> getMissingImageUris()
    {
        List<Uri> missingUris = new ArrayList<Uri>();
        for(CacheEntry entry : _CacheMetadata.getEntries())
        {
            if(!hasImage(entry.getUri()))
                missingUris.add(entry.getUri());
        }
        return missingUris;
    }


    public CacheMetadata getMetadata()
    {
        return _CacheMetadata;
    }

    private void saveMetadata()
    {
        File metadataFile = new File(getCacheDirectory(), METADATA_FILE_NAME);
        if(_CacheMetadata == null && metadataFile.exists())
            metadataFile.delete();
        if(_CacheMetadata == null)
            return;
        try {
            FileUtils.writeTextFile(metadataFile, _CacheMetadata.toJSONObject().toString());
        } catch (IOException e) {
        } catch (JSONException e) {
        }
    }

    /**
     * cleans up all files that are no longer needed
     * (all files that are no longer referenced by the cache)
     */
    private void cleanUpFiles()
    {
        if(_CacheMetadata == null)
            return;
        List<String> requiredFiles = new ArrayList<String>();
        for(CacheEntry entry : _CacheMetadata.getEntries())
        {
            String fName = getFileNameFromUri(entry.getUri());
            if(!requiredFiles.contains(fName))
                requiredFiles.add(fName);
        }
        requiredFiles.add(METADATA_FILE_NAME);
        for(String fName : getCacheDirectory().list())
        {
            if(!requiredFiles.contains(fName)) {
                try {
                    new File(getCacheDirectory(), fName).delete();
                }
                catch(Exception e)
                {}
            }
        }
    }

    /**
     * loads a serialized copy of the metadata from the storage
     */
    private void loadMetadata() {
        _CacheMetadata = null;
        File metadataFile = new File(getCacheDirectory(), METADATA_FILE_NAME);
        if(metadataFile.exists())
        {
            try {
                String jsonString = FileUtils.readTextFile(metadataFile);
                JSONObject jsonObject = (JSONObject)new JSONTokener(jsonString).nextValue();
                _CacheMetadata = CacheMetadata.fromJSONObject(jsonObject);
            } catch (JSONException e) {
            } catch (IOException e) {
            }
        }
    }

    private File getCacheDirectory()
    {
        Context context = _Context.get();
        if(context == null)
            return null;

        return CacheUtils.getCacheDirectory(context);
    }

    /**
     * gets called when the download of an image is finished.
     * The image gets written to the storage
     * @param content
     * @param tag
     */
    @Override
    public synchronized void downloadFinished(byte[] content, Object tag) {
        if(content != null)
        {
            Uri uri = (Uri)tag;
            String fName = getFileNameFromUri(uri);
            File f = new File(getCacheDirectory(), fName);
            try {
                if(f.exists())
                    f.delete();
                FileOutputStream fOut = new FileOutputStream(f);
                fOut.write(content);
                fOut.flush();
                fOut.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean hasImage(int daysInThePast)
    {
        if(_CacheMetadata.getEntries().length <= daysInThePast)
            return false;
        return hasImage(_CacheMetadata.getEntries()[daysInThePast].getUri());
    }

    public synchronized boolean hasImage(Uri uri)
    {
        String fName = getFileNameFromUri(uri);
        return new File(getCacheDirectory(), fName).exists();
    }

    public synchronized String getFileName(int daysInThePast)
    {
        if(_CacheMetadata.getEntries().length <= daysInThePast)
            return null;
        return getFileNameFromUri(_CacheMetadata.getEntries()[daysInThePast].getUri());
    }

    final Character[] ReservedChars = {'|', '\\', '?', '*', '<', '\"', '\'', '/', ':', '>'};

    private String getFileNameFromUri(Uri uri)
    {
        String fName = uri.toString();

        for(Character c : ReservedChars)
        {
            fName = fName.replace(c, '_');
        }
        return fName;
    }
}
