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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import retrofit.RestAdapter;

/**
 * Created by devmil on 16.02.14.
 *
 * Uses the Bing REST API to get the metadata for the current (and the last few) images of the day
 */
public class BingImageOfTheDayMetadataRetriever {

    private static final String TAG = BingImageOfTheDayMetadataRetriever.class.getName();

    private static final String BING_URL = "http://www.bing.com";

    public static final int MAXIMUM_BING_IMAGE_NUMBER = 8;

    private BingMarket _Market;
    private BingImageDimension _Dimension;
    private boolean _Portrait;

    public BingImageOfTheDayMetadataRetriever(BingMarket market, BingImageDimension dimension, boolean portrait)
    {
        _Market = market;
        _Dimension = dimension;
        _Portrait = portrait;
    }

    public List<BingImageMetadata> getBingImageOfTheDayMetadata()
    {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setServer(BING_URL)
                .build();

        try
        {
            IBingImageService service = restAdapter.create(IBingImageService.class);
            IBingImageService.BingImageResponse response = service.getImageOfTheDayMetadata(MAXIMUM_BING_IMAGE_NUMBER, _Market.getMarketCode());

            if(response == null || response.images == null)
                return null;

            return getMetadata(response.images);
        }
        catch(Exception e)
        {
            Log.w(TAG, "Error from Bing: ", e);
            return null;
        }
    }

    private List<BingImageMetadata> getMetadata(List<IBingImageService.BingImage> bingImages)
    {
        List<BingImageMetadata> result = new ArrayList<BingImageMetadata>();
        for(IBingImageService.BingImage bingImage : bingImages)
        {
            Uri uri = Uri.parse(BING_URL + bingImage.urlbase + "_" + _Dimension.getStringRepresentation(_Portrait) + ".jpg");

            result.add(new BingImageMetadata(uri, bingImage.copyright, bingImage.fullstartdate));
        }
        return result;
    }
}
