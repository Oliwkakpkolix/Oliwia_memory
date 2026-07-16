package com.example.oliwia_wojdalska_275804_memory

import android.content.Context
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
// tryb wyzwanie
// pobiera czas poziomu
private fun getLevelTimes(context: Context, level: Int, setSize: Int): Triple<Int?, Int?, Int?> {
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    val best = prefs.getInt("best_time_${setSize}_${level}", -1).takeIf { it != -1 }
    val worst = prefs.getInt("worst_time_${setSize}_${level}", -1).takeIf { it != -1 }
    val last = prefs.getInt("last_time_${setSize}_${level}", -1).takeIf { it != -1 }
    return Triple(best, worst, last)
}

// listy rozmiarów plansz
private val LEVEL_SIZES_2 = listOf(4, 6, 8, 12, 16, 20, 24, 28, 32, 36, 40, 48, 56)
private val LEVEL_SIZES_3 = listOf(9, 12, 15, 18, 24, 27, 30, 36, 42, 45, 48, 54, 63, 72, 81, 90)
private val LEVEL_SIZES_4 = listOf(8, 12, 16, 24, 28, 32, 36, 40, 48, 64, 72, 80)
private val LEVEL_SIZES_5 = listOf(15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80)

private const val PREFS_NAME = "MemoryGamePrefs"
private const val KEY_THEME = "dark_mode_enabled"

