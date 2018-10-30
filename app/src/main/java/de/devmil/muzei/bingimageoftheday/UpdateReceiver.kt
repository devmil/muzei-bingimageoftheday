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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.devmil.common.utils.LogUtil

class UpdateReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        LogUtil.LOGD(TAG, "received update")

        if(BingImageOfTheDayArtProvider.isActive ?: false) {
            LogUtil.LOGD(TAG, "Updating provider")
            BingImageOfTheDayArtProvider.doUpdate()
        }

        if(!(BingImageOfTheDayArtProvider.isActive ?: true)) {
            LogUtil.LOGD(TAG, "Updating art source")
            BingImageOfTheDayArtSource.ensureInitialized(context)
        }
    }

    companion object {
        private const val TAG = "UpdateReceiver"
    }
}
