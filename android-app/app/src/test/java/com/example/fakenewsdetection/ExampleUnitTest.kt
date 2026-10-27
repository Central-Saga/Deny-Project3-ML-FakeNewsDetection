package com.example.fakenewsdetection

import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun jsonParsing_isCorrect() {
        val obj = JSONObject("{\"threshold\": 0.20}")
        assertEquals(0.20, obj.getDouble("threshold"), 1e-6)
    }
}