package com.example.fakenewsdetection

import java.util.Locale
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Preprocessing murni (tanpa Context, tanpa ONNX, tanpa I/O) yang dipakai
 * [FakeNewsClassifier]. Dipisah agar bisa diuji langsung di JVM dan dikunci
 * paritasnya terhadap pipeline Python (lihat src/generate_golden.py dan
 * TextPreprocessorTest / GoldenParityTest).
 *
 * Setiap perubahan di file ini WAJIB diikuti regenerasi golden fixture:
 *   python src/generate_golden.py
 */
object TextPreprocessor {

    // =========================================================
    // COMMON REGEX
    // =========================================================

    private val whitespaceRegex = Regex("""\s+""")

    // =========================================================
    // ENGLISH PREPROCESSING
    // =========================================================

    private val enUrlRegex = Regex("""http\S+|www\S+""")
    private val enHtmlRegex = Regex("""<.*?>""")
    private val enReutersRegex = Regex("""\breuters\b""")
    private val enNumberRegex = Regex("""\p{Nd}+""")
    private val enWordTokenRegex = Regex("""[\p{L}\p{N}_]{2,}""")

    private val punctuationChars =
        "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toSet()

    // =========================================================
    // INDONESIAN PREPROCESSING
    // =========================================================

    private val idUrlRegex =
        Regex("""https?://\S+|www\.\S+""", RegexOption.IGNORE_CASE)

    private val idEmailRegex =
        Regex("""\b[\w\.-]+@[\w\.-]+\.\w+\b""", RegexOption.IGNORE_CASE)

    private val idDomainRegex =
        Regex("""\b[\w\-]+\.(?:com|id|net|org|co\.id)\b""", RegexOption.IGNORE_CASE)

    private val idNonAlnumRegex = Regex("""[^a-z0-9\s]""")

    private val idTokenRegex = Regex("""[a-z0-9]{2,}""")

    private val idRemovePatterns = listOf(
        """\breferensi\b""",
        """\bpenjelasan\b""",
        """\bbaca juga\b""",
        """\bcek fakta\b""",
        """\btim cek fakta\b""",
        """\bhasil penelusuran\b""",
        """\bberdasarkan hasil penelusuran\b""",
        """\bsetelah ditelusuri\b""",
        """\bpenelusuran diketahui\b""",
        """\bdengan demikian\b""",
        """\bdemikian klaim\b""",
        """\bdalam kategori\b""",
        """\bmasuk ke dalam kategori\b""",
        """\bnarasi\b""",
        """\bklaim\b""",
        """\bhoax\b""",
        """\bhoaks\b""",
        """\bfoto hoaks\b""",
        """\bturnbackhoax\b""",
        """\boleh akun\b""",
        """\bakun facebook\b""",
        """\bakun twitter\b""",
        """\bakun instagram\b""",
        """\bdibagikan oleh\b""",
        """\beditor\b""",
        """\bpenulis\b""",
        """\breporter\b""",
        """\bsumber\b"""
    ).map { Regex(it) }

    private val idNamePatterns = listOf(
        """\bbentang febrylian\b""",
        """\bfebrylian\b""",
        """\bjulnis firmansyah\b""",
        """\bjulnis\b""",
        """\bfirmansyah\b"""
    ).map { Regex(it) }

    // =========================================================
    // ENGLISH CLEANING (v1.3)
    // =========================================================

    fun cleanEnglishText(input: String): String {
        var text = input.lowercase(Locale.ROOT)
        text = text.replace(enUrlRegex, " ")
        text = text.replace(enHtmlRegex, " ")
        text = text.replace(enReutersRegex, " ")
        text = text.replace(enNumberRegex, " ")

        // English v1.3: punctuation dihapus, bukan diganti spasi.
        text = text.filter { it !in punctuationChars }

        return text.replace(whitespaceRegex, " ").trim()
    }

    // =========================================================
    // INDONESIAN v3 CLEANING — sama dengan sanitize_v3() Python
    // =========================================================

    fun cleanIndonesianText(input: String): String {
        var text = input.lowercase(Locale.ROOT)
        text = text.replace(idUrlRegex, " ")
        text = text.replace(idEmailRegex, " ")
        text = text.replace(idDomainRegex, " ")

        for (pattern in idRemovePatterns) {
            text = text.replace(pattern, " ")
        }
        for (pattern in idNamePatterns) {
            text = text.replace(pattern, " ")
        }

        text = text.replace(idNonAlnumRegex, " ")
        return text.replace(whitespaceRegex, " ").trim()
    }

