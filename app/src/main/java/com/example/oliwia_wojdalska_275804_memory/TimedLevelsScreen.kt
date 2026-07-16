package com.example.oliwia_wojdalska_275804_memory

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.text.TextStyle

private const val PREFS_NAME = "MemoryGamePrefs"
private const val KEY_THEME = "dark_mode_enabled"
private const val KEY_HIGHEST_LEVEL = "highest_unlocked_level"
private const val KEY_LAST_LEVEL = "last_played_level"
private const val KEY_UNLOCKED_STYLES = "unlocked_styles"
private const val KEY_COMPLETED_LEVELS = "completed_levels"
// wczytywanie zestawu odblokowanych stylów kart
fun loadUnlockedStyles(context: Context): MutableSet<Int> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    return prefs.getStringSet(KEY_UNLOCKED_STYLES, emptySet())?.mapNotNull { it.toIntOrNull() }?.toMutableSet()
        ?: mutableSetOf()
}
// zapisywanie odblokowanych stylów kart
fun saveUnlockedStyles(context: Context, styles: Set<Int>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(KEY_UNLOCKED_STYLES, styles.map { it.toString() }.toSet()).apply()
}
// wczytywanie ukończonych poziomów
fun loadCompletedLevels(context: Context): MutableSet<Int> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_COMPLETED_LEVELS, emptySet())
        ?.mapNotNull { it.toIntOrNull() }
        ?.toMutableSet() ?: mutableSetOf()
}
// zapis ukończonych poziomów
fun saveCompletedLevels(context: Context, levels: Set<Int>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(KEY_COMPLETED_LEVELS, levels.map { it.toString() }.toSet()).apply()
}

@Composable
fun TimedLevelsScreen(navController: NavController) {
    // aktualny poziom
    var selectedLevel by remember { mutableStateOf<Int?>(null) }
    var restartTrigger by remember { mutableStateOf(false) }

    if (selectedLevel == null) {
        TimedLevelMenu(
            onLevelSelected = { level ->
                selectedLevel = level
                restartTrigger = false
            },
            navController = navController
        )
    } else {
        // wymuszenie przeładowania całego poziomu po restarcie
        key(selectedLevel, restartTrigger) {
            TimedLevelGame(
                level = selectedLevel!!,
                onExit = { selectedLevel = null },
                onRestart = { restartTrigger = !restartTrigger },
                onNextLevel = { next ->
                    selectedLevel = next
                    restartTrigger = false
                }
            )
        }

    }

}


