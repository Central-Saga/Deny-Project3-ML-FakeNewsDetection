package com.example.fakenewsdetection

import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.abs

class FakeNewsClassifierTest {

    private lateinit var classifier: FakeNewsClassifier

    companion object {
        val PARITY_SAMPLES_EN = listOf(
            "Breaking: President signs new trade deal with Europe" to 1,
            "Aliens land in Washington DC, government confirms" to 0,
            "Federal Reserve raises interest rates by 0.25 percent" to 1,
            "Doctors say drinking bleach cures all diseases" to 0,
            "Congress passes bipartisan infrastructure bill" to 1,
            "Obama secretly born on Mars, leaked documents show" to 0,
            "Scientists discover new species of deep-sea fish" to 1,
            "Vaccines contain microchips that track your location" to 0,
            "Stock markets rally on positive jobs report" to 1,
            "Pope Francis reveals he is an alien from another galaxy" to 0,
            "WHO issues new guidelines on COVID-19 prevention" to 1,
            "Hillary Clinton runs secret pizza parlor crime ring" to 0,
            "NASA successfully launches new weather satellite" to 1,
            "5G towers are spreading coronavirus, experts warn" to 0,
            "Supreme Court rules on landmark voting rights case" to 1,
            "George Soros controls entire global banking system" to 0,
            "European Union agrees on new climate change targets" to 1,
            "Bill Gates plans to depopulate Earth with vaccines" to 0,
            "United Nations holds emergency session on famine crisis" to 1,
            "Chemtrails are government mind control program revealed" to 0
        )

        val PARITY_SAMPLES_ID = listOf(
            "Presiden Joko Widodo meresmikan jalan tol baru di Jawa Tengah" to 0, // REAL in raw model
            "Beredar kabar pemerintah membagikan bantuan uang tunai 100 juta rupiah lewat whatsapp" to 1, // FAKE
            "Kementerian Keuangan mengumumkan realisasi anggaran pendapatan belanja negara" to 0,
            "Minum air rebusan daun cabai dapat menyembuhkan stroke secara instan" to 1,
            "Gubernur Bank Indonesia menyampaikan stabilitas nilai tukar rupiah tetap terjaga" to 0,
            "Foto viral memperlihatkan menteri ditangkap KPK di bandara soekarno hatta" to 1,
            "Kementerian Kesehatan mengimbau masyarakat waspada terhadap perubahan cuaca" to 0,
            "Beredar klaim pendaftaran bansos khusus pemilu melalui link telegram" to 1,
            "DPR menyetujui rancangan undang-undang dalam rapat paripurna hari ini" to 0,
            "Masyarakat diminta waspada modus penipuan berkedok undian berhadiah bank" to 1
        )

        fun findAssetFile(fileName: String): File {
            val candidates = listOf(
                File("src/main/assets", fileName),
                File("app/src/main/assets", fileName),
                File("../app/src/main/assets", fileName),
                File("../../android-app/app/src/main/assets", fileName),
                File("../android-app/app/src/main/assets", fileName)
            )
            return candidates.firstOrNull { it.exists() }
                ?: throw FileNotFoundException("Asset not found: $fileName in $candidates (cwd=${File(".").absolutePath})")
        }

        fun findFixtureFile(): File? {
            val candidates = listOf(
                File("../../src/parity_fixtures.json"),
                File("../src/parity_fixtures.json"),
                File("src/parity_fixtures.json"),
                File("../../data/parity_fixtures.json"),
                File("../data/parity_fixtures.json"),
                File("data/parity_fixtures.json")
            )
            return candidates.firstOrNull { it.exists() }
        }
    }

    @Before
    fun setUp() {
        classifier = FakeNewsClassifier(
            assetReader = { fileName ->
                findAssetFile(fileName)
                    .inputStream()
                    .use { FakeNewsClassifier.readAssetText(it) }
            },
            onnxScorerProvider = {
                OnnxSvmScorer(
                    modelBytesEn = findAssetFile("fake_news_model_en_v13.onnx").readBytes(),
                    modelBytesId = findAssetFile("fake_news_model_id_political_v3.onnx").readBytes()
                )
            }
        )
    }

