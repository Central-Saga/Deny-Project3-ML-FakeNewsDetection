package com.example.fakenewsdetection

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import kotlin.math.abs

/**
 * Golden test paritas preprocessing Python ↔ Kotlin (Issue #17).
 *
 * Fixture `src/test/resources/golden_preprocessing.json` dihasilkan oleh
 * `python src/generate_golden.py`:
 *   - clean_text  : clean_english_text / clean_indonesian_text (src/evaluate.py)
 *   - word/char   : analyzer asli scikit-learn (TfidfVectorizer.build_analyzer)
 *   - tfidf       : sublinear TF + L2 dengan vocabulary/idf artefak aplikasi
 *
 * Satu karakter beda di regex/tokenisasi Kotlin akan membuat test ini merah.
 * Setiap test mengumpulkan SEMUA selisih agar mudah didiagnosis.
 */
class GoldenParityTest {

    companion object {
        private const val TFIDF_TOLERANCE = 1e-5

        private lateinit var golden: JSONObject
        private lateinit var classifier: FakeNewsClassifier

        @BeforeClass
        @JvmStatic
        fun loadFixtures() {
            val stream = GoldenParityTest::class.java.classLoader
                ?.getResourceAsStream("golden_preprocessing.json")
            assertNotNull(
                "golden_preprocessing.json tidak ada — jalankan: python src/generate_golden.py",
                stream
            )
            golden = JSONObject(stream!!.bufferedReader(Charsets.UTF_8).use { it.readText() })

            // Hanya vocabulary/idf/stop words yang dibutuhkan; ONNX tidak pernah dibuka.
            classifier = FakeNewsClassifier(
                assetReader = { name ->
                    FakeNewsClassifierTest.findAssetFile(name)
                        .inputStream()
                        .use { FakeNewsClassifier.readAssetText(it) }
                },
                onnxScorerProvider = {
                    throw IllegalStateException("Golden test tidak boleh memakai ONNX")
                }
            )
        }
    }

    private val english: JSONArray get() = golden.getJSONArray("english")
    private val indonesian: JSONArray get() = golden.getJSONArray("indonesian")

    // =========================================================
    // ENGLISH v1.3
    // =========================================================

    @Test
    fun english_cleanText_matchesPython() {
        assertSampleCount(english, 15)
        checkAll(english) { s ->
            val expected = s.getString("clean_text")
            val actual = TextPreprocessor.cleanEnglishText(s.getString("input"))
            if (expected == actual) null else "expected='$expected'\n      actual='$actual'"
        }
    }

    @Test
    fun english_wordFeatures_matchSklearnAnalyzer() {
        checkAll(english) { s ->
            diffCounts(
                s.getJSONObject("word_counts"),
                TextPreprocessor.englishWordCounts(s.getString("clean_text"), classifier.enWordStopWords)
            )
        }
    }

    @Test
    fun english_charWbFeatures_matchSklearnAnalyzer() {
        checkAll(english) { s ->
            diffCounts(
                s.getJSONObject("char_counts"),
                TextPreprocessor.englishCharCounts(s.getString("clean_text"))
            )
        }
    }

    @Test
    fun english_tfidfVectors_matchPython() {
        checkAll(english) { s ->
            val cleaned = TextPreprocessor.cleanEnglishText(s.getString("input"))
            val word = TextPreprocessor.tfidfVector(
                TextPreprocessor.englishWordCounts(cleaned, classifier.enWordStopWords),
                classifier.enWordVocabulary,
                classifier.enWordIdf
            )
            val char = TextPreprocessor.tfidfVector(
                TextPreprocessor.englishCharCounts(cleaned),
                classifier.enCharVocabulary,
                classifier.enCharIdf
            )
            listOfNotNull(
                diffSparse("word", s.getJSONObject("word_tfidf"), word),
                diffSparse("char", s.getJSONObject("char_tfidf"), char)
            ).joinToString("\n      ").ifEmpty { null }
        }
    }

    // =========================================================
    // INDONESIAN POLITICAL v3
    // =========================================================

    @Test
    fun indonesian_cleanText_matchesPython() {
        assertSampleCount(indonesian, 15)
        checkAll(indonesian) { s ->
            val expected = s.getString("clean_text")
            val actual = TextPreprocessor.cleanIndonesianText(s.getString("input"))
            if (expected == actual) null else "expected='$expected'\n      actual='$actual'"
        }
    }

    @Test
    fun indonesian_wordFeatures_matchSklearnAnalyzer() {
        checkAll(indonesian) { s ->
            diffCounts(
                s.getJSONObject("word_counts"),
                TextPreprocessor.indonesianWordCounts(s.getString("clean_text"))
            )
        }
    }

    @Test
    fun indonesian_tfidfVectors_matchPython() {
        checkAll(indonesian) { s ->
            val cleaned = TextPreprocessor.cleanIndonesianText(s.getString("input"))
            val vec = TextPreprocessor.tfidfVector(
                TextPreprocessor.indonesianWordCounts(cleaned),
                classifier.idVocabulary,
                classifier.idIdf
            )
            diffSparse("tfidf", s.getJSONObject("tfidf"), vec)
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun assertSampleCount(samples: JSONArray, expected: Int) {
        assertTrue(
            "Golden fixture harus berisi $expected sampel, ada ${samples.length()}",
            samples.length() == expected
        )
    }

    /** Jalankan [check] untuk setiap sampel; gagal sekali dengan daftar semua selisih. */
    private fun checkAll(samples: JSONArray, check: (JSONObject) -> String?) {
        val failures = mutableListOf<String>()
        for (i in 0 until samples.length()) {
            val sample = samples.getJSONObject(i)
            check(sample)?.let {
                failures += "[#$i] '${sample.getString("input").take(50)}'\n      $it"
            }
        }
        assertTrue(
            "${failures.size}/${samples.length()} sampel drift dari Python:\n" +
                failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    private fun diffCounts(expectedJson: JSONObject, actual: Map<String, Int>): String? {
        val expected = HashMap<String, Int>()
        expectedJson.keys().forEach { expected[it] = expectedJson.getInt(it) }
        if (expected == actual) return null

        val missing = (expected.keys - actual.keys).sorted().take(10)
        val extra = (actual.keys - expected.keys).sorted().take(10)
        val wrong = expected.keys.intersect(actual.keys)
            .filter { expected[it] != actual[it] }
            .sorted().take(10)
            .map { "'$it' py=${expected[it]} kt=${actual[it]}" }
        return "missing=${missing.map { "'$it'" }} extra=${extra.map { "'$it'" }} countDiff=$wrong"
    }

    private fun diffSparse(name: String, expectedJson: JSONObject, actual: FloatArray): String? {
        val idx = expectedJson.getJSONArray("indices")
        val vals = expectedJson.getJSONArray("values")
        val expected = HashMap<Int, Double>()
        for (k in 0 until idx.length()) expected[idx.getInt(k)] = vals.getDouble(k)

        val actualNonZero = actual.indices.filter { actual[it] != 0f }.toSet()
        if (actualNonZero != expected.keys) {
            return "$name: indeks non-zero beda (py=${expected.size}, kt=${actualNonZero.size}, " +
                "hanya-py=${(expected.keys - actualNonZero).take(5)}, " +
                "hanya-kt=${(actualNonZero - expected.keys).take(5)})"
        }
        val worst = expected.maxOfOrNull { (i, v) -> abs(actual[i] - v) } ?: 0.0
        return if (worst <= TFIDF_TOLERANCE) null else "$name: max |diff| = $worst > $TFIDF_TOLERANCE"
    }
}
