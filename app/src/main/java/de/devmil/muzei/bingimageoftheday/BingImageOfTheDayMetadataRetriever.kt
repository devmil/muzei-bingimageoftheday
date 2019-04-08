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
package de.devmil.muzei.bingimageoftheday

import android.net.Uri
import android.util.Log

import java.util.ArrayList

import retrofit2.Retrofit

/**
 * Created by devmil on 16.02.14.

 * Uses the Bing REST API to get the metadata for the current (and the last few) images of the day
 */
class BingImageOfTheDayMetadataRetriever(private val market: BingMarket, private val dimension: BingImageDimension, private val portrait: Boolean) {

    val bingImageOfTheDayMetadata: List<BingImageMetadata>?
        get() {
            val restAdapter = Retrofit.Builder()
                    .baseUrl(BING_URL)
                    .build()

            try {
                val service = restAdapter.create(IBingImageService::class.java)
                val response = service.getImageOfTheDayMetadata(MAXIMUM_BING_IMAGE_NUMBER, market.marketCode)

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
            val uri = Uri.parse(BING_URL + bingImage.urlbase + "_" + dimension.getStringRepresentation(portrait) + ".jpg")

            result.add(BingImageMetadata(uri, bingImage.copyright!!, bingImage.fullstartdate!!))
        }
        return result
    }

    companion object {

        private val TAG = BingImageOfTheDayMetadataRetriever::class.java.name

        private val BING_URL = "https://www.bing.com"

        val MAXIMUM_BING_IMAGE_NUMBER = 8
    }
}
