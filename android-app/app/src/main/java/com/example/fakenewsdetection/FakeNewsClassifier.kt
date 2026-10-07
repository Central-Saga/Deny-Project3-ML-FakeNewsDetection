package com.example.fakenewsdetection

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.ln
import kotlin.math.sqrt

class FakeNewsClassifier(context: Context) {

    data class PredictionResult(
        val label: Int,
        val score: Double
    )

    companion object {
        const val LABEL_UNCERTAIN = -1
        const val LABEL_FAKE = 0
        const val LABEL_REAL = 1

        private const val MODEL_FILE =
            "fake_news_model_en_v13.json"
    }

    // =========================================================
    // WORD MODEL
    // =========================================================

    private val wordVocabulary =
        HashMap<String, Int>()

    private val wordStopWords =
        HashSet<String>()

    private lateinit var wordIdf: DoubleArray
    private lateinit var wordCoef: DoubleArray

    // =========================================================
    // CHARACTER MODEL
    // =========================================================

    private val charVocabulary =
        HashMap<String, Int>()

    private lateinit var charIdf: DoubleArray
    private lateinit var charCoef: DoubleArray

    // =========================================================
    // SVM
    // =========================================================

    private lateinit var classes: IntArray

    private var intercept = 0.0
    private var uncertainThreshold = 0.20

    // =========================================================
    // REGEX
    // =========================================================

    private val urlRegex =
        Regex("""http\S+|www\S+""")

    private val htmlRegex =
        Regex("""<.*?>""")

    private val reutersRegex =
        Regex("""\breuters\b""")

    private val numberRegex =
        Regex("""\p{Nd}+""")

    private val whitespaceRegex =
        Regex("""\s+""")

    /*
     * Pendekatan Android untuk Python:
     *
     * (?u)\b\w\w+\b
     *
     * Kita gunakan Unicode letter/number/underscore.
     * Ini menghindari masalah (?U) pada Android Regex.
     */
    private val wordTokenRegex =
        Regex("""[\p{L}\p{N}_]{2,}""")

    // Sama dengan Python string.punctuation
    private val punctuationChars =
        "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toSet()

    init {
        loadModel(context)
    }

    // =========================================================
    // LOAD MODEL v1.3
    // =========================================================

    private fun loadModel(context: Context) {

        val jsonString =
            context.assets
                .open(MODEL_FILE)
                .bufferedReader()
                .use { reader ->
                    reader.readText()
                }

        val root =
            JSONObject(jsonString)

        // =====================================================
        // THRESHOLD
        // =====================================================

        uncertainThreshold =
            root.getDouble(
                "uncertain_threshold"
            )

        // =====================================================
        // WORD TF-IDF
        // =====================================================

        val wordObject =
            root.getJSONObject("word")

        loadVocabulary(
            wordObject.getJSONObject(
                "vocabulary"
            ),
            wordVocabulary
        )

        wordIdf =
            jsonToDoubleArray(
                wordObject.getJSONArray(
                    "idf"
                )
            )

        wordCoef =
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
            wordStopWords.add(
                stopWordsJson.getString(i)
            )
        }

        // =====================================================
        // CHARACTER TF-IDF
        // =====================================================

        val charObject =
            root.getJSONObject("char")

        loadVocabulary(
            charObject.getJSONObject(
                "vocabulary"
            ),
            charVocabulary
        )

        charIdf =
            jsonToDoubleArray(
                charObject.getJSONArray(
                    "idf"
                )
            )

        charCoef =
            jsonToDoubleArray(
                charObject.getJSONArray(
                    "coef"
                )
            )

        // =====================================================
        // LINEAR SVM
        // =====================================================

        val svmObject =
            root.getJSONObject("svm")

        intercept =
            svmObject.getDouble(
                "intercept"
            )

        val classesJson =
            svmObject.getJSONArray(
                "classes"
            )

        classes =
            IntArray(
                classesJson.length()
            ) { index ->
                classesJson.getInt(index)
            }

        // =====================================================
        // VALIDASI MODEL
        // =====================================================

        require(
            wordVocabulary.size ==
                    wordIdf.size
        ) {
            "Word vocabulary dan IDF tidak cocok."
        }

        require(
            wordIdf.size ==
                    wordCoef.size
        ) {
            "Word IDF dan coefficient tidak cocok."
        }

        require(
            charVocabulary.size ==
                    charIdf.size
        ) {
            "Char vocabulary dan IDF tidak cocok."
        }

        require(
            charIdf.size ==
                    charCoef.size
        ) {
            "Char IDF dan coefficient tidak cocok."
        }

        require(classes.size >= 2) {
            "Classes model tidak valid."
        }
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

