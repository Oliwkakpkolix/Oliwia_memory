package com.example.oliwia_wojdalska_275804_memory

import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Star
import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
// ćwiczenie umysłu
fun getBackgroundForTheme(context: Context): Int {
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    val selectedTheme = prefs.getString("selected_theme", "theme1") ?: "theme1"
    val textColor = when (selectedTheme) {
        "theme2" -> Color(0xFFE3C77B)
        "theme3" -> Color(0xFF80D8FF)
        "theme4" -> Color(0xFFFF8A80)
        "theme5" -> Color(0xFF9BE2FF)
        else -> Color(0xFF000000)
    }
    // tło do gry
    return when (selectedTheme) {
        "theme2" -> R.drawable.t3_tlo
        "theme3" -> R.drawable.t4_tlo
        "theme4" -> R.drawable.t2_tlo
        "theme5" -> R.drawable.t5_tlo
        else -> R.drawable.t1_tlo
    }
}

interface BaseGameStats {
    var cognitiveErrors: Int
    var associationErrors: Int
    var geographicalErrorsWithAnchor: Int
    var attentionErrors: Int
    var levelTime: Int
}

data class LevelAttempt(
    val successRate: Float,
    val errorCount: Int,
    val time: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val setSize: Int,
    val cardsCount: Int,
    var level: Int = 0,
    // szczegółowe błędy
    var cognitiveErrors: Int = 0,
    var associationErrors: Int = 0,
    var geographicalErrorsWithAnchor: Int = 0,
    var attentionErrors: Int = 0
)

enum class ProgressionResult {
    ADVANCE,            // przejście do następnego poziomu
    REPEAT_SAME_LEVEL,  // powtórzenie tego samego poziomu
    REGRESS,            // cofnięcie do poprzedniego poziomu
    NOT_ENOUGH_ATTEMPTS // niewystarczająca liczba prób
}

// stałe
private const val PREFS_NAME = "MemoryGamePrefs"
private const val KEY_LEVEL = "level"
private const val KEY_LEVEL_INDEX = "currentLevelIndex"
private const val KEY_SET_SIZE = "set_size" // 2, 3, 4 lub 5
private const val KEY_LEVEL_ATTEMPTS = "level_attempts" // przechowuje wyniki prób dla każdego poziomu

private const val KEY_REPORT_ATTEMPTS = "report_attempts"

// rozmiary plansz dla różnych trybów
private val LEVEL_SIZES_2 = listOf(4, 6, 8, 12, 16, 20, 24, 28, 32, 36, 40, 48, 56) // pary
private val LEVEL_SIZES_3 = listOf(9, 12, 15, 18, 24, 27, 30, 36, 42, 45, 48, 54, 63, 72, 81, 90) // trójki
private val LEVEL_SIZES_4 = listOf(8, 12, 16, 24, 28, 32, 36, 40, 48, 64, 72, 80) // czwórki
private val LEVEL_SIZES_5 = listOf(15, 20, 25, 30, 25, 40, 45, 50, 60, 70, 80) // piątki

// pobieranie aktualnej listy rozmiarów
private fun getCurrentLevelSizes(setOfCardsSize: Int): List<Int> {
    return when (setOfCardsSize) {
        3 -> LEVEL_SIZES_3
        4 -> LEVEL_SIZES_4
        5 -> LEVEL_SIZES_5
        else -> LEVEL_SIZES_2 // domyślnie pary
    }
}

