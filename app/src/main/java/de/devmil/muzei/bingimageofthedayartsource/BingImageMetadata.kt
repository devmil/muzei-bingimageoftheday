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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by devmil on 17.02.14.

 * This class represents one Bing image containing all data that this app needs
 */
class BingImageMetadata(uri: Uri, copyright: String, startDateString: String) {
    var uri: Uri? = uri
    var copyright: String? = copyright
    var startDate: Date? = null

    private fun parseStartDate(startDateString: String): Date {
        val df = SimpleDateFormat("yyyyMMddHHmm", Locale.US)
        df.timeZone = TimeZone.getTimeZone("GMT")

        var result: Date? = null
        try {
            result = df.parse(startDateString)
        } catch (e: ParseException) {
        }

        val localTime = Calendar.getInstance()

        if (result == null)
            return localTime.time

        localTime.timeInMillis = result.time

        return localTime.time
    }

    val copyrightOrEmpty: String
        get() {
            if (copyright == null)
                return ""
            return copyright!!
        }

    init {
        startDate = parseStartDate(startDateString)
    }
}
