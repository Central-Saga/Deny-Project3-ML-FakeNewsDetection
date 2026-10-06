package com.example.fakenewsdetection.network

data class DetectionRequest(
    val title: String,
    val content: String,
    val prediction: String,
    val score: Double?
)

data class CreateHistoryResponse(
    val message: String,
    val id: Int
)

data class DeleteHistoryResponse(
    val message: String
)

data class DetectionHistory(
    val id: Int,
    val title: String,
    val content: String,
    val prediction: String,
    val score: Double?,
    val created_at: String
)