        while (keys.hasNext()) {

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
    // CLEAN TEXT
    // Sama dengan preprocessing Python
    // =========================================================

    private fun cleanText(
        input: String
    ): String {

        var text =
            input.lowercase(
                Locale.ROOT
            )

        // Hapus URL
        text =
            text.replace(
                urlRegex,
                " "
            )

        // Hapus HTML
        text =
            text.replace(
                htmlRegex,
                " "
            )

        // Hapus kata Reuters
        text =
            text.replace(
                reutersRegex,
                " "
            )

        // Hapus angka
        text =
            text.replace(
                numberRegex,
                " "
            )

        /*
         * Python:
         *
         * text.translate(
         *     str.maketrans(
         *         '',
         *         '',
         *         string.punctuation
         *     )
         * )
         *
         * Jadi punctuation DIHAPUS,
         * bukan diganti dengan spasi.
         */
        text =
            text.filter { char ->
                char !in punctuationChars
            }

        // Collapse whitespace
        text =
            text.replace(
                whitespaceRegex,
                " "
            ).trim()

        return text
    }

    // =========================================================
    // WORD ANALYZER
    //
    // unigram + bigram
    // stop_words = english
    // =========================================================

    private fun createWordCounts(
        text: String
    ): HashMap<String, Int> {

        val tokens =
            wordTokenRegex
                .findAll(text)
                .map { match ->
                    match.value
                }
                .filter { token ->
                    token !in wordStopWords
                }
                .toList()

        val counts =
            HashMap<String, Int>()

        // Unigram
        for (token in tokens) {

            incrementCount(
                counts,
                token
            )
        }

        // Bigram
        for (
        i in 0 until
                (tokens.size - 1)
                    .coerceAtLeast(0)
        ) {

            val bigram =
                tokens[i] +
                        " " +
                        tokens[i + 1]

            incrementCount(
                counts,
                bigram
            )
        }

        return counts
    }

    // =========================================================
    // CHARACTER ANALYZER
    //
    // analyzer = char_wb
    // ngram_range = (3, 5)
    //
    // Harus sama dengan sklearn char_wb.
    // =========================================================

    private fun createCharCounts(
        text: String
    ): HashMap<String, Int> {

        val counts =
            HashMap<String, Int>()

        if (text.isBlank()) {
            return counts
        }

        val normalizedText =
            text.replace(
                whitespaceRegex,
                " "
            )

        val words =
            normalizedText.split(" ")

        for (word in words) {

            if (word.isEmpty()) {
                continue
            }

            /*
             * sklearn char_wb memberikan
             * padding satu spasi pada
             * awal dan akhir setiap word.
             */
            val padded =
                " $word "

            val wordLength =
                padded.length

            for (n in 3..5) {

                var offset = 0

                /*
                 * Python slicing tetap aman
                 * jika n > panjang string.
                 */
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

                    offset += 1

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

                /*
                 * Sama dengan implementasi
                 * sklearn char_wb.
                 *
                 * Untuk word sangat pendek,
                 * loop n-gram berhenti.
                 */
                if (offset == 0) {
                    break
                }
            }
        }

        return counts
    }

    // =========================================================
    // COUNT HELPER
    // =========================================================

    private fun incrementCount(
        counts: HashMap<String, Int>,
        feature: String
    ) {

        counts[feature] =
            (counts[feature] ?: 0) + 1
    }

    // =========================================================
    // TF-IDF + LINEAR SVM BRANCH
    //
    // sublinear_tf = true
    //
    // tf = 1 + ln(count)
    //
    // kemudian L2 normalization
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
        (feature, count)
        in counts
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
                tf * idf[index]

            normSquared +=
                tfidf * tfidf

            weightedSum +=
                tfidf *
                        coefficients[index]
        }

        if (normSquared <= 0.0) {
            return 0.0
        }

        val norm =
            sqrt(normSquared)

        return weightedSum / norm
    }

    // =========================================================
    // PREDIKSI
    // =========================================================

    fun predict(
        title: String,
        content: String
    ): PredictionResult {

        // Sama seperti Python:
        // title + " " + content
        val combinedText =
            "$title $content"

        val cleaned =
            cleanText(
                combinedText
            )

        // =====================================================
        // WORD BRANCH
        // =====================================================

        val wordCounts =
            createWordCounts(
                cleaned
            )

        val wordScore =
            calculateBranchScore(
                counts = wordCounts,
                vocabulary = wordVocabulary,
                idf = wordIdf,
                coefficients = wordCoef
            )

        // =====================================================
        // CHAR BRANCH
        // =====================================================

        val charCounts =
            createCharCounts(
                cleaned
            )

        val charScore =
            calculateBranchScore(
                counts = charCounts,
                vocabulary = charVocabulary,
                idf = charIdf,
                coefficients = charCoef
            )

        // =====================================================
        // LINEAR SVM
        // =====================================================

        val score =
            wordScore +
                    charScore +
                    intercept

        // =====================================================
        // THREE-WAY DECISION
        //
        // score <= -0.20 → FAKE
        // -0.20 < score < 0.20 → UNCERTAIN
        // score >= 0.20 → REAL
        // =====================================================

        val prediction =
            when {

                score <=
                        -uncertainThreshold -> {

                    classes[0]
                }

                score >=
                        uncertainThreshold -> {

                    classes[1]
                }

                else -> {

                    LABEL_UNCERTAIN
                }
            }

        return PredictionResult(
            label = prediction,
            score = score
        )
    }
}