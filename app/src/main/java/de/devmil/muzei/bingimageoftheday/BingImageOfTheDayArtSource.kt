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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder

/**
 * This class is kept only to serve as a tombstone to Muzei to know to replace it
 * with [BingImageOfTheDayArtProvider].
 */
class BingImageOfTheDayArtSource : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {

        private const val SOURCE_NAME = "BingImageOfTheDayArtSource"

        fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences("muzeiartsource_$SOURCE_NAME", 0)
        }
    }
}