    @After
    fun tearDown() {
        classifier.close()
    }

    // =========================================================
    // 1. PREPROCESSING TESTS — ENGLISH
    // =========================================================

    @Test
    fun testCleanEnglishText_removesUrls() {
        val input = "Read this http://example.com/breaking or https://news.org/update now"
        val cleaned = classifier.cleanEnglishText(input)
        assertEquals("read this or now", cleaned)
    }

    @Test
    fun testCleanEnglishText_removesHtmlTags() {
        val input = "<h1>Breaking News</h1><p>Major announcement today.</p>"
        val cleaned = classifier.cleanEnglishText(input)
        assertEquals("breaking news major announcement today", cleaned)
    }

    @Test
    fun testCleanEnglishText_removesReutersPattern() {
        val input = "WASHINGTON (Reuters) - The White House announced today"
        val cleaned = classifier.cleanEnglishText(input)
        assertEquals("washington the white house announced today", cleaned)
    }

    @Test
    fun testCleanEnglishText_removesNumbers() {
        val input = "Inflation dropped 25 percent in 2026 across 50 states"
        val cleaned = classifier.cleanEnglishText(input)
        assertEquals("inflation dropped percent in across states", cleaned)
    }

    @Test
    fun testCleanEnglishText_deletesPunctuation() {
        // In EN v1.3: punctuation is deleted, not replaced with space
        val input = "Hello, world! It's a test: \"quoted\" text & more."
        val cleaned = classifier.cleanEnglishText(input)
        assertEquals("hello world its a test quoted text more", cleaned)
    }

    @Test
    fun testCleanEnglishText_normalizesWhitespaceAndTrims() {
        val input = "   Multiple   spaces \t and \n newlines   "
        val cleaned = classifier.cleanEnglishText(input)
        assertEquals("multiple spaces and newlines", cleaned)
    }

    // =========================================================
    // 2. WORD & CHAR COUNTS TESTS — ENGLISH
    // =========================================================

    @Test
    fun testCreateEnglishWordCounts_unigramsAndBigrams() {
        val text = "global market rally"
        val counts = classifier.createEnglishWordCounts(text)

        // Tokens: "global", "market", "rally" (assuming none in stop_words)
        assertTrue("Must contain unigram 'global'", counts.containsKey("global"))
        assertTrue("Must contain unigram 'market'", counts.containsKey("market"))
        assertTrue("Must contain unigram 'rally'", counts.containsKey("rally"))
        assertTrue("Must contain bigram 'global market'", counts.containsKey("global market"))
        assertTrue("Must contain bigram 'market rally'", counts.containsKey("market rally"))
        assertEquals(1, counts["global"])
        assertEquals(1, counts["global market"])
    }

    @Test
    fun testCreateEnglishWordCounts_filtersStopWords() {
        val text = "the market is rally"
        val counts = classifier.createEnglishWordCounts(text)
        assertFalse("Should filter stop word 'the'", counts.containsKey("the"))
        assertFalse("Should filter stop word 'is'", counts.containsKey("is"))
    }

    @Test
    fun testCreateEnglishCharCounts_charWbNgrams() {
        val text = "news"
        val counts = classifier.createEnglishCharCounts(text)
        // padded word is " news "
        // 3-grams: " ne", "new", "ews", "ws "
        assertTrue(counts.containsKey(" ne"))
        assertTrue(counts.containsKey("new"))
        assertTrue(counts.containsKey("ews"))
        assertTrue(counts.containsKey("ws "))
        // 4-grams: " new", "news", "ews "
        assertTrue(counts.containsKey(" new"))
        assertTrue(counts.containsKey("news"))
        assertTrue(counts.containsKey("ews "))
        // 5-grams: " news", "news "
        assertTrue(counts.containsKey(" news"))
        assertTrue(counts.containsKey("news "))
    }

    // =========================================================
    // 3. PREPROCESSING TESTS — INDONESIAN
    // =========================================================

