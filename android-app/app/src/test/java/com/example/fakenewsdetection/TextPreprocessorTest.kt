package com.example.fakenewsdetection

import com.example.fakenewsdetection.FakeNewsClassifier.Companion.LABEL_FAKE
import com.example.fakenewsdetection.FakeNewsClassifier.Companion.LABEL_REAL
import com.example.fakenewsdetection.FakeNewsClassifier.Companion.LABEL_UNCERTAIN
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Unit test murni untuk [TextPreprocessor] — tidak butuh asset, ONNX, maupun
 * Android Context, sehingga cepat dan deterministik di CI.
 */
class TextPreprocessorTest {

    // Kelas EN v1.3: [0 = FAKE, 1 = REAL]; ID v3 mentah: [0 = REAL, 1 = FAKE].
    private val enClasses = intArrayOf(0, 1)
    private val idClasses = intArrayOf(0, 1)

    // =========================================================
    // TF-IDF
    // =========================================================

    @Test
    fun tfidf_twoTokenDocument_isL2NormalizedAndPlacedAtVocabularyIndex() {
        val vocabulary = mapOf("market" to 3, "rally" to 1)
        val idf = doubleArrayOf(9.0, 2.0, 9.0, 1.5, 9.0)

        val vec = TextPreprocessor.tfidfVector(
            TextPreprocessor.unigramBigramCounts(listOf("market", "rally")),
            vocabulary,
            idf
        )

        // "market rally" (bigram) tidak ada di vocabulary → diabaikan.
        // tf = 1 + ln(1) = 1 untuk kedua token.
        val norm = sqrt(1.5 * 1.5 + 2.0 * 2.0)
        assertEquals(5, vec.size)
        assertEquals(1.5 / norm, vec[3].toDouble(), 1e-6)
        assertEquals(2.0 / norm, vec[1].toDouble(), 1e-6)
        assertEquals(0f, vec[0])
        assertEquals(0f, vec[2])
        assertEquals(0f, vec[4])
        assertEquals(1.0, vec.sumOf { it.toDouble() * it }, 1e-6)
    }

    @Test
    fun tfidf_repeatedToken_usesSublinearTf() {
        val vec = TextPreprocessor.tfidfVector(
            mapOf("a" to 3, "b" to 1),
            mapOf("a" to 0, "b" to 1),
            doubleArrayOf(1.0, 1.0)
        )
        val tfA = 1.0 + ln(3.0)
        val norm = sqrt(tfA * tfA + 1.0)
        assertEquals(tfA / norm, vec[0].toDouble(), 1e-6)
        assertEquals(1.0 / norm, vec[1].toDouble(), 1e-6)
    }

    @Test
    fun tfidf_noKnownFeatures_returnsZeroVectorWithoutNaN() {
        val vec = TextPreprocessor.tfidfVector(
            mapOf("xyzzy" to 2, "qwerty" to 1),
            mapOf("market" to 0),
            doubleArrayOf(1.0, 1.0, 1.0)
        )
        assertArrayEquals(FloatArray(3), vec, 0f)
        assertTrue(vec.none { it.isNaN() })
    }

    // =========================================================
    // TEKS KOSONG
    // =========================================================

    @Test
    fun emptyAndBlankText_produceNoFeatures() {
        // EN v1.3 membuang angka & tanda baca → "123 456 !!!" juga kosong.
        for (input in listOf("", "   ", "\t\n", "123 456 !!!")) {
            val en = TextPreprocessor.cleanEnglishText(input)
            assertEquals("EN clean of '$input'", "", en)
            assertTrue(TextPreprocessor.englishWordCounts(en, emptySet()).isEmpty())
            assertTrue(TextPreprocessor.englishCharCounts(en).isEmpty())
        }
        // ID v3 mempertahankan angka, jadi hanya input kosong/blank yang dicek.
        for (input in listOf("", "   ", "\t\n", "!!! ??? ...")) {
            val id = TextPreprocessor.cleanIndonesianText(input)
            assertEquals("ID clean of '$input'", "", id)
            assertTrue(TextPreprocessor.indonesianWordCounts(id).isEmpty())
        }
    }

    @Test
    fun charWb_singleCharacterWord_emitsOnlyPaddedTrigram() {
        // sklearn char_wb: " a " (panjang 3) → hanya 3-gram, loop n berhenti.
        assertEquals(mapOf(" a " to 1), TextPreprocessor.englishCharCounts("a"))
    }

    @Test
    fun charWb_neverCrossesWordBoundaries() {
        val counts = TextPreprocessor.englishCharCounts("ab cd")
        assertTrue(counts.keys.none { it.trim().contains(' ') })
        assertEquals(1, counts[" ab "])
        assertEquals(1, counts[" cd "])
    }

    // =========================================================
    // THRESHOLD — ENGLISH v1.3 (uncertain = 0.20)
    // =========================================================

    @Test
    fun englishLabel_insideUncertainBand_isUncertain() {
        for (score in listOf(0.0, 0.1999, -0.1999, 0.05, -0.05)) {
            assertEquals(
                "score=$score",
                LABEL_UNCERTAIN,
                TextPreprocessor.decideEnglishLabel(score, 0.20, enClasses)
            )
        }
    }

    @Test
    fun englishLabel_exactlyAtThreshold_isConfident() {
        // Sama dengan src/evaluate.py: uncertain hanya jika abs(score) < threshold.
        assertEquals(LABEL_REAL, TextPreprocessor.decideEnglishLabel(0.20, 0.20, enClasses))
        assertEquals(LABEL_FAKE, TextPreprocessor.decideEnglishLabel(-0.20, 0.20, enClasses))
    }

    @Test
    fun englishLabel_outsideBand_followsSign() {
        assertEquals(LABEL_REAL, TextPreprocessor.decideEnglishLabel(1.3, 0.20, enClasses))
        assertEquals(LABEL_FAKE, TextPreprocessor.decideEnglishLabel(-1.3, 0.20, enClasses))
    }

    @Test
    fun englishLabel_nanScore_isUncertain() {
        assertEquals(LABEL_UNCERTAIN, TextPreprocessor.decideEnglishLabel(Double.NaN, 0.20, enClasses))
    }

    // =========================================================
    // THRESHOLD — INDONESIAN v3 (threshold = 0.10, 0=REAL 1=FAKE mentah)
    // =========================================================

    @Test
    fun indonesianLabel_atOrAboveThreshold_isFake() {
        assertEquals(LABEL_FAKE, TextPreprocessor.decideIndonesianLabel(0.10, 0.10, idClasses))
        assertEquals(LABEL_FAKE, TextPreprocessor.decideIndonesianLabel(1.2, 0.10, idClasses))
    }

    @Test
    fun indonesianLabel_belowThreshold_isReal() {
        assertEquals(LABEL_REAL, TextPreprocessor.decideIndonesianLabel(0.0999, 0.10, idClasses))
        assertEquals(LABEL_REAL, TextPreprocessor.decideIndonesianLabel(-2.0, 0.10, idClasses))
    }

    @Test
    fun indonesianLabel_nanScore_isUncertain() {
        assertEquals(LABEL_UNCERTAIN, TextPreprocessor.decideIndonesianLabel(Double.NaN, 0.10, idClasses))
    }
}
