package com.example.fakenewsdetection.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/history")
    fun createHistory(
        @Body request: DetectionRequest
    ): Call<CreateHistoryResponse>

    @GET("api/history")
    fun getHistory(): Call<List<DetectionHistory>>

    @DELETE("api/history/{id}")
    fun deleteHistory(
        @Path("id") id: Int
    ): Call<DeleteHistoryResponse>
}