package com.example.fakenewsdetection

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.view.View
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

    private lateinit var edtTitle: EditText
    private lateinit var edtContent: EditText

    private lateinit var btnDetect: Button
    private lateinit var btnHistory: Button

    private lateinit var txtResult: TextView
    private lateinit var txtModelInfo: TextView

    private lateinit var resultContainer: LinearLayout

    private lateinit var classifier: FakeNewsClassifier

    private var modelReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // =====================================================
        // VIEW INITIALIZATION
        // =====================================================

        edtTitle = findViewById(R.id.edtTitle)
        edtContent = findViewById(R.id.edtContent)

        btnDetect = findViewById(R.id.btnDetect)
        btnHistory = findViewById(R.id.btnHistory)

        txtResult = findViewById(R.id.txtResult)
        txtModelInfo = findViewById(R.id.txtModelInfo)

        resultContainer = findViewById(R.id.resultContainer)

        // Sembunyikan hasil saat aplikasi pertama dibuka
        resultContainer.visibility = View.GONE

        // Load Machine Learning model
        loadMachineLearningModel()

        // =====================================================
        // BUTTON DETECT
        // =====================================================

        btnDetect.setOnClickListener {

            val title =
                edtTitle.text
                    .toString()
                    .trim()

            val content =
                edtContent.text
                    .toString()
                    .trim()

            // Pastikan model sudah siap
            if (!modelReady) {

                Toast.makeText(
                    this,
                    "Model masih dimuat, silakan tunggu.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Validasi judul
            if (title.isBlank()) {

                edtTitle.error =
                    "Judul berita harus diisi"

                edtTitle.requestFocus()

                return@setOnClickListener
            }

            // Validasi isi berita
            if (content.isBlank()) {

                edtContent.error =
                    "Isi berita harus diisi"

                edtContent.requestFocus()

                return@setOnClickListener
            }

            // Jalankan prediksi
            detectNews(
                title = title,
                content = content
            )
        }

        // =====================================================
        // BUTTON HISTORY
        // =====================================================

        btnHistory.setOnClickListener {

            val intent =
                Intent(
                    this,
                    HistoryActivity::class.java
                )

            startActivity(intent)
        }
    }

    // =========================================================
    // LOAD MACHINE LEARNING MODEL
    // =========================================================

    private fun loadMachineLearningModel() {

        modelReady = false

        btnDetect.isEnabled = false
        btnDetect.text = "MEMUAT MODEL..."

        Thread {

            // Jalankan proses berat sebagai background priority
            Process.setThreadPriority(
                Process.THREAD_PRIORITY_BACKGROUND
            )

            try {

                // Load model dari JSON
                val loadedClassifier =
                    FakeNewsClassifier(
                        applicationContext
                    )

                runOnUiThread {

                    classifier = loadedClassifier
                    modelReady = true

                    btnDetect.isEnabled = true
                    btnDetect.text = "DETEKSI BERITA"

                    Toast.makeText(
                        this,
                        "Model Machine Learning berhasil dimuat",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    modelReady = false

                    btnDetect.isEnabled = false
                    btnDetect.text = "MODEL GAGAL DIMUAT"

                    Toast.makeText(
                        this,
                        "Gagal memuat model: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.apply {

            name = "ML-Model-Loader"

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

        btnDetect.isEnabled = false
        btnDetect.text = "MENGANALISIS..."

        Thread {

            // Prediksi juga dijalankan sebagai background priority
            Process.setThreadPriority(
                Process.THREAD_PRIORITY_BACKGROUND
            )

            try {

                // Prediksi menggunakan model ML offline
                val result =
                    classifier.predict(
                        title,
                        content
                    )

                runOnUiThread {

                    resultContainer.visibility =
                        View.VISIBLE

                    val predictionText: String

                    // =================================================
                    // HASIL KLASIFIKASI
                    // =================================================

                    if (result.label == 0) {

                        predictionText =
                            "FAKE NEWS"

                        txtResult.text =
                            predictionText

                        txtResult.setTextColor(
                            Color.parseColor(
                                "#DC2626"
                            )
                        )

                    } else {

                        predictionText =
                            "REAL NEWS"

                        txtResult.text =
                            predictionText

                        txtResult.setTextColor(
                            Color.parseColor(
                                "#16A34A"
                            )
                        )
                    }

                    // Informasi model
                    txtModelInfo.text =
                        "Linear SVM • Accuracy 98.95%"

                    // =================================================
                    // SIMPAN HASIL KE FASTAPI / POSTGRESQL
                    // =================================================

                    saveDetectionHistory(
                        title = title,
                        content = content,
                        prediction = predictionText,
                        score = result.score
                    )

                    btnDetect.isEnabled = true
                    btnDetect.text = "DETEKSI BERITA"
                }

            } catch (e: Exception) {

                runOnUiThread {

                    btnDetect.isEnabled = true
                    btnDetect.text = "DETEKSI BERITA"

                    Toast.makeText(
                        this,
                        "Prediksi gagal: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.apply {

            name = "ML-Prediction"

            start()
        }
    }

    // =========================================================
    // SAVE DETECTION HISTORY TO FASTAPI
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
            .createHistory(request)
            .enqueue(
                object :
                    Callback<CreateHistoryResponse> {

                    // =============================================
                    // RESPONSE DARI SERVER
                    // =============================================

                    override fun onResponse(
                        call: Call<CreateHistoryResponse>,
                        response: Response<CreateHistoryResponse>
                    ) {

                        if (response.isSuccessful) {

                            val historyResponse =
                                response.body()

                            Toast.makeText(
                                this@MainActivity,
                                "Riwayat berhasil disimpan (ID: ${historyResponse?.id})",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            Toast.makeText(
                                this@MainActivity,
                                "Gagal menyimpan riwayat. HTTP ${response.code()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    // =============================================
                    // GAGAL TERHUBUNG KE API
                    // =============================================

                    override fun onFailure(
                        call: Call<CreateHistoryResponse>,
                        t: Throwable
                    ) {

                        Toast.makeText(
                            this@MainActivity,
                            "API tidak dapat dihubungi: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }
}