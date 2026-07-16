package com.example.oliwia_wojdalska_275804_memory

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.res.painterResource
import kotlin.Triple
import androidx.compose.ui.draw.alpha
private const val PREFS_NAME = "MemoryGamePrefs"
private const val KEY_THEME = "dark_mode_enabled"
private const val KEY_CARD_THEME = "selected_theme"

@Composable
fun ThemeScreen(navController: NavController) {

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    // zakupione motywy
    val purchasedThemes = listOf(
        EconomyManager.isPurchased(context, "theme2"),
        EconomyManager.isPurchased(context, "theme3"),
        EconomyManager.isPurchased(context, "theme4"),
        EconomyManager.isPurchased(context, "theme5")
    )
    val themePrices = listOf(0, 600, 800, 1000, 0)

    val theme = prefs.getString("selected_theme", "theme1") ?: "theme1"
    val completedLevels = prefs.getStringSet("completed_levels", emptySet()) ?: emptySet()
    val isTheme5Unlocked = completedLevels.size >= 32

    var darkMode by remember { mutableStateOf(prefs.getBoolean(KEY_THEME, false)) }
    val savedTheme = prefs.getString(KEY_CARD_THEME, "theme1") ?: "theme1"
    var selectedTheme by remember { mutableStateOf(
        when(savedTheme){
            "theme1" -> 0
            "theme2" -> 1
            "theme3" -> 2
            "theme4" -> 3
            "theme5" -> 4
            else -> 0
        }
    ) }

    fun saveTheme(mode: Boolean) {
        prefs.edit().putBoolean(KEY_THEME, mode).apply()
    }

    fun saveCardTheme(theme: Int) {
        val themeName = when(theme) {
            0 -> "theme1"
            1 -> "theme2"
            2 -> "theme3"
            3 -> "theme4"
            4 -> "theme5"
            else -> "theme1"
        }

        prefs.edit().putString(KEY_CARD_THEME, themeName).apply()

        val colorScheme = when (theme) {
            0 -> "gold"
            1 -> "magic"
            2 -> "sea"
            3 -> "pixel"
            4 -> "night"
            else -> "gold"
        }

        prefs.edit().putString("selected_color_scheme", colorScheme).apply()


        selectedTheme = theme

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
            Icons.Filled.ArrowBack,
            contentDescription = "Back",
            tint = textColor,
            modifier = Modifier
                .padding(top = 32.dp)
                .size(46.dp)
                .align(Alignment.TopStart)
                .clickable { navController.popBackStack() }
        )

        Icon(
            if (darkMode) Icons.Filled.BrightnessHigh else Icons.Filled.Brightness2,
            contentDescription = "Theme",
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
                text = "Motywy kart",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(Modifier.height(16.dp))
            Divider(color = textColor.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))

            val themeNames = listOf(
                "Złoty klasyk",
                "Magiczny blask",
                "Morska głębia",
                "Pixel retro",
                "Gwieździsta noc"
            )

            val themeImages = listOf(
                R.drawable.theme_default_preview,
                R.drawable.theme_magic_preview,
                R.drawable.theme_sea_preview,
                R.drawable.theme_pixel_preview,
                R.drawable.theme_night_preview
            )

            themeNames.forEachIndexed { index, name ->

                val isUnlocked = when (index) {
                    0 -> true
                    1 -> purchasedThemes[0]
                    2 -> purchasedThemes[1]
                    3 -> purchasedThemes[2]
                    4 -> isTheme5Unlocked
                    else -> true
                }

                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable(enabled = isUnlocked) {
                            saveCardTheme(index)
                        },
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = when {
                            selectedTheme == index -> Color(0x334CAF50)
                            !isUnlocked -> Color(0x22000000)
                            else -> Color(0x11FFFFFF)
                        }
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {

                        androidx.compose.foundation.Image(
                            painter = painterResource(themeImages[index]),
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
                                val price = themePrices[index]
                                val priceText = if (index == 4)
                                    "Odblokuj wszystkie 32 poziomy"
                                else
                                    "Koszt: $price 💠 (kup w sklepie)"
                                Text(
                                    text = priceText,
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (selectedTheme == index) {
                            Text(
                                text = "✔",
                                color = Color(0xFF4CAF50),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

//                if (!isUnlocked && index == 4) {
//                    Text(
//                        text = "Odblokuj wszystkie 32 poziomy, aby uzyskać dostęp!",
//                        color = Color.Gray,
//                        fontSize = 14.sp,
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//                }
            }

        }
    }
}