@Composable
fun ChallengeScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var darkMode by remember { mutableStateOf(prefs.getBoolean(KEY_THEME, false)) }

    // kolory zależne od trybu
    val bgColor = if (darkMode) Color(0xFF111111) else Color.White
    val textColor = if (darkMode) Color.White else Color.Black
    val cardColor = if (darkMode) Color(0xFF222222) else Color(0xFFE0E0E0)
    val selectedColor = if (darkMode) Color(0xFF4CAF50) else Color(0xFF81C784)
    // aktualnie wybrany tryb 2 - 5 i plansza
    var selectedMode by remember { mutableStateOf(2) }
    var selectedBoard by remember { mutableStateOf(0) }
    // dostępne tryby
    val modeOptions = listOf(2, 3, 4, 5)
    // lista plansz zależna od trybu
    val boardSizes = when (selectedMode) {
        2 -> LEVEL_SIZES_2
        3 -> LEVEL_SIZES_3
        4 -> LEVEL_SIZES_4
        else -> LEVEL_SIZES_5
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Powrót",
            tint = textColor,
            modifier = Modifier
                .padding(top = 32.dp)
                .size(46.dp)
                .align(Alignment.TopStart)
                .clickable { navController.popBackStack() }
        )
        // przełącznik trybu
        Icon(
            imageVector = if (darkMode) Icons.Filled.BrightnessHigh else Icons.Filled.Brightness2,
            contentDescription = "Tryb",
            tint = textColor,
            modifier = Modifier
                .padding(top = 32.dp)
                .size(46.dp)
                .align(Alignment.TopEnd)
                .clickable {
                    darkMode = !darkMode
                    prefs.edit().putBoolean(KEY_THEME, darkMode).apply()
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
                text = "WYZWANIE",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.height(16.dp))
            Divider(color = textColor.copy(alpha = 0.4f))
            Spacer(Modifier.height(20.dp))
            // wybór trybu
            Text(
                text = "Wybierz typ układu:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Spacer(Modifier.height(12.dp))
            // wiersz z trybami
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                modeOptions.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) selectedColor else cardColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            // zmiana trybu
                            .clickable { selectedMode = mode }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            "$mode-pary",
                            color = if (isSelected) Color.Black else textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Wybierz planszę:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Spacer(Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                var expandedIndex by remember { mutableStateOf(-1) }
                boardSizes.forEachIndexed { index, size ->
                    val isSelected = selectedBoard == index
                    val isExpanded = expandedIndex == index
                    val (best, worst, last) = getLevelTimes(context, index + 1, selectedMode)

                    val prefs =
                        context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
                    val bestCognitive =
                        prefs.getInt("best_cognitive_${selectedMode}_${index + 1}", 0)
                    val bestAssociation =
                        prefs.getInt("best_association_${selectedMode}_${index + 1}", 0)
                    val bestLocalization =
                        prefs.getInt("best_localization_${selectedMode}_${index + 1}", 0)
                    val bestAttention =
                        prefs.getInt("best_attention_${selectedMode}_${index + 1}", 0)

                    val lastCognitive =
                        prefs.getInt("last_cognitive_${selectedMode}_${index + 1}", 0)
                    val lastAssociation =
                        prefs.getInt("last_association_${selectedMode}_${index + 1}", 0)
                    val lastLocalization =
                        prefs.getInt("last_localization_${selectedMode}_${index + 1}", 0)
                    val lastAttention =
                        prefs.getInt("last_attention_${selectedMode}_${index + 1}", 0)

                    val worstCognitive    = prefs.getInt("worst_cognitive_${selectedMode}_${index + 1}", 0)
                    val worstAssociation  = prefs.getInt("worst_association_${selectedMode}_${index + 1}", 0)
                    val worstLocalization = prefs.getInt("worst_localization_${selectedMode}_${index + 1}", 0)
                    val worstAttention    = prefs.getInt("worst_attention_${selectedMode}_${index + 1}", 0)
                    val bestErrors = prefs.getInt("best_errors_${selectedMode}_${index + 1}", -1).takeIf { it != -1 }
                    val worstErrors = prefs.getInt("worst_errors_${selectedMode}_${index + 1}", -1).takeIf { it != -1 }
                    val lastErrors = prefs.getInt("last_errors_${selectedMode}_${index + 1}", -1).takeIf { it != -1 }
                    // val lastErrorsTime = prefs.getInt("last_errors_time_${selectedMode}_${index + 1}", -1).takeIf { it != -1 }
                    val bestErrorsTotalPref = prefs.getInt("best_errors_total_${selectedMode}_${index + 1}", -1).takeIf { it != -1 }
                    val worstErrorsTotalPref = prefs.getInt("worst_errors_total_${selectedMode}_${index + 1}", -1).takeIf { it != -1 }
                    val lastErrorsTotalPref = prefs.getInt("last_errors_total_${selectedMode}_${index + 1}", -1).takeIf { it != -1 }

                    val bestErrorsTime = prefs.getInt("best_errors_time_${selectedMode}_${index + 1}", -1).takeIf { it != -1 }
                    val worstErrorsTime = prefs.getInt("worst_errors_time_${selectedMode}_${index + 1}", -1).takeIf { it != -1 }
                    val lastErrorsTime = prefs.getInt("last_errors_time_${selectedMode}_${index + 1}", -1)
                        .takeIf { it != -1 }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) selectedColor else cardColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                expandedIndex = if (isExpanded) -1 else index
                                selectedBoard = index
                            }
                            .padding(vertical = 10.dp, horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Plansza ${index + 1}: $size kart",
                                color = if (isSelected) Color.Black else textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isExpanded) "▲" else "▼",
                                color = if (isSelected) Color.Black else textColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isExpanded) {
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (darkMode) Color(0xFF1B1B1B) else Color(0xFFF2F2F2),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(14.dp)
                            )
                            {
                                Column {

                                    // odxzyt danych dla czasu, cztery typy błędów
                                    val bestCognitive = prefs.getInt("best_cognitive_${selectedMode}_${index + 1}", 0)
                                    val bestAssociation = prefs.getInt("best_association_${selectedMode}_${index + 1}", 0)
                                    val bestLocalization = prefs.getInt("best_localization_${selectedMode}_${index + 1}", 0)
                                    val bestAttention = prefs.getInt("best_attention_${selectedMode}_${index + 1}", 0)

                                    val worstCognitive    = prefs.getInt("worst_cognitive_${selectedMode}_${index + 1}", 0)
                                    val worstAssociation  = prefs.getInt("worst_association_${selectedMode}_${index + 1}", 0)
                                    val worstLocalization = prefs.getInt("worst_localization_${selectedMode}_${index + 1}", 0)
                                    val worstAttention    = prefs.getInt("worst_attention_${selectedMode}_${index + 1}", 0)

                                    // odczyt danych dla błędów, cztery typy błędów
                                    val bestErrorsCognitive = prefs.getInt("best_errors_cognitive_${selectedMode}_${index + 1}", 0)
                                    val bestErrorsAssociation = prefs.getInt("best_errors_association_${selectedMode}_${index + 1}", 0)
                                    val bestErrorsLocalization = prefs.getInt("best_errors_localization_${selectedMode}_${index + 1}", 0)
                                    val bestErrorsAttention = prefs.getInt("best_errors_attention_${selectedMode}_${index + 1}", 0)

                                    val lastErrorsCognitive = prefs.getInt("last_cognitive_${selectedMode}_${index + 1}", 0)
                                    val lastErrorsAssociation = prefs.getInt("last_association_${selectedMode}_${index + 1}", 0)
                                    val lastErrorsLocalization = prefs.getInt("last_localization_${selectedMode}_${index + 1}", 0)
                                    val lastErrorsAttention = prefs.getInt("last_attention_${selectedMode}_${index + 1}", 0)

                                    val worstErrorsCognitive = prefs.getInt("worst_errors_cognitive_${selectedMode}_${index + 1}", 0)
                                    val worstErrorsAssociation = prefs.getInt("worst_errors_association_${selectedMode}_${index + 1}", 0)
                                    val worstErrorsLocalization = prefs.getInt("worst_errors_localization_${selectedMode}_${index + 1}", 0)
                                    val worstErrorsAttention = prefs.getInt("worst_errors_attention_${selectedMode}_${index + 1}", 0)
                                    // suma dla najlepszego czasu (best_time)
                                    val bestTimeTotalErrors =
                                        bestCognitive + bestAssociation + bestLocalization + bestAttention
                                    // suma dla najgorszego czasu (worst_time)
                                    val worstTimeTotalErrors =
                                        worstCognitive + worstAssociation + worstLocalization + worstAttention
                                    // suma dla njmniej błędów (best_errors)
                                    val bestErrorsTotal =
                                        bestErrorsCognitive + bestErrorsAssociation + bestErrorsLocalization + bestErrorsAttention
                                    // suma dla najnowszego (last)
                                    val lastErrorsTotal =
                                        lastErrorsCognitive + lastErrorsAssociation + lastErrorsLocalization + lastErrorsAttention
                                    // suma dla najwięcej błędów (worst_errors)
                                    val worstErrorsTotal =
                                        worstErrorsCognitive + worstErrorsAssociation + worstErrorsLocalization + worstErrorsAttention
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .background(
                                                        Color(0x144CAF50),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    "Najlepszy \nczas",
                                                    color = Color(0xFF4CAF50),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Czas: ${best ?: " - "} s",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "Suma błędów: $bestTimeTotalErrors",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "Poznawcze: $bestCognitive\nSkojarzeniowe: $bestAssociation\nLokalizacyjne: $bestLocalization\nUwaga: $bestAttention",
                                                    color = textColor.copy(alpha = 0.9f),
                                                    fontSize = 13.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            // najnowszy czas
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .background(
                                                        Color(0x141976D2),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    "Najnowszy \nczas",
                                                    color = Color(0xFF1976D2),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Czas: ${last ?: " - "} s",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "Suma błędów: $lastErrorsTotal",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "Poznawcze: $lastErrorsCognitive\nSkojarzeniowe: $lastErrorsAssociation\nLokalizacyjne: $lastLocalization\nUwaga: $lastAttention",
                                                    color = textColor.copy(alpha = 0.9f),
                                                    fontSize = 13.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            // najgorszy czas
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .background(
                                                        Color(0x10E53935),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    "Najgorszy \nczas",
                                                    color = Color(0xFFE53935),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Czas: ${worst ?: " - "} s",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "Suma błędów: $worstTimeTotalErrors",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "Poznawcze: $worstCognitive\nSkojarzeniowe: $worstAssociation\nLokalizacyjne: $worstLocalization\nUwaga: $worstAttention",
                                                    color = textColor.copy(alpha = 0.9f),
                                                    fontSize = 13.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            // najmniej błędów
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .background(
                                                        Color(0x144CAF50),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    "Najmniej błędów",
                                                    color = Color(0xFF4CAF50),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Suma błędów: $bestErrorsTotal",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "Czas: ${bestErrorsTime ?: " - "} s",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "Poznawcze: $bestErrorsCognitive\nSkojarzeniowe: $bestErrorsAssociation\nLokalizacyjne: $bestErrorsLocalization\nUwaga: $bestErrorsAttention",
                                                    color = textColor.copy(alpha = 0.9f),
                                                    fontSize = 13.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            // najnowsze błędy
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .background(
                                                        Color(0x141976D2),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    "Najnowsze błędy",
                                                    color = Color(0xFF1976D2),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Suma błędów: $lastErrorsTotal",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "Czas: ${lastErrorsTime ?: " - "} s",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "Poznawcze: $lastErrorsCognitive\nSkojarzeniowe: $lastErrorsAssociation\nLokalizacyjne: $lastErrorsLocalization\nUwaga: $lastErrorsAttention",
                                                    color = textColor.copy(alpha = 0.9f),
                                                    fontSize = 13.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            // najwięcej błędów
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .background(
                                                        Color(0x10E53935),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    "Najwięcej błędów",
                                                    color = Color(0xFFE53935),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Suma błędów: $worstErrorsTotal",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "Czas: ${worstErrorsTime ?: " - "} s",
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "Poznawcze: $worstErrorsCognitive\nSkojarzeniowe: $worstErrorsAssociation\nLokalizacyjne: $worstErrorsLocalization\nUwaga: $worstErrorsAttention",
                                                    color = textColor.copy(alpha = 0.9f),
                                                    fontSize = 13.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .background(selectedColor, shape = RoundedCornerShape(12.dp))
                        .clickable {
                            val chosenCards = boardSizes[selectedBoard]
                            prefs.edit()
                                .putInt("challenge_set_size", selectedMode)
                                .putInt("challenge_level_index", selectedBoard)
                                .apply()

                            navController.navigate("challenge_game")
                        }
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Start",
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(Modifier.height(50.dp))
            }
        }
    }
}