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
private const val KEY_MENU_BG = "selected_menu_background"

@Composable
fun MenuBackgroundSettings(navController: NavController) {

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // które tła są kupione
    val purchasedBackgrounds = listOf(
        EconomyManager.isPurchased(context, "bg2"),
        EconomyManager.isPurchased(context, "bg3"),
        EconomyManager.isPurchased(context, "bg4"),
        EconomyManager.isPurchased(context, "bg5")
    )
    var darkMode by remember { mutableStateOf(prefs.getBoolean(KEY_THEME, false)) }
    var selectedBg by remember { mutableStateOf(prefs.getString(KEY_MENU_BG, "bg1") ?: "bg1") }
    fun saveTheme(mode: Boolean) {
        prefs.edit().putBoolean(KEY_THEME, mode).apply()
    }
    fun saveBackground(id: String) {
        prefs.edit().putString(KEY_MENU_BG, id).apply()
        selectedBg = id
    }
    val bgColor = if (darkMode) Color(0xFF111111) else Color.White
    val textColor = if (darkMode) Color.White else Color.Black
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 16.dp)
    ) {
        // okona powrotu
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
        // ikona zmiany motywu
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
                text = "Tło menu głównego",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.height(16.dp))
            Divider(color = textColor.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))
            // tłaa menu
            val backgrounds = listOf(
                Triple("bg1", "Złoty klasyk", R.drawable.t1_ekran),
                Triple("bg2", "Magiczny blask", R.drawable.t3_ekran),
                Triple("bg3", "Morska głębia", R.drawable.t4_ekran),
                Triple("bg4", "Pixel retro", R.drawable.t3_splash2),
                Triple("bg5", "Gwieździsta noc", R.drawable.t5_ekran)

            )
            // cena za każde tło
            val bgPrices = mapOf(
                "bg1" to 0,
                "bg2" to 400,
                "bg3" to 700,
                "bg4" to 800,
                "bg5" to 1000
            )
            backgrounds.forEachIndexed { index, (id, name, img) ->
                val isUnlocked = when (id) {
                    "bg1" -> true // darmowe
                    "bg2" -> purchasedBackgrounds[0]
                    "bg3" -> purchasedBackgrounds[1]
                    "bg4" -> purchasedBackgrounds[2]
                    "bg5" -> purchasedBackgrounds[3]
                    else -> true
                }
                val price = bgPrices[id] ?: 0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable(enabled = isUnlocked) { // blokada
                            saveBackground(id)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            selectedBg == id -> Color(0x334CAF50)
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
                                .alpha(if (isUnlocked) 1f else 0.4f) // przyciemnienie
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                color = if (isUnlocked) textColor else Color.Gray,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // napis zakup w sklepie
                            if (!isUnlocked) {
                                Text(
                                    text = "Koszt: $price 💠 (kup w sklepie)",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        // gdy wybrane ikona ✔
                        if (selectedBg == id) {
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