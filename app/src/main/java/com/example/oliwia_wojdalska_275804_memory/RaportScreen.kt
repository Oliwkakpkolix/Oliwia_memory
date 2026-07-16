package com.example.oliwia_wojdalska_275804_memory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
// raport
private const val PREFS_NAME = "MemoryGamePrefs"
private const val KEY_LEVEL_ATTEMPTS = "level_attempts"
private const val KEY_THEME = "dark_mode_enabled"

data class ReportEntry(
    val score: Float,
    val level: Int,
    val success: Float,
    val errors: Int,
    val time: Int,
    val timestamp: Long,
    val setSize: Int,
    val cardsCount: Int,
    val cognitiveErrors: Int = 0,
    val associationErrors: Int = 0,
    val geographicalErrorsWithAnchor: Int = 0,
    val attentionErrors: Int = 0
)

private const val ERROR_WEIGHT = 2f
private fun combinedScore(success: Float, errors: Int): Float {
    // wynik nie może spaść poniżej 0
    return maxOf(0f, success - (errors * ERROR_WEIGHT))
}
// wczytanie prób poziomów
private fun loadAllAttempts(context: Context): List<ReportEntry> {
    // wszystkie zapisane próby
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val entries = mutableListOf<ReportEntry>()
    for (lvl in 1..200) {
        // odczyt próbzapisanych dla danego poziomu
        val raw = prefs.getString("${KEY_LEVEL_ATTEMPTS}_${lvl}", "") ?: ""
        if (raw.isEmpty()) continue
        raw.split("|").forEach { row ->
            val p = row.split(",")
            if (p.size == 6) {
                try {
                    entries.add(
                        ReportEntry(
                            level = lvl,
                            success = p[0].toFloat(),
                            errors = p[1].toInt(),
                            time = p[2].toInt(),
                            setSize = p[3].toInt(),
                            cardsCount = p[4].toInt(),
                            score = combinedScore(p[0].toFloat(), p[1].toInt()),
                            timestamp = p[5].toLong()
                        )
                    )
                } catch (_: Exception) { }
            }
        }
    }
    // zwrócenie listy posortowanej od najnowszej próby
    return entries.sortedByDescending { it.timestamp }
}
private fun loadGlobalReport(context: Context): List<ReportEntry> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // odczyt całego raportu
    val raw = prefs.getString("report_attempts", "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw.split("|").mapNotNull { row ->
        val p = row.split(",")
        try {
            when (p.size) {
                // NOWY FORMAT
                11 -> ReportEntry(
                    success = p[0].toFloat(),                                                   // skuteczność
                    errors = p[1].toInt(),                                                      // błędy
                    time = p[2].toInt(),                                                        // czas
                    setSize = p[3].toInt(),                                                     // tryb 2 - 5
                    cardsCount = p[4].toInt(),                                                  // liczba kart
                    timestamp = p[5].toLong(),                                                  // czas zapisu
                    level = p[6].toInt(),                                                       // poziom
                    score = combinedScore(p[0].toFloat(), p[1].toInt()),      // wynik łączny
                    cognitiveErrors = p[7].toInt(),
                    associationErrors = p[8].toInt(),
                    geographicalErrorsWithAnchor = p[9].toInt(),
                    attentionErrors = p[10].toInt()
                )
                // STARY FORMAT
                7 -> ReportEntry(
                    success = p[0].toFloat(),
                    errors = p[1].toInt(),
                    time = p[2].toInt(),
                    setSize = p[3].toInt(),
                    cardsCount = p[4].toInt(),
                    timestamp = p[5].toLong(),
                    level = p[6].toInt(),
                    score = combinedScore(p[0].toFloat(), p[1].toInt())
                )
                // NAJSTARSZY FORMAT
                6 -> ReportEntry(
                    success = p[0].toFloat(),
                    errors = p[1].toInt(),
                    time = p[2].toInt(),
                    setSize = p[3].toInt(),
                    cardsCount = p[4].toInt(),
                    timestamp = p[5].toLong(),
                    level = 0,
                    score = combinedScore(p[0].toFloat(), p[1].toInt())
                )
                else -> null
            }
        } catch (_: Exception) { null }
    }.sortedByDescending { it.timestamp }
}
@Composable
fun RowScope.TableCell(
    text: String,           // tresc komórki
    color: Color,           // kolor tekstu
    weight: Float,          // szerokość
    bold: Boolean = false   // tekst pogrubiony
) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .weight(weight)
            .padding(vertical = 4.dp)
    )
}
@Composable
fun CardSelectionDialog(
    darkMode: Boolean,              // jasny ciemny
    availableCards: List<Int>,      // dostępne opcje
    selectedCards: Int,             // aktualnie wybrana opcja
    onCardSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg = if (darkMode) Color(0xFF212121) else Color.White
    val textColor = if (darkMode) Color.White else Color.Black
    val highlightColor = Color(0xFF4CAF50)

    // używamy Dialog, aby umieścić go na środku ekranu, bez tła systemowego
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(280.dp)
                .background(dialogBg, shape = RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .background(dialogBg)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Wybierz liczbę kart",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = highlightColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Divider(color = highlightColor.copy(alpha = 0.5f), thickness = 1.dp)

                // przewijana lista
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp) // ogranicz wysokość i pozwól na przewijanie
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp)
                ) {
                    availableCards.forEach { count ->
                        val isSelected = count == selectedCards
                        val label = if (count == 0) "Wszystko" else "$count kart"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCardSelected(count) }
                                .background(
                                    if (isSelected) highlightColor.copy(alpha = 0.2f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) highlightColor else textColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.weight(1f))
                            if (isSelected) {
                                // mały wskaźnik wyboru
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(highlightColor, shape = RoundedCornerShape(5.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ReportScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var darkMode by remember { mutableStateOf(prefs.getBoolean(KEY_THEME, false)) }
    fun saveTheme(mode: Boolean) { prefs.edit().putBoolean(KEY_THEME, mode).apply() }
    val bgColor = if (darkMode) Color(0xFF111111) else Color.White
    val textColor = if (darkMode) Color.White else Color.Black
    var reportData by remember { mutableStateOf(emptyList<ReportEntry>()) }
    LaunchedEffect(Unit) { reportData = loadGlobalReport(context) }
    // dynamiczne wartości liczby kart, generowane z danych
    val distinctCardCounts = remember(reportData) {
        val counts = reportData.map { it.cardsCount }.distinct().sorted()
        listOf(0) + counts // 0 oznacza wszystko
    }
    var selectedCards by remember { mutableStateOf(0) }

    // filtry
    var selectedSet by remember { mutableStateOf(0) }
    val tabs = listOf("Wszystko", "2-pary", "3-pary", "4-pary", "5-par")
    val setSizes = listOf(0, 2, 3, 4, 5)

    val timeTabs = listOf("1h", "24h", "7 dni", "30 dni", "Wszystko")
    val timeRanges = listOf(1, 24, 7 * 24, 30 * 24, 0)
    var selectedTime by remember { mutableStateOf(0) } // przechowujemy bezpośrednio liczbę godzin
    val userName = remember { prefs.getString("user_name", "Gracz") ?: "Gracz" }
    // sortowanie  z datą
    val sortOptions = listOf(
        "Najnowsze → Najstarsze", // domyślny
        "Najstarsze → Najnowsze",
        "Czas ↑", "Czas ↓",
        "Błędy ↑", "Błędy ↓",
        "Wynik ↑", "Wynik ↓"
    )
    var selectedSort by remember { mutableStateOf("Najnowsze → Najstarsze") }

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
                .fillMaxSize()
                .padding(top = 90.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Text(
                text = "Ranking - $userName",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(6.dp))
            Text(
                "Odśwież ranking",
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { reportData = loadGlobalReport(context) }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Raport do PDF (uwzględnia bieżące filtry)",
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
                        val userName = prefs.getString("user_name", "Gracz") ?: "Gracz"

                        exportReportToPdf(
                            context = context,
                            allData = reportData,
                            selectedSet = selectedSet,
                            selectedCards = selectedCards,
                            selectedTimeHours = selectedTime,
                            userName = userName
                        )
                    }
            )
            Divider(color = textColor.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))
            // filtry, liczba par (Wszystko / 2 / 3 / 4 / 5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEachIndexed { index, label ->
                    val isActive = selectedSet == setSizes[index]
                    Text(
                        text = label,
                        color = if (isActive) Color(0xFF4CAF50) else textColor,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                selectedSet = setSizes[index]
                                selectedCards = 0
                            }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // liczba kart pojawia się tylko po wyborze konkretnego zestawu
            if (selectedSet != 0) {
                var showCardSelectionDialog by remember { mutableStateOf(false) }
                // listy dostępnych kart dla danego rozmiaru zestawu
                val availableCards = remember(selectedSet) {
                    when (selectedSet) {
                        2 -> listOf(0) + listOf(4, 6, 8, 12, 16, 20, 24, 28, 32, 36, 40, 48, 56)
                        3 -> listOf(0) + listOf(9, 12, 15, 18, 24, 27, 30, 36, 42, 45, 48, 54, 63, 72, 81, 90)
                        4 -> listOf(0) + listOf(8, 12, 16, 24, 28, 32, 36, 40, 48, 64, 72, 80)
                        5 -> listOf(0) + listOf(15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80)
                        else -> listOf(0)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                ) {
                    // przycisk otwierający listę
                    Box(
                        modifier = Modifier
                            .background(
                                if (darkMode) Color(0xFF1B5E20) else Color(0xFF4CAF50),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showCardSelectionDialog = true }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .align(Alignment.Center)
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (selectedCards == 0)
                                    "Wybierz liczbę kart"
                                else
                                    "Liczba kart: $selectedCards",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 6.dp)
                            )

                        }
                    }
                }

                // wybor kart
                if (showCardSelectionDialog) {
                    CardSelectionDialog(
                        darkMode = darkMode,
                        availableCards = availableCards,
                        selectedCards = selectedCards,
                        onCardSelected = { count ->
                            selectedCards = count
                            showCardSelectionDialog = false
                        },
                        onDismiss = { showCardSelectionDialog = false }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            // filtry czas (1h, 24h, 7 dni, 30 dni, Wszystko)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                timeTabs.forEachIndexed { index, label ->
                    val value = timeRanges[index]
                    val isActive = selectedTime == value
                    Text(
                        text = label,
                        color = if (isActive) Color(0xFF4CAF50) else textColor,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { selectedTime = value }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Divider(color = textColor.copy(alpha = 0.4f))
            // sortowanie
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // sortowanie po dacie
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Najnowsze → Najstarsze", "Najstarsze → Najnowsze").forEach { option ->
                        val isActive = selectedSort == option
                        Text(
                            text = option,
                            color = if (isActive) Color(0xFF1976D2) else textColor,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { selectedSort = option }
                                .padding(4.dp)
                        )
                    }
                }

                // sortowanie po czasie, błędach, wyniku
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Czas ↑", "Czas ↓", "Błędy ↑", "Błędy ↓", "Wynik ↑", "Wynik ↓").forEach { option ->
                        val isActive = selectedSort == option
                        Text(
                            text = option,
                            color = if (isActive) Color(0xFF1976D2) else textColor,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { selectedSort = option }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Divider(color = textColor.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth()) {
                TableCell("Lvl", textColor, 0.9f, true)
                TableCell("Plansza", textColor, 1.4f, true)
                TableCell("Skut.", textColor, 1.2f, true)
                TableCell("Błędy", textColor, 1f, true)
                TableCell("Czas", textColor, 1f, true)
                TableCell("Wynik", textColor, 1.2f, true)
            }
            Divider(color = textColor.copy(alpha = 0.4f))
            Spacer(Modifier.height(6.dp))

            // zastosowanie filtrów jak w PDF
            val now = remember { System.currentTimeMillis() }
            // filtr po rozmiarze planszy
            val sizeFiltered = if (selectedSet == 0) reportData else reportData.filter { it.setSize == selectedSet }
            // filtr po liczbie kart
            val cardsFiltered = if (selectedCards == 0) sizeFiltered else sizeFiltered.filter { it.cardsCount == selectedCards }
            // filtr po czasie
            val filtered = cardsFiltered.filter { entry ->
                if (selectedTime == 0) true else {
                    val diffHours = (now - entry.timestamp) / (1000 * 60 * 60)
                    diffHours <= selectedTime
                }
            }
            Spacer(Modifier.height(10.dp))
            val sorted = when (selectedSort) {
                "Najnowsze → Najstarsze" -> filtered.sortedByDescending { it.timestamp }
                "Najstarsze → Najnowsze" -> filtered.sortedBy { it.timestamp }
                "Czas ↑" -> filtered.sortedBy { it.time }
                "Czas ↓" -> filtered.sortedByDescending { it.time }
                "Błędy ↑" -> filtered.sortedBy { it.errors }
                "Błędy ↓" -> filtered.sortedByDescending { it.errors }
                "Wynik ↑" -> filtered.sortedBy { it.score }
                "Wynik ↓" -> filtered.sortedByDescending { it.score }
                else -> filtered
            }

            var expandedRow by remember { mutableStateOf<Long?>(null) }
            sorted.forEach { entry ->

            val scoreColor = when {
                    entry.score >= 80 -> Color(0xFF4CAF50)
                    entry.score >= 50 -> Color(0xFFFFA000)
                    else -> Color(0xFFE53935)
                }
                val isExpanded = expandedRow == entry.timestamp

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedRow = if (isExpanded) null else entry.timestamp
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell("P${entry.level}", textColor, 0.9f)
                        TableCell("${entry.cardsCount} (${entry.setSize} pary)", textColor, 1.4f)
                        TableCell(String.format("%.1f%%", entry.success), textColor, 1.2f)
                        TableCell("${entry.errors}", textColor, 1f)
                        TableCell("${entry.time}s", textColor, 1f)
                        TableCell(String.format("%.1f", entry.score), scoreColor, 1.2f)

                        Text(
                            text = if (isExpanded) "▲" else "▼",
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    if (isExpanded) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0D4CAF50))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    "Szczegóły sesji",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color(0xFF4CAF50)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Poziom: P${entry.level}", color = textColor)
                                Text("Układ kart: ${entry.cardsCount} (${entry.setSize} par)", color = textColor)
                                Text("Czas: ${entry.time} s", color = textColor)
                                Text("Błędy: ${entry.errors} [liczba]", color = textColor)
                                Text("Skuteczność: ${String.format("%.1f", entry.success)} %", color = textColor)
                                Text("Wynik: ${String.format("%.1f", entry.score)} [skuteczność − 2×błędy]", color = scoreColor)

                                Spacer(Modifier.height(10.dp))
                                Divider(color = Color(0x334CAF50))
                                Spacer(Modifier.height(8.dp))

                                Text("Analiza błędów", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Color(0xFF4CAF50))
                                Spacer(Modifier.height(6.dp))
                                Text("Błędy pamięciowe: ${entry.cognitiveErrors} [liczba]", color = textColor, fontSize = 14.sp)
                                Text("Opis: trudność w odtworzeniu układu kart.", color = textColor.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text("Błędy skojarzeń: ${entry.associationErrors} [liczba]", color = textColor, fontSize = 14.sp)
                                Text("Opis: pomylenie kart podobnych wizualnie/tematycznie.", color = textColor.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text("Błędy lokalizacji: ${entry.geographicalErrorsWithAnchor} [liczba]", color = textColor, fontSize = 14.sp)
                                Text("Opis: błędne miejsce lokalizacyjne.", color = textColor.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text("Błędy uwagi: ${entry.attentionErrors} [liczba]", color = textColor, fontSize = 14.sp)
                                Text("Opis: zbyt szybkie reakcje lub nieuwaga.", color = textColor.copy(alpha = 0.8f), fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Divider(color = textColor.copy(alpha = 0.1f))
                }
            }

            if (reportData.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Brak danych - zagraj, aby zobaczyć raport.", color = textColor.copy(alpha = 0.6f))
            }
        }
    }
}

// pdf
private fun timeRangeLabel(hours: Int): String = when (hours) {
    1 -> "1 godzina"
    24 -> "24 godziny"
    7 * 24 -> "7 dni"
    30 * 24 -> "30 dni"
    else -> "cały okres"
}

private fun setLabel(setSize: Int): String = if (setSize == 0) "wszystkie rozmiary" else "$setSize par"

private fun dominantErrorName(rows: List<ReportEntry>): String {
    if (rows.isEmpty()) return "błąd ogólny"
    val s = intArrayOf(
        rows.sumOf { it.cognitiveErrors },
        rows.sumOf { it.associationErrors },
        rows.sumOf { it.geographicalErrorsWithAnchor },
        rows.sumOf { it.attentionErrors }
    )
    val i = s.indices.maxByOrNull { s[it] } ?: 0
    return when (i) {
        0 -> "Pamięciowe (Zapominanie)"
        1 -> "Skojarzeń (Mylenie podobnych)"
        2 -> "Lokalizacji (Pamięć miejsca)"
        else -> "Uwagi (Pośpiech)"
    }
}
// średnia zmiana 5 ostatnich ptób
private fun List<ReportEntry>.trendOf(selector: (ReportEntry) -> Float): Float {
    // używamy ostatnich 5 prób
    val tail = this.takeLast(5)
    if (tail.size < 2) return 0f
    var s = 0f
    // suma zmian między kolejnymi próbami
    for (i in 1 until tail.size) s += selector(tail[i]) - selector(tail[i - 1])
    return s / (tail.size - 1)
}

// bierze historię oraz aktywne filtry i generuje raport
fun exportReportToPdf(
    context: Context,
    allData: List<ReportEntry>,
    selectedSet: Int,
    selectedCards: Int,
    selectedTimeHours: Int,
    userName: String
){
    if (allData.isEmpty()) {
        Toast.makeText(context, "Brak danych do eksportu", Toast.LENGTH_SHORT).show()
        return
    }

    val now = System.currentTimeMillis()

    // filtr po liczbie par
    val sizeFiltered = if (selectedSet == 0) allData else allData.filter { it.setSize == selectedSet }

    // filtr po liczbie kart
    val cardsFiltered = if (selectedCards == 0) sizeFiltered else sizeFiltered.filter { it.cardsCount == selectedCards }

    // filtr po czasie
    val filtered = cardsFiltered.filter { e ->
        if (selectedTimeHours == 0) true else {
            val diffHours = (now - e.timestamp) / (1000L * 60L * 60L)
            diffHours <= selectedTimeHours
        }
    }.sortedBy { it.timestamp }


    if (filtered.isEmpty()) {
        Toast.makeText(context, "Brak danych dla wybranych filtrów", Toast.LENGTH_SHORT).show()
        return
    }

    // ustawienia strony
    val pageWidth = 595
    val pageHeight = 842
    val left = 40f
    val right = (pageWidth - 40).toFloat()
    val topMargin = 56f
    val bottomMargin = 80f


    // kolory i styl
    fun paint(size: Float, color: Int = android.graphics.Color.BLACK, bold: Boolean = false, align: android.graphics.Paint.Align = android.graphics.Paint.Align.LEFT) =
        android.graphics.Paint().apply {
            textSize = size
            this.color = color
            isFakeBoldText = bold
            textAlign = align
            isAntiAlias = true
        }
    val green = android.graphics.Color.parseColor("#2E7D32")
    val mint  = android.graphics.Color.parseColor("#A5D6A7")
    val grayD = android.graphics.Color.parseColor("#424242")
    val grayM = android.graphics.Color.parseColor("#757575")
    val grayL = android.graphics.Color.parseColor("#BDBDBD")

    val h1 = paint(20f, green, true)
    val h2 = paint(14.5f, grayD, true)
    val small = paint(10.5f, grayM)
    val body = paint(12f, grayD)
    val bodyBold = paint(12f, grayD, true)
    val caption = paint(10f, grayM)
    val axis = android.graphics.Paint().apply { color = grayL; strokeWidth = 1.6f; isAntiAlias = true }
    val grid = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#E0E0E0"); strokeWidth = 1f; isAntiAlias = true }
    val lineSuccess = android.graphics.Paint().apply { color = green; strokeWidth = 2.5f; isAntiAlias = true }
    val lineErrors  = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#E53935"); strokeWidth = 2.5f; isAntiAlias = true }
    val lineScore   = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#1E88E5"); strokeWidth = 2.5f; isAntiAlias = true }

    val pdf = PdfDocument()
    var pageIndex = 0
    lateinit var page: PdfDocument.Page
    lateinit var canvas: android.graphics.Canvas
    var y = topMargin

    fun startPage(): Unit {
        pageIndex++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
        canvas = page.canvas
        y = topMargin

        // nagłówek na każdej stronie
        try {
            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            val scaled = Bitmap.createScaledBitmap(bmp, 64, 64, true)
            canvas.drawBitmap(scaled, left, y - 24f, null)
        } catch (_: Exception) { }

        canvas.drawText("Raport Progresji - Memory Trainer", left + 80f, y, h1)
        y += 20f
        canvas.drawText("Zaawansowana analiza treningu pamięci", left + 80f, y, bodyBold)
        y += 16f
        // imię użytkownika
        canvas.drawText("Użytkownik: $userName", left + 80f, y, body)
        y += 16f


        val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Data generacji: ${df.format(Date())}", left + 80f, y, small)
        y += 10f
        canvas.drawText(
            "Zakres: ${setLabel(selectedSet)}, karty: ${if (selectedCards == 0) "wszystkie" else "$selectedCards"}, okres: ${timeRangeLabel(selectedTimeHours)}",
            left + 80f, y, small
        )
        y += 15f
        y += 20f

    }

    fun finishPage() {
        // stopka z numerem strony
        val fy = (pageHeight - 24).toFloat()
        canvas.drawLine(left, fy - 10, right, fy - 10, axis)
        val capRight = android.graphics.Paint(caption)
        capRight.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText("Strona $pageIndex", right, fy, capRight)
        canvas.drawText("© Memory Trainer • raport analityczny", left, fy, caption)
        pdf.finishPage(page)
    }

    fun ensureSpace(required: Float, header: (() -> Unit)? = null) {
        // brak ściśnięcia przy końcu strony
        if (y + required > pageHeight - bottomMargin - 40f) {
            finishPage()
            startPage()
            header?.invoke()
        }
    }

    fun meter(x: Float, title: String, valueText: String, pct: Float) {
        canvas.drawText(title, x, y, bodyBold)
        canvas.drawText(valueText, x, y + 14f, small)
        val barW = 110f; val barH = 8f
        val top = y + 22f
        val bg = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#EEEEEE") }
        val fill = android.graphics.Paint().apply { color = mint }
        canvas.drawRoundRect(x, top, x + barW, top + barH, 4f, 4f, bg)
        canvas.drawRoundRect(x, top, x + barW * pct.coerceIn(0f, 1f), top + barH, 4f, 4f, fill)
    }

    // strona 1 tabela nagłówki, wykres i opisy
    startPage()

    // średnie
    val avgSuccess = filtered.map { it.success }.average().toFloat()
    val avgTime = filtered.map { it.time }.average().toFloat()
    val avgErrors = filtered.map { it.errors }.average().toFloat()
    val avgScore = filtered.map { it.score }.average().toFloat()

    meter(left +   0f, "Średnia skuteczność", String.format("%.1f %%", avgSuccess), avgSuccess / 100f)
    meter(left + 155f, "Średni wynik (score)", String.format("%.1f", avgScore), (avgScore / 100f).coerceIn(0f, 1f))
    meter(left + 310f, "Średnie błędy / próba", String.format("%.1f", avgErrors), (1f - (avgErrors / 10f)).coerceAtLeast(0f))
    y += 46f

    val colX = floatArrayOf(
        left,           // poziom
        left + 65,      // plansza
        left + 190,     // skuteczność
        left + 290,     // błędy
        left + 350,     // czas
        left + 420,     // wynik
        right
    )

    // tabela
    val tableLeft = left
    val tableRight = right
    // Poziom  Plansza  Skut.%  Błędy  Czas   Wynik
    val colW = floatArrayOf(50f,    120f,   90f,   60f,  60f,  70f)

    // start i end każdej kolumny pozwala wygodnie wyrównywać tekst
    val colStart = FloatArray(6)
    val colEnd   = FloatArray(6)
    colStart[0] = tableLeft
    for (i in 1 until colStart.size) colStart[i] = colStart[i - 1] + colW[i - 1]
    for (i in colEnd.indices) colEnd[i] = colStart[i] + colW[i]

    val padL = 6f
    val padR = 6f

    fun drawTableHeader() {
        ensureSpace(40f)
        canvas.drawText("Tabela prób (wg filtrów)", tableLeft, y, h2)
        y += 14f

        val headers = arrayOf("Poziom", "Plansza", "Skuteczność [%]", "Błędy", "Czas [s]", "Wynik")
        val headerPaint = paint(12f, grayD, true)

        y += 16f
        for (i in headers.indices) {
            if (i <= 1) {
                canvas.drawText(headers[i], colStart[i] + padL, y, headerPaint)
            } else {
                val rp = android.graphics.Paint(headerPaint).apply { textAlign = android.graphics.Paint.Align.RIGHT }
                canvas.drawText(headers[i], colEnd[i] - padR, y, rp)
            }
        }
        y += 14f
    }

    drawTableHeader()
    filtered.forEach { r ->
        ensureSpace(18f) { drawTableHeader() }
        // linia pozioma pod wierszem
        canvas.drawLine(tableLeft, y + 6f, tableRight, y + 6f, grid)

        // pomocniczo lewo/prawo wyrównanie
        fun drawCellLeft(i: Int, t: String) =
            canvas.drawText(t, colStart[i] + padL, y, body)

        fun drawCellRight(i: Int, t: String) {
            val rp = android.graphics.Paint(body).apply { textAlign = android.graphics.Paint.Align.RIGHT }
            canvas.drawText(t, colEnd[i] - padR, y, rp)
        }

        // 0,1 tekstowe lewo, 2..5 liczbowe prawo
        drawCellLeft(0, "P${r.level}")
        drawCellLeft(1, "${r.cardsCount} (${r.setSize} par)")
        drawCellRight(2, String.format("%.1f", r.success))
        drawCellRight(3, r.errors.toString())
        drawCellRight(4, r.time.toString())
        drawCellRight(5, String.format("%.1f", r.score))

        y += 26f
    }

    ensureSpace(40f)
    y += 8f

    fun drawSingleChart(
        title: String,
        rows: List<ReportEntry>,
        mapY: (ReportEntry) -> Float,
        color: Int,
        yLabel: String,
        minY: Float,
        maxY: Float
    ) {
        val chartLeft = left + 10f
        val chartTop = y + 20f
        val chartW = right - left - 20f
        val chartH = 140f

        // ramka
        val framePaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.parseColor("#A5D6A7")
            this.style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = 2f
            this.isAntiAlias = true
        }

        // tło
        val bgPaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.parseColor("#FCFCFC")
            this.style = android.graphics.Paint.Style.FILL
        }

        // siatka
        val gridPaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.parseColor("#E0E0E0")
            this.strokeWidth = 1f
            this.isAntiAlias = true
        }

        // tytuł nad ramką
        val titlePaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.parseColor("#2E7D32")
            this.textSize = 15f
            this.isFakeBoldText = true
            this.textAlign = android.graphics.Paint.Align.CENTER
            this.isAntiAlias = true
        }

        val titleCenterX = chartLeft + chartW / 2
        canvas.drawText(title, titleCenterX, y + 14f, titlePaint)

        // tło i ramka
        canvas.drawRoundRect(chartLeft, chartTop, chartLeft + chartW, chartTop + chartH, 10f, 10f, bgPaint)
        canvas.drawRoundRect(chartLeft, chartTop, chartLeft + chartW, chartTop + chartH, 10f, 10f, framePaint)

        // siatka pozioma i pionowa
        for (i in 1..4) {
            val yy = chartTop + i * (chartH / 5)
            canvas.drawLine(chartLeft, yy, chartLeft + chartW, yy, gridPaint)
        }
        for (i in 1..5) {
            val xx = chartLeft + i * (chartW / 6)
            canvas.drawLine(xx, chartTop, xx, chartTop + chartH, gridPaint)
        }

        // oznaczenia osi Y
        val labelPaint = paint(10f, android.graphics.Color.parseColor("#757575"))
        for (i in 0..5) {
            val v = minY + i * ((maxY - minY) / 5)
            val yy = chartTop + chartH - ((v - minY) / (maxY - minY)) * chartH
            canvas.drawText(String.format("%.0f", v), chartLeft - 24f, yy + 4f, labelPaint)
        }

        // linie danych
        val dataPaint = android.graphics.Paint().apply {
            this.color = color
            this.strokeWidth = 2.6f
            this.isAntiAlias = true
        }

        for (i in 0 until rows.size - 1) {
            val n = (rows.size - 1).coerceAtLeast(1)
            val x1 = chartLeft + (i / n.toFloat()) * chartW
            val x2 = chartLeft + ((i + 1) / n.toFloat()) * chartW
            val y1 = chartTop + chartH - (mapY(rows[i]) * chartH)
            val y2 = chartTop + chartH - (mapY(rows[i + 1]) * chartH)
            canvas.drawLine(x1, y1, x2, y2, dataPaint)
        }

        // opisy osi
        y += chartH + 30f
        canvas.drawText("Oś X: kolejne próby", chartLeft, y, labelPaint)
        canvas.drawText("Oś Y: $yLabel", chartLeft, y + 12f, labelPaint)
        y += 25f // odstęp między wykresami
    }


    // rysowanie wykresów na nowej stronie
    finishPage()
    startPage()
    y += 20f // lekki margines pod nagłówkiem

    // wszystkie trzy wykresy na jednej stronie
    drawSingleChart(
        "Skuteczność [%]", filtered,
        { it.success / 100f },
        android.graphics.Color.parseColor("#2E7D32"),
        "Skuteczność [%]", 0f, 100f
    )

    drawSingleChart(
        "Wynik (score)", filtered,
        { it.score / 100f },
        android.graphics.Color.parseColor("#1E88E5"),
        "Wynik (0-100)", 0f, 100f
    )

    val maxErr = (filtered.maxOfOrNull { it.errors } ?: 10).toFloat().coerceAtLeast(1f)
    drawSingleChart(
        "Błędy [liczba]", filtered,
        { it.errors / maxErr },
        android.graphics.Color.parseColor("#E53935"),
        "Błędy [liczba]", 0f, maxErr
    )
    ensureSpace(300f)

    y += 20f
    y += 10f

    fun definitionBullet(title: String, desc: String) {
        val dot = android.graphics.Paint().apply { color = green }
        canvas.drawCircle(left + 4f, y - 4f, 2.4f, dot)

        canvas.drawText(title, left + 14f, y, bodyBold)
        y += 18f
        ensureSpace(50f)

        // Logic for wrapping text
        val words = desc.split(" ")
        val maxWidth = right - left - 20f
        var line = ""
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            val width = body.measureText(test)
            if (width > maxWidth) {
                canvas.drawText(line.trim(), left + 14f, y, body)
                y += 18f
                line = word
                ensureSpace(50f)
            } else line = test
        }
        if (line.isNotBlank()) {
            canvas.drawText(line.trim(), left + 14f, y, body)
            y += 18f
        }
        y += 8f
    }

    fun simpleBullet(text: String) {
        val dot = android.graphics.Paint().apply { color = green }
        val words = text.split(" ")
        val maxWidth = right - left - 20f
        var line = ""
        var isFirstLine = true

        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            val width = body.measureText(test)
            if (width > maxWidth) {
                if (line.isNotBlank()) {
                    if (isFirstLine) {
                        canvas.drawCircle(left + 4f, y - 4f, 2.4f, dot)
                        isFirstLine = false
                    }
                    canvas.drawText(line.trim(), left + 14f, y, body)
                    y += 18f
                    ensureSpace(50f)
                }
                line = word
            } else line = test
        }
        if (line.isNotBlank()) {
            if (isFirstLine) canvas.drawCircle(left + 4f, y - 4f, 2.4f, dot)
            canvas.drawText(line.trim(), left + 14f, y, body)
            y += 18f
        }
        y += 4f
    }


    // definicje i interpretacje
    ensureSpace(120f)
    canvas.drawText("Co Oznaczają Wskaźniki Wyników?", left, y, h2); y += 28f

    definitionBullet(
        title = "Skuteczność [%]",
        desc = "Określa, jak wiele par kart udało się prawidłowo dopasować. Wysoka skuteczność oznacza dobrą koncentrację i dokładność zapamiętywania. Zmniejszanie się skuteczności może sugerować zmęczenie lub zbyt duże tempo pracy."
    )
    definitionBullet(
        title = "Błędy [liczba]",
        desc = "Liczba pomyłek w danej sesji. Pokazuje poziom ostrożności i uwagi. Mała liczba błędów oznacza dobre skupienie, a ich wzrost może wskazywać na rozproszenie lub pośpiech."
    )

    definitionBullet(
        title = "Czas [s]",
        desc = "Czas trwania gry mierzony w sekundach. Pomaga ocenić tempo reakcji i sposób działania. Zbyt krótki czas przy dużej liczbie błędów sugeruje pośpiech, a zbyt długi - nadmierne zastanawianie się."
    )
    definitionBullet(
        title = "Wynik (Score)",
        desc = "To zintegrowana ocena skuteczności i liczby błędów. Im wyższy wynik, tym lepiej gracz łączy pamięć, uwagę i refleks. Jest to najważniejszy wskaźnik ogólnego postępu."
    )

    y += 10f
    canvas.drawText("Rodzaje Błędów i ich Przyczyny:", left, y, bodyBold); y += 20f

    // rozpisane błędy
    definitionBullet(
        title = "Błędy pamięciowe",
        desc = "Wynikają z trudności w przypomnieniu sobie, gdzie leżała dana karta. Mogą świadczyć o chwilowym zmęczeniu lub osłabieniu pamięci krótkotrwałej."
    )

    definitionBullet(
        title = "Błędy skojarzeń",
        desc = "Polegają na myleniu kart o podobnym wyglądzie lub tematyce. Wskazują na brak skupienia na detalach lub zbyt szybkie decyzje bez analizy różnic."
    )

    definitionBullet(
        title = "Błędy lokalizacji",
        desc = "Oznaczają pomyłki w zapamiętaniu położenia kart. Często pojawiają się, gdy gracz skupia się bardziej na obrazkach niż na przestrzennym układzie planszy."
    )
    definitionBullet(
        title = "Błędy Uwagi (Pośpiech)",
        desc = "Pomyłki popełnione zbyt szybko, bez zastanowienia. Jest to problem z kontrolą impulsywną i skupieniem w trakcie sesji."
    )

    definitionBullet(
        title = "Błędy uwagi",
        desc = "Są skutkiem pośpiechu lub rozproszenia. Najczęściej występują, gdy gracz klika zbyt szybko lub traci koncentrację podczas gry."
    )

    // analiza wyników
    ensureSpace(140f)
    canvas.drawText("Podsumowanie Wyników i Wnioski", left, y, h2)
    y += 20f

    // Użycie nowej, zewnętrznej funkcji trendOf
    val trendSucc = filtered.trendOf { it.success }
    val trendErrs = filtered.trendOf { it.errors.toFloat() }
    val trendScore = filtered.trendOf { it.score }


    fun trendWord(v: Float, higherIsBetter: Boolean = true): String {
        return when {
            v > 2.0f -> if (higherIsBetter) "wyraźnie rosnący (poprawa)" else "wyraźnie rosnący (pogorszenie)"
            v < -2.0f -> if (higherIsBetter) "spadkowy (pogorszenie)" else "spadkowy (poprawa)"
            else -> "stabilny (bez większych zmian)"
        }
    }

