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

import retrofit.http.GET
import retrofit.http.Query

/**
 * Created by devmil on 18.02.14.

 * Interface for the Bing Image Of the day REST API
 */
interface IBingImageService {

    @GET("/HPImageArchive.aspx?format=js&idx=0")
    fun getImageOfTheDayMetadata(@Query("n") number: Int, @Query("mkt") market: String): BingImageResponse

    class BingImageResponse {
        internal var images: List<BingImage>? = null
    }

    class BingImage {
        internal var startdate: String? = null
        internal var fullstartdate: String? = null
        internal var enddate: String? = null
        internal var url: String? = null
        internal var urlbase: String? = null
        internal var copyright: String? = null
        internal var copyrightlink: String? = null
        internal var wp: Boolean = false
        internal var hsh: String? = null
        internal var drk: Int = 0
        internal var top: Int = 0
        internal var bot: Int = 0
        internal var walle: String? = null
        internal var walls: String? = null
    }
}
