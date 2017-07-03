package com.shimastripe.gpsmountainview;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by takagi_go on 2017/07/03.
 */

public interface DocomoAPIInterface {
    String END_POINT = "https://api.apigw.smt.docomo.ne.jp/";

    @Headers({
            "Accept: application/json",
            "Content-type: application/json"
    })

    @GET("/mountainIdentification/v1/ridgeRendering/")
    Call<MountainRepository> getMountainData(@Query("lat") double latitude,
                                             @Query("lon") double longitude,
                                             @Query("azimuth") double azimuth,
                                             @Query("altura") double altura,
                                             @Query("angleOfView") double angleOfView,
                                             @Query("APIKEY") String apikey);
}
