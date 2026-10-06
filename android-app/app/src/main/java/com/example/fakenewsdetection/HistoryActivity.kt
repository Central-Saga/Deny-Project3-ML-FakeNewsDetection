package com.example.fakenewsdetection

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fakenewsdetection.network.ApiClient
import com.example.fakenewsdetection.network.DeleteHistoryResponse
import com.example.fakenewsdetection.network.DetectionHistory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var btnRefresh: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var txtEmpty: TextView
    private lateinit var historyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_history)

        // =====================================================
        // VIEW INITIALIZATION
        // =====================================================

        btnRefresh =
            findViewById(R.id.btnRefresh)

        progressBar =
            findViewById(R.id.progressBar)

        txtEmpty =
            findViewById(R.id.txtEmpty)

        historyContainer =
            findViewById(R.id.historyContainer)

        // =====================================================
        // BUTTON REFRESH
        // =====================================================

        btnRefresh.setOnClickListener {
            loadHistory()
        }

        // Load data saat halaman pertama dibuka
        loadHistory()
    }

    // =========================================================
    // LOAD HISTORY FROM FASTAPI
    // =========================================================

    private fun loadHistory() {

        progressBar.visibility =
            View.VISIBLE

        txtEmpty.visibility =
            View.GONE

        historyContainer.removeAllViews()

        ApiClient.apiService
            .getHistory()
            .enqueue(
                object : Callback<List<DetectionHistory>> {

                    override fun onResponse(
                        call: Call<List<DetectionHistory>>,
                        response: Response<List<DetectionHistory>>
                    ) {

                        progressBar.visibility =
                            View.GONE

                        if (!response.isSuccessful) {

                            Toast.makeText(
                                this@HistoryActivity,
                                "Gagal mengambil riwayat. HTTP ${response.code()}",
                                Toast.LENGTH_LONG
                            ).show()

                            return
                        }

                        val histories =
                            response.body().orEmpty()

                        // Jika database kosong
                        if (histories.isEmpty()) {

                            txtEmpty.visibility =
                                View.VISIBLE

                            return
                        }

                        // Tampilkan seluruh data
                        histories.forEach { history ->

                            addHistoryItem(
                                history
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<List<DetectionHistory>>,
                        t: Throwable
                    ) {

                        progressBar.visibility =
                            View.GONE

                        Toast.makeText(
                            this@HistoryActivity,
                            "API tidak dapat dihubungi: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    // =========================================================
    // ADD HISTORY CARD
    // =========================================================

    private fun addHistoryItem(
        history: DetectionHistory
    ) {

        // =====================================================
        // CARD CONTAINER
        // =====================================================

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    32,
                    28,
                    32,
                    28
                )

                val params =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                params.setMargins(
                    0,
                    0,
                    0,
                    24
                )

                layoutParams =
                    params

                setBackgroundColor(
                    Color.WHITE
                )

                elevation =
                    5f
            }

        // =====================================================
        // TITLE
        // =====================================================

        val title =
            TextView(this).apply {

                text =
                    history.title

                textSize =
                    17f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.parseColor(
                        "#111827"
                    )
                )
            }

        // =====================================================
        // PREDICTION
        // =====================================================

        val prediction =
            TextView(this).apply {

                text =
                    history.prediction

                textSize =
                    16f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    if (
                        history.prediction ==
                        "FAKE NEWS"
                    ) {

                        Color.parseColor(
                            "#DC2626"
                        )

                    } else {

                        Color.parseColor(
                            "#16A34A"
                        )
                    }
                )

                setPadding(
                    0,
                    14,
                    0,
                    8
                )
            }

        // =====================================================
        // SCORE
        // =====================================================

        val formattedScore =
            history.score?.let {

                String.format(
                    Locale.US,
                    "%.3f",
                    it
                )

            } ?: "-"

        val score =
            TextView(this).apply {

                text =
                    "Score: $formattedScore"

                textSize =
                    14f

                setTextColor(
                    Color.parseColor(
                        "#475569"
                    )
                )
            }

        // =====================================================
        // DATE
        // =====================================================

        val date =
            TextView(this).apply {

                text =
                    "Tanggal: ${formatDate(history.created_at)}"

                textSize =
                    13f

                setTextColor(
                    Color.parseColor(
                        "#64748B"
                    )
                )

                setPadding(
                    0,
                    6,
                    0,
                    14
                )
            }

        // =====================================================
        // DELETE BUTTON
        // =====================================================

        val btnDelete =
            Button(this).apply {

                text =
                    "HAPUS"

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.parseColor(
                        "#DC2626"
                    )
                )

                setOnClickListener {

                    isEnabled =
                        false

                    text =
                        "MENGHAPUS..."

                    deleteHistory(
                        id = history.id,
                        button = this
                    )
                }
            }

        // =====================================================
        // ADD VIEW TO CARD
        // =====================================================

        card.addView(title)
        card.addView(prediction)
        card.addView(score)
        card.addView(date)
        card.addView(btnDelete)

        // Tambahkan card ke halaman
        historyContainer.addView(card)
    }

    // =========================================================
    // DELETE HISTORY
    // =========================================================

    private fun deleteHistory(
        id: Int,
        button: Button
    ) {

        ApiClient.apiService
            .deleteHistory(id)
            .enqueue(
                object :
                    Callback<DeleteHistoryResponse> {

                    override fun onResponse(
                        call: Call<DeleteHistoryResponse>,
                        response: Response<DeleteHistoryResponse>
                    ) {

                        if (response.isSuccessful) {

                            Toast.makeText(
                                this@HistoryActivity,
                                "Riwayat berhasil dihapus",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Refresh data setelah delete
                            loadHistory()

                        } else {

                            button.isEnabled =
                                true

                            button.text =
                                "HAPUS"

                            Toast.makeText(
                                this@HistoryActivity,
                                "Gagal menghapus riwayat. HTTP ${response.code()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<DeleteHistoryResponse>,
                        t: Throwable
                    ) {

                        button.isEnabled =
                            true

                        button.text =
                            "HAPUS"

                        Toast.makeText(
                            this@HistoryActivity,
                            "API tidak dapat dihubungi: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    // =========================================================
    // FORMAT DATE
    // =========================================================

    private fun formatDate(
        value: String
    ): String {

        return try {

            /*
             * Contoh dari FastAPI:
             * 2026-10-06T10:20:31.010713
             *
             * PostgreSQL/FastAPI bisa mengirim microseconds.
             */

            val inputFormat =
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                    Locale.US
                )

            val outputFormat =
                SimpleDateFormat(
                    "dd MMM yyyy, HH:mm",
                    Locale.ENGLISH
                )

            val parsedDate =
                inputFormat.parse(value)

            if (parsedDate != null) {

                outputFormat.format(
                    parsedDate
                )

            } else {

                value
            }

        } catch (e: Exception) {

            // Jika format tanggal berbeda,
            // tampilkan string asli agar aplikasi tidak crash
            value
        }
    }
}