package com.example.fakenewsdetection.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("api/history")
    fun createHistory(
        @Body request: DetectionRequest
    ): Call<CreateHistoryResponse>

    @GET("api/history")
    fun getHistory(): Call<List<DetectionHistory>>
}