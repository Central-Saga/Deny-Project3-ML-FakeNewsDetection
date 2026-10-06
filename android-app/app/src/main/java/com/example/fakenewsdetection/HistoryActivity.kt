package com.example.fakenewsdetection

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private lateinit var btnBackToDetection: Button

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

        btnBackToDetection =
            findViewById(R.id.btnBackToDetection)

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

        // =====================================================
        // BUTTON BACK TO DETECTION
        // =====================================================

        btnBackToDetection.setOnClickListener {

            // Menutup HistoryActivity
            // dan kembali ke MainActivity
            finish()
        }

        // Load data pertama kali
        loadHistory()
    }

    // =========================================================
    // LOAD HISTORY
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

                        if (histories.isEmpty()) {

                            txtEmpty.visibility =
                                View.VISIBLE

                            return
                        }

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

        val isFake =
            history.prediction == "FAKE NEWS"

        // =====================================================
        // CARD CONTAINER
        // =====================================================

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
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
                    dp(14)
                )

                layoutParams =
                    params

                background =
                    createRoundedBackground(
                        color = Color.WHITE,
                        radius = 18f
                    )

                elevation =
                    dp(3).toFloat()
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
                        "#0F172A"
                    )
                )
            }

        // =====================================================
        // STATUS BADGE
        // =====================================================

        val prediction =
            TextView(this).apply {

                text =
                    history.prediction

                textSize =
                    13f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    if (isFake) {

                        Color.parseColor(
                            "#B91C1C"
                        )

                    } else {

                        Color.parseColor(
                            "#15803D"
                        )
                    }
                )

                setPadding(
                    dp(12),
                    dp(7),
                    dp(12),
                    dp(7)
                )

                background =
                    createRoundedBackground(
                        color =
                            if (isFake) {

                                Color.parseColor(
                                    "#FEE2E2"
                                )

                            } else {

                                Color.parseColor(
                                    "#DCFCE7"
                                )
                            },
                        radius = 30f
                    )

                val badgeParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                badgeParams.setMargins(
                    0,
                    dp(12),
                    0,
                    dp(10)
                )

                layoutParams =
                    badgeParams
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
                    "Score Model: $formattedScore"

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
                    dp(5),
                    0,
                    dp(14)
                )
            }

        // =====================================================
        // DELETE BUTTON
        // =====================================================

        val btnDelete =
            Button(this).apply {

                text =
                    "HAPUS RIWAYAT"

                textSize =
                    13f

                setTextColor(
                    Color.WHITE
                )

                background =
                    createRoundedBackground(
                        color = Color.parseColor(
                            "#DC2626"
                        ),
                        radius = 14f
                    )

                setOnClickListener {

                    showDeleteConfirmation(
                        historyId = history.id,
                        title = history.title,
                        button = this
                    )
                }
            }

        // =====================================================
        // ADD VIEWS
        // =====================================================

        card.addView(title)
        card.addView(prediction)
        card.addView(score)
        card.addView(date)
        card.addView(btnDelete)

        historyContainer.addView(card)
    }

    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    private fun showDeleteConfirmation(
        historyId: Int,
        title: String,
        button: Button
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Hapus Riwayat"
            )
            .setMessage(
                "Apakah kamu yakin ingin menghapus riwayat:\n\n$title?"
            )
            .setNegativeButton(
                "BATAL"
            ) { dialog, _ ->

                dialog.dismiss()
            }
            .setPositiveButton(
                "HAPUS"
            ) { dialog, _ ->

                button.isEnabled =
                    false

                button.text =
                    "MENGHAPUS..."

                deleteHistory(
                    id = historyId,
                    button = button
                )

                dialog.dismiss()
            }
            .show()
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

                            loadHistory()

                        } else {

                            button.isEnabled =
                                true

                            button.text =
                                "HAPUS RIWAYAT"

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
                            "HAPUS RIWAYAT"

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

            value
        }
    }

    // =========================================================
    // CREATE ROUNDED BACKGROUND
    // =========================================================

    private fun createRoundedBackground(
        color: Int,
        radius: Float
    ): GradientDrawable {

        return GradientDrawable().apply {

            shape =
                GradientDrawable.RECTANGLE

            setColor(
                color
            )

            cornerRadius =
                dp(
                    radius.toInt()
                ).toFloat()
        }
    }

    // =========================================================
    // DP CONVERTER
    // =========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}