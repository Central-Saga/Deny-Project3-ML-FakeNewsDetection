package com.example.fakenewsdetection

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test: memuat artefak ONNX + JSON asli dari APK assets dan
 * menjalankan inferensi di perangkat/emulator (onnxruntime-android).
 *
 * Jalankan: ./gradlew connectedDebugAndroidTest (butuh emulator/perangkat).
 */
@RunWith(AndroidJUnit4::class)
class FakeNewsClassifierInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun loadsBundledModelsAndScoresEnglish() {
        FakeNewsClassifier(context).use { classifier ->
            val result = classifier.predictEnglish(
                "Federal Reserve raises interest rates",
                "Officials said the decision was made according to the latest inflation data."
            )
            assertEquals(FakeNewsClassifier.ModelLanguage.ENGLISH, result.language)
            assertFalse("Score must not be NaN", result.score.isNaN())
            assertTrue(result.label in setOf(-1, 0, 1))
        }
    }

    @Test
    fun loadsBundledModelsAndScoresIndonesian() {
        FakeNewsClassifier(context).use { classifier ->
            val result = classifier.predictAuto(
                "Presiden meresmikan jalan tol baru",
                "Pemerintah menyatakan bahwa proyek ini telah selesai dan akan dibuka untuk masyarakat."
            )
            assertEquals(FakeNewsClassifier.ModelLanguage.INDONESIAN, result.language)
            assertFalse("Score must not be NaN", result.score.isNaN())
            assertTrue(result.label in setOf(-1, 0, 1))
        }
    }
}
