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
import com.example.fakenewsdetection.network.ApiClient
import com.example.fakenewsdetection.network.CreateHistoryResponse
import com.example.fakenewsdetection.network.DetectionRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var edtTitle: EditText
    private lateinit var edtContent: EditText

    private lateinit var btnDetect: Button
    private lateinit var btnHistory: Button
    private lateinit var btnReset: Button

    private lateinit var txtModelStatus: TextView
    private lateinit var txtResult: TextView
    private lateinit var txtDetectedLanguage: TextView
    private lateinit var txtModelInfo: TextView
    private lateinit var txtDisclaimer: TextView

    private lateinit var resultContainer: LinearLayout

    // =========================================================
    // ML
    // =========================================================

    private lateinit var classifier: FakeNewsClassifier

    private var modelReady = false


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        window.statusBarColor =
            Color.parseColor("#F6F7FB")

        window.navigationBarColor =
            Color.parseColor("#FFFFFF")

        initializeViews()

        resultContainer.visibility =
            View.GONE

        setupListeners()

        loadMachineLearningModel()
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        edtTitle =
            findViewById(R.id.edtTitle)

        edtContent =
            findViewById(R.id.edtContent)

        btnDetect =
            findViewById(R.id.btnDetect)

        btnHistory =
            findViewById(R.id.btnHistory)

        btnReset =
            findViewById(R.id.btnReset)

        txtModelStatus =
            findViewById(R.id.txtModelStatus)

        txtResult =
            findViewById(R.id.txtResult)

        txtDetectedLanguage =
            findViewById(R.id.txtDetectedLanguage)

        txtModelInfo =
            findViewById(R.id.txtModelInfo)

        txtDisclaimer =
            findViewById(R.id.txtDisclaimer)

        resultContainer =
            findViewById(R.id.resultContainer)
    }


    // =========================================================
    // BUTTON LISTENERS
    // =========================================================

    private fun setupListeners() {

        // -----------------------------------------------------
        // DETECT
        // -----------------------------------------------------

        btnDetect.setOnClickListener {

            detectButtonClicked()
        }


        // -----------------------------------------------------
        // HISTORY
        // -----------------------------------------------------

        btnHistory.setOnClickListener {

            val intent =
                Intent(
                    this,
                    HistoryActivity::class.java
                )

            startActivity(intent)
        }


        // -----------------------------------------------------
        // RESET
        // -----------------------------------------------------

        btnReset.setOnClickListener {

            resetForm()
        }
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


        edtTitle.error = null
        edtContent.error = null


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
    // LOAD MACHINE LEARNING
    // =========================================================

    private fun loadMachineLearningModel() {

        modelReady = false

        btnDetect.isEnabled = false

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

                // =================================================
                // AUTO LANGUAGE + AUTO MODEL
                // =================================================

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

                            FakeNewsClassifier.LABEL_FAKE -> {

                                "FAKE NEWS"
                            }


                            FakeNewsClassifier.LABEL_REAL -> {

                                "REAL NEWS"
                            }


                            else -> {

                                "UNCERTAIN"
                            }
                        }


                    // =================================================
                    // SAVE HISTORY
                    // =================================================

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
        // RESULT LABEL
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
        // LANGUAGE + MODEL INFO
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
        // SHOW RESULT CARD
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
    // RESET FORM
    // =========================================================

    private fun resetForm() {

        edtTitle.text.clear()
        edtContent.text.clear()

        edtTitle.error = null
        edtContent.error = null

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

        Toast.makeText(
            this,
            "Form berhasil dibersihkan",
            Toast.LENGTH_SHORT
        ).show()
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


    // =========================================================
    // SAVE HISTORY
    // =========================================================

    private fun saveDetectionHistory(
        title: String,
        content: String,
        prediction: String,
        score: Double
    ) {

        val request =
            DetectionRequest(
                title = title,
                content = content,
                prediction = prediction,
                score = score
            )


        ApiClient.apiService
            .createHistory(
                request
            )
            .enqueue(

                object :
                    Callback<CreateHistoryResponse> {


                    override fun onResponse(
                        call: Call<CreateHistoryResponse>,
                        response: Response<CreateHistoryResponse>
                    ) {

                        if (!response.isSuccessful) {

                            Toast.makeText(
                                this@MainActivity,
                                "Prediksi berhasil, tetapi riwayat belum tersimpan.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }


                    override fun onFailure(
                        call: Call<CreateHistoryResponse>,
                        t: Throwable
                    ) {

                        // Tidak perlu menggagalkan hasil prediksi.
                        // Model tetap dapat digunakan secara offline.
                    }
                }
            )
    }
}