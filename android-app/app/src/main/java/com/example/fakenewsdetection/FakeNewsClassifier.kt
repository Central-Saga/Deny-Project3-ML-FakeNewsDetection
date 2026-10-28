package com.example.fakenewsdetection

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class FakeNewsClassifier internal constructor(
    private val assetReader: (String) -> String,
    private val onnxScorerProvider: () -> OnnxSvmScorer
) : AutoCloseable {

    constructor(context: Context) : this(
        assetReader = { fileName ->
            context.assets
                .open(fileName)
                .bufferedReader()
                .use { it.readText() }
        },
        onnxScorerProvider = { OnnxSvmScorer(context.applicationContext) }
    )

    enum class ModelLanguage {
        ENGLISH,
        INDONESIAN
    }

    data class PredictionResult(
        val label: Int,
        val score: Double,
        val language: ModelLanguage
    )

    companion object {

        // Label yang dipakai aplikasi
        const val LABEL_UNCERTAIN = -1
        const val LABEL_FAKE = 0
        const val LABEL_REAL = 1

        private const val MODEL_FILE_EN =
            "fake_news_model_en_v13.json"

        private const val MODEL_FILE_ID =
            "fake_news_model_id_political_v3.json"
    }

    // =========================================================
    // ENGLISH MODEL
    // =========================================================

    internal val enWordVocabulary = HashMap<String, Int>()

    internal val enWordStopWords = HashSet<String>()

    internal lateinit var enWordIdf: DoubleArray

    internal val enCharVocabulary = HashMap<String, Int>()

    internal lateinit var enCharIdf: DoubleArray

    internal lateinit var enClasses: IntArray

    internal var enUncertainThreshold = 0.20

    // =========================================================
    // INDONESIAN POLITICAL v3
    // =========================================================

    internal val idVocabulary = HashMap<String, Int>()

    internal lateinit var idIdf: DoubleArray
    internal lateinit var idClasses: IntArray

    internal var idThreshold = 0.10

    // =========================================================
    // PREPROCESSING
    // Seluruh regex, cleaning, tokenisasi, dan TF-IDF berada di
    // TextPreprocessor (objek murni tanpa Context) agar bisa diuji
    // di JVM dan dikunci paritasnya dengan pipeline Python.
    // =========================================================

    // =========================================================
    // AUTO LANGUAGE DETECTION
    // =========================================================

    private val languageTokenRegex = Regex("""[\p{L}]{2,}""")

    private val indonesianHints =
        setOf(
            "yang",
            "dan",
            "dari",
            "untuk",
            "dengan",
            "pada",
            "adalah",
            "ini",
            "itu",
            "tidak",
            "akan",
            "dalam",
            "sebagai",
            "oleh",
            "karena",
            "juga",
            "telah",
            "sudah",
            "atau",
            "kepada",
            "tersebut",
            "mereka",
            "menjadi",
            "dilakukan",
            "pemerintah",
            "presiden",
            "masyarakat",
            "negara",
            "berita",
            "menteri",
            "indonesia"
        )

    private val englishHints =
        setOf(
            "the",
            "and",
            "of",
            "to",
            "in",
            "is",
            "that",
            "for",
            "on",
            "with",
            "as",
            "was",
            "are",
            "by",
            "from",
            "at",
            "this",
            "has",
            "have",
            "will",
            "been",
            "were",
            "government",
            "president",
            "people",
            "news",
            "country",
            "minister",
            "officials",
            "according"
        )

    // =========================================================
    // INIT
    // =========================================================

    // Dibuka hanya pada prediksi pertama agar startup tidak semakin berat.
    // close() perlu dipanggil saat classifier sudah tidak digunakan.
    private val onnxScorerDelegate = lazy {
        onnxScorerProvider()
    }

    private val onnxScorer by onnxScorerDelegate

    init {
        loadEnglishModel()
        loadIndonesianModel()
    }

    override fun close() {
        if (onnxScorerDelegate.isInitialized()) {
            onnxScorer.close()
        }
    }

    // =========================================================
    // LOAD ENGLISH MODEL
    // =========================================================

    private fun loadEnglishModel() {

        val root = JSONObject(assetReader(MODEL_FILE_EN))

        enUncertainThreshold = root.getDouble("uncertain_threshold")

        // WORD
        val wordObject = root.getJSONObject("word")

        loadVocabulary(wordObject.getJSONObject("vocabulary"), enWordVocabulary)

        enWordIdf = jsonToDoubleArray(wordObject.getJSONArray("idf"))

        val stopWordsJson = wordObject.getJSONArray("stop_words")

        for (i in 0 until stopWordsJson.length()) {
            enWordStopWords.add(stopWordsJson.getString(i))
        }

        // CHAR
        val charObject = root.getJSONObject("char")

        loadVocabulary(charObject.getJSONObject("vocabulary"), enCharVocabulary)

        enCharIdf = jsonToDoubleArray(charObject.getJSONArray("idf"))

        // SVM
        val classesJson = root.getJSONObject("svm").getJSONArray("classes")

        enClasses = IntArray(classesJson.length()) { index ->
            classesJson.getInt(index)
        }

        require(enWordVocabulary.size == enWordIdf.size)

        require(enCharVocabulary.size == enCharIdf.size)

        require(enClasses.size >= 2)
    }

    // =========================================================
    // LOAD INDONESIAN MODEL
    // =========================================================

    private fun loadIndonesianModel() {

        val root = JSONObject(assetReader(MODEL_FILE_ID))

        loadVocabulary(root.getJSONObject("vocabulary"), idVocabulary)

        idIdf = jsonToDoubleArray(root.getJSONArray("idf"))

        idThreshold = root.getDouble("threshold")

        val classesJson = root.getJSONArray("classes")

        idClasses = IntArray(classesJson.length()) { index ->
            classesJson.getInt(index)
        }

        require(idVocabulary.size == idIdf.size)

        require(idClasses.size == 2)

        require(idClasses[0] == 0 && idClasses[1] == 1)
    }

    // =========================================================
    // JSON HELPERS
    // =========================================================

    private fun loadVocabulary(
        json: JSONObject,
        destination: HashMap<String, Int>
    ) {
        val keys = json.keys()

        while (keys.hasNext()) {
            val feature = keys.next()
            destination[feature] = json.getInt(feature)
        }
    }

    private fun jsonToDoubleArray(json: JSONArray): DoubleArray {
        return DoubleArray(json.length()) { index ->
            json.getDouble(index)
        }
    }

    // =========================================================
    // LANGUAGE DETECTION
    // =========================================================

    internal fun detectLanguage(
        title: String,
        content: String
    ): ModelLanguage {

        val rawText = "$title $content".lowercase(Locale.ROOT)

        val tokens = languageTokenRegex
            .findAll(rawText)
            .map { it.value }
            .toList()

        var indonesiaScore = 0
        var englishScore = 0

        for (token in tokens) {
            if (token in indonesianHints) {
                indonesiaScore++
            }
            if (token in englishHints) {
                englishScore++
            }
        }

        // Jika seri, default ke Indonesia karena
        // aplikasi utamanya ditujukan untuk pengguna Indonesia.
        return if (englishScore > indonesiaScore) {
            ModelLanguage.ENGLISH
        } else {
            ModelLanguage.INDONESIAN
        }
    }

    // =========================================================
    // PREPROCESSING WRAPPERS (delegasi ke TextPreprocessor)
    // =========================================================

    // English v1.3:
    // punctuation dihapus, bukan diganti spasi.
    internal fun cleanEnglishText(input: String): String =
        TextPreprocessor.cleanEnglishText(input)

    // INDONESIAN v3 CLEANING
    // Sama dengan sanitize_v3() Python
    internal fun cleanIndonesianText(input: String): String =
        TextPreprocessor.cleanIndonesianText(input)

    internal fun createEnglishWordCounts(text: String): HashMap<String, Int> =
        TextPreprocessor.englishWordCounts(text, enWordStopWords)

    internal fun createIndonesianWordCounts(text: String): HashMap<String, Int> =
        TextPreprocessor.indonesianWordCounts(text)

    internal fun createUnigramBigramCounts(tokens: List<String>): HashMap<String, Int> =
        TextPreprocessor.unigramBigramCounts(tokens)

    internal fun createEnglishCharCounts(text: String): HashMap<String, Int> =
        TextPreprocessor.englishCharCounts(text)

    // =========================================================
    // TF-IDF (ONNX LINEAR SVM SCORING)
    // =========================================================

    // Preprocessing lama tetap dipakai; hanya skor Linear SVM yang
    // dipindahkan ke ONNX Runtime. Setiap indeks harus sesuai vocabulary.
    internal fun createTfidfVector(
        counts: HashMap<String, Int>,
        vocabulary: HashMap<String, Int>,
        idf: DoubleArray
    ): FloatArray = TextPreprocessor.tfidfVector(counts, vocabulary, idf)

    // =========================================================
    // AUTO PREDICTION
    // =========================================================

    fun predictAuto(
        title: String,
        content: String
    ): PredictionResult {

        return when (detectLanguage(title, content)) {
            ModelLanguage.INDONESIAN -> predictIndonesian(title, content)
            ModelLanguage.ENGLISH -> predictEnglish(title, content)
        }
    }

    // Agar kode lama yang memanggil predict()
    // tetap bisa berjalan, sekarang diarahkan
    // ke auto language.
    fun predict(
        title: String,
        content: String
    ): PredictionResult {
        return predictAuto(title, content)
    }

    // =========================================================
    // ENGLISH
    // =========================================================

    fun predictEnglish(
        title: String,
        content: String
    ): PredictionResult {

        val cleaned = cleanEnglishText("$title $content")

        val wordTfidf = createTfidfVector(
            createEnglishWordCounts(cleaned),
            enWordVocabulary,
            enWordIdf
        )

        val charTfidf = createTfidfVector(
            createEnglishCharCounts(cleaned),
            enCharVocabulary,
            enCharIdf
        )

        // ONNX sudah menyimpan bobot dan intercept English v1.3.
        val score = onnxScorer.scoreEnglish(wordTfidf, charTfidf)

        return PredictionResult(
            label = TextPreprocessor.decideEnglishLabel(score, enUncertainThreshold, enClasses),
            score = score,
            language = ModelLanguage.ENGLISH
        )
    }

    // =========================================================
    // INDONESIAN POLITICAL v3
    //
    // RAW MODEL:
    // 0 = REAL
    // 1 = FAKE
    //
    // APP:
    // 0 = FAKE
    // 1 = REAL
    // =========================================================

    fun predictIndonesian(
        title: String,
        content: String
    ): PredictionResult {

        val cleaned = cleanIndonesianText("$title $content")

        val tfidf = createTfidfVector(
            createIndonesianWordCounts(cleaned),
            idVocabulary,
            idIdf
        )

        // ONNX sudah menyimpan bobot dan intercept Political v3.
        val score = onnxScorer.scoreIndonesian(tfidf)

        return PredictionResult(
            label = TextPreprocessor.decideIndonesianLabel(score, idThreshold, idClasses),
            score = score,
            language = ModelLanguage.INDONESIAN
        )
    }
}
