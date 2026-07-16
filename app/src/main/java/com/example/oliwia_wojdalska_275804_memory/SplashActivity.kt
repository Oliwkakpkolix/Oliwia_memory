package com.example.oliwia_wojdalska_275804_memory

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Color as AndroidColor
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // zapisany styl ekranu (domyślnie screen1)
        val prefs = getSharedPreferences("MemoryGamePrefs", MODE_PRIVATE)
        val selectedStyle = prefs.getString("selected_screen_style", "screen1")
        val userName = prefs.getString("user_name", "Graczu")  // jeśli brak imienia, pokaże "Graczu"
        // tworzymy layout dynamicznie
        val layout = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        val logo = ImageView(this).apply {

            // odpowiednie tło splash na podstawie wybranego motywu
            val splashRes = when (selectedStyle) {
                "screen2" -> R.drawable.t3_splash
                "screen3" -> R.drawable.t4_splash
                "screen4" -> R.drawable.t2_splash
                "screen5" -> R.drawable.t5_splash
                else -> R.drawable.t1_splah
            }

            setImageResource(splashRes)

            scaleType = ImageView.ScaleType.CENTER_CROP // dopasuj proporcje do ekranu
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        val textGradientColors = when (selectedStyle) {
            "screen2" -> listOf(
                AndroidColor.parseColor("#FFE9D8B4"),
                AndroidColor.parseColor("#FFD7C093")
            )
            "screen4" -> listOf(
                AndroidColor.parseColor("#E1BEE7"),
                AndroidColor.parseColor("#CE93D8")
            )
            "screen3" -> listOf(
                AndroidColor.parseColor("#81D4FA"),
                AndroidColor.parseColor("#FFFFFF")
            )
            "screen5" -> listOf(
                AndroidColor.parseColor("#B3E5FC"),
                AndroidColor.parseColor("#81D4FA")
            )
            else -> listOf(
                AndroidColor.parseColor("#FFE082"),
                AndroidColor.parseColor("#FFC107")
            )
        }
        val welcomeText = TextView(this).apply {
            text = "Witaj, $userName!"
            textSize = 35f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            // Tworzymy gradient
            paint.shader = LinearGradient(
                0f, 0f, 0f, textSize * 1.2f,
                textGradientColors[0],
                textGradientColors[1],
                Shader.TileMode.CLAMP
            )
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = 2000
            }
        }
        layout.addView(logo)        // najpierw tło
        layout.addView(welcomeText) // potem tekst na wierzchu
        setContentView(layout)

        // po 2 sekundach przechodzimy do MainActivity
        layout.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }
}