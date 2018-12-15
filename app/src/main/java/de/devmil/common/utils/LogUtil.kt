/*
 * Copyright 2014 Google Inc.
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

package de.devmil.common.utils

import android.util.Log

import de.devmil.muzei.bingimageoftheday.BuildConfig


/**
 * Happily taken from the Muzei project by Roman Nurik: https://github.com/romannurik/muzei
 */

/**
 * Helper methods that make logging more consistent throughout the app.
 */
object LogUtil {

    private val LOG_PREFIX = "muzei_biot_"
    private val LOG_PREFIX_LENGTH = LOG_PREFIX.length
    private val MAX_LOG_TAG_LENGTH = 23

    fun makeLogTag(str: String): String {
        if (str.length > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1)
        }

        return LOG_PREFIX + str
    }

    /**
     * WARNING: Don't use this when obfuscating class names with Proguard!
     */
    fun makeLogTag(cls: Class<*>): String {
        return makeLogTag(cls.simpleName)
    }

    fun LOGD(tag: String, message: String) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG || Log.isLoggable(normalizeTag(tag), Log.DEBUG)) {
            Log.d(normalizeTag(tag), message)
        }
    }

    fun LOGD(tag: String, message: String, cause: Throwable) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG || Log.isLoggable(normalizeTag(tag), Log.DEBUG)) {
            Log.d(normalizeTag(tag), message, cause)
        }
    }

    fun LOGV(tag: String, message: String) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG && Log.isLoggable(normalizeTag(tag), Log.VERBOSE)) {
            Log.v(normalizeTag(tag), message)
        }
    }

    fun LOGV(tag: String, message: String, cause: Throwable) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG && Log.isLoggable(normalizeTag(tag), Log.VERBOSE)) {
            Log.v(normalizeTag(tag), message, cause)
        }
    }

    fun LOGI(tag: String, message: String) {
        Log.i(normalizeTag(tag), message)
    }

    fun LOGI(tag: String, message: String, cause: Throwable) {
        Log.i(normalizeTag(tag), message, cause)
    }

    fun LOGW(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(normalizeTag(tag), message, Throwable()) // create a stacktrace
        } else {
            Log.w(normalizeTag(tag), message)
        }
    }

    fun LOGW(tag: String, message: String, cause: Throwable) {
        Log.w(normalizeTag(tag), message, cause)
    }

    fun LOGE(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.e(normalizeTag(tag), message, Throwable()) // create a stacktrace
        } else {
            Log.e(normalizeTag(tag), message)
        }
    }

    fun LOGE(tag: String, message: String, cause: Throwable) {
        Log.e(normalizeTag(tag), message, cause)
    }

    private fun normalizeTag(tag: String): String {
        var result = tag
        if (result.length > MAX_LOG_TAG_LENGTH)
        //this is the limit for log tags
        {
            result = result.substring(0, MAX_LOG_TAG_LENGTH)
        }
        return result
    }
}