// zapis listy ptób danego poziomu
private fun saveLevelAttempts(context: Context, level: Int, attempts: List<LevelAttempt>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val data = attempts.joinToString("|") {
        "${it.successRate},${it.errorCount},${it.time},${it.setSize},${it.cardsCount},${it.timestamp},${it.level}"
    }
    prefs.edit().putString("${KEY_LEVEL_ATTEMPTS}_${level}", data).apply()
}
// wczytanie listy prób danego poziomu
private fun loadLevelAttempts(context: Context, level: Int): List<LevelAttempt> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString("${KEY_LEVEL_ATTEMPTS}_${level}", "") ?: ""
    if (raw.isBlank()) return emptyList() // brak danych, pusta lista

    return raw.split("|").mapNotNull { row ->
        val p = row.split(",")

        try {
            when (p.size) {
                // nowy format 7 pól
                7 -> LevelAttempt(
                    successRate = p[0].toFloatOrNull() ?: 0f,
                    errorCount  = p[1].toIntOrNull() ?: 0,
                    time        = p[2].toIntOrNull() ?: ((p[2].toLongOrNull() ?: 0L) / 1000L).toInt(),
                    setSize     = p[3].toIntOrNull() ?: 2,
                    cardsCount  = p[4].toIntOrNull() ?: 4,
                    timestamp   = p[5].toLongOrNull() ?: System.currentTimeMillis(),
                    level       = p[6].toIntOrNull() ?: 0
                )

                // stary format 4 pola
                4 -> LevelAttempt(
                    successRate = p[0].toFloatOrNull() ?: 0f,
                    errorCount = p[1].toIntOrNull() ?: 0,
                    time = p[2].toIntOrNull()
                        ?: ((p[2].toLongOrNull() ?: 0L) / 1000L).toInt(),
                    setSize = 2,
                    cardsCount = 4,
                    timestamp = p[3].toLongOrNull() ?: System.currentTimeMillis()
                )
                else -> null
            }
        } catch (e: Exception) {
            null // w przypadku błędu ignorujemy
        }
    }
}
// usunięcie zapisanych prób danego poziomu
private fun clearLevelAttempts(context: Context, level: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().remove("${KEY_LEVEL_ATTEMPTS}_${level}").apply()
}
// zapis każdej próby
private fun appendReportAttempt(context: Context, attempt: LevelAttempt) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val old = prefs.getString(KEY_REPORT_ATTEMPTS, "") ?: ""
    // nowy wiersz z pełnymi danymi próby, szczegółowe statystyki
    val newRow =
        "${attempt.successRate},${attempt.errorCount},${attempt.time},${attempt.setSize},${attempt.cardsCount}," +
                "${attempt.timestamp},${attempt.level},${attempt.cognitiveErrors},${attempt.associationErrors}," +
                "${attempt.geographicalErrorsWithAnchor},${attempt.attentionErrors}"
    // dodajemy do staregoo rapartu lub tworzymy nowy
    val updated = if (old.isBlank()) newRow else "$old|$newRow"
    prefs.edit().putString(KEY_REPORT_ATTEMPTS, updated).apply()
}
// wczytanie raportu prób
private fun loadReportAttempts(context: Context): MutableList<LevelAttempt> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_REPORT_ATTEMPTS, "") ?: ""
    if (raw.isBlank()) return mutableListOf()
    // mapowanie rekordów
    val list = raw.split("|").mapNotNull { row ->
        val p = row.split(",")
        try {
            when (p.size) {
                7 -> LevelAttempt(
                    successRate = p[0].toFloatOrNull() ?: 0f,
                    errorCount  = p[1].toIntOrNull() ?: 0,
                    time        = p[2].toIntOrNull() ?: ((p[2].toLongOrNull() ?: 0L) / 1000L).toInt(),
                    setSize     = p[3].toIntOrNull() ?: 2,
                    cardsCount  = p[4].toIntOrNull() ?: 4,
                    timestamp   = p[5].toLongOrNull() ?: System.currentTimeMillis(),
                    level       = p[6].toIntOrNull() ?: 0
                )

                6 -> LevelAttempt( // starszy format bez level
                    successRate = p[0].toFloatOrNull() ?: 0f,
                    errorCount  = p[1].toIntOrNull() ?: 0,
                    time        = p[2].toIntOrNull() ?: ((p[2].toLongOrNull() ?: 0L) / 1000L).toInt(),
                    setSize     = p[3].toIntOrNull() ?: 2,
                    cardsCount  = p[4].toIntOrNull() ?: 4,
                    timestamp   = p[5].toLongOrNull() ?: System.currentTimeMillis(),
                    level       = 0
                )

                4 -> LevelAttempt( // bardzo stary format
                    successRate = p[0].toFloatOrNull() ?: 0f,
                    errorCount  = p[1].toIntOrNull() ?: 0,
                    time        = p[2].toIntOrNull() ?: ((p[2].toLongOrNull() ?: 0L) / 1000L).toInt(),
                    setSize     = 2,
                    cardsCount  = 4,
                    timestamp   = p[3].toLongOrNull() ?: System.currentTimeMillis(),
                    level       = 0
                )

                else -> null
            }

        } catch (_: Exception) { null }
    }
    return list.toMutableList()
}
// usunięcie raportu prób
private fun clearReportAttempts(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().remove(KEY_REPORT_ATTEMPTS).apply()
}
// stała określająca karę za błąd
private const val ERROR_WEIGHT = 2f
// oblicza wynik po uwzględnieniu liczby błędów
private fun combinedScore(success: Float, errors: Int): Float {
    // wynik nie może spaść poniżej 0
    return maxOf(0f, success - (errors * ERROR_WEIGHT))
}
// co zrobić z graczem po kilku próbach
private fun evaluateProgressionConditions(attempts: List<LevelAttempt>): ProgressionResult {
    // ostatnia próba idealna = awans
    if (attempts.isNotEmpty()) {
        val last = attempts.last()
        if (last.errorCount == 0 && last.successRate >= 99f) {
            return ProgressionResult.ADVANCE
        }
    }
    // do 3 prób
    if (attempts.size < 3) return ProgressionResult.NOT_ENOUGH_ATTEMPTS
    val a = attempts[attempts.size - 3]
    val b = attempts[attempts.size - 2]
    val c = attempts.last()
    // modyfikacja trudności w zależności od liczby kart
    val difficultyModifier = when {
        c.cardsCount >= 70 -> 0.75f  // duże plansze = łagodniejsza ocena
        c.cardsCount >= 50 -> 0.85f
        c.cardsCount >= 30 -> 0.9f
        else -> 1f
    }

    // oblicza jakość próby i średnią z poprzednich
    val avgBefore = (
            combinedScore(a.successRate, a.errorCount) +
                    combinedScore(b.successRate, b.errorCount)
            ) / 2f
    val current = combinedScore(c.successRate, c.errorCount) * difficultyModifier
    val diffQuality = current - avgBefore
    val avgTime = (a.time + b.time) / 2f
    val diffTime = avgTime - c.time
    var score = 0

    // trend jakości
    score += when {
        diffQuality > 8 -> 50
        diffQuality > 3 -> 30
        diffQuality >= 0 -> 10
        diffQuality > -3 -> 0
        else -> -25
    }

    // trend czasu
    score += when {
        diffTime > 5 -> 25
        diffTime > 0 -> 15
        diffTime >= 0 -> 5
        diffTime > -5 -> 0
        else -> -15
    }

    // minimalny próg jakości pamięci
    if (current < 30) return ProgressionResult.REGRESS
    if (current in 30f..49f) return ProgressionResult.REPEAT_SAME_LEVEL
    // decyzja o progresji
    return when {
        score >= 50 -> ProgressionResult.ADVANCE
        score >= 15 -> ProgressionResult.REPEAT_SAME_LEVEL
        else -> ProgressionResult.REGRESS
    }

}
// oobliczanie całkowitej liczby błędów
fun calculateTotalErrors(stats: BaseGameStats): Int {
    return stats.cognitiveErrors + stats.associationErrors +
            stats.geographicalErrorsWithAnchor + stats.attentionErrors
}
// zapisywanie postępów gracza
private fun saveProgress(context: Context, level: Int, currentLevelIndex: Int, setOfCardsSize: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
        putInt(KEY_LEVEL, level)
        putInt(KEY_LEVEL_INDEX, currentLevelIndex)
        putInt(KEY_SET_SIZE, setOfCardsSize) // zapis trybu gry 2 - 5
        apply()
    }
}
// wczytuje postęp gracza
private fun loadProgress(context: Context): Triple<Int, Int, Int> { // level, index, setSize
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val level = prefs.getInt(KEY_LEVEL, 1)
    val setSize = prefs.getInt(KEY_SET_SIZE, 2).coerceIn(2, 5) // domyślnie pary
    val currentSizes = getCurrentLevelSizes(setSize)
    // wczytany index mieści się w zakresie aktualnego trybu
    val index = prefs.getInt(KEY_LEVEL_INDEX, 0).coerceIn(0, currentSizes.lastIndex)

    return Triple(level, index, setSize)
}
// resetuje postęp do wartości początkowych
private fun resetProgress(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
        putInt(KEY_LEVEL, 1)
        putInt(KEY_LEVEL_INDEX, 0)
        putInt(KEY_SET_SIZE, 2)
        apply()
    }
    // czyszczenie wszystkich zapisanych prób przy resecie
    for (level in 1..100) { // zakładamy maksymalnie 100 poziomów
        clearLevelAttempts(context, level)
    }
}
// bezpieczne zwalnianie MediaPlayer
fun safeReleaseMediaPlayer(player: MediaPlayer?): MediaPlayer? {
    try {
        if (player != null) {
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
    } catch (e: Exception) {
        println("Błąd podczas zwalniania MediaPlayer: ${e.message}")
    }
    return null
}

// pobiera nazwę trybu
private fun getSetTypeText(setOfCardsSize: Int): String {
    return when (setOfCardsSize) {
        2 -> "Par"
        3 -> "Trójek"
        4 -> "Czwórek"
        5 -> "Piątek"
        else -> "Kart"
    }
}

@Composable
fun MindTrainingScreen(navController: NavController) {
    val context = LocalContext.current
    // kolory napisów (Gotowy, Start, Czas) zależne tylko od motywu kart
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    val selectedTheme = prefs.getString("selected_theme", "theme1") ?: "theme1"
    val textGradientColors = when (selectedTheme) {
        "theme2" -> listOf(
            Color(0xFFFFE082), Color(0xFFFFC107)
        )
        "theme3" -> listOf(
            Color(0xFF81D4FA).copy(alpha = 0.95f),
            Color(0xFFFFFFFF).copy(alpha = 0.92f)
        )
        "theme4" -> listOf(
            Color(0xFFFF8A80), Color(0xFFE53935)
        )
        "theme5" -> listOf(
            Color(0xFF9BE2FF), Color(0xFFE0F7FA)
        )

        else -> listOf(
            Color(0xFFFFECB3), Color(0xFFFFC107)
        )
    }


    // wymuszenie restartu tego samego poziomu (bez zmiany level)
    var reloadKey by remember { mutableStateOf(0) }

    // wczytanie postępu
    val (initialLevel, initialIndex, initialSetSize) = remember { loadProgress(context) }
    var startSound: MediaPlayer? by remember { mutableStateOf(null) }
    var resultMusic: MediaPlayer? by remember { mutableStateOf(null) }
    var elapsedTime by remember { mutableStateOf(0) }
    var gameRunning by remember { mutableStateOf(false) }

    // użycie wczytanych stanów
    var level by remember { mutableStateOf(initialLevel) }
    var currentLevelIndex by remember { mutableStateOf(initialIndex) }
    var setOfCardsSize by remember { mutableStateOf(initialSetSize) }
    var currentSessionAttemptNumber by remember { mutableStateOf(1) }
    // śledzenie aktualnych prób poziomu
    var currentLevelAttempts by remember { mutableStateOf(loadLevelAttempts(context, level)) }
    var progressionResult by remember { mutableStateOf<ProgressionResult?>(null) }
// trwale rosnąca lista prób (nie czyści się przy starcie poziomu)
    var reportStats by remember { mutableStateOf(loadReportAttempts(context)) }

    // dynamiczne pobieranie aktualnych rozmiarów
    val currentLevelSizes = getCurrentLevelSizes(setOfCardsSize)
    val maxLevelIndex = currentLevelSizes.lastIndex
    val cardsCount = currentLevelSizes[currentLevelIndex] // liczba kart dla aktualnego poziomu

    var showStartText by remember { mutableStateOf(true) }
    val alphaAnim = remember { Animatable(0f) }
    val bounceAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    var levelCompleted by remember { mutableStateOf(false) }
    // stan animacji nagrody
    var earnedCoins by remember { mutableStateOf(0) }
    var showCoinReward by remember { mutableStateOf(false) }

    var levelTimeResult by remember { mutableStateOf(0) }
    var successRate by remember { mutableStateOf(0f) }

    var errorCount by remember { mutableStateOf(0) } // licznik błędów

    var cognitiveErrorsResult by remember { mutableStateOf(0) }
    var associationErrorsResult by remember { mutableStateOf(0) }
    var geographicalErrorsWithAnchorResult by remember { mutableStateOf(0) }
    var attentionErrorsResult by remember { mutableStateOf(0) }

    val userGender = prefs.getString("user_gender", "none")
    val readyText = when(userGender) {
        "male" -> "Gotowy?"
        "female" -> "Gotowa?"
        else -> "Gotowi?"
    }

    // restart tej samej planszy (bez zmiany level, indexów i trybu)
    fun restartSameLevel() {
        resultMusic = safeReleaseMediaPlayer(resultMusic)
        levelCompleted = false
        gameRunning = false
        elapsedTime = 0
        levelTimeResult = 0
        successRate = 0f
        errorCount = 0
        currentSessionAttemptNumber++
        reloadKey++
    }
    fun goNextLevel() { // awans
        resultMusic = safeReleaseMediaPlayer(resultMusic)
        if (currentLevelIndex < maxLevelIndex) {
            currentLevelIndex++
        } else {
            if (setOfCardsSize < 5) {
                setOfCardsSize++
            }
            currentLevelIndex = 0
        }
        level++

        saveProgress(context, level, currentLevelIndex, setOfCardsSize) // zapis
        currentSessionAttemptNumber = 1

        clearLevelAttempts(context, level)
        currentLevelAttempts = emptyList()

        levelCompleted = false
        gameRunning = false
        elapsedTime = 0
    }

    fun goRepeatLevel() { // powtórka
        resultMusic = safeReleaseMediaPlayer(resultMusic)
        // tu nie zmieniamy level, ale zapisujemy obecną pozycję
        saveProgress(context, level, currentLevelIndex, setOfCardsSize) // zapis
// nowy poziom, czyścimy próby tego poziomu
        clearLevelAttempts(context, level)
        currentLevelAttempts = emptyList()

        restartSameLevel()
    }

    fun goPreviousLevel() { // poprzedni poziom
        resultMusic = safeReleaseMediaPlayer(resultMusic)

        // cofnij poziom numer
        level = maxOf(1, level - 1)

        // cofnij index planszy
        if (currentLevelIndex > 0) {
            currentLevelIndex--
        } else {
            if (setOfCardsSize > 2) {
                setOfCardsSize--
                val previousSizes = getCurrentLevelSizes(setOfCardsSize)
                currentLevelIndex = previousSizes.lastIndex
            } else {
                currentLevelIndex = 0
            }
        }

        // zachowujemy dane NIE czyścimy historii prób
        saveProgress(context, level, currentLevelIndex, setOfCardsSize)
        currentSessionAttemptNumber = 1
        // nowy poziom, czyścimy próby tego poziomu
        clearLevelAttempts(context, level)
        currentLevelAttempts = emptyList()

        // reset gry
        levelCompleted = false
        gameRunning = false
        elapsedTime = 0

        // wymuszenie przeładowania planszy
        reloadKey++
    }

    // animacja startu gry
    // start poziomu uruchamiany gdy zmieni się level (awans/regres)
// lub gdy wymusimy restart tej samej planszy (reloadKey)
    LaunchedEffect(level, reloadKey) {
        elapsedTime = 0
        levelCompleted = false
        progressionResult = null
        showStartText = true
        alphaAnim.snapTo(0f)
        bounceAnim.snapTo(1f)

        // wczytaj próby dla AKTUALNEGO numeru level (nie zmieniaj level przy powtórce!!!!!)
        currentLevelAttempts = loadLevelAttempts(context, level)

        startSound = MediaPlayer.create(context, R.raw.start_sound).apply {
            setVolume(SoundSettings.startVolume, SoundSettings.startVolume)
        }
        alphaAnim.animateTo(targetValue = 1f, animationSpec = tween(800))
        scope.launch {
            delay(500L)
            startSound?.start()
            bounceAnim.animateTo(
                targetValue = 1.3f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            bounceAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        delay(800L)
        alphaAnim.animateTo(targetValue = 0f, animationSpec = tween(800))
        delay(200L)
        showStartText = false
        gameRunning = true

        startSound = safeReleaseMediaPlayer(startSound)
    }

    // licznik w górę, działa tylko gdy gra trwa
    LaunchedEffect(gameRunning) {
        if (gameRunning) {
            while (gameRunning) {
                delay(1000L)
                elapsedTime++
            }
        }
    }
    // muzyka po zakończeniu poziomu, zapis następnego poziomu
    LaunchedEffect(levelCompleted) {
        if (levelCompleted) {
            val gamePrefs = context.getSharedPreferences("GameStats", Context.MODE_PRIVATE)
            val editor = gamePrefs.edit()

            // zwiększ licznik ukończonych poziomów
            val completed = gamePrefs.getInt("levels_completed", 0)
            editor.putInt("levels_completed", completed + 1)

            // zwiększ licznik rozegranych gier
            val played = gamePrefs.getInt("games_played", 0)
            editor.putInt("games_played", played + 1)

            // zapisz aktualną godzinę dla odznak
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            editor.putInt("last_play_hour", hour)

            // jeśli gracz nie popełnił błędów:
            if (errorCount == 0) {
                val flawless = gamePrefs.getInt("flawless_levels", 0)
                editor.putInt("flawless_levels", flawless + 1)
            }
            // jeśli gra trwała krótko (np. < 20s), zalicz jako „szybki poziom"
            if (levelTimeResult < 20) {
                val fast = gamePrefs.getInt("fast_levels", 0)
                editor.putInt("fast_levels", fast + 1)
            }
            editor.apply()

            // zapisanie aktualnej próby i ocena progresji
            val newAttempt = LevelAttempt(
                successRate = successRate,
                errorCount = errorCount,
                time = levelTimeResult,
                timestamp = System.currentTimeMillis(),
                setSize = setOfCardsSize,
                cardsCount = cardsCount,
                level = level,
                cognitiveErrors = cognitiveErrorsResult, // licznik błędów poznawczych
                associationErrors = associationErrorsResult, // licznik błędów asocjacyjnych
                geographicalErrorsWithAnchor = geographicalErrorsWithAnchorResult, // licznik błędów lokalizacji
                attentionErrors = attentionErrorsResult // licznik błędów uwagi
            )

            // nagroda za poziom
            val baseReward = when (setOfCardsSize) {
                2 -> 10
                3 -> 25
                4 -> 45
                5 -> 70
                else -> 10
            }
            val reward = (baseReward * (successRate / 100f)).toInt().coerceAtLeast(5)
            EconomyManager.addCurrency(context, reward)

            // animacja przyznanych monet
            earnedCoins = reward
            showCoinReward = true
            scope.launch {
                delay(2000L)
                showCoinReward = false
            }
            val updatedAttempts = currentLevelAttempts + newAttempt
            saveLevelAttempts(context, level, updatedAttempts)
            currentLevelAttempts = updatedAttempts

            appendReportAttempt(context, newAttempt)
            reportStats.add(newAttempt)

            // ocena warunków progresji
            val result = evaluateProgressionConditions(updatedAttempts)
            progressionResult = result

            // logika zapisu postępu na podstawie wyniku progresji
            when (result) {
                ProgressionResult.ADVANCE -> {
                    // normalna progresja do następnego poziomu
                    var nextLevel = level + 1
                    var nextSetSize = setOfCardsSize
                    var nextLevelIndex = currentLevelIndex

                    if (currentLevelIndex < maxLevelIndex) {
                        nextLevelIndex = currentLevelIndex + 1
                    } else {
                        if (setOfCardsSize < 5) {
                            nextSetSize++
                            nextLevelIndex = 0
                        } else {
                            nextLevelIndex = 0 // reset w obrębie piątek
                        }
                    }
                    saveProgress(context, nextLevel, nextLevelIndex, nextSetSize)

                    // startujemy NOWY poziom z pustą historią prób
                    clearLevelAttempts(context, nextLevel)
                    currentLevelAttempts = emptyList()
                }

                ProgressionResult.REPEAT_SAME_LEVEL -> {
                    // pozostajemy na tym samym poziomie, nie zmieniamy zapisu
                }

                ProgressionResult.REGRESS -> {
                    // cofnięcie do poprzedniego poziomu
                    var previousLevel = level - 1
                    var previousSetSize = setOfCardsSize
                    var previousLevelIndex = currentLevelIndex

                    if (currentLevelIndex > 0) {
                        previousLevelIndex = currentLevelIndex - 1
                    } else {
                        if (setOfCardsSize > 2) {
                            previousSetSize--
                            val previousSizes = getCurrentLevelSizes(previousSetSize)
                            previousLevelIndex = previousSizes.lastIndex
                        } else {
                            previousLevelIndex = 0
                        }
                    }
                    previousLevel = maxOf(1, previousLevel)
                    saveProgress(context, previousLevel, previousLevelIndex, previousSetSize)

                    // czyścimy próby dla poziomu, do tkórego wracamy
                    clearLevelAttempts(context, previousLevel)
                    currentLevelAttempts = emptyList()
                }
                ProgressionResult.NOT_ENOUGH_ATTEMPTS -> {
                    // nie robimy nic, czekamy na więcej prób
                }
            }
            resultMusic = safeReleaseMediaPlayer(resultMusic)

            // włączone ustawienie głośności po wygranej
            resultMusic = MediaPlayer.create(context, R.raw.menu_music2).apply {
                isLooping = true
                setVolume(SoundSettings.winVolume, SoundSettings.winVolume)
                start()
            }
        }
    }
    // zatrzymanie muzyki po wyjściu z ekranu
    DisposableEffect(Unit) {
        onDispose {
            resultMusic = safeReleaseMediaPlayer(resultMusic)
            startSound = safeReleaseMediaPlayer(startSound)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val backgroundRes = getBackgroundForTheme(context)
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Tło motywu",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Powrót do menu",
            tint = Color(0xFFFFE082),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 16.dp)
                .size(32.dp)
                .clickable {
                    resultMusic = safeReleaseMediaPlayer(resultMusic)
                    navController.popBackStack()
                }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            val backgroundRes = getBackgroundForTheme(context)

            Image(
                painter = painterResource(id = backgroundRes),
                contentDescription = "Tło motywu",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (!levelCompleted) {
                // kolory paska zależne od motywu
                val (barColors, iconTint, textColor) = when (selectedTheme) {
                    "theme2" -> Triple(
                        listOf(Color(0xAA6D4C41), Color(0x552E1F10)),
                        Color(0xFFFFE082),
                        Color(0xFFFFE082)
                    )

                    "theme1" -> Triple(
                        listOf(Color(0xAA6D4C41), Color(0x552E1F10)),
                        Color(0xFFFFE082),
                        Color(0xFFFFE082)
                    )

                    "theme3" -> Triple(
                        listOf(Color(0xAA003C8F), Color(0x880028CA)),
                        Color(0xFFBBDEFB),
                        Color(0xFFBBDEFB)
                    )

                    "theme4" -> Triple(
                        listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2)),
                        Color(0xFF5D4037),
                        Color(0xFF4E342E)
                    )

                    "theme5" -> Triple(
                        listOf(Color(0xAA004D80), Color(0x5500B0FF)),
                        Color(0xFFB3E5FC),
                        Color(0xFFE1F5FE)
                    )

                    else -> Triple(
                        listOf(Color(0xAA4E342E), Color.Transparent),
                        Color(0xFFFFE082),
                        Color(0xFFFFE082)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 60.dp, end = 20.dp)
                        .background(Brush.verticalGradient(colors = barColors)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Poziom $level",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = textColor,
                        style = TextStyle(
                            shadow = Shadow(
                                color = textColor.copy(alpha = 0.4f),
                                offset = Offset(1f, 2f),
                                blurRadius = 8f
                            )
                        )
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                if (selectedTheme == "theme4") Color(0xFFFFF8E1).copy(alpha = 0.8f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Czas: ${elapsedTime}s",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = textColor,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = textColor.copy(alpha = 0.3f),
                                    offset = Offset(1f, 2f),
                                    blurRadius = 8f
                                )
                            )
                        )
                    }
                }
                // strzałka powrotu
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Powrót do menu",
                    tint = iconTint,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 40.dp, start = 16.dp)
                        .size(32.dp)
                        .clickable {
                            resultMusic = safeReleaseMediaPlayer(resultMusic)
                            navController.popBackStack()
                        }
                )
            }

