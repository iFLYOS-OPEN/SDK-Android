package com.iflytek.cyber.product.ota

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface OtaApi {
    @GET("/ota/client/packages")
    fun getPackages(): Call<List<PackageEntity>>

    @PUT("/ota/client/packages")
    fun putPackages(@Body report: ReportEntity): Call<Void>
}