    @Test
    fun testCleanIndonesianText_removesUrlsEmailsDomains() {
        val input = "Simak di https://turnbackhoax.id atau hubungi redaksi@detik.com di portal kompas.com"
        val cleaned = classifier.cleanIndonesianText(input)
        assertFalse("Should not contain turnbackhoax", cleaned.contains("turnbackhoax"))
        assertFalse("Should not contain url", cleaned.contains("https"))
        assertFalse("Should not contain email", cleaned.contains("@"))
        assertFalse("Should not contain domain", cleaned.contains(".com"))
    }

    @Test
    fun testCleanIndonesianText_removesFactCheckBoilerplate() {
        val input = "Hasil penelusuran cek fakta membuktikan bahwa klaim tersebut adalah hoaks"
        val cleaned = classifier.cleanIndonesianText(input)
        assertFalse("Should remove 'hasil penelusuran'", cleaned.contains("hasil penelusuran"))
        assertFalse("Should remove 'cek fakta'", cleaned.contains("cek fakta"))
        assertFalse("Should remove 'hoaks'", cleaned.contains("hoaks"))
    }

    @Test
    fun testCreateIndonesianWordCounts_unigramsAndBigrams() {
        val text = "ekonomi indonesia tumbuh"
        val counts = classifier.createIndonesianWordCounts(text)
        assertTrue(counts.containsKey("ekonomi"))
        assertTrue(counts.containsKey("indonesia"))
        assertTrue(counts.containsKey("tumbuh"))
        assertTrue(counts.containsKey("ekonomi indonesia"))
        assertTrue(counts.containsKey("indonesia tumbuh"))
    }

    // =========================================================
    // 4. TF-IDF VECTOR COMPUTATION TESTS
    // =========================================================

    @Test
    fun testCreateTfidfVector_sublinearTfAndL2Norm() {
        val counts = hashMapOf("market" to 2, "rally" to 1)
        val vocab = hashMapOf("market" to 0, "rally" to 1)
        val idf = doubleArrayOf(1.5, 2.0)

        val vec = classifier.createTfidfVector(counts, vocab, idf)
        assertEquals(2, vec.size)

        // Sublinear TF: 1 + ln(count)
        val expectedTf0 = 1.0 + kotlin.math.ln(2.0)
        val expectedTf1 = 1.0 + kotlin.math.ln(1.0)
        val raw0 = expectedTf0 * 1.5
        val raw1 = expectedTf1 * 2.0
        val norm = kotlin.math.sqrt(raw0 * raw0 + raw1 * raw1)

        assertEquals(raw0 / norm, vec[0].toDouble(), 1e-4)
        assertEquals(raw1 / norm, vec[1].toDouble(), 1e-4)

        // Vector length norm check
        val sumSquares = vec.fold(0.0) { acc, v -> acc + v * v }
        assertEquals(1.0, sumSquares, 1e-4)
    }

    // =========================================================
    // 5. LANGUAGE DETECTION TESTS
    // =========================================================

    @Test
    fun testDetectLanguage() {
        val enResult = classifier.detectLanguage(
            "Breaking: Global market rallied today",
            "Officials from the ministry of finance released the statement according to reports."
        )
        assertEquals(FakeNewsClassifier.ModelLanguage.ENGLISH, enResult)

        val idResult = classifier.detectLanguage(
            "Presiden meresmikan proyek infrastruktur nasional",
            "Pemerintah indonesia melalui kementerian terkait menyatakan bahwa pembangunan ini telah selesai."
        )
        assertEquals(FakeNewsClassifier.ModelLanguage.INDONESIAN, idResult)
    }

    // =========================================================
    // 6. END-TO-END PARITY & DETERMINISM TEST (20 SAMPLES EN + 10 ID)
    // =========================================================