//                //  wyświetlanie liczby prób
//                Text(
//                    text = "Próba: ${currentLevelAttempts.size + 1}/2",
//                    fontWeight = FontWeight.ExtraBold,
//                    fontSize = 20.sp,
//                    style = TextStyle(
//                        brush = Brush.verticalGradient(
//                            colors = listOf(Color(0xFFFFE082), Color(0xFFFFC107))
//                        )
//                    )
//                )
        }
    }

    // Ekran "Gotowy? Start!"
    if (showStartText) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .alpha(alphaAnim.value),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // kolor napisów "Gotowy" i "Start" zależny od motywu
                val startTextColor = when (selectedTheme) {
                    "theme2" -> Color(0xFFFFE082)
                    "theme3" -> Color(0xFFBBDEFB)
                    "theme4" -> Color(0xFF4E342E)
                    "theme5" -> Color(0xFF40C4FF)
                    else -> Color(0xFFFFF176)
                }
                Text(
                    text = readyText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 42.sp,
                    textAlign = TextAlign.Center,
                    color = startTextColor,
                    style = TextStyle(
                        shadow = Shadow(
                            color = startTextColor.copy(alpha = 0.4f),
                            offset = Offset(1f, 2f),
                            blurRadius = 6f
                        )
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Start!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 46.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(bounceAnim.value),
                    color = startTextColor,
                    style = TextStyle(
                        shadow = Shadow(
                            color = startTextColor.copy(alpha = 0.5f),
                            offset = Offset(1f, 2f),
                            blurRadius = 8f
                        )
                    )
                )
            }

        }
    } else {
        // plansza gry
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp)
                .blur(if (levelCompleted) 8.dp else 0.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when (setOfCardsSize) {
                2 -> {
                    MemoryGameBoardStay(
                        cardsCount = cardsCount,
                        elapsedTime = elapsedTime,
                        setOfCardsSize = setOfCardsSize,
                        onLevelFinished = { stats ->
                            gameRunning = false
                            levelCompleted = true
                            levelTimeResult = stats.levelTime
                            errorCount = calculateTotalErrors(stats) // zapis błędów
                            // przypisanie nowych zmiennych
                            cognitiveErrorsResult = stats.cognitiveErrors
                            associationErrorsResult = stats.associationErrors
                            geographicalErrorsWithAnchorResult = stats.geographicalErrorsWithAnchor
                            attentionErrorsResult = stats.attentionErrors
                            // skuteczność
                            successRate =
                                if ((stats.correctPairs + stats.cognitiveErrors + stats.associationErrors
                                            + stats.geographicalErrorsWithAnchor + stats.attentionErrors) > 0
                                )
                                    (stats.correctPairs.toFloat() /
                                            (stats.correctPairs + stats.cognitiveErrors + stats.associationErrors
                                                    + stats.geographicalErrorsWithAnchor + stats.attentionErrors).toFloat()) * 100f
                                else 0f
                        }
                    )
                }

                3 -> {
                    MemoryGameBoardTriple(
                        cardsCount = cardsCount,
                        elapsedTime = elapsedTime,
                        onLevelFinished = { stats ->
                            gameRunning = false
                            levelCompleted = true
                            levelTimeResult = stats.levelTime
                            errorCount = calculateTotalErrors(stats)
                            cognitiveErrorsResult = stats.cognitiveErrors
                            associationErrorsResult = stats.associationErrors
                            geographicalErrorsWithAnchorResult = stats.geographicalErrorsWithAnchor
                            attentionErrorsResult = stats.attentionErrors
                            successRate =
                                if ((stats.correctTriples + stats.cognitiveErrors + stats.associationErrors
                                            + stats.geographicalErrorsWithAnchor + stats.attentionErrors) > 0
                                )
                                    (stats.correctTriples.toFloat() /
                                            (stats.correctTriples + stats.cognitiveErrors + stats.associationErrors
                                                    + stats.geographicalErrorsWithAnchor + stats.attentionErrors).toFloat()) * 100f
                                else 0f
                        }
                    )
                }

                4 -> {
                    MemoryGameBoardQuad(
                        cardsCount = cardsCount,
                        elapsedTime = elapsedTime,
                        onLevelFinished = { stats ->
                            gameRunning = false
                            levelCompleted = true
                            levelTimeResult = stats.levelTime
                            errorCount = calculateTotalErrors(stats)
                            cognitiveErrorsResult = stats.cognitiveErrors
                            associationErrorsResult = stats.associationErrors
                            geographicalErrorsWithAnchorResult = stats.geographicalErrorsWithAnchor
                            attentionErrorsResult = stats.attentionErrors
                            successRate =
                                if ((stats.correctQuads + stats.cognitiveErrors + stats.associationErrors
                                            + stats.geographicalErrorsWithAnchor + stats.attentionErrors) > 0
                                )
                                    (stats.correctQuads.toFloat() /
                                            (stats.correctQuads + stats.cognitiveErrors + stats.associationErrors
                                                    + stats.geographicalErrorsWithAnchor + stats.attentionErrors).toFloat()) * 100f
                                else 0f
                        }
                    )
                }

                5 -> {
                    MemoryGameBoardPenta(
                        cardsCount = cardsCount,
                        elapsedTime = elapsedTime,
                        onLevelFinished = { stats ->
                            gameRunning = false
                            levelCompleted = true
                            levelTimeResult = stats.levelTime
                            errorCount = calculateTotalErrors(stats)
                            cognitiveErrorsResult = stats.cognitiveErrors
                            associationErrorsResult = stats.associationErrors
                            geographicalErrorsWithAnchorResult = stats.geographicalErrorsWithAnchor
                            attentionErrorsResult = stats.attentionErrors
                            successRate =
                                if ((stats.correctPentas + stats.cognitiveErrors + stats.associationErrors
                                            + stats.geographicalErrorsWithAnchor + stats.attentionErrors) > 0
                                )
                                    (stats.correctPentas.toFloat() /
                                            (stats.correctPentas + stats.cognitiveErrors + stats.associationErrors
                                                    + stats.geographicalErrorsWithAnchor + stats.attentionErrors).toFloat()) * 100f
                                else 0f
                        }
                    )
                }
            }

        }

        // ekran wyników
        if (levelCompleted) {
            val alphaAnim = remember { Animatable(0f) }
            val scaleAnim = remember { Animatable(1.2f) }
            val stars = when {
                successRate >= 90f -> 5
                successRate >= 70f -> 4
                successRate >= 50f -> 3
                successRate >= 30f -> 2
                else -> 1
            }

            LaunchedEffect(levelCompleted) {
                alphaAnim.animateTo(1f, animationSpec = tween(700))
                scaleAnim.animateTo(
                    1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .alpha(alphaAnim.value),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                        // DEBUG INFO - TYMCZASOWE
//                        Text(
//                            text = "DEBUG: Prób: ${currentLevelAttempts.size}, Level: $level",
//                            color = Color.Red,
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold
//                        )

                    // Tekst tytułowy
                    val nextSetSizeText = getSetTypeText(setOfCardsSize + 1)
                    val titleText = when {
                        progressionResult == ProgressionResult.ADVANCE && setOfCardsSize < 5 && currentLevelIndex == maxLevelIndex ->
                            "Brawo! Przechodzisz do trybu ${nextSetSizeText}!"

                        progressionResult == ProgressionResult.ADVANCE && setOfCardsSize == 5 && currentLevelIndex == maxLevelIndex ->
                            "Gratulacje! Ukończono wszystkie tryby!"

                        progressionResult == ProgressionResult.ADVANCE ->
                            "Gratulacje! Awansujesz na kolejny poziom!"

                        else ->
                            "Poziom ukończony!"
                    }

                    Text(
                        text = titleText,
                        color = Color(0xFFFFEB3B),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.scale(scaleAnim.value),
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.7f),
                                offset = Offset(3f, 3f),
                                blurRadius = 10f
                            )
                        )
                    )

                    // gwiazdki
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .scale(scaleAnim.value)
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = if (index < stars) Color(0xFFFFC107) else Color.Gray.copy(
                                    alpha = 0.5f
                                ),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // wynik progresji
                    progressionResult?.let { result ->
                        val resultText = when (result) {
                            ProgressionResult.ADVANCE -> "Gratulacje! Awansujesz do następnego poziomu!"
                            ProgressionResult.REPEAT_SAME_LEVEL -> "Dobra robota! Powtórz ten poziom dla lepszych wyników!"
                            ProgressionResult.REGRESS -> "Potrzebujesz więcej praktyki. Spróbujemy łatwiejszego poziomu?"
                            ProgressionResult.NOT_ENOUGH_ATTEMPTS -> {
                                if (currentLevelAttempts.size == 1)
                                    "Kontynuuj grę! Potrzebna druga próba!"
                                else
                                    "Kontynuuj grę! Potrzebna trzecia próba!"
                            }

                        }
//                            Text(
//                                text = "Score progresji: ${if (progressionResult == null) "-" else ""}",
//                                color = Color.LightGray,
//                                fontSize = 16.sp,
//                                textAlign = TextAlign.Center
//                            )


                        Text(
                            text = resultText,
                            color = when (result) {
                                ProgressionResult.ADVANCE -> Color(0xFF4CAF50)
                                ProgressionResult.REPEAT_SAME_LEVEL -> Color(0xFFFFC107)
                                ProgressionResult.REGRESS -> Color(0xFFF44336)
                                ProgressionResult.NOT_ENOUGH_ATTEMPTS -> Color(0xFF2196F3)
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }


                    Spacer(modifier = Modifier.height(8.dp))
                    // score jakości (liczbowy)
                    val scoreValue = combinedScore(successRate, errorCount)
                    Text(
                        text = "Score jakości: ${"%.1f".format(scoreValue)}",
                        color = Color.Cyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // pasek postępu score
                    val scorePct = (scoreValue / 100f).coerceIn(0f, 1f)
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .background(
                                Color.DarkGray.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(scorePct)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF00E5FF), Color(0xFF00C853))
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

//                        Text(
//                            text = "Min. score do awansu: 60",
//                            color = Color.LightGray,
//                            fontSize = 16.sp,
//                            textAlign = TextAlign.Center
//                        )

                    Text(
                        text = "Czas: ${levelTimeResult}s\nSkuteczność: ${"%.1f".format(successRate)}%\nBłędy: $errorCount",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.scale(scaleAnim.value)
                    )

                    // DODANE: Wyświetlanie historii prób
//                        if (currentLevelAttempts.isNotEmpty()) {
//                            Spacer(modifier = Modifier.height(12.dp))
//                            Text(
//                                text = "Historia prób:",
//                                color = Color(0xFFFFCC80),
//                                fontSize = 18.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//                            currentLevelAttempts.forEachIndexed { index, attempt ->
//                                Text(
//                                    text = "Próba ${index + 1}: ${"%.1f".format(attempt.successRate)}%, ${attempt.time}s, ${attempt.errorCount} błędów",
//                                    color = Color.White,
//                                    fontSize = 14.sp
//                                )
//                            }
//                        }

                    Spacer(modifier = Modifier.height(20.dp))

                    // przycisk kontynuuj
                    Button(
                        onClick = {
                            resultMusic = safeReleaseMediaPlayer(resultMusic)

                            // logika kontynuacji na podstawie wyniku progresji
                            when (progressionResult) {
                                ProgressionResult.ADVANCE -> {
                                    if (currentLevelIndex < maxLevelIndex) {
                                        currentLevelIndex++
                                    } else {
                                        if (setOfCardsSize < 5) {
                                            setOfCardsSize++
                                        }
                                        currentLevelIndex = 0
                                    }
                                    level++
                                    currentSessionAttemptNumber =
                                        1
                                    levelCompleted = false
                                    gameRunning = false
                                    elapsedTime = 0
                                }

                                ProgressionResult.REPEAT_SAME_LEVEL -> {
                                    // ta sama plansza, druga próba / powtórka
                                    // nie zmieniamy level, indexów ani trybu
                                    restartSameLevel()   // wymusi sekwencję startową
                                }

                                ProgressionResult.REGRESS -> {
                                    // wracamy o jedną planszę wstecz
                                    if (currentLevelIndex > 0) {
                                        currentLevelIndex--
                                    } else {
                                        if (setOfCardsSize > 2) {
                                            setOfCardsSize--
                                            val previousSizes = getCurrentLevelSizes(setOfCardsSize)
                                            currentLevelIndex = previousSizes.lastIndex
                                        } else {
                                            currentLevelIndex = 0
                                        }
                                    }
                                    level = maxOf(1, level - 1)
                                    currentSessionAttemptNumber =
                                        1
                                    levelCompleted = false
                                    gameRunning = false
                                    elapsedTime = 0
                                }

                                ProgressionResult.NOT_ENOUGH_ATTEMPTS -> {
                                    // druga próba na tej samej planszy
                                    restartSameLevel()
                                }

                                null -> {
                                    // progresja do kolejnej planszy
                                    if (currentLevelIndex < maxLevelIndex) {
                                        currentLevelIndex++
                                    } else {
                                        if (setOfCardsSize < 5) {
                                            setOfCardsSize++
                                        }
                                        currentLevelIndex = 0
                                    }
                                    level++
                                    currentSessionAttemptNumber = 1
                                    levelCompleted = false
                                    gameRunning = false
                                    elapsedTime = 0
                                }
                            }

                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color.Black
                        ),
                        enabled = true
                    ) {
                        Text(
                            when (progressionResult) {
                                ProgressionResult.ADVANCE -> "Następny poziom"
                                ProgressionResult.REPEAT_SAME_LEVEL -> "Powtórz poziom"
                                ProgressionResult.REGRESS -> "Poprzedni poziom"
                                ProgressionResult.NOT_ENOUGH_ATTEMPTS -> "Kontynuuj grę"
                                null -> "Kontynuuj"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    // dwa szare przyciski pod głównym (alternatywne opcje)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        when (progressionResult) {
                            ProgressionResult.ADVANCE -> {
                                // następny poziom  [poprzedni] [powtórz]
                                SmallAltButton("Poprzedni poziom") { goPreviousLevel() }
                                SmallAltButton("Powtórz poziom") { goRepeatLevel() }
                            }

                            ProgressionResult.REPEAT_SAME_LEVEL,
                            ProgressionResult.NOT_ENOUGH_ATTEMPTS,
                            null -> {
                                // powtórz poziom  [poprzedni] [następny]
                                SmallAltButton("Poprzedni poziom") { goPreviousLevel() }
                                SmallAltButton("Następny poziom") { goNextLevel() }
                            }

                            ProgressionResult.REGRESS -> {
                                // poprzedni poziom  [powtórz] [następny]
                                SmallAltButton("Powtórz poziom") { goRepeatLevel() }
                                SmallAltButton("Następny poziom") { goNextLevel() }
                            }
                        }
                    }

//                        Spacer(modifier = Modifier.height(10.dp))
//
//                        SmallAltButton("Wyczyść RAPORT") {
//                            clearReportAttempts(context)
//                            reportStats.clear()
//                        }
                    Spacer(modifier = Modifier.height(8.dp))

//  podsumowanie raportu
//                        Text(
//                            text = "Raport: ${reportStats.size} prób łącznie",
//                            color = Color(0xFF90CAF9),
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.SemiBold
//                        )


//                        Button(
//                            onClick = {
//                                resultMusic = safeReleaseMediaPlayer(resultMusic)
//                                resetProgress(context)
//                                currentLevelIndex = 0
//                                level = 1
//                                setOfCardsSize = 2
//                                level++
//                            },
//                            colors = ButtonDefaults.buttonColors(
//                                containerColor = Color(0xFF6D4C41),
//                                contentColor = Color(0xFFFFE082)
//                            )
//                        ) {
//                            Text(
//                                "Zacznij od początku (Reset)",
//                                fontSize = 18.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//                        }
                }
            }
        }
    }
    // animowany tekst nagrody za poziom
    if (showCoinReward && earnedCoins > 0) {
        val alpha = remember { Animatable(0f) }
        val offsetY = remember { Animatable(40f) }

        LaunchedEffect(showCoinReward) {
            alpha.animateTo(1f, tween(400))
            offsetY.animateTo(0f, tween(600))
            delay(1600)
            alpha.animateTo(0f, tween(400))
        }

        if (showCoinReward && earnedCoins > 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val alpha = remember { Animatable(0f) }
                val offsetY = remember { Animatable(40f) }

                LaunchedEffect(showCoinReward) {
                    alpha.animateTo(1f, tween(400))
                    offsetY.animateTo(0f, tween(600))
                    delay(1600)
                    alpha.animateTo(0f, tween(400))
                }

                Text(
                    text = "+${earnedCoins} monet!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD600),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset(y = offsetY.value.dp)
                        .alpha(alpha.value)
                        .padding(bottom = 90.dp),
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            offset = Offset(2f, 2f),
                            blurRadius = 6f
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun SmallAltButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(170.dp)
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xAA2E2E2E),
            contentColor = Color.White
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp,
            focusedElevation = 6.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0x55FFFFFF)
        )
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

    }

}


