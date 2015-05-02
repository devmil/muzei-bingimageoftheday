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

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by devmil on 18.02.14.
 *
 * Interface for the Bing Image Of the day REST API
 */
public interface IBingImageService {

    @GET("/HPImageArchive.aspx?format=js&idx=0")
    BingImageResponse getImageOfTheDayMetadata(@Query("n")int number, @Query("mkt")String market);

    static class BingImageResponse
    {
        List<BingImage> images;
    }

    static class BingImage
    {
        String startdate;
        String fullstartdate;
        String enddate;
        String url;
        String urlbase;
        String copyright;
        String copyrightlink;
        boolean wp;
        String hsh;
        int drk;
        int top;
        int bot;
        String walle;
        String walls;
    }
}
