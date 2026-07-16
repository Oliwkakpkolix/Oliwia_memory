package com.example.oliwia_wojdalska_275804_memory

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextOverflow
// odznaki
private const val PREFS_NAME = "MemoryGamePrefs"
private const val KEY_THEME = "dark_mode_enabled"

data class Badge(
    val id: String,                     // identyfikator odznaki
    val name: String,
    val requirement: String,            // warunek zdobycia
    val iconRes: Int,                   // ikona drawable
    val isRepeatable: Boolean = false   // odznaka zdobywana wielokrotnie
)
object BadgeManager {
    // funkcja zapisująca zdobycie odznaki
    fun unlockBadge(context: Context, badge: Badge) {
        val prefs = context.getSharedPreferences("Badges", Context.MODE_PRIVATE)
        // edytor do zapisu danych
        val editor = prefs.edit()
        // aktualna liczba zdobyć odznaki
        val currentCount = prefs.getInt("${badge.id}_count", 0)
        // ikona zdobywana wielokrotnie currentCount + 1
        // ikona zdobywana jednokrotnie 1
        if (badge.isRepeatable) {
            editor.putInt("${badge.id}_count", currentCount + 1)
        } else if (currentCount == 0) {
            editor.putInt("${badge.id}_count", 1)
        }
        editor.apply()
    }
    // zwraca liczbę zdobyć odznaki
    fun getBadgeCount(context: Context, badgeId: String): Int {
        val prefs = context.getSharedPreferences("GameStats", Context.MODE_PRIVATE)
        // odczyt wszystkich statystyk
        val levelsCompleted = prefs.getInt("levels_completed", 0)
        val flawlessLevels = prefs.getInt("flawless_levels", 0)
        val fastLevels = prefs.getInt("fast_levels", 0)
        val totalLevels = prefs.getInt("total_levels", 50)
        val mistakeFreeStreak = prefs.getInt("mistake_free_streak", 0)
        val repeatSameLevel = prefs.getInt("repeat_same_level", 0)
        val playCount = prefs.getInt("games_played", 0)
        val accuracyStreak = prefs.getFloat("accuracy_streak", 0f)
        val regressions = prefs.getInt("regressions", 0)
        val lastPlayHour = prefs.getInt("last_play_hour", 12)
        val changedThemeEveryGame = prefs.getBoolean("theme_changed_each_time", false)
        val storyCompleted = prefs.getBoolean("story_completed", false)
        val daysInRow = prefs.getInt("days_in_row", 0)
        val hoursSinceLastPlay = prefs.getLong("hours_since_last_play", 0)

        // wymuszenie odblokowań odznak czasowych
        val isTimeTestUnlocked = prefs.getBoolean("test_unlock_time_badges", false)
        return when (badgeId) {
            // postęp
            "first_step" -> if (levelsCompleted >= 1) 1 else 0
            "rookie_explorer" -> if (levelsCompleted >= 5) 1 else 0
            "halfway_there" -> if (levelsCompleted >= totalLevels / 2) 1 else 0
            "memory_master" -> if (levelsCompleted >= totalLevels) 1 else 0
            "memory_explorer" -> if (prefs.getBoolean("all_modes_completed", false)) 1 else 0

            // umiejętności poznawcze
            "keen_eye" -> if (mistakeFreeStreak >= 3) 1 else 0
            "perfect_memory" -> flawlessLevels
            "fast_brain" -> fastLevels
            "zen_master" -> if (mistakeFreeStreak >= 5) 1 else 0
            "logical_strategist" -> if (prefs.getInt("logic_perfect", 0) >= 3) 1 else 0

            // wytrwałość
            "unbreakable" -> if (repeatSameLevel >= 10) 1 else 0
            "persistent_explorer" -> if (playCount >= 20) 1 else 0
            "win_streak" -> if (prefs.getInt("win_streak", 0) >= 5) 1 else 0
            "cool_analyst" -> if (accuracyStreak >= 0.9f) 1 else 0
            "resilient_mind" -> if (regressions >= 3 && prefs.getBoolean("rank_up_after_regression", false)) 1 else 0

            // gra
            "night_guardian" -> if (isTimeTestUnlocked || lastPlayHour in 23..24 || lastPlayHour in 0..5) 1 else 0
            "experimenter" -> if (changedThemeEveryGame) 1 else 0
            "absolute_mind" -> if (
                isUnlocked(context, "keen_eye") &&
                isUnlocked(context, "perfect_memory") &&
                isUnlocked(context, "fast_brain") &&
                isUnlocked(context, "zen_master") &&
                isUnlocked(context, "logical_strategist")
            ) 1 else 0
            "memory_sorcerer" -> if (prefs.getBoolean("all_themes_completed", false)) 1 else 0
            "true_explorer" -> if (storyCompleted) 1 else 0

            // czas i rutyna
            "morning_trainer" -> if (isTimeTestUnlocked || lastPlayHour in 6..10) 1 else 0
            "daily_return" -> if (hoursSinceLastPlay >= 24) 1 else 0
            "regular_player" -> if (daysInRow >= 7) 1 else 0
            "routine_master" -> if (daysInRow >= 30) 1 else 0

            else -> 0
        }
    }
    // czy dana odznaka odblokowana
    fun isUnlocked(context: Context, badgeId: String): Boolean {
        return getBadgeCount(context, badgeId) > 0
    }
    // reeset zapisanych odznak
    fun resetAll(context: Context) {
        context.getSharedPreferences("Badges", Context.MODE_PRIVATE).edit().clear().apply()
    }
}

