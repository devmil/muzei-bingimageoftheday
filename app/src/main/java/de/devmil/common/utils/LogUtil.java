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

package de.devmil.common.utils;

import android.util.Log;

import de.devmil.muzei.bingimageofthedayartsource.BuildConfig;


/**
 * Happily taken from the Muzei project by Roman Nurik: https://github.com/romannurik/muzei
 */

/**
 * Helper methods that make logging more consistent throughout the app.
 */
public class LogUtil {
    private static final String TAG = makeLogTag(LogUtil.class);

    private static final String LOG_PREFIX = "muzei_biot_";
    private static final int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static final int MAX_LOG_TAG_LENGTH = 23;

    private LogUtil() {
    }

    public static String makeLogTag(String str) {
        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1);
        }

        return LOG_PREFIX + str;
    }

    /**
     * WARNING: Don't use this when obfuscating class names with Proguard!
     */
    public static String makeLogTag(Class cls) {
        return makeLogTag(cls.getSimpleName());
    }

    public static void LOGD(final String tag, String message) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG || Log.isLoggable(normalizeTag(tag), Log.DEBUG)) {
            Log.d(normalizeTag(tag), message);
        }
    }

    public static void LOGD(final String tag, String message, Throwable cause) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG || Log.isLoggable(normalizeTag(tag), Log.DEBUG)) {
            Log.d(normalizeTag(tag), message, cause);
        }
    }

    public static void LOGV(final String tag, String message) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG && Log.isLoggable(normalizeTag(tag), Log.VERBOSE)) {
            Log.v(normalizeTag(tag), message);
        }
    }

    public static void LOGV(final String tag, String message, Throwable cause) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG && Log.isLoggable(normalizeTag(tag), Log.VERBOSE)) {
            Log.v(normalizeTag(tag), message, cause);
        }
    }

    public static void LOGI(final String tag, String message) {
        Log.i(normalizeTag(tag), message);
    }

    public static void LOGI(final String tag, String message, Throwable cause) {
        Log.i(normalizeTag(tag), message, cause);
    }

    public static void LOGW(final String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.w(normalizeTag(tag), message, new Throwable()); // create a stacktrace
        } else {
            Log.w(normalizeTag(tag), message);
        }
    }

    public static void LOGW(final String tag, String message, Throwable cause) {
        Log.w(normalizeTag(tag), message, cause);
    }

    public static void LOGE(final String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.e(normalizeTag(tag), message, new Throwable()); // create a stacktrace
        } else {
            Log.e(normalizeTag(tag), message);
        }
    }

    public static void LOGE(final String tag, String message, Throwable cause) {
        Log.e(normalizeTag(tag), message, cause);
    }

    private static String normalizeTag(String tag)
    {
        String result = tag;
        if(result.length() > MAX_LOG_TAG_LENGTH) //this is the limit for log tags
        {
            result = result.substring(0, MAX_LOG_TAG_LENGTH);
        }
        return result;
    }
}
