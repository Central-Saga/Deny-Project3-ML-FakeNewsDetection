package com.example.fakenewsdetection

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    // =========================================================
    // VIEW
    // =========================================================

    private lateinit var historyContainer: LinearLayout
    private lateinit var emptyState: LinearLayout

    private lateinit var txtHistoryCount: TextView
    private lateinit var btnClearHistory: TextView

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private lateinit var navHome: LinearLayout
    private lateinit var navDetection: LinearLayout
    private lateinit var navHistory: LinearLayout
    private lateinit var navGuide: LinearLayout

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
            R.layout.activity_history
        )

        historyManager =
            LocalHistoryManager(
                applicationContext
            )

        initializeViews()

        setupListeners()

        loadHistory()
    }

    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {
        super.onResume()

        loadHistory()
    }

    // =========================================================
    // INITIALIZE VIEW
    // =========================================================

    private fun initializeViews() {

        historyContainer =
            findViewById(
                R.id.historyContainer
            )

        emptyState =
            findViewById(
                R.id.emptyState
            )

        txtHistoryCount =
            findViewById(
                R.id.txtHistoryCount
            )

        btnClearHistory =
            findViewById(
                R.id.btnClearHistory
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
    // LISTENERS
    // =========================================================

    private fun setupListeners() {

        // -----------------------------------------------------
        // CLEAR ALL
        // -----------------------------------------------------

        btnClearHistory.setOnClickListener {

            showClearHistoryDialog()
        }

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

            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                )
            )

            finish()
        }

        // -----------------------------------------------------
        // HISTORY
        // -----------------------------------------------------

        navHistory.setOnClickListener {

            // Sudah berada di halaman Riwayat
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
    // LOAD HISTORY
    // =========================================================

    private fun loadHistory() {

        val history =
            historyManager.getHistory()

        historyContainer.removeAllViews()

        txtHistoryCount.text =
            when {

                history.isEmpty() -> {

                    "Belum ada hasil analisis"
                }

                history.size == 1 -> {

                    "1 hasil analisis tersimpan"
                }

                else -> {

                    "${history.size} hasil analisis tersimpan"
                }
            }

        if (history.isEmpty()) {

            emptyState.visibility =
                View.VISIBLE

            historyContainer.visibility =
                View.GONE

            btnClearHistory.visibility =
                View.GONE

            return
        }

        emptyState.visibility =
            View.GONE

        historyContainer.visibility =
            View.VISIBLE

        btnClearHistory.visibility =
            View.VISIBLE

        history.forEach { item ->

            historyContainer.addView(
                createHistoryCard(
                    item
                )
            )
        }
    }

    // =========================================================
    // CREATE CARD
    // =========================================================

    private fun createHistoryCard(
        history: DetectionHistory
    ): View {

        val card =
            LinearLayout(
                this
            ).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(18),
                    dp(20),
                    dp(16)
                )

                background =
                    createCardBackground()

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        bottomMargin =
                            dp(12)
                    }
            }

        // =====================================================
        // TOP ROW
        // =====================================================

        val topRow =
            LinearLayout(
                this
            ).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        // =====================================================
        // RESULT BADGE
        // =====================================================

        val badge =
            TextView(
                this
            ).apply {

                text =
                    "●  ${history.prediction}"

                textSize =
                    11f

                typeface =
                    Typeface.DEFAULT_BOLD

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(12),
                    dp(6),
                    dp(12),
                    dp(6)
                )

                setTextColor(
                    getPredictionTextColor(
                        history.prediction
                    )
                )

                background =
                    createPredictionBackground(
                        history.prediction
                    )
            }

        // =====================================================
        // DATE
        // =====================================================

        val date =
            TextView(
                this
            ).apply {

                text =
                    formatDate(
                        history.timestamp
                    )

                textSize =
                    11.5f

                setTextColor(
                    Color.parseColor(
                        "#64748B"
                    )
                )

                gravity =
                    Gravity.END

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {

                        marginStart =
                            dp(10)
                    }
            }

        topRow.addView(
            badge
        )

        topRow.addView(
            date
        )

        card.addView(
            topRow
        )

        // =====================================================
        // TITLE
        // =====================================================

        val title =
            TextView(
                this
            ).apply {

                text =
                    history.title.ifBlank {

                        "Tanpa Judul"
                    }

                textSize =
                    16f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.parseColor(
                        "#0F172A"
                    )
                )

                maxLines =
                    2

                ellipsize =
                    android.text.TextUtils.TruncateAt.END

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        topMargin =
                            dp(14)
                    }
            }

        card.addView(
            title
        )

        // =====================================================
        // CONTENT PREVIEW
        // =====================================================

        val content =
            TextView(
                this
            ).apply {

                text =
                    history.content

                textSize =
                    13f

                setTextColor(
                    Color.parseColor(
                        "#475569"
                    )
                )

                setLineSpacing(
                    dp(2).toFloat(),
                    1f
                )

                maxLines =
                    3

                ellipsize =
                    android.text.TextUtils.TruncateAt.END

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        topMargin =
                            dp(6)
                    }
            }

        card.addView(
            content
        )

        // =====================================================
        // DIVIDER
        // =====================================================

        val divider =
            View(this).apply {
                setBackgroundColor(Color.parseColor("#F1F5F9"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {
                    topMargin = dp(14)
                }
            }

        card.addView(divider)

        // =====================================================
        // DELETE
        // =====================================================

        val deleteText =
            TextView(
                this
            ).apply {

                text =
                    "Hapus Riwayat"

                textSize =
                    12f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.parseColor(
                        "#C62828"
                    )
                )

                gravity =
                    Gravity.END

                setPadding(
                    dp(8),
                    dp(10),
                    0,
                    dp(2)
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.END
                    }

                setOnClickListener {

                    showDeleteDialog(
                        history
                    )
                }
            }

        card.addView(
            deleteText
        )

        return card
    }

    // =========================================================
    // DELETE ONE
    // =========================================================

    private fun showDeleteDialog(
        history: DetectionHistory
    ) {

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Hapus Riwayat"
            )
            .setMessage(
                "Hapus hasil analisis ini dari perangkat?"
            )
            .setNegativeButton(
                "Batal",
                null
            )
            .setPositiveButton(
                "Hapus"
            ) { _, _ ->

                historyManager.deleteHistory(
                    history.id
                )

                loadHistory()

                Toast.makeText(
                    this,
                    "Riwayat berhasil dihapus.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    // =========================================================
    // CLEAR ALL
    // =========================================================

    private fun showClearHistoryDialog() {

        if (
            historyManager
                .getHistory()
                .isEmpty()
        ) {

            return
        }

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Hapus Semua Riwayat"
            )
            .setMessage(
                "Semua hasil analisis yang tersimpan di perangkat akan dihapus."
            )
            .setNegativeButton(
                "Batal",
                null
            )
            .setPositiveButton(
                "Hapus Semua"
            ) { _, _ ->

                historyManager.clearHistory()

                loadHistory()

                Toast.makeText(
                    this,
                    "Semua riwayat berhasil dihapus.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    // =========================================================
    // CARD BACKGROUND
    // =========================================================

    private fun createCardBackground():
            GradientDrawable {

        return GradientDrawable().apply {

            shape =
                GradientDrawable.RECTANGLE

            setColor(
                Color.WHITE
            )

            cornerRadius =
                dp(22).toFloat()

            setStroke(
                dp(1),
                Color.parseColor(
                    "#E2E8F0"
                )
            )
        }
    }

    // =========================================================
    // PREDICTION BACKGROUND
    // =========================================================

    private fun createPredictionBackground(
        prediction: String
    ): GradientDrawable {

        val (backgroundColor, strokeColor) =
            when (
                prediction.uppercase(
                    Locale.ROOT
                )
            ) {

                "REAL NEWS" -> {
                    Color.parseColor("#F0FDF4") to Color.parseColor("#BBF7D0")
                }

                "FAKE NEWS" -> {
                    Color.parseColor("#FEF2F2") to Color.parseColor("#FECACA")
                }

                else -> {
                    Color.parseColor("#FFFBEB") to Color.parseColor("#FDE68A")
                }
            }

        return GradientDrawable().apply {

            shape =
                GradientDrawable.RECTANGLE

            setColor(
                backgroundColor
            )

            setStroke(
                dp(1),
                strokeColor
            )

            cornerRadius =
                dp(50).toFloat()
        }
    }

    // =========================================================
    // RESULT TEXT COLOR
    // =========================================================

    private fun getPredictionTextColor(
        prediction: String
    ): Int {

        return when (
            prediction.uppercase(
                Locale.ROOT
            )
        ) {

            "REAL NEWS" -> {

                Color.parseColor(
                    "#15803D"
                )
            }

            "FAKE NEWS" -> {

                Color.parseColor(
                    "#C62828"
                )
            }

            else -> {

                Color.parseColor(
                    "#B45309"
                )
            }
        }
    }

    // =========================================================
    // FORMAT DATE
    // =========================================================

    private fun formatDate(
        timestamp: Long
    ): String {

        if (timestamp <= 0L) {

            return ""
        }

        val formatter =
            SimpleDateFormat(
                "dd MMM yyyy • HH:mm",
                Locale.forLanguageTag(
                    "id-ID"
                )
            )

        return formatter.format(
            Date(
                timestamp
            )
        )
    }

    // =========================================================
    // DP HELPER
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
