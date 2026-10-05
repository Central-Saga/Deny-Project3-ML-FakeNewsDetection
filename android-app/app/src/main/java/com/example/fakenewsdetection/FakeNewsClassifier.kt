package com.example.fakenewsdetection

import android.content.Context
import org.json.JSONObject
import kotlin.math.sqrt

class FakeNewsClassifier(context: Context) {

    data class PredictionResult(
        val label: Int,
        val score: Double
    )

    private val vocabulary = HashMap<String, Int>()
    private val stopWords = HashSet<String>()

    private lateinit var idf: DoubleArray
    private lateinit var coef: DoubleArray
    private lateinit var classes: IntArray

    private var intercept: Double = 0.0

    // Sama dengan Python string.punctuation
    private val punctuationChars =
        """!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~""".toSet()

    init {
        loadModel(context)
    }

    // =========================================================
    // LOAD MODEL JSON
    // =========================================================

    private fun loadModel(context: Context) {

        val jsonString = context.assets
            .open("fake_news_model_data.json")
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(jsonString)

        // =========================
        // Vocabulary
        // =========================

        val vocabularyJson =
            root.getJSONObject("vocabulary")

        val vocabularyKeys =
            vocabularyJson.keys()

        while (vocabularyKeys.hasNext()) {

            val word =
                vocabularyKeys.next()

            vocabulary[word] =
                vocabularyJson.getInt(word)
        }

        // =========================
        // IDF
        // =========================

        val idfJson =
            root.getJSONArray("idf")

        idf = DoubleArray(
            idfJson.length()
        ) { index ->

            idfJson.getDouble(index)
        }

        // =========================
        // Koefisien Linear SVM
        // =========================

        val coefJson =
            root.getJSONArray("coef")

        coef = DoubleArray(
            coefJson.length()
        ) { index ->

            coefJson.getDouble(index)
        }

        // =========================
        // Intercept
        // =========================

        intercept =
            root.getDouble("intercept")

        // =========================
        // Classes
        // =========================

        val classesJson =
            root.getJSONArray("classes")

        classes = IntArray(
            classesJson.length()
        ) { index ->

            classesJson.getInt(index)
        }

        // =========================
        // Stopwords
        // =========================

        val stopWordsJson =
            root.getJSONArray("stop_words")

        for (i in 0 until stopWordsJson.length()) {

            stopWords.add(
                stopWordsJson.getString(i)
            )
        }
    }

    // =========================================================
    // PREPROCESSING
    // =========================================================

    private fun cleanText(input: String): String {

        var text = input.lowercase()

        // Hapus URL
        text = text.replace(
            Regex("""http\S+|www\S+"""),
            " "
        )

        // Hapus HTML
        text = text.replace(
            Regex("""<.*?>"""),
            " "
        )

        // Hapus kata Reuters
        text = text.replace(
            Regex("""\breuters\b"""),
            " "
        )

        // Hapus angka
        text = text.replace(
            Regex("""\d+"""),
            " "
        )

        // Hapus punctuation
        // Sama seperti Python:
        // text.translate(str.maketrans('', '', string.punctuation))
        text = text.filter { char ->
            char !in punctuationChars
        }

        // Hapus spasi berlebih
        text = text.replace(
            Regex("""\s+"""),
            " "
        ).trim()

        return text
    }

    // =========================================================
    // PREDIKSI
    // =========================================================

    fun predict(
        title: String,
        content: String
    ): PredictionResult {

        // Gabungkan judul dan isi berita
        val combinedText =
            "$title $content"

        // Preprocessing
        val cleaned =
            cleanText(combinedText)

        // =========================
        // Tokenisasi
        // =========================
        //
        // Android tidak menggunakan (?U)
        // karena menyebabkan:
        // Syntax error in regexp pattern
        //
        // Dataset kita berbahasa Inggris,
        // sehingga pattern berikut aman digunakan.

        val tokens =
            Regex("""\b[a-zA-Z0-9_]{2,}\b""")
                .findAll(cleaned)
                .map { it.value }
                .filter { it !in stopWords }
                .toList()

        // =========================
        // Term Frequency
        // =========================

        val counts =
            HashMap<String, Int>()

        for (token in tokens) {

            counts[token] =
                (counts[token] ?: 0) + 1
        }

        // =========================
        // TF × IDF
        // =========================

        val values =
            HashMap<Int, Double>()

        for ((word, count) in counts) {

            val index =
                vocabulary[word]
                    ?: continue

            values[index] =
                count.toDouble() *
                        idf[index]
        }

        // =========================
        // L2 Normalization
        // =========================

        var normSquared = 0.0

        for (value in values.values) {

            normSquared +=
                value * value
        }

        val norm =
            sqrt(normSquared)

        // =========================
        // LINEAR SVM
        // =========================

        var score =
            intercept

        if (norm > 0.0) {

            for ((index, value) in values) {

                val normalizedValue =
                    value / norm

                score +=
                    normalizedValue *
                            coef[index]
            }
        }

        // =========================
        // HASIL KLASIFIKASI
        // =========================

        val prediction =
            if (score > 0.0) {

                classes[1]

            } else {

                classes[0]
            }

        return PredictionResult(
            label = prediction,
            score = score
        )
    }
}