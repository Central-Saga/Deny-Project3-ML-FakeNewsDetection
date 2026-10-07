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
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var edtTitle: EditText
    private lateinit var edtContent: EditText

    private lateinit var btnDetect: Button
    private lateinit var btnHistory: Button
    private lateinit var btnReset: Button

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

        txtResult =
            findViewById(R.id.txtResult)

        txtModelInfo =
            findViewById(R.id.txtModelInfo)

        resultContainer =
            findViewById(R.id.resultContainer)

        // Sembunyikan hasil saat aplikasi pertama dibuka
        resultContainer.visibility =
            View.GONE

        // Load model ML
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

            // Jalankan deteksi
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

        // =====================================================
        // BUTTON RESET
        // =====================================================

        btnReset.setOnClickListener {

            // Kosongkan input
            edtTitle.text.clear()
            edtContent.text.clear()

            // Hilangkan error input
            edtTitle.error = null
            edtContent.error = null

            // Sembunyikan hasil
            resultContainer.visibility =
                View.GONE

            // Bersihkan hasil
            txtResult.text = ""
            txtModelInfo.text = ""

            // Fokus kembali ke judul
            edtTitle.requestFocus()

            Toast.makeText(
                this,
                "Form berhasil dibersihkan",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // LOAD MACHINE LEARNING MODEL
    // =========================================================

    private fun loadMachineLearningModel() {

        modelReady = false

        btnDetect.isEnabled =
            false

        btnDetect.text =
            "MEMUAT MODEL..."

        Thread {

            // Model v1.3 lebih besar,
            // jadi tetap dijalankan di background thread.
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
                        "DETEKSI BERITA"

                    Toast.makeText(
                        this,
                        "Model English v1.3 berhasil dimuat",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    modelReady =
                        false

                    btnDetect.isEnabled =
                        false

                    btnDetect.text =
                        "MODEL GAGAL DIMUAT"

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
            "MENGANALISIS..."

        Thread {

            Process.setThreadPriority(
                Process.THREAD_PRIORITY_BACKGROUND
            )

            try {

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
                    // HASIL PREDIKSI v1.3
                    //
                    //  0 = FAKE
                    // -1 = UNCERTAIN
                    //  1 = REAL
                    // =================================================

                    when (result.label) {

                        FakeNewsClassifier.LABEL_FAKE -> {

                            predictionText =
                                "FAKE NEWS"

                            txtResult.text =
                                predictionText

                            txtResult.setTextColor(
                                Color.parseColor(
                                    "#DC2626"
                                )
                            )
                        }

                        FakeNewsClassifier.LABEL_REAL -> {

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

                        FakeNewsClassifier.LABEL_UNCERTAIN -> {

                            predictionText =
                                "UNCERTAIN"

                            txtResult.text =
                                predictionText

                            // Amber / orange
                            txtResult.setTextColor(
                                Color.parseColor(
                                    "#D97706"
                                )
                            )
                        }

                        else -> {

                            predictionText =
                                "UNCERTAIN"

                            txtResult.text =
                                predictionText

                            txtResult.setTextColor(
                                Color.parseColor(
                                    "#D97706"
                                )
                            )
                        }
                    }

                    // =================================================
                    // MODEL INFO
                    // =================================================

                    val formattedScore =
                        String.format(
                            Locale.US,
                            "%.10f",
                            result.score
                        )

                    txtModelInfo.text =
                        buildString {

                            append(
                                "English Model v1.3"
                            )

                            append(
                                "\nWord + Character TF-IDF • Linear SVM"
                            )

                            append(
                                "\nScore: "
                            )

                            append(
                                formattedScore
                            )

                            append(
                                " • Threshold: ±0.20"
                            )

                            append(
                                "\nISOT: 99.34% • WELFake: 95.57%"
                            )

                            if (
                                result.label ==
                                FakeNewsClassifier.LABEL_UNCERTAIN
                            ) {

                                append(
                                    "\nModel tidak cukup yakin. Verifikasi dari sumber terpercaya."
                                )
                            }
                        }

                    // =================================================
                    // SIMPAN KE FASTAPI / POSTGRESQL
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
                        "DETEKSI BERITA"
                }

            } catch (e: Exception) {

                runOnUiThread {

                    btnDetect.isEnabled =
                        true

                    btnDetect.text =
                        "DETEKSI BERITA"

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
    // SAVE HISTORY TO FASTAPI
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