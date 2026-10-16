package com.example.fakenewsdetection

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {

    // =========================================================
    // VIEW
    // =========================================================

    private lateinit var btnStartAnalysis: MaterialButton

    private lateinit var menuDetection: LinearLayout
    private lateinit var menuHistory: LinearLayout
    private lateinit var menuGuide: LinearLayout

    private lateinit var navHome: LinearLayout
    private lateinit var navDetection: LinearLayout
    private lateinit var navHistory: LinearLayout
    private lateinit var navGuide: LinearLayout

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        initializeViews()

        setupListeners()
    }

    // =========================================================
    // INITIALIZE VIEW
    // =========================================================

    private fun initializeViews() {

        btnStartAnalysis =
            findViewById(
                R.id.btnStartAnalysis
            )

        menuDetection =
            findViewById(
                R.id.menuDetection
            )

        menuHistory =
            findViewById(
                R.id.menuHistory
            )

        menuGuide =
            findViewById(
                R.id.menuGuide
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

        // Tombol utama
        btnStartAnalysis.setOnClickListener {

            openDetection()
        }

        // Menu fitur deteksi
        menuDetection.setOnClickListener {

            openDetection()
        }

        // Menu fitur riwayat
        menuHistory.setOnClickListener {

            openHistory()
        }

        // Menu fitur panduan
        menuGuide.setOnClickListener {

            openGuide()
        }

        // Bottom navigation - Home
        navHome.setOnClickListener {

            // Sudah berada di halaman Beranda
        }

        // Bottom navigation - Detection
        navDetection.setOnClickListener {

            openDetection()
        }

        // Bottom navigation - History
        navHistory.setOnClickListener {

            openHistory()
        }

        // Bottom navigation - Guide
        navGuide.setOnClickListener {

            openGuide()
        }
    }

    // =========================================================
    // OPEN DETECTION
    // =========================================================

    private fun openDetection() {

        startActivity(
            Intent(
                this,
                MainActivity::class.java
            )
        )
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
    }

    // =========================================================
    // OPEN GUIDE
    // =========================================================

    private fun openGuide() {

        startActivity(
            Intent(
                this,
                GuideActivity::class.java
            )
        )
    }
}