@Composable
fun TimedLevelMenu(onLevelSelected: (Int) -> Unit, navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var darkMode by remember { mutableStateOf(prefs.getBoolean(KEY_THEME, false)) }
    var highestUnlocked by remember { mutableStateOf(prefs.getInt(KEY_HIGHEST_LEVEL, 1)) }

    fun saveTheme(mode: Boolean) {
        prefs.edit().putBoolean(KEY_THEME, mode).apply()
    }

    val bgGradient = if (darkMode)
        Brush.verticalGradient(listOf(Color(0xFF0B1026), Color(0xFF1A1A40), Color(0xFF24264D)))
    else
        Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Color(0xFF81D4FA), Color(0xFFE3F2FD)))

    val textColor = if (darkMode) Color.White else Color.Black
    val glowColor = if (darkMode)
        listOf(Color(0xFFB388FF), Color(0xFF7C4DFF))
    else
        listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))

    // val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastPlayed = prefs.getInt(KEY_LAST_LEVEL, 1)
    val scrollState = rememberScrollState((lastPlayed * 180).coerceAtMost(9999))

    val levels = (1..32).toList()
    // animacja pulsowania
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = bgGradient)
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
                .verticalScroll(scrollState)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tryb poziomowy (na czas)",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Odblokuj nowy styl kart!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.height(16.dp))
            Divider(color = textColor.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))

            levels.forEachIndexed { index, level ->
                val offsetX = when (index % 6) {
                    0 -> (-80).dp
                    1 -> (60).dp
                    2 -> (-90).dp
                    3 -> (70).dp
                    4 -> (-60).dp
                    else -> (90).dp
                }

                val isUnlocked = level <= highestUnlocked
                val alpha = if (isUnlocked) 1f else 0.4f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = offsetX)
                        .padding(vertical = 25.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(width = 2.dp, height = 120.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        drawLine(
                            color = glowColor.last().copy(alpha = 0.35f),
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 8f
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(if (isUnlocked) scaleAnim else 1f)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        if (isUnlocked) glowColor.first().copy(alpha = 0.9f)
                                        else Color.Gray.copy(alpha = 0.7f),
                                        if (isUnlocked) glowColor.last().copy(alpha = 0.5f)
                                        else Color.DarkGray.copy(alpha = 0.7f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .alpha(alpha)
                            .clickable(enabled = isUnlocked) {
                                prefs.edit().putInt(KEY_LAST_LEVEL, level).apply()
                                onLevelSelected(level)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$level",
                            color = if (isUnlocked) Color.White else Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun TimedLevelGame(
    level: Int,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onNextLevel: (Int) -> Unit
) {

    val context = LocalContext.current
    // czas dla
    val timeLimits = listOf(
        10, 15, 25, 25, 30, 50, 80, 80,         // pary
        30, 50, 70, 80, 80, 90, 90, 90,         // trójki
        20, 40, 80, 90, 90, 100, 100, 100,      // czwórki
        30, 30, 50, 50, 70, 90, 100, 200        // piątki
    )

    val levelTimeLimit = timeLimits.getOrNull(level - 1) ?: 60

    // wybór trybu zależny od numeru poziomu
    val setOfCardsSize = when (level) {
        in 1..8 -> 2
        in 9..16 -> 3
        in 17..24 -> 4
        else -> 5
    }

    // liczby kart dla każdego typu
    val levelSizesPairs = listOf(4, 8, 12, 16, 20, 28, 36, 48)
    val levelSizesTriples = listOf(9, 15, 24, 30, 36, 48, 63, 72)
    val levelSizesQuads = listOf(8, 16, 24, 28, 32, 36, 48, 64)
    val levelSizesPentas = listOf(15, 20, 25, 30, 35, 40, 50, 70)

    // dobieranie liczby kart na podstawie poziomu
    val cardsCount = when (level) {
        in 1..8 -> levelSizesPairs[level - 1]
        in 9..16 -> levelSizesTriples[level - 9]
        in 17..24 -> levelSizesQuads[level - 17]
        in 25..32 -> levelSizesPentas[level - 25]
        else -> 8
    }

    var elapsedTime by remember { mutableStateOf(0) }
    var gameRunning by remember { mutableStateOf(false) }
    var levelCompleted by remember { mutableStateOf(false) }
    var levelFailed by remember { mutableStateOf(false) }
    // system kolekcji stylów
    val unlockedStyles = remember { loadUnlockedStyles(context) }
    val completedLevels = remember { loadCompletedLevels(context) }

    var newlyUnlockedStyle by remember { mutableStateOf<Int?>(null) }
    var showGiftBox by remember { mutableStateOf(false) }
    var giftOpened by remember { mutableStateOf(false) }

    val backgroundRes = getBackgroundForTheme(context)
    val prefsTheme = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    val selectedTheme = prefsTheme.getString("selected_theme", "theme1") ?: "theme1"
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val userGender = prefs.getString("user_gender", "none")

    val readyText = when(userGender) {
        "male" -> "Gotowy?"
        "female" -> "Gotowa?"
        else -> "Gotowi?"
    }

    val (textColor, accentColor, buttonColor) = when (selectedTheme) {
        "theme2" -> Triple(Color(0xFFFFE082), Color(0xFFD7CCC8), Color(0xFF8D6E63))
        "theme3" -> Triple(Color(0xFFBBDEFB), Color(0xFFB3E5FC), Color(0xFF0288D1))
        "theme4" -> Triple(Color(0xFF4E342E), Color(0xFF795548), Color(0xFFFFF3E0))
        "theme5" -> Triple(Color(0xFFB3E5FC), Color(0xFF81D4FA), Color(0xFF00BCD4))
        else -> Triple(Color(0xFFFFF176), Color(0xFFFFFF8D), Color(0xFFFFEB3B))
    }

    var resultMusic: MediaPlayer? by remember { mutableStateOf(null) }
    var startSound: MediaPlayer? by remember { mutableStateOf(null) }

    // odblokowywanie kolejnego poziomu
    fun unlockNextLevel() {
        val current = prefs.getInt(KEY_HIGHEST_LEVEL, 1)
        if (level >= current) {
            prefs.edit().putInt(KEY_HIGHEST_LEVEL, level + 1).apply()
        }
    }

    val alphaAnim = remember { Animatable(0f) }
    val bounceAnim = remember { Animatable(1f) }
    var showStartText by remember { mutableStateOf(true) }

    var remainingTime by remember { mutableStateOf(levelTimeLimit) }

    LaunchedEffect(gameRunning) {
        if (gameRunning) {
            while (gameRunning && remainingTime > 0) {
                delay(1000L)
                remainingTime--
                if (remainingTime <= 0 && !levelCompleted) {
                    gameRunning = false
                    levelFailed = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        alphaAnim.snapTo(0f)
        bounceAnim.snapTo(1f)
        startSound = MediaPlayer.create(context, R.raw.start_sound)
        startSound?.start()
        alphaAnim.animateTo(1f, tween(600))
        bounceAnim.animateTo(1.25f, spring())
        bounceAnim.animateTo(1f, spring())
        delay(1200L)
        alphaAnim.animateTo(0f, tween(550))
        showStartText = false
        gameRunning = true
        startSound = safeReleaseMediaPlayer(startSound)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Tło gry",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // pasek u góry
        val (barColors, iconTint, textColorTop) = when (selectedTheme) {
            "theme2" -> Triple(
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
                listOf(
                    Color(0xAA0D47A1),
                    Color(0x552196F3)
                ),
                Color(0xFFB3E5FC),
                Color(0xFFB3E5FC)
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
                color = textColorTop,
                style = TextStyle(
                    shadow = Shadow(
                        color = textColorTop.copy(alpha = 0.4f),
                        offset = Offset(1f, 2f),
                        blurRadius = 8f
                    )
                )
            )

            Box(
                modifier = Modifier
                    .background(
                        if (selectedTheme == "theme5")
                            Color(0xFFFFF8E1).copy(alpha = 0.8f)
                        else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Czas: ${remainingTime}s",
                    color = if (remainingTime > 0) textColorTop else Color.Red,

                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = textColorTop.copy(alpha = 0.3f),
                            offset = Offset(1f, 2f),
                            blurRadius = 8f
                        )
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Powrót",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 16.dp)
                .size(32.dp)
                .clickable {
                    resultMusic = safeReleaseMediaPlayer(resultMusic)
                    onExit()
                }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp)
                .blur(if (levelCompleted || levelFailed) 8.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!levelCompleted && !showStartText && !levelFailed) {
                when (setOfCardsSize) {
                    2 -> MemoryGameBoardStay(
                        cardsCount,
                        elapsedTime,
                        setOfCardsSize,
                        onLevelFinished = { _ ->
                            gameRunning = false
                            levelCompleted = true
                            unlockNextLevel()

                            // prezent tylko przy pierwszym ukończeniu poziomu
                            if (!completedLevels.contains(level)) {
                                showGiftBox = true
                                completedLevels.add(level)
                                saveCompletedLevels(context, completedLevels)
                            }
                        })

                    3 -> MemoryGameBoardTriple(cardsCount, elapsedTime, onLevelFinished = { _ ->
                        gameRunning = false
                        levelCompleted = true
                        unlockNextLevel()

                        if (!completedLevels.contains(level)) {
                            showGiftBox = true
                            completedLevels.add(level)
                            saveCompletedLevels(context, completedLevels)
                        }

                    })

                    4 -> MemoryGameBoardQuad(cardsCount, elapsedTime, onLevelFinished = { _ ->
                        gameRunning = false
                        levelCompleted = true
                        unlockNextLevel()

                        if (!completedLevels.contains(level)) {
                            showGiftBox = true
                            completedLevels.add(level)
                            saveCompletedLevels(context, completedLevels)
                        }

                    })

                    5 -> MemoryGameBoardPenta(cardsCount, elapsedTime, onLevelFinished = { _ ->
                        gameRunning = false
                        levelCompleted = true
                        unlockNextLevel()

                        if (!completedLevels.contains(level)) {
                            showGiftBox = true
                            completedLevels.add(level)
                            saveCompletedLevels(context, completedLevels)
                        }
                    })
                }
//                if (!levelCompleted && !showStartText && !levelFailed) {
//                    Text(
//                        text = "Podgląd motywu: $selectedTheme",
//                        color = textColor,
//                        fontSize = 26.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                }

            }
        }
        val timeTaken = levelTimeLimit - remainingTime

        // poziom ukończony / czas minął
        if (levelCompleted || levelFailed) {
            val alphaAnim = remember { Animatable(0f) }
            val scaleAnim = remember { Animatable(1.2f) }

            // włączenie muzyki wyników
            LaunchedEffect(levelCompleted, levelFailed) {
                resultMusic = safeReleaseMediaPlayer(resultMusic)
                val soundRes =
                    if (levelFailed) R.raw.start_sound else R.raw.menu_music2
                resultMusic = MediaPlayer.create(context, soundRes).apply {
                    isLooping = !levelFailed // przy wygranej leci w tle
                    setVolume(1.0f, 1.0f) // pełna głośność
                    start()
                }
            }

            // animacja pojawienia się wyników
            LaunchedEffect(Unit) {
                alphaAnim.animateTo(1f, tween(700))
                scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .alpha(alphaAnim.value),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {

                    Text(
                        text = if (levelFailed) "Czas minął!" else "Poziom ukończony!",
                        color = if (levelFailed) Color(0xFFFF5252) else Color(0xFFFFEB3B),
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

                    Spacer(Modifier.height(24.dp))

                    if (levelFailed) {
                        Text(
                            text = "Czas minął! Limit: ${levelTimeLimit}s",
                            color = Color(0xFFFFCDD2),
                            textAlign = TextAlign.Center,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Ukończono w: ${timeTaken}s z ${levelTimeLimit}s",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }


                    Spacer(Modifier.height(32.dp))

                    if (levelFailed) {
                        Button(
                            onClick = {
                                resultMusic = safeReleaseMediaPlayer(resultMusic)
                                prefs.edit().putInt(KEY_LAST_LEVEL, level).apply()
                                onRestart()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFEB3B),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(52.dp)
                        ) {
                            Text("Spróbuj ponownie", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                resultMusic = safeReleaseMediaPlayer(resultMusic)
                                onExit()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(52.dp)
                        ) {
                            Text("Powrót do menu", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                resultMusic = safeReleaseMediaPlayer(resultMusic)
                                prefs.edit().putInt(KEY_LAST_LEVEL, level + 1).apply()
                                onNextLevel(level + 1) //
                            },

                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFEB3B),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(52.dp)
                        ) {
                            Text("Następny poziom", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                resultMusic = safeReleaseMediaPlayer(resultMusic)
                                onExit()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(52.dp)
                        ) {
                            Text("Powrót do menu", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        // klikalny prezent po wygraniu poziomu
        if (showGiftBox && !giftOpened && levelCompleted && !levelFailed) {
            GiftBox(
                onOpen = {
                    giftOpened = true
                    // losowanie nowej karty motywu po kliknięciu prezentu
                    val totalStyles = 32
                    val availableStyles = (1..totalStyles).filter { it !in unlockedStyles }
                    if (availableStyles.isNotEmpty()) {
                        val randomStyle = availableStyles.random()
                        unlockedStyles.add(randomStyle)
                        saveUnlockedStyles(context, unlockedStyles)
                        newlyUnlockedStyle = randomStyle

                        // automatyczne odblokowanie motywu po zdobyciu wszystkich kart
//                        if (unlockedStyles.size == totalStyles) {
//                            prefs.edit().putString("selected_theme", "theme5").apply()
//                        }
                    }
                }
            )
        }

/// sekcja nagrody , odkrycie nowego stylu
        if (newlyUnlockedStyle != null) {
            val alphaAnim = remember { Animatable(0f) }
            val scaleAnim = remember { Animatable(0.8f) }

            LaunchedEffect(Unit) {
                alphaAnim.animateTo(1f, tween(700))
                scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .alpha(alphaAnim.value),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        tonalElevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .size(220.dp)
                            .scale(scaleAnim.value)
                    ) {
                        Image(
                            painter = painterResource(id = getCardRes(newlyUnlockedStyle!!)),
                            contentDescription = "Nowy styl",
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Nowy styl odblokowany! (${unlockedStyles.size}/32)",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            newlyUnlockedStyle = null
                            showGiftBox = false
                            giftOpened = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEB3B),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(52.dp)
                    ) {
                        Text("Odbierz", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showStartText) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .alpha(alphaAnim.value),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = readyText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 42.sp,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Start!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 46.sp,
                    color = textColor,
                    modifier = Modifier.scale(bounceAnim.value)
                )
                Spacer(modifier = Modifier.height(10.dp))
                // informacja o trybie gry
                val modeText = when (setOfCardsSize) {
                    2 -> "Tryb: Pary (2)"
                    3 -> "Tryb: Trójki (3)"
                    4 -> "Tryb: Czwórki (4)"
                    else -> "Tryb: Piątki (5)"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 90.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = modeText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }

            }
        }
    }
}
@Composable
fun GiftBox(onOpen: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.prezent),
                contentDescription = "Prezent",
                modifier = Modifier
                    .size(150.dp)
                    .scale(bounce)
                    .clickable { onOpen() }
            )
            Text(
                text = "Kliknij, aby odebrać nagrodę!",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 40.dp, top = 12.dp)
            )
        }
    }
}
fun getCardRes(number: Int): Int {
    return when (number) {
        1 -> R.drawable.t5_1
        2 -> R.drawable.t5_2
        3 -> R.drawable.t5_3
        4 -> R.drawable.t5_4
        5 -> R.drawable.t5_5
        6 -> R.drawable.t5_6
        7 -> R.drawable.t5_7
        8 -> R.drawable.t5_8
        9 -> R.drawable.t5_9
        10 -> R.drawable.t5_10
        11 -> R.drawable.t5_11
        12 -> R.drawable.t5_12
        13 -> R.drawable.t5_13
        14 -> R.drawable.t5_14
        15 -> R.drawable.t5_15
        16 -> R.drawable.t5_16
        17 -> R.drawable.t5_17
        18 -> R.drawable.t5_18
        19 -> R.drawable.t5_19
        20 -> R.drawable.t5_20
        21 -> R.drawable.t5_21
        22 -> R.drawable.t5_22
        23 -> R.drawable.t5_23
        24 -> R.drawable.t5_24
        25 -> R.drawable.t5_25
        26 -> R.drawable.t5_26
        27 -> R.drawable.t5_27
        28 -> R.drawable.t5_28
        29 -> R.drawable.t5_29
        30 -> R.drawable.t5_30
        31 -> R.drawable.t5_31
        32 -> R.drawable.t5_32
        else -> R.drawable.t5_tylkart
    }
}