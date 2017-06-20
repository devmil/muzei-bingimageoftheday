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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Calendar

import de.devmil.muzei.bingimageofthedayartsource.utils.FileUtils
import de.devmil.common.utils.LogUtil

/**
 * Created by devmil on 25.02.14.

 * This class represents the image cache. It controls the (serialized) metadata
 * and triggers the cleanup / image download depending on the metadata it gets
 */
class BingImageCache(context: Context) : ImageDownloadRequest.OnDownloadFinishedListener {

    class CacheEntry {
        var uri: Uri? = null
            private set
        var description: String? = null
            private set
        var copyright: String? = null
            private set

        private constructor()

        constructor(uri: Uri, description: String, copyright: String) {
            this.uri = uri
            this.description = description
            this.copyright = copyright
        }

        @Throws(JSONException::class)
        fun toJSONObject(): JSONObject {
            val result = JSONObject()
            result.put("uri", uri!!.toString())
            result.put("desc", description)
            result.put("copyright", copyright)
            return result
        }

        @Throws(JSONException::class)
        private fun readFromJSONObject(json: JSONObject) {
            uri = Uri.parse(json.getString("uri"))
            description = json.getString("desc")
            copyright = json.getString("copyright")
        }

        companion object {

            @Throws(JSONException::class)
            fun fromJSONObject(json: JSONObject): CacheEntry {
                val entry = CacheEntry()
                entry.readFromJSONObject(json)
                return entry
            }
        }
    }

    class CacheMetadata {
        private constructor()

        constructor(entries: Array<CacheEntry>, validThru: Calendar) {
            this.entries = entries
            this.validThru = validThru
        }

        var entries: Array<CacheEntry>? = null
            private set
        var validThru: Calendar? = null

        @Throws(JSONException::class)
        fun toJSONObject(): JSONObject {
            val result = JSONObject()
            val entriesArray = JSONArray()
            for (i in entries!!.indices) {
                entriesArray.put(entries!![i].toJSONObject())
            }
            result.put("entries", entriesArray)
            result.put("validThru", validThru!!.timeInMillis)
            return result
        }

        @Throws(JSONException::class)
        private fun readFromJSONObject(json: JSONObject) {
            val entriesArray = json.getJSONArray("entries")
            val entries = (0..entriesArray.length() - 1).map { CacheEntry.fromJSONObject(entriesArray.getJSONObject(it)) }
            this.entries = entries.toTypedArray()
            validThru = Calendar.getInstance()
            validThru!!.timeInMillis = json.getLong("validThru")
        }

        companion object {

            @Throws(JSONException::class)
            fun fromJSONObject(json: JSONObject): CacheMetadata {
                val result = CacheMetadata()
                result.readFromJSONObject(json)
                return result
            }
        }
    }

    private var _CacheMetadata: CacheMetadata? = null
    private val _Context: WeakReference<Context> = WeakReference(context)

    private val _ImageDownloader: ImageDownloader = ImageDownloader(context, this)

    init {
        loadMetadata()
    }

    fun ensureMissingImages() {
        LogUtil.LOGD(TAG, "ensuring missing items")
        val missingImageUris = missingImageUris
        if (!missingImageUris.isEmpty()) {
            LogUtil.LOGD(TAG, "start downloading missing items")
            _ImageDownloader.download(missingImageUris)
        } else
            LogUtil.LOGD(TAG, "we have all needed items => nothing to download")
    }

    private val missingImageUris: List<Uri>
        get() {
            val missingUris = _CacheMetadata!!.entries!!
                    .filterNot { hasImage(it.uri!!) }
                    .map { it.uri!! }
            return missingUris
        }


    var metadata: CacheMetadata?
        get() = _CacheMetadata
        set(cacheMetadata) {
            _CacheMetadata = cacheMetadata
            saveMetadata()
            cleanUpFiles()
        }

    private fun saveMetadata() {
        val metadataFile = File(cacheDirectory, METADATA_FILE_NAME)
        if (_CacheMetadata == null && metadataFile.exists())
            metadataFile.delete()
        if (_CacheMetadata == null)
            return
        try {
            FileUtils.writeTextFile(metadataFile, _CacheMetadata!!.toJSONObject().toString())
        } catch (e: IOException) {
        } catch (e: JSONException) {
        }

    }

    /**
     * cleans up all files that are no longer needed
     * (all files that are no longer referenced by the cache)
     */
    private fun cleanUpFiles() {
        if (_CacheMetadata == null)
            return
        val requiredFiles = ArrayList<String>()
        _CacheMetadata!!.entries!!
                .map { getFileNameFromUri(it.uri!!) }
                .filterNot { requiredFiles.contains(it) }
                .forEach { requiredFiles.add(it) }
        requiredFiles.add(METADATA_FILE_NAME)
        cacheDirectory!!.list()
                .filterNot { requiredFiles.contains(it) }
                .forEach {
                    try {
                        File(cacheDirectory, it).delete()
                    } catch (e: Exception) {
                    }
                }
    }

    /**
     * loads a serialized copy of the metadata from the storage
     */
    private fun loadMetadata() {
        _CacheMetadata = null
        val metadataFile = File(cacheDirectory, METADATA_FILE_NAME)
        if (metadataFile.exists()) {
            try {
                val jsonString = FileUtils.readTextFile(metadataFile)
                val jsonObject = JSONTokener(jsonString).nextValue() as JSONObject
                _CacheMetadata = CacheMetadata.fromJSONObject(jsonObject)
            } catch (e: JSONException) {
            } catch (e: IOException) {
            }

        }
    }

    private val cacheDirectory: File?
        get() {
            val context = _Context.get() ?: return null

            return CacheUtils.getCacheDirectory(context)
        }

    /**
     * gets called when the download of an image is finished.
     * The image gets written to the storage
     * @param content
     * *
     * @param tag
     */
    @Synchronized override fun downloadFinished(content: ByteArray?, tag: Any) {
        if (content != null) {
            val uri = tag as Uri
            val fName = getFileNameFromUri(uri)
            val f = File(cacheDirectory, fName)
            try {
                if (f.exists())
                    f.delete()
                val fOut = FileOutputStream(f)
                fOut.write(content)
                fOut.flush()
                fOut.close()
            } catch (e: FileNotFoundException) {
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    @Synchronized fun hasImage(daysInThePast: Int): Boolean {
        if (_CacheMetadata!!.entries!!.size <= daysInThePast)
            return false
        return hasImage(_CacheMetadata!!.entries!![daysInThePast].uri!!)
    }

    @Synchronized fun hasImage(uri: Uri): Boolean {
        val fName = getFileNameFromUri(uri)
        return File(cacheDirectory, fName).exists()
    }

    @Synchronized fun getFileName(daysInThePast: Int): String? {
        if (_CacheMetadata!!.entries!!.size <= daysInThePast)
            return null
        return getFileNameFromUri(_CacheMetadata!!.entries!![daysInThePast].uri!!)
    }

    internal val ReservedChars = arrayOf('|', '\\', '?', '*', '<', '\"', '\'', '/', ':', '>')

    private fun getFileNameFromUri(uri: Uri): String {
        var fName = uri.toString()

        for (c in ReservedChars) {
            fName = fName.replace(c, '_')
        }
        return fName
    }

    companion object {

        private val TAG = LogUtil.makeLogTag(BingImageCache::class.java)
        private val METADATA_FILE_NAME = "metadata.json"
    }
}
