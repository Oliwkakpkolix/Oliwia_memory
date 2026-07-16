package com.example.oliwia_wojdalska_275804_memory


import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.oliwia_wojdalska_275804_memory.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.oliwia_wojdalska_275804_memory.R
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
// tryb wyzwanie
// wynik łączny wyzwania, skuteczność i błędy
fun combinedScoreChallenge(success: Float, errors: Int): Float {
    return maxOf(0f, success - (errors * 5f))
}
@Composable
fun DefinitionBullet(title: String, desc: String, color: Color) {
    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(0.9f)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 18.sp
        )
        Text(
            text = desc,    // opis definicji
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 15.sp,
            lineHeight = 20.sp
        )
    }
}
// odczytanie czasu dla konkretnej planszy (najlepszy / najgorszy / ostatni)
private fun getLevelTimes(context: Context, level: Int, setSize: Int): Triple<Int?, Int?, Int?> {
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    // najlepszy czas
    val best = prefs.getInt("best_time_${setSize}_${level}", -1).takeIf { it != -1 }
    // najgorszy czas
    val worst = prefs.getInt("worst_time_${setSize}_${level}", -1).takeIf { it != -1 }
    // ostatni rozegrany czas
    val last = prefs.getInt("last_time_${setSize}_${level}", -1).takeIf { it != -1 }
    return Triple(best, worst, last)
}
// wynik ukończenia poziomu z błędami
private fun saveLevelResult(
    context: Context,
    level: Int,         // numer poziomu
    setSize: Int,       // rozmiar zestawu kart
    newTime: Int,       // czas ukończenia poziomu
    cognitive: Int,     // błędy: poznawcze
    association: Int,   // asocjacyjne
    localization: Int,  // lokalizacyjne
    attention: Int      // uwagi
) {
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    // edytor do zapisu
    val e = prefs.edit()
    // suma błędów
    val totalErrors = cognitive + association + localization + attention

    // poprzedni najlepszy / najgorszy czas
    val bestTimePrev = prefs.getInt("best_time_${setSize}_${level}", -1)
    val worstTimePrev = prefs.getInt("worst_time_${setSize}_${level}", -1)

    // aktualizacja najlepszego czasu, musi być krótszy
    if (bestTimePrev == -1 || newTime < bestTimePrev) {
        e.putInt("best_time_${setSize}_${level}", newTime)
        e.putInt("best_cognitive_${setSize}_${level}", cognitive)
        e.putInt("best_association_${setSize}_${level}", association)
        e.putInt("best_localization_${setSize}_${level}", localization)
        e.putInt("best_attention_${setSize}_${level}", attention)
        e.putInt("best_total_errors_${setSize}_${level}", totalErrors)
    }

    // aktualizacja najgorszego czasu, musi być dłuższy
    if (worstTimePrev == -1 || newTime > worstTimePrev) {
        e.putInt("worst_time_${setSize}_${level}", newTime)
        e.putInt("worst_cognitive_${setSize}_${level}", cognitive)
        e.putInt("worst_association_${setSize}_${level}", association)
        e.putInt("worst_localization_${setSize}_${level}", localization)
        e.putInt("worst_attention_${setSize}_${level}", attention)
        e.putInt("worst_total_errors_${setSize}_${level}", totalErrors)
    }

    // poprzednie najwięcej / najmniej błędów
    val bestErrorsPrev = prefs.getInt("best_errors_total_${setSize}_${level}", Int.MAX_VALUE)
    val worstErrorsPrev = prefs.getInt("worst_errors_total_${setSize}_${level}", Int.MIN_VALUE)

    // aktualizacja mnijeszej ilości błędów
    if (totalErrors < bestErrorsPrev) {
        e.putInt("best_errors_total_${setSize}_${level}", totalErrors)
        e.putInt("best_errors_time_${setSize}_${level}", newTime)
        e.putInt("best_errors_cognitive_${setSize}_${level}", cognitive)
        e.putInt("best_errors_association_${setSize}_${level}", association)
        e.putInt("best_errors_localization_${setSize}_${level}", localization)
        e.putInt("best_errors_attention_${setSize}_${level}", attention)
    } else if (totalErrors == bestErrorsPrev) {
        // jeśli tyle samo błędów, ale krótszy czas to aktualizuj czas
        val prevTime = prefs.getInt("best_errors_time_${setSize}_${level}", Int.MAX_VALUE)
        if (newTime < prevTime) {
            e.putInt("best_errors_time_${setSize}_${level}", newTime)
        }
    }

    // aktualizacja większej ilości błędów
    if (totalErrors > worstErrorsPrev) {
        e.putInt("worst_errors_total_${setSize}_${level}", totalErrors)
        e.putInt("worst_errors_time_${setSize}_${level}", newTime)
        e.putInt("worst_errors_cognitive_${setSize}_${level}", cognitive)
        e.putInt("worst_errors_association_${setSize}_${level}", association)
        e.putInt("worst_errors_localization_${setSize}_${level}", localization)
        e.putInt("worst_errors_attention_${setSize}_${level}", attention)
    }

    // zapis najnowszego wyniku
    e.putInt("last_time_${setSize}_${level}", newTime)
    e.putInt("last_cognitive_${setSize}_${level}", cognitive)
    e.putInt("last_association_${setSize}_${level}", association)
    e.putInt("last_localization_${setSize}_${level}", localization)
    e.putInt("last_attention_${setSize}_${level}", attention)
    e.putInt("last_errors_total_${setSize}_${level}", totalErrors)
    e.putInt("last_errors_time_${setSize}_${level}", newTime)

    e.apply()
}
@Composable
fun ChallengeGameScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    val setOfCardsSize = prefs.getInt("challenge_set_size", 2)
    // indeks wybranego poziomu
    val selectedIndex = prefs.getInt("challenge_level_index", 0)
    // lista liczby kart do każdej planszy
    val levelSizes = when (setOfCardsSize) {
        3 -> listOf(9, 12, 15, 18, 24, 27, 30, 36, 42, 45, 48, 54, 63, 72, 81, 90)
        4 -> listOf(8, 12, 16, 24, 28, 32, 36, 40, 48, 64, 72, 80)
        5 -> listOf(15, 20, 25, 30, 25, 40, 45, 50, 60, 70, 80)
        else -> listOf(4, 6, 8, 12, 16, 20, 24, 28, 32, 36, 40, 48, 56)
    }
    // liczba kart dla aktualnego poziomu
    val cardsCount = levelSizes.getOrNull(selectedIndex) ?: 8
    var elapsedTime by remember { mutableStateOf(0) }
    var gameRunning by remember { mutableStateOf(false) }
    var levelCompleted by remember { mutableStateOf(false) }
    // skutecznosc i bledy
    var successRate by remember { mutableStateOf(0f) }
    var errorCount by remember { mutableStateOf(0) }

    var cognitiveErrors by remember { mutableStateOf(0) }
    var associationErrors by remember { mutableStateOf(0) }
    var geographicalErrors by remember { mutableStateOf(0) }
    var attentionErrors by remember { mutableStateOf(0) }
    // muzyka po poziomie
    var resultMusic: MediaPlayer? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    // animacja, start
    var showStartText by remember { mutableStateOf(true) }
    val alphaAnim = remember { Animatable(0f) }
    val bounceAnim = remember { Animatable(1f) }
    var startSound: MediaPlayer? by remember { mutableStateOf(null) }
    // płeć użytkowinka odpowiednia forma
    val userGender = prefs.getString("user_gender", "none")
    val readyText = when(userGender) {
        "male" -> "Gotowy?"
        "female" -> "Gotowa?"
        else -> "Gotowi?"
    }

    // odliczanie czasu
    LaunchedEffect(gameRunning) {
        if (gameRunning) {
            while (gameRunning) {
                delay(1000L)
                elapsedTime++
            }
        }
    }
    LaunchedEffect(Unit) {
        showStartText = true
        alphaAnim.snapTo(0f)
        bounceAnim.snapTo(1f)
        // dźwięk startowy
        startSound = MediaPlayer.create(context, R.raw.start_sound).apply {
            setVolume(SoundSettings.startVolume, SoundSettings.startVolume)
        }
        // animacja  tekstu
        alphaAnim.animateTo(1f, tween(600))
        startSound?.start()
        bounceAnim.animateTo(
            targetValue = 1.25f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        bounceAnim.animateTo(1f, spring())

        delay(600L) // krótsze wyświetlenie napisu
        alphaAnim.animateTo(0f, tween(550))
        showStartText = false
        gameRunning = true
        startSound = safeReleaseMediaPlayer(startSound)
    }
    val backgroundRes = getBackgroundForTheme(context)
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Tło gry",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // pasek u góry - taki sam jak w MindTrainingScreen
        if (!levelCompleted) {
            val prefs = LocalContext.current.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
            val selectedTheme = prefs.getString("selected_theme", "theme1") ?: "theme1"

            val (barColors, iconTint, textColor) = when (selectedTheme) {
                "theme2" -> Triple(
                    listOf(Color(0xAA6D4C41), Color(0x552E1F10)), // magiczny - złoto-brązowy
                    Color(0xFFFFE082), // ikona
                    Color(0xFFFFE082)    // tekst
                )
                "theme3" -> Triple(
                    listOf(Color(0xAA003C8F), Color(0x880028CA)), // głębia - niebieski
                    Color(0xFFBBDEFB),
                    Color(0xFFBBDEFB)
                )
                "theme4" -> Triple(
                    listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2)), // pixel Retro - kremowe tło paska
                    Color(0xFF5D4037),
                    Color(0xFF4E342E)
                )
                "theme5" -> Triple(
                    listOf(Color(0xAA004D80), Color(0x5500B0FF)), // chłodny błękit
                    Color(0xFFB3E5FC),
                    Color(0xFFE1F5FE)
                )

                else -> Triple(
                    listOf(Color(0xAA4E342E), Color.Transparent), // złoty klasyk
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
                    text = "Wyzwanie",
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
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Powrót do menu",
                tint = iconTint,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 40.dp, start = 16.dp)
                    .size(32.dp)
                    .clickable {
                        resultMusic?.stop()
                        resultMusic?.release()
                        resultMusic = null
                        navController.popBackStack()
                    }
            )
        }
        // plansza gry
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp)
                .blur(if (levelCompleted) 8.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!levelCompleted && !showStartText) {
                when (setOfCardsSize) {
                    2 -> MemoryGameBoardStay(
                        cardsCount = cardsCount,
                        elapsedTime = elapsedTime,
                        setOfCardsSize = setOfCardsSize,
                        onLevelFinished = { stats ->
                            // zatrzymanie gry
                            gameRunning = false
                            levelCompleted = true
                            // zapis wyników
                            saveLevelResult(
                                context,
                                selectedIndex + 1,
                                setOfCardsSize,
                                elapsedTime,
                                stats.cognitiveErrors,
                                stats.associationErrors,
                                stats.geographicalErrorsWithAnchor,
                                stats.attentionErrors
                            )
                            // błędy
                            errorCount = calculateTotalErrors(stats)
                            cognitiveErrors = stats.cognitiveErrors
                            associationErrors = stats.associationErrors
                            geographicalErrors = stats.geographicalErrorsWithAnchor
                            attentionErrors = stats.attentionErrors
                            // skuteczność
                            val denom = (stats.correctPairs + stats.cognitiveErrors + stats.associationErrors +
                                    stats.geographicalErrorsWithAnchor + stats.attentionErrors)
                            successRate = if (denom > 0)
                                (stats.correctPairs.toFloat() / denom.toFloat()) * 100f else 0f
                        }
                    )
                    3 -> MemoryGameBoardTriple(
                        cardsCount = cardsCount,
                        elapsedTime = elapsedTime,
                        onLevelFinished = { stats ->
                            gameRunning = false
                            levelCompleted = true
                            saveLevelResult(
                                context,
                                selectedIndex + 1,
                                setOfCardsSize,
                                elapsedTime,
                                stats.cognitiveErrors,
                                stats.associationErrors,
                                stats.geographicalErrorsWithAnchor,
                                stats.attentionErrors
                            )
                            errorCount = calculateTotalErrors(stats)
                            cognitiveErrors = stats.cognitiveErrors
                            associationErrors = stats.associationErrors
                            geographicalErrors = stats.geographicalErrorsWithAnchor
                            attentionErrors = stats.attentionErrors
                            val denom = (stats.correctTriples + stats.cognitiveErrors + stats.associationErrors +
                                    stats.geographicalErrorsWithAnchor + stats.attentionErrors)
                            successRate = if (denom > 0)
                                (stats.correctTriples.toFloat() / denom.toFloat()) * 100f else 0f
                        }
                    )
                    4 -> MemoryGameBoardQuad(
                        cardsCount = cardsCount,
                        elapsedTime = elapsedTime,
                        onLevelFinished = { stats ->
                            gameRunning = false
                            levelCompleted = true

                            saveLevelResult(
                                context,
                                selectedIndex + 1,
                                setOfCardsSize,
                                elapsedTime,
                                stats.cognitiveErrors,
                                stats.associationErrors,
                                stats.geographicalErrorsWithAnchor,
                                stats.attentionErrors
                            )
                            errorCount = calculateTotalErrors(stats)
                            cognitiveErrors = stats.cognitiveErrors
                            associationErrors = stats.associationErrors
                            geographicalErrors = stats.geographicalErrorsWithAnchor
                            attentionErrors = stats.attentionErrors
                            val denom = (stats.correctQuads + stats.cognitiveErrors + stats.associationErrors +
                                    stats.geographicalErrorsWithAnchor + stats.attentionErrors)
                            successRate = if (denom > 0)
                                (stats.correctQuads.toFloat() / denom.toFloat()) * 100f else 0f
                        }
                    )
                    5 -> MemoryGameBoardPenta(
                        cardsCount = cardsCount,
                        elapsedTime = elapsedTime,
                        onLevelFinished = { stats ->
                            gameRunning = false
                            levelCompleted = true
                            saveLevelResult(
                                context,
                                selectedIndex + 1,
                                setOfCardsSize,
                                elapsedTime,
                                stats.cognitiveErrors,
                                stats.associationErrors,
                                stats.geographicalErrorsWithAnchor,
                                stats.attentionErrors
                            )
                            errorCount = calculateTotalErrors(stats)
                            cognitiveErrors = stats.cognitiveErrors
                            associationErrors = stats.associationErrors
                            geographicalErrors = stats.geographicalErrorsWithAnchor
                            attentionErrors = stats.attentionErrors
                            val denom = (stats.correctPentas + stats.cognitiveErrors + stats.associationErrors +
                                    stats.geographicalErrorsWithAnchor + stats.attentionErrors)
                            successRate = if (denom > 0)
                                (stats.correctPentas.toFloat() / denom.toFloat()) * 100f else 0f
                        }
                    )
                }
            }
        }
        // ekran wyników, stały nagłówek i przewijane błędy
        if (levelCompleted) {
            val alphaAnim = remember { Animatable(0f) }
            val scaleAnim = remember { Animatable(1.2f) }

            // muzyka po ukończeniu poziomu
            LaunchedEffect(levelCompleted) {
                if (levelCompleted) {
                    resultMusic = safeReleaseMediaPlayer(resultMusic)
                    resultMusic = MediaPlayer.create(context, R.raw.menu_music2).apply {
                        isLooping = true
                        setVolume(SoundSettings.winVolume, SoundSettings.winVolume)
                        start()
                    }
                }
            }
            // gwiazki
            val stars = when {
                successRate >= 90f -> 5
                successRate >= 70f -> 4
                successRate >= 50f -> 3
                successRate >= 30f -> 2
                else -> 1
            }
            val scoreValue = combinedScoreChallenge(successRate, errorCount)
            val scorePct = (scoreValue / 100f).coerceIn(0f, 1f)
            LaunchedEffect(Unit) {
                alphaAnim.animateTo(1f, tween(700))
                scaleAnim.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .alpha(alphaAnim.value),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Poziom ukończony!",
                        color = Color(0xFFFFEB3B),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .scale(scaleAnim.value)
                            .statusBarsPadding()
                            .padding(top = 8.dp),
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
                            .padding(vertical = 10.dp)
                            .scale(scaleAnim.value)
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = if (index < stars) Color(0xFFFFC107) else Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    // skuteczność / błędy / czas
                    Text(
                        text = "Skuteczność: ${"%.1f".format(successRate)}%\n" +
                                "Błędy: $errorCount\n" +
                                "Czas: ${elapsedTime}s",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    if (errorCount > 0) {
                        val scroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .verticalScroll(scroll)
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (cognitiveErrors > 0) {
                                    DefinitionBullet(
                                        title = "Błędy pamięciowe ($cognitiveErrors)",
                                        desc = "Wynikają z trudności w przypomnieniu sobie, gdzie leżała dana karta. Mogą świadczyć o chwilowym zmęczeniu lub osłabieniu pamięci krótkotrwałej.",
                                        color = Color(0xFFFFCDD2)
                                    )
                                } else {
                                    DefinitionBullet(
                                        title = "Świetna pamięć!",
                                        desc = "Nie popełniono żadnego błędu pamięciowego. Twoja koncentracja oraz zapamiętywanie są na bardzo wysokim poziomie!",
                                        color = Color(0xFFA5D6A7)
                                    )
                                }
                                if (associationErrors > 0) {
                                    DefinitionBullet(
                                        title = "Błędy skojarzeń ($associationErrors)",
                                        desc = "Polegają na myleniu kart o podobnym wyglądzie lub tematyce. Wskazują na brak skupienia na detalach lub zbyt szybkie decyzje bez analizy różnic.",
                                        color = Color(0xFFFFE0B2)
                                    )
                                } else {
                                    DefinitionBullet(
                                        title = "Idealne skojarzenia!",
                                        desc = "Świetnie rozpoznajesz powiązania oraz podobieństwa. Twój umysł łączy informacje błyskawicznie i trafnie!",
                                        color = Color(0xFFFFCC80)
                                    )
                                }
                                if (geographicalErrors > 0) {
                                    DefinitionBullet(
                                        title = "Błędy lokalizacji ($geographicalErrors)",
                                        desc = "Oznaczają pomyłki w zapamiętaniu położenia kart. Często pojawiają się, gdy gracz skupia się bardziej na obrazkach niż na przestrzennym układzie planszy.",
                                        color = Color(0xFFBBDEFB)
                                    )
                                } else {
                                    DefinitionBullet(
                                        title = "Perfekcyjna orientacja!",
                                        desc = "Twoja orientacja przestrzenna działa bezbłędnie. Wiesz dokładnie, gdzie co było - to cecha świetnych strategów!",
                                        color = Color(0xFFFFF59D)
                                    )
                                }
                                if (attentionErrors > 0) {
                                    DefinitionBullet(
                                        title = "Błędy uwagi ($attentionErrors)",
                                        desc = "Są skutkiem pośpiechu lub rozproszenia. Najczęściej występują, gdy gracz klika zbyt szybko lub traci koncentrację podczas gry.",
                                        color = Color(0xFFC5E1A5)
                                    )
                                } else {
                                    DefinitionBullet(
                                        title = "Doskonała uwaga!",
                                        desc = "Zachowano pełne skupienie przez cały poziom. To umiejętność, która czyni z Ciebie prawdziwego mistrza spostrzegawczości!",
                                        color = Color(0xFF81D4FA)
                                    )
                                }
                            }
                        }
                    } else {
                        // pozytywne komunikaty przy braku błędów
                        val scroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .verticalScroll(scroll)
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                DefinitionBullet(
                                    title = "Świetna pamięć!",
                                    desc = "Nie popełniono żadnego błędu pamięciowego. Twoja koncentracja oraz zapamiętywanie są na bardzo wysokim poziomie!",
                                    color = Color(0xFFA5D6A7)
                                )
                                DefinitionBullet(
                                    title = "Idealne skojarzenia!",
                                    desc = "Świetnie rozpoznajesz powiązania oraz podobieństwa. Twój umysł łączy informacje błyskawicznie i trafnie!",
                                    color = Color(0xFFFFCC80)
                                )
                                DefinitionBullet(
                                    title = "Perfekcyjna orientacja!",
                                    desc = "Twoja orientacja przestrzenna działa bezbłędnie. Wiesz dokładnie, gdzie co było - to cecha świetnych strategów!",
                                    color = Color(0xFFFFF59D)
                                )
                                DefinitionBullet(
                                    title = "Doskonała uwaga!",
                                    desc = "Zachowano pełne skupienie przez cały poziom. To umiejętność, która czyni z Ciebie prawdziwego mistrza spostrzegawczości!",
                                    color = Color(0xFF81D4FA)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // score jakości i pasek
                    Text(
                        text = "Score jakości: ${"%.1f".format(scoreValue)}",
                        color = Color(0xFF00E5FF),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .background(Color.DarkGray.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
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

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            resultMusic = safeReleaseMediaPlayer(resultMusic)
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEB3B),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Powrót do menu", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // Gotowi Start
        if (showStartText) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .alpha(alphaAnim.value),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
                    val selectedTheme = prefs.getString("selected_theme", "theme1") ?: "theme1"

                    val startTextColor = when (selectedTheme) {
                        "theme2" -> Color(0xFFFFE082) // magiczny - złoty
                        "theme3" -> Color(0xFFBBDEFB) // głębia - błękitny
                        "theme4" -> Color(0xFF4E342E) // ixel Retro - brązowy
                        "theme5" -> Color(0xFF40C4FF) // krystaliczny błękit

                        else -> Color(0xFFFFF176)     // klasyczny - żółty
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
        }
    }
}