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
package de.devmil.muzei.bingimageofthedayartsource;

import android.net.Uri;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by devmil on 17.02.14.
 *
 * This class represents one Bing image containing all data that this app needs
 */
public class BingImageMetadata {
    private Uri _Uri;
    private String _Copyright;
    private Date _StartDate;

    public BingImageMetadata()
    {
    }

    public BingImageMetadata(Uri uri, String copyright, String startDateString)
    {
        _Uri = uri;
        _Copyright = copyright;
        _StartDate = parseStartDate(startDateString);
    }

    private Date parseStartDate(String startDateString) {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date result = null;
        try {
            result = df.parse(startDateString);
        } catch (ParseException e) {
        }

        Calendar localTime = Calendar.getInstance();

        if(result == null)
            return localTime.getTime();

        localTime.setTimeInMillis(result.getTime());

        return localTime.getTime();
    }

    public Uri getUri()
    {
        return _Uri;
    }

    public void setUri(Uri uri)
    {
        _Uri = uri;
    }

    public String getCopyrightOrEmpty()
    {
        if(getCopyright() == null)
            return "";
        return getCopyright();
    }

    public String getCopyright()
    {
        return _Copyright;
    }

    public void setCopyright(String copyright)
    {
        _Copyright = copyright;
    }

    public Date getStartDate()
    {
        return _StartDate;
    }
}
