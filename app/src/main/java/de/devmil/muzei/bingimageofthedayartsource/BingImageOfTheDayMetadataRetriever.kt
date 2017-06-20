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
package de.devmil.muzei.bingimageofthedayartsource

import android.net.Uri
import android.util.Log

import java.util.ArrayList

import retrofit.RestAdapter

/**
 * Created by devmil on 16.02.14.

 * Uses the Bing REST API to get the metadata for the current (and the last few) images of the day
 */
class BingImageOfTheDayMetadataRetriever(private val _Market: BingMarket, private val _Dimension: BingImageDimension, private val _Portrait: Boolean) {

    val bingImageOfTheDayMetadata: List<BingImageMetadata>?
        get() {
            val restAdapter = RestAdapter.Builder()
                    .setServer(BING_URL)
                    .build()

            try {
                val service = restAdapter.create(IBingImageService::class.java)
                val response = service.getImageOfTheDayMetadata(MAXIMUM_BING_IMAGE_NUMBER, _Market.marketCode)

                if (response.images == null)
                    return null

                return getMetadata(response.images!!)
            } catch (e: Exception) {
                Log.w(TAG, "Error from Bing: ", e)
                return null
            }

        }

    private fun getMetadata(bingImages: List<IBingImageService.BingImage>): List<BingImageMetadata> {
        val result = ArrayList<BingImageMetadata>()
        for (bingImage in bingImages) {
            val uri = Uri.parse(BING_URL + bingImage.urlbase + "_" + _Dimension.getStringRepresentation(_Portrait) + ".jpg")

            result.add(BingImageMetadata(uri, bingImage.copyright!!, bingImage.fullstartdate!!))
        }
        return result
    }

    companion object {

        private val TAG = BingImageOfTheDayMetadataRetriever::class.java.name

        private val BING_URL = "http://www.bing.com"

        val MAXIMUM_BING_IMAGE_NUMBER = 8
    }
}