    // =========================================================
    // WORD COUNTS
    // =========================================================

    fun englishWordCounts(text: String, stopWords: Set<String>): HashMap<String, Int> {
        val tokens = enWordTokenRegex.findAll(text)
            .map { it.value }
            .filter { it !in stopWords }
            .toList()
        return unigramBigramCounts(tokens)
    }

    fun indonesianWordCounts(text: String): HashMap<String, Int> {
        val tokens = idTokenRegex.findAll(text).map { it.value }.toList()
        return unigramBigramCounts(tokens)
    }

    fun unigramBigramCounts(tokens: List<String>): HashMap<String, Int> {
        val counts = HashMap<String, Int>()
        for (token in tokens) {
            increment(counts, token)
        }
        for (i in 0 until tokens.size - 1) {
            increment(counts, "${tokens[i]} ${tokens[i + 1]}")
        }
        return counts
    }

    // =========================================================
    // ENGLISH CHAR_WB 3-5 — replikasi sklearn _char_wb_ngrams
    // =========================================================

    fun englishCharCounts(text: String): HashMap<String, Int> {
        val counts = HashMap<String, Int>()
        if (text.isBlank()) return counts

        val words = text.replace(whitespaceRegex, " ").split(" ")
        for (word in words) {
            if (word.isEmpty()) continue

            val padded = " $word "
            val length = padded.length

            for (n in 3..5) {
                var offset = 0
                increment(counts, padded.substring(offset, minOf(offset + n, length)))

                while (offset + n < length) {
                    offset++
                    increment(counts, padded.substring(offset, minOf(offset + n, length)))
                }

                // Kata terlalu pendek untuk n berikutnya (perilaku sklearn).
                if (offset == 0) break
            }
        }
        return counts
    }

    // =========================================================
    // TF-IDF — sublinear_tf + L2 (sesuai TfidfVectorizer)
    // =========================================================

    fun tfidfVector(
        counts: Map<String, Int>,
        vocabulary: Map<String, Int>,
        idf: DoubleArray
    ): FloatArray {
        val raw = DoubleArray(idf.size)
        var normSquared = 0.0

        for ((feature, count) in counts) {
            val index = vocabulary[feature] ?: continue
            val value = (1.0 + ln(count.toDouble())) * idf[index]
            raw[index] = value
            normSquared += value * value
        }

        if (normSquared <= 0.0) return FloatArray(idf.size)

        val norm = sqrt(normSquared)
        return FloatArray(idf.size) { (raw[it] / norm).toFloat() }
    }

    // =========================================================
    // LABEL DECISION
    // =========================================================

    /**
     * English v1.3 (3 arah). Sama dengan evaluate.py: `abs(score) < threshold`
     * → UNCERTAIN. Skor TEPAT di ±threshold dianggap yakin (inklusif).
     */
    fun decideEnglishLabel(score: Double, uncertainThreshold: Double, classes: IntArray): Int =
        when {
            score.isNaN() -> FakeNewsClassifier.LABEL_UNCERTAIN
            score <= -uncertainThreshold -> classes[0]
            score >= uncertainThreshold -> classes[1]
            else -> FakeNewsClassifier.LABEL_UNCERTAIN
        }

    /**
     * Indonesian Political v3. Kelas mentah model: 0 = REAL, 1 = FAKE;
     * dikonversi ke label aplikasi (0 = FAKE, 1 = REAL).
     */
    fun decideIndonesianLabel(score: Double, threshold: Double, classes: IntArray): Int {
        if (score.isNaN()) return FakeNewsClassifier.LABEL_UNCERTAIN
        val rawClass = if (score >= threshold) classes[1] else classes[0]
        return when (rawClass) {
            0 -> FakeNewsClassifier.LABEL_REAL
            1 -> FakeNewsClassifier.LABEL_FAKE
            else -> FakeNewsClassifier.LABEL_UNCERTAIN
        }
    }

    private fun increment(counts: HashMap<String, Int>, feature: String) {
        counts[feature] = (counts[feature] ?: 0) + 1
    }
}
