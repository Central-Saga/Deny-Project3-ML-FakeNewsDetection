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
import com.example.fakenewsdetection.network.DetectionHistory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HistoryActivity : AppCompatActivity() {

    private lateinit var btnRefresh: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var txtEmpty: TextView
    private lateinit var historyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_history)

        btnRefresh = findViewById(R.id.btnRefresh)
        progressBar = findViewById(R.id.progressBar)
        txtEmpty = findViewById(R.id.txtEmpty)
        historyContainer = findViewById(R.id.historyContainer)

        btnRefresh.setOnClickListener {
            loadHistory()
        }

        loadHistory()
    }

    private fun loadHistory() {

        progressBar.visibility = View.VISIBLE
        txtEmpty.visibility = View.GONE
        historyContainer.removeAllViews()

        ApiClient.apiService
            .getHistory()
            .enqueue(object : Callback<List<DetectionHistory>> {

                override fun onResponse(
                    call: Call<List<DetectionHistory>>,
                    response: Response<List<DetectionHistory>>
                ) {

                    progressBar.visibility = View.GONE

                    if (!response.isSuccessful) {

                        Toast.makeText(
                            this@HistoryActivity,
                            "Gagal mengambil riwayat. HTTP ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    val histories = response.body().orEmpty()

                    if (histories.isEmpty()) {

                        txtEmpty.visibility = View.VISIBLE
                        return
                    }

                    histories.forEach { history ->
                        addHistoryItem(history)
                    }
                }

                override fun onFailure(
                    call: Call<List<DetectionHistory>>,
                    t: Throwable
                ) {

                    progressBar.visibility = View.GONE

                    Toast.makeText(
                        this@HistoryActivity,
                        "API tidak dapat dihubungi: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun addHistoryItem(
        history: DetectionHistory
    ) {

        val card = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                32,
                28,
                32,
                28
            )

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(
                0,
                0,
                0,
                24
            )

            layoutParams = params

            setBackgroundColor(
                Color.WHITE
            )

            elevation = 5f
        }

        val title = TextView(this).apply {

            text = history.title

            textSize = 17f

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(
                Color.parseColor("#111827")
            )
        }

        val prediction = TextView(this).apply {

            text = history.prediction

            textSize = 16f

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(
                if (history.prediction == "FAKE NEWS") {
                    Color.parseColor("#DC2626")
                } else {
                    Color.parseColor("#16A34A")
                }
            )

            setPadding(
                0,
                14,
                0,
                8
            )
        }

        val score = TextView(this).apply {

            text =
                "Score: ${history.score ?: "-"}"

            textSize = 14f

            setTextColor(
                Color.parseColor("#475569")
            )
        }

        val date = TextView(this).apply {

            text =
                "Tanggal: ${history.created_at}"

            textSize = 13f

            setTextColor(
                Color.parseColor("#64748B")
            )

            setPadding(
                0,
                6,
                0,
                0
            )
        }

        card.addView(title)
        card.addView(prediction)
        card.addView(score)
        card.addView(date)

        historyContainer.addView(card)
    }
}