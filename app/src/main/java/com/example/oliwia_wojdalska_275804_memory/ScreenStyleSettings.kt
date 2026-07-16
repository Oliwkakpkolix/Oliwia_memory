package com.example.oliwia_wojdalska_275804_memory

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.draw.alpha

private const val PREFS_NAME = "MemoryGamePrefs"
private const val KEY_THEME = "dark_mode_enabled"
private const val KEY_SCREEN_STYLE = "selected_screen_style"

@Composable
fun ScreenStyleSettings(navController: NavController) {

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // które motywy ekranu są kupione
    val purchasedScreens = listOf(
        EconomyManager.isPurchased(context, "screen2"),
        EconomyManager.isPurchased(context, "screen3"),
        EconomyManager.isPurchased(context, "screen4"),
        EconomyManager.isPurchased(context, "screen5")
    )

    var darkMode by remember { mutableStateOf(prefs.getBoolean(KEY_THEME, false)) }
    var selectedStyle by remember { mutableStateOf(prefs.getString(KEY_SCREEN_STYLE, "screen1") ?: "screen1") }

    fun saveTheme(mode: Boolean) {
        prefs.edit().putBoolean(KEY_THEME, mode).apply()
    }

    fun saveScreenStyle(id: String) {
        prefs.edit().putString(KEY_SCREEN_STYLE, id).apply()
        selectedStyle = id
    }

    val bgColor = if (darkMode) Color(0xFF111111) else Color.White
    val textColor = if (darkMode) Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 16.dp)
    ) {

        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back",
            tint = textColor,
            modifier = Modifier
                .padding(top = 32.dp)
                .size(46.dp)
                .align(Alignment.TopStart)
                .clickable { navController.popBackStack() }
        )

        Icon(
            imageVector = if (darkMode) Icons.Filled.BrightnessHigh else Icons.Filled.Brightness2,
            contentDescription = "Theme toggle",
            tint = textColor,
            modifier = Modifier
                .padding(top = 32.dp)
                .size(46.dp)
                .align(Alignment.TopEnd)
                .clickable {
                    darkMode = !darkMode
                    saveTheme(darkMode)
                }
        )

        Column(
            modifier = Modifier
                .padding(top = 90.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Motywy ekranu głównego",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(Modifier.height(16.dp))
            Divider(color = textColor.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))

            val styles = listOf(
                Triple("screen1", "Złoty klasyk", R.drawable.t1_splash),
                Triple("screen2", "Magiczny blask", R.drawable.t3_splash),
                Triple("screen3", "Morska głębia", R.drawable.t4_splash),
                Triple("screen4", "Pixel retro", R.drawable.t2_splash),
                Triple("screen5", "Gwieździsta noc", R.drawable.t5_splash)

            )
            val screenPrices = mapOf(
                "screen1" to 0,
                "screen2" to 300,
                "screen3" to 400,
                "screen4" to 600,
                "screen5" to 800
            )

            styles.forEachIndexed { index, (id, name, img) ->
                val isUnlocked = when (id) {
                    "screen1" -> true // zawsze darmowy
                    "screen2" -> purchasedScreens[0]
                    "screen3" -> purchasedScreens[1]
                    "screen4" -> purchasedScreens[2]
                    "screen5" -> purchasedScreens[3]
                    else -> true
                }

                val price = screenPrices[id] ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable(enabled = isUnlocked) {
                            saveScreenStyle(id)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            selectedStyle == id -> Color(0x334CAF50)
                            !isUnlocked -> Color(0x22000000)
                            else -> Color(0x11FFFFFF)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Image(
                            painter = painterResource(img),
                            contentDescription = name,
                            modifier = Modifier
                                .size(90.dp)
                                .padding(end = 16.dp)
                                .alpha(if (isUnlocked) 1f else 0.4f)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                color = if (isUnlocked) textColor else Color.Gray,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (!isUnlocked) {
                                Text(
                                    text = "Koszt: $price 💠 (kup w sklepie)",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (selectedStyle == id) {
                            Text(
                                text = "✔",
                                color = Color(0xFF4CAF50),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}