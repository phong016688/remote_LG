package com.mentos_koder.remote_lg_tv.database

import retrofit2.Call
import com.mentos_koder.remote_lg_tv.model.AppInfoSamsung
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SamsungApiService {
    @GET("applications/{appId}")
    fun getAppInfo(@Path("appId") appId: String): Call<AppInfoSamsung>

    @POST("applications/{appId}")
    fun postAppInfo(
        @Path("appId") appId: String,
    ): Call<AppInfoSamsung>
}
