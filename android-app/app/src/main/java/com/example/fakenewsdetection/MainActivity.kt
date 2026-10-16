package com.example.fakenewsdetection

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // =========================================================
    // INPUT
    // =========================================================

    private lateinit var edtTitle: EditText
    private lateinit var edtContent: EditText

    // =========================================================
    // BUTTON
    // =========================================================

    private lateinit var btnDetect: Button
    private lateinit var btnHistory: Button
    private lateinit var btnReset: Button

    // =========================================================
    // RESULT
    // =========================================================

    private lateinit var txtModelStatus: TextView
    private lateinit var txtResult: TextView
    private lateinit var txtDetectedLanguage: TextView
    private lateinit var txtModelInfo: TextView
    private lateinit var txtDisclaimer: TextView

    private lateinit var resultContainer: LinearLayout

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private lateinit var navHome: LinearLayout
    private lateinit var navDetection: LinearLayout
    private lateinit var navHistory: LinearLayout
    private lateinit var navGuide: LinearLayout

    // =========================================================
    // MACHINE LEARNING
    // =========================================================

    private lateinit var classifier: FakeNewsClassifier

    private var modelReady =
        false

    // =========================================================
    // LOCAL HISTORY
    // =========================================================

    private lateinit var historyManager: LocalHistoryManager

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        initializeViews()

        historyManager =
            LocalHistoryManager(
                applicationContext
            )

        resultContainer.visibility =
            View.GONE

        setupListeners()

        setupBottomNavigation()

        loadMachineLearningModel()
    }

    // =========================================================
    // INITIALIZE VIEW
    // =========================================================

    private fun initializeViews() {

        edtTitle =
            findViewById(
                R.id.edtTitle
            )

        edtContent =
            findViewById(
                R.id.edtContent
            )

        btnDetect =
            findViewById(
                R.id.btnDetect
            )

        btnHistory =
            findViewById(
                R.id.btnHistory
            )

        btnReset =
            findViewById(
                R.id.btnReset
            )

        txtModelStatus =
            findViewById(
                R.id.txtModelStatus
            )

        txtResult =
            findViewById(
                R.id.txtResult
            )

        txtDetectedLanguage =
            findViewById(
                R.id.txtDetectedLanguage
            )

        txtModelInfo =
            findViewById(
                R.id.txtModelInfo
            )

        txtDisclaimer =
            findViewById(
                R.id.txtDisclaimer
            )

        resultContainer =
            findViewById(
                R.id.resultContainer
            )

        navHome =
            findViewById(
                R.id.navHome
            )

        navDetection =
            findViewById(
                R.id.navDetection
            )

        navHistory =
            findViewById(
                R.id.navHistory
            )

        navGuide =
            findViewById(
                R.id.navGuide
            )
    }

    // =========================================================
    // MAIN BUTTON LISTENERS
    // =========================================================

    private fun setupListeners() {

        btnDetect.setOnClickListener {

            detectButtonClicked()
        }

        btnHistory.setOnClickListener {

            openHistory()
        }

        btnReset.setOnClickListener {

            resetForm()
        }
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun setupBottomNavigation() {

        // -----------------------------------------------------
        // HOME
        // -----------------------------------------------------

        navHome.setOnClickListener {

            val intent =
                Intent(
                    this,
                    HomeActivity::class.java
                ).apply {

                    addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }

            startActivity(
                intent
            )

            finish()
        }

        // -----------------------------------------------------
        // DETECTION
        // -----------------------------------------------------

        navDetection.setOnClickListener {

            // Sudah berada di halaman Deteksi
        }

        // -----------------------------------------------------
        // HISTORY
        // -----------------------------------------------------

        navHistory.setOnClickListener {

            openHistory()
        }

        // -----------------------------------------------------
        // GUIDE
        // -----------------------------------------------------

        navGuide.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    GuideActivity::class.java
                )
            )

            finish()
        }
    }

    // =========================================================
    // OPEN HISTORY
    // =========================================================

    private fun openHistory() {

        startActivity(
            Intent(
                this,
                HistoryActivity::class.java
            )
        )

        finish()
    }

    // =========================================================
    // DETECT BUTTON
    // =========================================================

    private fun detectButtonClicked() {

        val title =
            edtTitle.text
                .toString()
                .trim()

        val content =
            edtContent.text
                .toString()
                .trim()

        edtTitle.error =
            null

        edtContent.error =
            null

        // -----------------------------------------------------
        // MODEL READY
        // -----------------------------------------------------

        if (!modelReady) {

            Toast.makeText(
                this,
                "Model masih dimuat, silakan tunggu.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // -----------------------------------------------------
        // TITLE VALIDATION
        // -----------------------------------------------------

        if (title.isBlank()) {

            edtTitle.error =
                "Judul berita harus diisi"

            edtTitle.requestFocus()

            return
        }

        // -----------------------------------------------------
        // CONTENT VALIDATION
        // -----------------------------------------------------

        if (content.isBlank()) {

            edtContent.error =
                "Isi berita harus diisi"

            edtContent.requestFocus()

            return
        }

        hideKeyboard()

        detectNews(
            title = title,
            content = content
        )
    }

    // =========================================================
    // LOAD MACHINE LEARNING MODEL
    // =========================================================

    private fun loadMachineLearningModel() {

        modelReady =
            false

        btnDetect.isEnabled =
            false

        btnDetect.text =
            "Memuat model..."

        txtModelStatus.text =
            "Menyiapkan model offline..."

        Thread {

            Process.setThreadPriority(
                Process.THREAD_PRIORITY_BACKGROUND
            )

            try {

                val loadedClassifier =
                    FakeNewsClassifier(
                        applicationContext
                    )

                runOnUiThread {

                    classifier =
                        loadedClassifier

                    modelReady =
                        true

                    btnDetect.isEnabled =
                        true

                    btnDetect.text =
                        "Analisis Berita"

                    txtModelStatus.text =
                        "2 model aktif • Bahasa dipilih otomatis"
                }

            } catch (e: Exception) {

                runOnUiThread {

                    modelReady =
                        false

                    btnDetect.isEnabled =
                        false

                    btnDetect.text =
                        "Model tidak tersedia"

                    txtModelStatus.text =
                        "Model gagal dimuat"

                    Toast.makeText(
                        this,
                        "Gagal memuat model: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.apply {

            name =
                "ML-Model-Loader"

            start()
        }
    }

    // =========================================================
    // DETECT NEWS
    // =========================================================

    private fun detectNews(
        title: String,
        content: String
    ) {

        btnDetect.isEnabled =
            false

        btnDetect.text =
            "Menganalisis..."

        Thread {

            Process.setThreadPriority(
                Process.THREAD_PRIORITY_BACKGROUND
            )

            try {

                // =============================================
                // AUTO LANGUAGE + AUTO MODEL
                // =============================================

                val result =
                    classifier.predictAuto(
                        title,
                        content
                    )

                runOnUiThread {

                    showPrediction(
                        result
                    )

                    val predictionText =
                        when (result.label) {

                            FakeNewsClassifier.LABEL_FAKE ->
                                "FAKE NEWS"

                            FakeNewsClassifier.LABEL_REAL ->
                                "REAL NEWS"

                            else ->
                                "UNCERTAIN"
                        }

                    // =========================================
                    // SAVE LOCAL HISTORY
                    // =========================================

                    saveDetectionHistory(
                        title = title,
                        content = content,
                        prediction = predictionText,
                        score = result.score
                    )

                    btnDetect.isEnabled =
                        true

                    btnDetect.text =
                        "Analisis Berita"
                }

            } catch (e: Exception) {

                runOnUiThread {

                    btnDetect.isEnabled =
                        true

                    btnDetect.text =
                        "Analisis Berita"

                    Toast.makeText(
                        this,
                        "Prediksi gagal: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.apply {

            name =
                "ML-Prediction"

            start()
        }
    }

    // =========================================================
    // SHOW RESULT
    // =========================================================

    private fun showPrediction(
        result: FakeNewsClassifier.PredictionResult
    ) {

        // =====================================================
        // LABEL
        // =====================================================

        when (result.label) {

            FakeNewsClassifier.LABEL_FAKE -> {

                txtResult.text =
                    "FAKE NEWS"

                txtResult.setTextColor(
                    Color.parseColor(
                        "#C62828"
                    )
                )

                resultContainer
                    .setBackgroundResource(
                        R.drawable.bg_result_fake
                    )
            }

            FakeNewsClassifier.LABEL_REAL -> {

                txtResult.text =
                    "REAL NEWS"

                txtResult.setTextColor(
                    Color.parseColor(
                        "#15803D"
                    )
                )

                resultContainer
                    .setBackgroundResource(
                        R.drawable.bg_result_real
                    )
            }

            else -> {

                txtResult.text =
                    "UNCERTAIN"

                txtResult.setTextColor(
                    Color.parseColor(
                        "#B45309"
                    )
                )

                resultContainer
                    .setBackgroundResource(
                        R.drawable.bg_result_uncertain
                    )
            }
        }

        // =====================================================
        // LANGUAGE + MODEL
        // =====================================================

        when (result.language) {

            FakeNewsClassifier.ModelLanguage.INDONESIAN -> {

                txtDetectedLanguage.text =
                    "Bahasa terdeteksi: Indonesia"

                txtModelInfo.text =
                    "Political v3 • TF-IDF + Linear SVM"

                txtDisclaimer.text =
                    "Model Indonesia terutama dilatih pada berita politik. " +
                            "Prediksi dibuat berdasarkan pola teks dan bukan " +
                            "verifikasi fakta secara real-time."
            }

            FakeNewsClassifier.ModelLanguage.ENGLISH -> {

                txtDetectedLanguage.text =
                    "Detected language: English"

                txtModelInfo.text =
                    "English v1.3 • TF-IDF + Linear SVM"

                txtDisclaimer.text =
                    "Prediction is based on textual patterns learned " +
                            "by the model and is not real-time fact verification."
            }
        }

        // =====================================================
        // SHOW RESULT
        // =====================================================

        resultContainer.visibility =
            View.VISIBLE

        resultContainer.alpha =
            0f

        resultContainer
            .animate()
            .alpha(1f)
            .setDuration(250)
            .start()
    }

    // =========================================================
    // SAVE LOCAL HISTORY
    // =========================================================

    private fun saveDetectionHistory(
        title: String,
        content: String,
        prediction: String,
        score: Double
    ) {

        try {

            historyManager.saveHistory(
                title = title,
                content = content,
                prediction = prediction,
                score = score
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Prediksi berhasil, tetapi riwayat gagal disimpan.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // RESET FORM
    // =========================================================

    private fun resetForm() {

        edtTitle.text.clear()

        edtContent.text.clear()

        edtTitle.error =
            null

        edtContent.error =
            null

        resultContainer.visibility =
            View.GONE

        txtResult.text =
            ""

        txtDetectedLanguage.text =
            ""

        txtModelInfo.text =
            ""

        txtDisclaimer.text =
            ""

        edtTitle.requestFocus()

        hideKeyboard()
    }

    // =========================================================
    // HIDE KEYBOARD
    // =========================================================

    private fun hideKeyboard() {

        val inputMethodManager =
            getSystemService(
                INPUT_METHOD_SERVICE
            ) as InputMethodManager

        inputMethodManager
            .hideSoftInputFromWindow(
                currentFocus?.windowToken,
                0
            )
    }
}