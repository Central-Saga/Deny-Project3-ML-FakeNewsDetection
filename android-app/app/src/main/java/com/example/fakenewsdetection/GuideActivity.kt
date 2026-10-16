package com.example.fakenewsdetection

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class GuideActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navDetection: LinearLayout
    private lateinit var navHistory: LinearLayout
    private lateinit var navGuide: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_guide)

        initializeViews()
        setupBottomNavigation()
    }

    private fun initializeViews() {

        navHome =
            findViewById(R.id.navHome)

        navDetection =
            findViewById(R.id.navDetection)

        navHistory =
            findViewById(R.id.navHistory)

        navGuide =
            findViewById(R.id.navGuide)
    }

    private fun setupBottomNavigation() {

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

            startActivity(intent)
            finish()
        }

        navDetection.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                )
            )

            finish()
        }

        navHistory.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    HistoryActivity::class.java
                )
            )

            finish()
        }

        navGuide.setOnClickListener {

            // Sudah berada di halaman Panduan
        }
    }
}