@Composable
fun FlippableBadgeCard(
    badge: Badge,
    unlocked: Boolean,
    darkMode: Boolean,
    accentGreen: Color,
    textColor: Color,
    count: Int
) {
    // czy karta jest aktualnie odwrócona
    var isFlipped by remember { mutableStateOf(false) }
    // animowana wartość obrotu osi Y (efekt 3D)
    val rotationY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // gradient tła dla trybu ciemnego/jasnego i odblokowanej odznaki
    val darkGradient = Brush.verticalGradient(listOf(Color(0xFF004D00), accentGreen))
    val lightGradient = Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)))
    // wybór gradientu od stanu
    val gradient = when {
        unlocked && darkMode -> darkGradient
        unlocked && !darkMode -> lightGradient
        !unlocked && darkMode -> Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF3A3A3A)))
        else -> Brush.verticalGradient(listOf(0xFFEEEEEE, 0xFFDDDDDD).map { Color(it) })
    }
    // obrót karty
    val flip = {
        if (!rotationY.isRunning) {
            scope.launch {
                // zmiana stanu
                isFlipped = !isFlipped
                // animacja do 180st - tył
                // animacja do 360st i reset do 0st
                if (isFlipped) {
                    rotationY.animateTo(180f, animationSpec = tween(500))
                } else {
                    rotationY.animateTo(360f, animationSpec = tween(500))
                    rotationY.snapTo(0f)
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = flip)
            .graphicsLayer {
                this.rotationY = rotationY.value
                cameraDistance = 12f * density  // efekt 3D
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .border(
                    2.dp,
                    if (unlocked) accentGreen else textColor.copy(alpha = 0.4f),
                    RoundedCornerShape(24.dp)
                )
                .background(gradient, RoundedCornerShape(24.dp))
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // czy widoczny przód karty
            val isFront = rotationY.value % 360 <= 90f || rotationY.value % 360 >= 270f
            // renderowanie przodu lub tyłu karty
            if (isFront) {
                FrontContent(badge, unlocked, count, accentGreen, darkMode)
            } else {
                BackContent(badge, unlocked, textColor, rotationY.value)
            }
        }
    }
}
// przód karty
@Composable
private fun FrontContent(
    badge: Badge,
    unlocked: Boolean,
    count: Int,
    accentGreen: Color,
    darkMode: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ikona odznaki
        Image(
            painter = painterResource(id = badge.iconRes),
            contentDescription = badge.name,
            modifier = Modifier.size(140.dp)
        )
        // licznikk dla odznak powtarzalnych
        if (badge.isRepeatable && count > 0) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xCC000000).copy(alpha = if (darkMode) 0.8f else 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    "x$count",
                    color = accentGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
// tył karty
@Composable
private fun BackContent(
    badge: Badge,
    unlocked: Boolean,
    textColor: Color,
    rotationYValue: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .graphicsLayer { scaleX = -1f } // odwrócenie lustrzane
            .padding(horizontal = 6.dp)
            .fillMaxSize()
    ) {
        Text(
            text = badge.name,
            color = if (unlocked) textColor else textColor.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            softWrap = true,
            maxLines = 2,
            overflow = TextOverflow.Clip,
            lineHeight = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        // warunek zdobycia
        Text(
            text = badge.requirement,
            color = if (unlocked) textColor.copy(alpha = 0.7f) else textColor.copy(alpha = 0.3f),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }
}
// ekran odznak
@Composable
fun BadgesScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var darkMode by remember { mutableStateOf(prefs.getBoolean(KEY_THEME, false)) }
    // aktualny tryb motywu
    fun saveTheme(mode: Boolean) { prefs.edit().putBoolean(KEY_THEME, mode).apply() }

    // wymuszenie odświeżenia LazyGrid po kliknięciu przycisku testowego
    var refreshKey by remember { mutableStateOf(0) }
    // kolor zmiennych
    val accentGreen = Color(0xFF4CAF50)
    val bgColor = if (darkMode) Color(0xFF111111) else Color.White
    val textColor = if (darkMode) Color.White else Color.Black
    // lista odznak z opisami
    val badges = listOf(
        // postęp
        Badge("first_step", "Pierwszy krok", "Ukończenie 1 poziomu", R.drawable.medal1, false),
        Badge("rookie_explorer", "Początkujący odkrywca", "Ukończenie 5 poziomów", R.drawable.medal2, false),
        Badge("halfway_there", "W połowie drogi", "Ukończenie połowy dostępnych poziomów", R.drawable.medal3, false),
        Badge("memory_master", "Mistrz pamięci", "Ukończenie wszystkich poziomów w danym trybie", R.drawable.medal4, false),
        Badge("memory_explorer", "Odkrywca wspomnień", "Ukończ wszystkie tryby gry (2-5 kart)", R.drawable.medal5, false),

        // umiejętności poznawcze
        Badge("keen_eye", "Sokole oko", "3 poziomy bez błędów uwagi", R.drawable.medal6, true),
        Badge("perfect_memory", "Perfekcyjna pamięć", "Ukończenie poziomu z 100% skutecznością", R.drawable.medal7, true),
        Badge("fast_brain", "Błyskawiczny umysł", "Ukończenie poziomu poniżej 15 sekund", R.drawable.medal8, true),
        Badge("zen_master", "Zen mistrz", "5 poziomów z rzędu bez błędów lokalizacyjnych", R.drawable.medal9, true),
        Badge("logical_strategist", "Logiczny strateg", "3 poziomy bez błędów asocjacyjnych", R.drawable.medal10, true),

        // wytrwałość
        Badge("unbreakable", "Nie do zdarcia", "10 powtórek tego samego poziomu", R.drawable.medal11, true),
        Badge("persistent_explorer", "Cierpliwy odkrywca", "20 rozegranych poziomów", R.drawable.medal12, true),
        Badge("win_streak", "Seria zwycięstw", "Ukończenie 5 poziomów z rzędu", R.drawable.medal13, true),
        Badge("cool_analyst", "Chłodny analityk", "Utrzymano skuteczność 90% przez 3 poziomyh", R.drawable.medal14, true),
        Badge("resilient_mind", "Twardy umysł", "Powrót do formy po 3 porażkach. Awans", R.drawable.medal15, false),

        // gra
        Badge("night_guardian", "Strażnik nocy", "Gra między 23:00 a 5:00", R.drawable.medal16, true),
        Badge("experimenter", "Eksperymentator", "Zmieniony motyw przed każdą grą", R.drawable.medal17, true),
        Badge("absolute_mind", "Umysł absolutny", "Zdobyte wszystkie odznaki umiejętności poznawczych", R.drawable.medal18, false),
        Badge("memory_sorcerer", "Mistrz wspomnień", "Ukończenie gry we wszystkich motywach", R.drawable.medal19, false),
        Badge("true_explorer", "Prawdziwy odkrywca", "Odkrycie całej historii narracyjnej", R.drawable.medal20, false),


        // czas i rutyna
        Badge("morning_trainer", "Poranny trening", "Zagrano między 6:00 a 10:00", R.drawable.medal21, true),
        Badge("daily_return", "Powrót dnia", "Powrót po 24h przerwy", R.drawable.medal22, true),
        Badge("regular_player", "Regularny gracz", "Grano codziennie przez 7 dni z rzędu", R.drawable.medal23, false),
        Badge("routine_master", "Mistrz rutyny", "Grano codziennie przez 30 dni z rzędu", R.drawable.medal24, false)
    )
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
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Odznaki",
                color = textColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Divider(color = textColor.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))
            // siatka z odznakami
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f) // wypełnienie dostępnej przestrzeni
            ) {
                items(
                    items = badges,
                    key = { badge -> badge.id + refreshKey } // odświeżenie po zmianie refreshKey
                ) { badge ->
                    val context = LocalContext.current
                    // liczba zdobyć odznaki
                    val count = BadgeManager.getBadgeCount(context, badge.id)
                    // czy odzanaka odblokowana
                    val unlocked = count > 0
                    // wyświetlenie karty odznaki
                    FlippableBadgeCard(
                        badge = badge,
                        unlocked = unlocked,
                        darkMode = darkMode,
                        accentGreen = accentGreen,
                        textColor = textColor,
                        count = count
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                modifier = Modifier
                    .width(220.dp)
                    .height(50.dp)
                    .shadow(6.dp, RoundedCornerShape(30.dp))
            ) {
                Text("Wróć do menu", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

        }
    }
}