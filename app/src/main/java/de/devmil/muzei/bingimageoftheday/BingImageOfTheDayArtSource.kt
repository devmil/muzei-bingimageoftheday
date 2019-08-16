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

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import com.google.android.apps.muzei.api.MuzeiArtSource
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource
import com.google.android.apps.muzei.api.UserCommand
import de.devmil.common.utils.LogUtil

/**
 * Muzei Art source for Bing images of the day.
 * This class is the plugin entry point for Muzei
 */
class BingImageOfTheDayArtSource : RemoteMuzeiArtSource("de.devmil.muzei.Bing") {

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        setUserCommands(
                UserCommand(MuzeiArtSource.BUILTIN_COMMAND_ID_NEXT_ARTWORK))
    }

    @Throws(RemoteMuzeiArtSource.RetryException::class)
    override fun onTryUpdate(updateReason: Int) {
        //NOOP
    }

    companion object {

        private val TAG = LogUtil.makeLogTag(BingImageOfTheDayArtSource::class.java)

        private val SOURCE_NAME = "BingImageOfTheDayArtSource"

        private val ACTION_ENSUREINITIALIZED = "de.devmil.muzei.bingimageoftheday.ACTION_ENSURE_INITIALIZED"

        fun getSharedPreferences(context: Context): SharedPreferences {
            return MuzeiArtSource.getSharedPreferences(context, SOURCE_NAME)
        }

        fun ensureInitialized(context: Context) {
            val thisServiceIntent = Intent()
            thisServiceIntent.setClass(context, BingImageOfTheDayArtSource::class.java)
            thisServiceIntent.action = ACTION_ENSUREINITIALIZED
            context.startService(thisServiceIntent)
        }
    }
}