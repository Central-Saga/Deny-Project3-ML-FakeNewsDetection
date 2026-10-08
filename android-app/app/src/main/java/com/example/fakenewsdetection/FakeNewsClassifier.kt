package com.example.fakenewsdetection

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.ln
import kotlin.math.sqrt

class FakeNewsClassifier(
    private val context: Context
) {

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

    private val enWordVocabulary =
        HashMap<String, Int>()

    private val enWordStopWords =
        HashSet<String>()

    private lateinit var enWordIdf: DoubleArray
    private lateinit var enWordCoef: DoubleArray

    private val enCharVocabulary =
        HashMap<String, Int>()

    private lateinit var enCharIdf: DoubleArray
    private lateinit var enCharCoef: DoubleArray

    private lateinit var enClasses: IntArray

    private var enIntercept = 0.0
    private var enUncertainThreshold = 0.20

    // =========================================================
    // INDONESIAN POLITICAL v3
    // =========================================================

    private val idVocabulary =
        HashMap<String, Int>()

    private lateinit var idIdf: DoubleArray
    private lateinit var idCoef: DoubleArray
    private lateinit var idClasses: IntArray

    private var idIntercept = 0.0
    private var idThreshold = 0.10

    // =========================================================
    // COMMON REGEX
    // =========================================================

    private val whitespaceRegex =
        Regex("""\s+""")

    // =========================================================
    // ENGLISH PREPROCESSING
    // =========================================================

    private val enUrlRegex =
        Regex("""http\S+|www\S+""")

    private val enHtmlRegex =
        Regex("""<.*?>""")

    private val enReutersRegex =
        Regex("""\breuters\b""")

    private val enNumberRegex =
        Regex("""\p{Nd}+""")

    private val enWordTokenRegex =
        Regex("""[\p{L}\p{N}_]{2,}""")

    private val punctuationChars =
        "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toSet()

    // =========================================================
    // INDONESIAN PREPROCESSING
    // =========================================================

    private val idUrlRegex =
        Regex(
            """https?://\S+|www\.\S+""",
            RegexOption.IGNORE_CASE
        )

    private val idEmailRegex =
        Regex(
            """\b[\w\.-]+@[\w\.-]+\.\w+\b""",
            RegexOption.IGNORE_CASE
        )

    private val idDomainRegex =
        Regex(
            """\b[\w\-]+\.(?:com|id|net|org|co\.id)\b""",
            RegexOption.IGNORE_CASE
        )

    private val idTokenRegex =
        Regex("""[a-z0-9]{2,}""")

    private val idRemovePatterns =
        listOf(
            Regex("""\breferensi\b"""),
            Regex("""\bpenjelasan\b"""),
            Regex("""\bbaca juga\b"""),
            Regex("""\bcek fakta\b"""),
            Regex("""\btim cek fakta\b"""),
            Regex("""\bhasil penelusuran\b"""),
            Regex("""\bberdasarkan hasil penelusuran\b"""),
            Regex("""\bsetelah ditelusuri\b"""),
            Regex("""\bpenelusuran diketahui\b"""),
            Regex("""\bdengan demikian\b"""),
            Regex("""\bdemikian klaim\b"""),
            Regex("""\bdalam kategori\b"""),
            Regex("""\bmasuk ke dalam kategori\b"""),
            Regex("""\bnarasi\b"""),
            Regex("""\bklaim\b"""),
            Regex("""\bhoax\b"""),
            Regex("""\bhoaks\b"""),
            Regex("""\bfoto hoaks\b"""),
            Regex("""\bturnbackhoax\b"""),
            Regex("""\boleh akun\b"""),
            Regex("""\bakun facebook\b"""),
            Regex("""\bakun twitter\b"""),
            Regex("""\bakun instagram\b"""),
            Regex("""\bdibagikan oleh\b"""),
            Regex("""\beditor\b"""),
            Regex("""\bpenulis\b"""),
            Regex("""\breporter\b"""),
            Regex("""\bsumber\b""")
        )

    private val idNamePatterns =
        listOf(
            Regex("""\bbentang febrylian\b"""),
            Regex("""\bfebrylian\b"""),
            Regex("""\bjulnis firmansyah\b"""),
            Regex("""\bjulnis\b"""),
            Regex("""\bfirmansyah\b""")
        )

    // =========================================================
    // AUTO LANGUAGE DETECTION
    // =========================================================

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

    init {
        loadEnglishModel()
        loadIndonesianModel()
    }

    // =========================================================
    // LOAD ENGLISH MODEL
    // =========================================================

    private fun loadEnglishModel() {

        val jsonString =
            context.assets
                .open(MODEL_FILE_EN)
                .bufferedReader()
                .use { it.readText() }

        val root =
            JSONObject(jsonString)

        enUncertainThreshold =
            root.getDouble(
                "uncertain_threshold"
            )

        // WORD
        val wordObject =
            root.getJSONObject("word")

        loadVocabulary(
            wordObject.getJSONObject(
                "vocabulary"
            ),
            enWordVocabulary
        )

        enWordIdf =
            jsonToDoubleArray(
                wordObject.getJSONArray(
                    "idf"
                )
            )

        enWordCoef =
            jsonToDoubleArray(
                wordObject.getJSONArray(
                    "coef"
                )
            )

        val stopWordsJson =
            wordObject.getJSONArray(
                "stop_words"
            )

        for (
        i in 0 until stopWordsJson.length()
        ) {
            enWordStopWords.add(
                stopWordsJson.getString(i)
            )
        }

        // CHAR
        val charObject =
            root.getJSONObject("char")

        loadVocabulary(
            charObject.getJSONObject(
                "vocabulary"
            ),
            enCharVocabulary
        )

        enCharIdf =
            jsonToDoubleArray(
                charObject.getJSONArray(
                    "idf"
                )
            )

        enCharCoef =
            jsonToDoubleArray(
                charObject.getJSONArray(
                    "coef"
                )
            )

        // SVM
        val svmObject =
            root.getJSONObject("svm")

        enIntercept =
            svmObject.getDouble(
                "intercept"
            )

        val classesJson =
            svmObject.getJSONArray(
                "classes"
            )

        enClasses =
            IntArray(
                classesJson.length()
            ) { index ->
                classesJson.getInt(index)
            }

        require(
            enWordVocabulary.size ==
                    enWordIdf.size
        )

        require(
            enWordIdf.size ==
                    enWordCoef.size
        )

        require(
            enCharVocabulary.size ==
                    enCharIdf.size
        )

        require(
            enCharIdf.size ==
                    enCharCoef.size
        )

        require(
            enClasses.size >= 2
        )
    }

    // =========================================================
    // LOAD INDONESIAN MODEL
    // =========================================================

    private fun loadIndonesianModel() {

        val jsonString =
            context.assets
                .open(MODEL_FILE_ID)
                .bufferedReader()
                .use { it.readText() }

        val root =
            JSONObject(jsonString)

        loadVocabulary(
            root.getJSONObject(
                "vocabulary"
            ),
            idVocabulary
        )

        idIdf =
            jsonToDoubleArray(
                root.getJSONArray(
                    "idf"
                )
            )

        idCoef =
            jsonToDoubleArray(
                root.getJSONArray(
                    "coef"
                )
            )

        idIntercept =
            root.getDouble(
                "intercept"
            )

        idThreshold =
            root.getDouble(
                "threshold"
            )

        val classesJson =
            root.getJSONArray(
                "classes"
            )

        idClasses =
            IntArray(
                classesJson.length()
            ) { index ->
                classesJson.getInt(index)
            }

        require(
            idVocabulary.size ==
                    idIdf.size
        )

        require(
            idIdf.size ==
                    idCoef.size
        )

        require(
            idClasses.size == 2
        )

        require(
            idClasses[0] == 0 &&
                    idClasses[1] == 1
        )
    }

    // =========================================================
    // JSON HELPERS
    // =========================================================

    private fun loadVocabulary(
        json: JSONObject,
        destination: HashMap<String, Int>
    ) {

        val keys =
            json.keys()

        while (
            keys.hasNext()
        ) {

            val feature =
                keys.next()

            destination[feature] =
                json.getInt(feature)
        }
    }

    private fun jsonToDoubleArray(
        json: JSONArray
    ): DoubleArray {

        return DoubleArray(
            json.length()
        ) { index ->

            json.getDouble(index)
        }
    }

    // =========================================================
    // LANGUAGE DETECTION
    // =========================================================

    private fun detectLanguage(
        title: String,
        content: String
    ): ModelLanguage {

        val rawText =
            "$title $content"
                .lowercase(
                    Locale.ROOT
                )

        val tokens =
            Regex("""[\p{L}]{2,}""")
                .findAll(rawText)
                .map {
                    it.value
                }
                .toList()

        var indonesiaScore = 0
        var englishScore = 0

        for (
        token in tokens
        ) {

            if (
                token in indonesianHints
            ) {
                indonesiaScore++
            }

            if (
                token in englishHints
            ) {
                englishScore++
            }
        }

        // Jika seri, default ke Indonesia karena
        // aplikasi utamanya ditujukan untuk pengguna Indonesia.
        return if (
            englishScore > indonesiaScore
        ) {

            ModelLanguage.ENGLISH

        } else {

            ModelLanguage.INDONESIAN
        }
    }

    // =========================================================
    // ENGLISH CLEANING
    // =========================================================

    private fun cleanEnglishText(
        input: String
    ): String {

        var text =
            input.lowercase(
                Locale.ROOT
            )

        text =
            text.replace(
                enUrlRegex,
                " "
            )

        text =
            text.replace(
                enHtmlRegex,
                " "
            )

        text =
            text.replace(
                enReutersRegex,
                " "
            )

        text =
            text.replace(
                enNumberRegex,
                " "
            )

        // English v1.3:
        // punctuation dihapus, bukan diganti spasi.
        text =
            text.filter { char ->
                char !in punctuationChars
            }

        return text
            .replace(
                whitespaceRegex,
                " "
            )
            .trim()
    }

    // =========================================================
    // INDONESIAN v3 CLEANING
    // Sama dengan sanitize_v3() Python
    // =========================================================

    private fun cleanIndonesianText(
        input: String
    ): String {

        var text =
            input.lowercase(
                Locale.ROOT
            )

        text =
            text.replace(
                idUrlRegex,
                " "
            )

        text =
            text.replace(
                idEmailRegex,
                " "
            )

        text =
            text.replace(
                idDomainRegex,
                " "
            )

        for (
        pattern in idRemovePatterns
        ) {

            text =
                text.replace(
                    pattern,
                    " "
                )
        }

        for (
        pattern in idNamePatterns
        ) {

            text =
                text.replace(
                    pattern,
                    " "
                )
        }

        text =
            text.replace(
                Regex(
                    """[^a-z0-9\s]"""
                ),
                " "
            )

        return text
            .replace(
                whitespaceRegex,
                " "
            )
            .trim()
    }

    // =========================================================
    // ENGLISH WORD COUNTS
    // =========================================================

    private fun createEnglishWordCounts(
        text: String
    ): HashMap<String, Int> {

        val tokens =
            enWordTokenRegex
                .findAll(text)
                .map {
                    it.value
                }
                .filter {
                    it !in enWordStopWords
                }
                .toList()

        return createUnigramBigramCounts(
            tokens
        )
    }

    // =========================================================
    // INDONESIAN WORD COUNTS
    // =========================================================

    private fun createIndonesianWordCounts(
        text: String
    ): HashMap<String, Int> {

        val tokens =
            idTokenRegex
                .findAll(text)
                .map {
                    it.value
                }
                .toList()

        return createUnigramBigramCounts(
            tokens
        )
    }

    // =========================================================
    // UNIGRAM + BIGRAM
    // =========================================================

    private fun createUnigramBigramCounts(
        tokens: List<String>
    ): HashMap<String, Int> {

        val counts =
            HashMap<String, Int>()

        for (
        token in tokens
        ) {

            incrementCount(
                counts,
                token
            )
        }

        if (
            tokens.size >= 2
        ) {

            for (
            i in 0 until
                    tokens.size - 1
            ) {

                val bigram =
                    "${tokens[i]} ${tokens[i + 1]}"

                incrementCount(
                    counts,
                    bigram
                )
            }
        }

        return counts
    }

    // =========================================================
    // ENGLISH CHAR_WB 3-5
    // =========================================================

    private fun createEnglishCharCounts(
        text: String
    ): HashMap<String, Int> {

        val counts =
            HashMap<String, Int>()

        if (
            text.isBlank()
        ) {
            return counts
        }

        val normalizedText =
            text.replace(
                whitespaceRegex,
                " "
            )

        val words =
            normalizedText.split(" ")

        for (
        word in words
        ) {

            if (
                word.isEmpty()
            ) {
                continue
            }

            val padded =
                " $word "

            val wordLength =
                padded.length

            for (
            n in 3..5
            ) {

                var offset = 0

                val firstEnd =
                    minOf(
                        offset + n,
                        wordLength
                    )

                val firstGram =
                    padded.substring(
                        offset,
                        firstEnd
                    )

                incrementCount(
                    counts,
                    firstGram
                )

                while (
                    offset + n <
                    wordLength
                ) {

                    offset++

                    val end =
                        minOf(
                            offset + n,
                            wordLength
                        )

                    val gram =
                        padded.substring(
                            offset,
                            end
                        )

                    incrementCount(
                        counts,
                        gram
                    )
                }

                if (
                    offset == 0
                ) {
                    break
                }
            }
        }

        return counts
    }

    private fun incrementCount(
        counts: HashMap<String, Int>,
        feature: String
    ) {

        counts[feature] =
            (
                    counts[feature]
                        ?: 0
                    ) + 1
    }

    // =========================================================
    // TF-IDF + LINEAR SVM
    // =========================================================

    private fun calculateBranchScore(
        counts: HashMap<String, Int>,
        vocabulary: HashMap<String, Int>,
        idf: DoubleArray,
        coefficients: DoubleArray
    ): Double {

        var normSquared = 0.0
        var weightedSum = 0.0

        for (
        (feature, count) in counts
        ) {

            val index =
                vocabulary[feature]
                    ?: continue

            val tf =
                1.0 +
                        ln(
                            count.toDouble()
                        )

            val tfidf =
                tf *
                        idf[index]

            normSquared +=
                tfidf *
                        tfidf

            weightedSum +=
                tfidf *
                        coefficients[index]
        }

        if (
            normSquared <= 0.0
        ) {
            return 0.0
        }

        return weightedSum /
                sqrt(normSquared)
    }

    // =========================================================
    // AUTO PREDICTION
    // =========================================================

    fun predictAuto(
        title: String,
        content: String
    ): PredictionResult {

        return when (
            detectLanguage(
                title,
                content
            )
        ) {

            ModelLanguage.INDONESIAN ->
                predictIndonesian(
                    title,
                    content
                )

            ModelLanguage.ENGLISH ->
                predictEnglish(
                    title,
                    content
                )
        }
    }

    // Agar kode lama yang memanggil predict()
    // tetap bisa berjalan, sekarang diarahkan
    // ke auto language.
    fun predict(
        title: String,
        content: String
    ): PredictionResult {

        return predictAuto(
            title,
            content
        )
    }

    // =========================================================
    // ENGLISH
    // =========================================================

    fun predictEnglish(
        title: String,
        content: String
    ): PredictionResult {

        val combinedText =
            "$title $content"

        val cleaned =
            cleanEnglishText(
                combinedText
            )

        val wordScore =
            calculateBranchScore(
                createEnglishWordCounts(
                    cleaned
                ),
                enWordVocabulary,
                enWordIdf,
                enWordCoef
            )

        val charScore =
            calculateBranchScore(
                createEnglishCharCounts(
                    cleaned
                ),
                enCharVocabulary,
                enCharIdf,
                enCharCoef
            )

        val score =
            wordScore +
                    charScore +
                    enIntercept

        val prediction =
            when {

                score <=
                        -enUncertainThreshold -> {

                    enClasses[0]
                }

                score >=
                        enUncertainThreshold -> {

                    enClasses[1]
                }

                else -> {

                    LABEL_UNCERTAIN
                }
            }

        return PredictionResult(
            label =
                prediction,

            score =
                score,

            language =
                ModelLanguage.ENGLISH
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

        val combinedText =
            "$title $content"

        val cleaned =
            cleanIndonesianText(
                combinedText
            )

        val featureScore =
            calculateBranchScore(
                createIndonesianWordCounts(
                    cleaned
                ),
                idVocabulary,
                idIdf,
                idCoef
            )

        val score =
            featureScore +
                    idIntercept

        val rawClass =
            if (
                score >= idThreshold
            ) {

                idClasses[1]

            } else {

                idClasses[0]
            }

        val appLabel =
            when (
                rawClass
            ) {

                0 ->
                    LABEL_REAL

                1 ->
                    LABEL_FAKE

                else ->
                    LABEL_UNCERTAIN
            }

        return PredictionResult(
            label =
                appLabel,

            score =
                score,

            language =
                ModelLanguage.INDONESIAN
        )
    }
}