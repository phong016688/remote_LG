package com.mentos_koder.remote_lg_tv.database

import retrofit2.Call
import com.mentos_koder.remote_lg_tv.model.LGAppInfo
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LGApiService {
    @GET("applications/{appId}")
    fun getAppInfo(@Path("appId") appId: String): Call<LGAppInfo>

    @POST("applications/{appId}")
    fun postAppInfo(
        @Path("appId") appId: String,
    ): Call<LGAppInfo>
}
