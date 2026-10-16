package com.example.fakenewsdetection

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DetectionHistory(
    val id: Long,
    val title: String,
    val content: String,
    val prediction: String,
    val score: Double,
    val timestamp: Long
)

class LocalHistoryManager(
    context: Context
) {

    companion object {
        private const val PREF_NAME =
            "fake_news_history"

        private const val KEY_HISTORY =
            "detections"

        private const val MAX_HISTORY =
            50
    }

    private val preferences =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    // =========================================================
    // SIMPAN RIWAYAT
    // =========================================================

    fun saveHistory(
        title: String,
        content: String,
        prediction: String,
        score: Double
    ) {

        val oldHistory =
            getHistory()

        val newHistory =
            DetectionHistory(
                id = System.currentTimeMillis(),
                title = title,
                content = content,
                prediction = prediction,
                score = score,
                timestamp = System.currentTimeMillis()
            )

        val combined =
            mutableListOf<DetectionHistory>()

        // Data terbaru berada di paling atas
        combined.add(newHistory)

        combined.addAll(
            oldHistory
        )

        // Maksimal 50 riwayat
        val limitedHistory =
            combined.take(
                MAX_HISTORY
            )

        saveList(
            limitedHistory
        )
    }

    // =========================================================
    // AMBIL SEMUA RIWAYAT
    // =========================================================

    fun getHistory():
            List<DetectionHistory> {

        val jsonString =
            preferences.getString(
                KEY_HISTORY,
                null
            )
                ?: return emptyList()

        return try {

            val jsonArray =
                JSONArray(
                    jsonString
                )

            val historyList =
                mutableListOf<DetectionHistory>()

            for (
            index in 0 until jsonArray.length()
            ) {

                val item =
                    jsonArray
                        .getJSONObject(
                            index
                        )

                historyList.add(
                    DetectionHistory(
                        id =
                            item.optLong(
                                "id"
                            ),

                        title =
                            item.optString(
                                "title"
                            ),

                        content =
                            item.optString(
                                "content"
                            ),

                        prediction =
                            item.optString(
                                "prediction"
                            ),

                        score =
                            item.optDouble(
                                "score"
                            ),

                        timestamp =
                            item.optLong(
                                "timestamp"
                            )
                    )
                )
            }

            historyList

        } catch (
            exception: Exception
        ) {

            emptyList()
        }
    }

    // =========================================================
    // HAPUS SATU RIWAYAT
    // =========================================================

    fun deleteHistory(
        id: Long
    ) {

        val updated =
            getHistory()
                .filter {
                    it.id != id
                }

        saveList(
            updated
        )
    }

    // =========================================================
    // HAPUS SEMUA RIWAYAT
    // =========================================================

    fun clearHistory() {

        preferences
            .edit()
            .remove(
                KEY_HISTORY
            )
            .apply()
    }

    // =========================================================
    // SAVE LIST
    // =========================================================

    private fun saveList(
        history:
        List<DetectionHistory>
    ) {

        val jsonArray =
            JSONArray()

        history.forEach {

            val jsonObject =
                JSONObject()

            jsonObject.put(
                "id",
                it.id
            )

            jsonObject.put(
                "title",
                it.title
            )

            jsonObject.put(
                "content",
                it.content
            )

            jsonObject.put(
                "prediction",
                it.prediction
            )

            jsonObject.put(
                "score",
                it.score
            )

            jsonObject.put(
                "timestamp",
                it.timestamp
            )

            jsonArray.put(
                jsonObject
            )
        }

        preferences
            .edit()
            .putString(
                KEY_HISTORY,
                jsonArray.toString()
            )
            .apply()
    }
}