// sekcja wniosków
    simpleBullet(
        "Główny obszar do poprawy - Najczęściej pojawiającym się typem błędów są ${dominantErrorName(filtered).lowercase()}. " +
                "Wynika to z indywidualnego stylu zapamiętywania i sposobu skupienia uwagi. " +
                "Aby poprawić ten aspekt, warto skoncentrować się na: " + when (dominantErrorName(filtered).split(" (").first()) {
            "Pamięciowe" -> "utrwalaniu informacji poprzez tworzenie krótkich skojarzeń lub powtarzanie sekwencji wzrokowych."
            "Skojarzeń" -> "ćwiczeniu rozróżniania szczegółów - analizie różnic między podobnymi kartami."
            "Lokalizacji" -> "utrwalaniu położenia kart, np. poprzez mentalne dzielenie planszy na strefy lub stosowanie metody „pałacu pamięci”."
            else -> "zwolnieniu tempa, utrzymaniu koncentracji i kontrolowaniu odruchowych reakcji, by ograniczyć błędy pośpiechu."
        }
    )

    simpleBullet(
        "Ocena ogólna - Twoja skuteczność wynosi ${"%.1f".format(avgSuccess)}%, co oznacza, że poziom zapamiętywania i koncentracji jest " +
                when {
                    avgSuccess >= 85 -> "bardzo dobry - pokazuje wysoką precyzję i kontrolę poznawczą."
                    avgSuccess >= 70 -> "dobry - widać solidne podstawy, które można dalej rozwijać poprzez regularne ćwiczenie."
                    else -> "wymagający poprawy - wskazuje na niestabilną koncentrację lub zbyt duże tempo działania."
                }
    )

    simpleBullet(
        "Analiza trendów (ostatnie 5 prób) - " +
                "trend wyniku: ${trendWord(trendScore, higherIsBetter = true)}, " +
                "trend skuteczności: ${trendWord(trendSucc, higherIsBetter = true)}, " +
                "trend błędów: ${trendWord(trendErrs, higherIsBetter = false)}. " +
                "Interpretacja: " + when {
            trendScore > 2.0f && trendErrs < -0.5f -> "Twoje wyniki rosną dynamicznie, a liczba błędów spada - to bardzo korzystny kierunek rozwoju."
            trendScore > 0f -> "postęp jest widoczny i stabilny, co sugeruje skuteczne strategie zapamiętywania."
            trendScore <= 0f && trendErrs > 0f -> "efektywność chwilowo spadła, co może wynikać ze zmęczenia lub braku koncentracji."
            else -> "wyniki są stabilne, co świadczy o utrwaleniu umiejętności, ale warto wprowadzić drobne zmiany w podejściu."
        }
    )
    // rekomendacje
    ensureSpace(100f)
    canvas.drawText("Konkretne Zalecenia Treningowe", left, y, h2)
    y += 20f

    simpleBullet(
        "Cel główny - Dąż do utrzymania skuteczności ≥ 85% oraz liczby błędów ≤ 3 w trzech kolejnych próbach. " +
                "Optymalna długość jednej sesji treningowej to 10-15 minut dziennie, najlepiej w spokojnym, cichym otoczeniu. " +
                "Regularność ma większe znaczenie niż długość pojedynczej gry."
    )

    simpleBullet(
        "Ćwiczenie „Pauza-Oczekiwanie” - Po odkryciu karty zatrzymaj się na sekundę, zanim wybierzesz drugą. " +
                "Ten prosty nawyk znacząco redukuje błędy uwagi i poprawia kontrolę poznawczą. " +
                "Zamiast działać automatycznie, naucz się „chwili skupienia” przed każdym ruchem."
    )

    simpleBullet(
        "Technika Wizualna - Gdy karty są podobne, twórz krótkie, zapamiętywalne etykiety słowne lub absurdalne skojarzenia (np. „kot z kapeluszem”, „drzewo z chmurą”). " +
                "Angażowanie wyobraźni i humoru zwiększa trwałość śladu pamięciowego nawet o kilkadziesiąt procent."
    )

    simpleBullet(
        "Zwiększanie Poziomu - Rozszerzaj planszę lub zwiększaj trudność tylko wtedy, gdy w ostatnich trzech próbach " +
                "utrzymujesz wynik (Score) ≥ 60 i liczba błędów nie przekracza 3. " +
                "Stopniowe podnoszenie poziomu pozwala uniknąć frustracji i utrwala dobre nawyki poznawcze."
    )

    simpleBullet(
        "Wskazówka dodatkowa - Jeśli zauważysz spadek skuteczności, zmień porę treningu lub skróć sesje. " +
                "Umysł zapamiętuje najlepiej, gdy jest wypoczęty i skupiony - unikaj gry w stanie zmęczenia."
    )

    y += 30f
    ensureSpace(50f)
    // zakończenie raportu
    ensureSpace(100f)
    canvas.drawText("Podsumowanie i Wnioski Końcowe", left, y, h2)
    y += 20f

    simpleBullet(
        "Trening pamięci to proces stopniowy - kluczowe znaczenie ma regularność, koncentracja i świadome tempo pracy. " +
                "Nawet niewielkie postępy w skuteczności z każdej sesji kumulują się, prowadząc do trwałego wzmocnienia zdolności poznawczych."
    )

    simpleBullet(
        "Rekomenduje się utrzymanie dotychczasowych postępów poprzez krótkie, ale systematyczne ćwiczenia. " +
                "Warto obserwować własne reakcje poznawcze i dopasowywać poziom trudności do bieżącej formy psychicznej."
    )

    simpleBullet(
        "Jeśli zauważalne będą wyraźne wahania skuteczności lub nadmierne zmęczenie poznawcze, " +
                "zaleca się krótką przerwę regeneracyjną lub konsultację z trenerem poznawczym / terapeutą neuropsychologicznym, " +
                "aby zoptymalizować dalszy proces treningowy."
    )

    y += 30f
    // ostatni akapit
    simpleBullet(
        "Pamiętaj: największy progres osiągają osoby, które ćwiczą konsekwentnie i z ciekawością. " +
                "Każda sesja to nie tylko gra, ale trening koncentracji, spostrzegawczości i samokontroli, " +
                "które realnie przekładają się na codzienne funkcjonowanie poznawcze."
    )

    y += 40f
    ensureSpace(50f)

    // stopka końcowa
    val footerPaint = paint(10.5f, android.graphics.Color.parseColor("#757575"))
    canvas.drawText(
        "Raport wygenerowano automatycznie w oparciu o dane z aplikacji Memory Trainer.",
        left,
        y,
        footerPaint
    )
    y += 14f
    canvas.drawText(
        "Wersja raportu: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}",
        left,
        y,
        footerPaint
    )

    y += 30f
    ensureSpace(40f)

    finishPage()

    // zapis
    try {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Raport_analityczny_${System.currentTimeMillis()}.pdf"
        )
        FileOutputStream(file).use { fos -> pdf.writeTo(fos) }
        pdf.close()
        Toast.makeText(context, "Zapisano: Pobrane/${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        pdf.close()
        Toast.makeText(context, "Błąd zapisu PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}