//@Composable
//fun ProgressionTestScreen() {
//    var s1 by remember { mutableStateOf("") }
//    var e1 by remember { mutableStateOf("") }
//    var t1 by remember { mutableStateOf("") }
//
//    var s2 by remember { mutableStateOf("") }
//    var e2 by remember { mutableStateOf("") }
//    var t2 by remember { mutableStateOf("") }
//
//    var s3 by remember { mutableStateOf("") }
//    var e3 by remember { mutableStateOf("") }
//    var t3 by remember { mutableStateOf("") }
//
//    var resultText by remember { mutableStateOf("") }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//            .background(Color(0xFF212121))
//            .verticalScroll(rememberScrollState()),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//
//        Text(
//            text = "TEST PROGRESJI (3 próby)",
//            color = Color(0xFFFFC107),
//            fontSize = 22.sp,
//            fontWeight = FontWeight.Bold,
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(Modifier.height(10.dp))
//
//        Text("Próba 1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
//        InputField("Skuteczność (%)", s1) { s1 = it.filter { c -> c.isDigit() || c == '.' } }
//        InputField("Błędy", e1) { e1 = it.filter { c -> c.isDigit() } }
//        InputField("Czas (s)", t1) { t1 = it.filter { c -> c.isDigit() } }
//
//        Spacer(Modifier.height(12.dp))
//
//        Text("Próba 2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
//        InputField("Skuteczność (%)", s2) { s2 = it.filter { c -> c.isDigit() || c == '.' } }
//        InputField("Błędy", e2) { e2 = it.filter { c -> c.isDigit() } }
//        InputField("Czas (s)", t2) { t2 = it.filter { c -> c.isDigit() } }
//
//        Spacer(Modifier.height(12.dp))
//
//        Text("Próba 3", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
//        InputField("Skuteczność (%)", s3) { s3 = it.filter { c -> c.isDigit() || c == '.' } }
//        InputField("Błędy", e3) { e3 = it.filter { c -> c.isDigit() } }
//        InputField("Czas (s)", t3) { t3 = it.filter { c -> c.isDigit() } }
//
//        Spacer(Modifier.height(16.dp))
//
//        Button(
//            onClick = {
//                val attempts = listOf(
//                    LevelAttempt(s1.toFloatOrNull() ?: 0f, e1.toIntOrNull() ?: 0, t1.toIntOrNull() ?: 0),
//                    LevelAttempt(s2.toFloatOrNull() ?: 0f, e2.toIntOrNull() ?: 0, t2.toIntOrNull() ?: 0),
//                    LevelAttempt(s3.toFloatOrNull() ?: 0f, e3.toIntOrNull() ?: 0, t3.toIntOrNull() ?: 0),
//                )
//
//                val result = evaluateProgressionConditions(attempts)
//                resultText = when (result) {
//                    ProgressionResult.ADVANCE -> "AWANS!"
//                    ProgressionResult.REPEAT_SAME_LEVEL -> "POWTÓRKA POZIOMU"
//                    ProgressionResult.REGRESS -> "REGRES"
//                    ProgressionResult.NOT_ENOUGH_ATTEMPTS -> "Za mało prób"
//                }
//            },
//            colors = ButtonDefaults.buttonColors(
//                containerColor = Color(0xFFFFC107),
//                contentColor = Color.Black
//            ),
//            modifier = Modifier.fillMaxWidth(0.8f)
//        ) {
//            Text("Sprawdź progresję", fontSize = 18.sp, fontWeight = FontWeight.Bold)
//        }
//
//        Spacer(Modifier.height(14.dp))
//
//        Text(
//            text = resultText,
//            color = when {
//                "AWANS" in resultText -> Color(0xFF4CAF50)
//                "POWTÓRKA" in resultText -> Color(0xFFFFC107)
//                "REGRES" in resultText -> Color(0xFFF44336)
//                else -> Color.White
//            },
//            fontSize = 22.sp,
//            fontWeight = FontWeight.Bold,
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(Modifier.height(30.dp))
//    }
//}
//
//
//@Composable
//private fun InputField(label: String, value: String, onValueChange: (String) -> Unit) {
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        Text(label, color = Color.LightGray, fontSize = 14.sp)
//        androidx.compose.material3.OutlinedTextField(
//            value = value,
//            onValueChange = onValueChange,
//            modifier = Modifier
//                .padding(vertical = 4.dp)
//                .width(200.dp),
//            singleLine = true,
//            textStyle = TextStyle(color = Color.White),
//            placeholder = {
//                Text("0", color = Color.Gray)
//            }
//        )
//    }
//}