package com.example.fakenewsdetection

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var edtTitle: EditText
    private lateinit var edtContent: EditText
    private lateinit var btnDetect: Button
    private lateinit var txtResult: TextView
    private lateinit var txtModelInfo: TextView
    private lateinit var resultContainer: LinearLayout

    private lateinit var classifier: FakeNewsClassifier

    private var modelReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        edtTitle = findViewById(R.id.edtTitle)
        edtContent = findViewById(R.id.edtContent)
        btnDetect = findViewById(R.id.btnDetect)
        txtResult = findViewById(R.id.txtResult)
        txtModelInfo = findViewById(R.id.txtModelInfo)
        resultContainer = findViewById(R.id.resultContainer)

        resultContainer.visibility = View.GONE

        // Load model ML
        loadMachineLearningModel()

        btnDetect.setOnClickListener {

            val title = edtTitle.text.toString().trim()
            val content = edtContent.text.toString().trim()

            if (!modelReady) {
                Toast.makeText(
                    this,
                    "Model masih dimuat, silakan tunggu.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (title.isBlank()) {
                edtTitle.error = "Judul berita harus diisi"
                edtTitle.requestFocus()
                return@setOnClickListener
            }

            if (content.isBlank()) {
                edtContent.error = "Isi berita harus diisi"
                edtContent.requestFocus()
                return@setOnClickListener
            }

            detectNews(title, content)
        }
    }

    private fun loadMachineLearningModel() {

        modelReady = false

        btnDetect.isEnabled = false
        btnDetect.text = "MEMUAT MODEL..."

        Thread {

            try {

                classifier =
                    FakeNewsClassifier(applicationContext)

                modelReady = true

                runOnUiThread {

                    btnDetect.isEnabled = true
                    btnDetect.text = "DETEKSI BERITA"

                    Toast.makeText(
                        this,
                        "Model Machine Learning berhasil dimuat",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                modelReady = false

                runOnUiThread {

                    btnDetect.isEnabled = false
                    btnDetect.text = "MODEL GAGAL DIMUAT"

                    Toast.makeText(
                        this,
                        "Gagal memuat model: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }

    private fun detectNews(
        title: String,
        content: String
    ) {

        btnDetect.isEnabled = false
        btnDetect.text = "MENGANALISIS..."

        Thread {

            try {

                val result = classifier.predict(
                    title,
                    content
                )

                runOnUiThread {

                    resultContainer.visibility =
                        View.VISIBLE

                    if (result.label == 0) {

                        txtResult.text = "FAKE NEWS"

                        txtResult.setTextColor(
                            Color.parseColor("#DC2626")
                        )

                    } else {

                        txtResult.text = "REAL NEWS"

                        txtResult.setTextColor(
                            Color.parseColor("#16A34A")
                        )
                    }

                    txtModelInfo.text =
                        "Linear SVM • Accuracy 98.95%"

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

        }.start()
    }
}