    @Test
    fun testParityAgainstCommittedFixture_English() {
        val fixtureFile = findFixtureFile()
        val fixtureJson = if (fixtureFile != null && fixtureFile.exists()) {
            JSONObject(fixtureFile.readText(Charsets.UTF_8))
        } else {
            null
        }

        val fixtureEnArray = fixtureJson?.optJSONArray("english_samples")

        var allPassed = true
        for (i in PARITY_SAMPLES_EN.indices) {
            val (sentence, expectedLabel) = PARITY_SAMPLES_EN[i]
            val result = classifier.predictEnglish(title = "", content = sentence)

            assertNotNull("Prediction result must not be null", result)
            assertFalse("Score must not be NaN", result.score.isNaN())

            if (fixtureEnArray != null && i < fixtureEnArray.length()) {
                val item = fixtureEnArray.getJSONObject(i)
                val expectedScore = item.getDouble("score_kotlin")
                val diff = abs(result.score - expectedScore)
                val pass = diff < 1e-3
                if (!pass) allPassed = false
                assertTrue(
                    "Score mismatch for sample $i ('$sentence'): got ${result.score}, expected $expectedScore, diff=$diff",
                    pass
                )
            }
        }
        assertTrue("All English parity test samples must pass", allPassed)
    }

    @Test
    fun testParityAgainstCommittedFixture_Indonesian() {
        val fixtureFile = findFixtureFile()
        val fixtureJson = if (fixtureFile != null && fixtureFile.exists()) {
            JSONObject(fixtureFile.readText(Charsets.UTF_8))
        } else {
            null
        }

        val fixtureIdArray = fixtureJson?.optJSONArray("indonesian_samples")

        var allPassed = true
        for (i in PARITY_SAMPLES_ID.indices) {
            val (sentence, expectedLabel) = PARITY_SAMPLES_ID[i]
            val result = classifier.predictIndonesian(title = "", content = sentence)

            assertNotNull("Result must not be null", result)
            assertFalse("Score must not be NaN", result.score.isNaN())

            if (fixtureIdArray != null && i < fixtureIdArray.length()) {
                val item = fixtureIdArray.getJSONObject(i)
                val expectedScore = item.getDouble("score_kotlin")
                val diff = abs(result.score - expectedScore)
                val pass = diff < 1e-3
                if (!pass) allPassed = false
                assertTrue(
                    "Score mismatch for ID sample $i: got ${result.score}, expected $expectedScore, diff=$diff",
                    pass
                )
            }
        }
        assertTrue("All Indonesian parity test samples must pass", allPassed)
    }

    @Test
    fun dumpKotlinScoresJson() {
        val root = JSONObject()
        val enArray = JSONArray()
        for (i in PARITY_SAMPLES_EN.indices) {
            val (sentence, expected) = PARITY_SAMPLES_EN[i]
            val pred = classifier.predictEnglish("", sentence)
            val obj = JSONObject()
            obj.put("index", i)
            obj.put("sentence", sentence)
            obj.put("expected_label", expected)
            obj.put("score_kotlin", pred.score)
            obj.put("predicted_label_kotlin", pred.label)
            enArray.put(obj)
        }
        root.put("english_samples", enArray)

        val idArray = JSONArray()
        for (i in PARITY_SAMPLES_ID.indices) {
            val (sentence, expected) = PARITY_SAMPLES_ID[i]
            val pred = classifier.predictIndonesian("", sentence)
            val obj = JSONObject()
            obj.put("index", i)
            obj.put("sentence", sentence)
            obj.put("expected_label", expected)
            obj.put("score_kotlin", pred.score)
            obj.put("predicted_label_kotlin", pred.label)
            idArray.put(obj)
        }
        root.put("indonesian_samples", idArray)

        val jsonStr = root.toString(2)
        println("=== KOTLIN_SCORES_JSON_START ===")
        println(jsonStr)
        println("=== KOTLIN_SCORES_JSON_END ===")

        val targetFiles = listOf(
            File("../../src/parity_fixtures.json"),
            File("../../data/parity_fixtures.json")
        )
        for (target in targetFiles) {
            if (target.parentFile?.exists() == true) {
                target.writeText(jsonStr, Charsets.UTF_8)
            }
        }
    }
}
