package com.example.oliwia_wojdalska_275804_memory
import com.example.oliwia_wojdalska_275804_memory.TimedLevelScreen

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.oliwia_wojdalska_275804_memory.ui.theme.Oliwia_Wojdalska_275804_memoryTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // muzyka menu
        mediaPlayer = MediaPlayer.create(this, R.raw.menu_music)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.4f, 0.4f)
        mediaPlayer?.start()

        setContent {
            Oliwia_Wojdalska_275804_memoryTheme {
                val navController = rememberNavController()
                val prefs = getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
                val userName = prefs.getString("user_name", null)
                NavHost(
                    navController = navController,
                    startDestination = if (userName == null) "welcome" else "menu"
                ) {
                    composable("welcome") { WelcomeScreen(navController) }
                    composable("menu") {
                        LaunchedEffect(Unit) {
                            if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                                mediaPlayer?.start()
                            }
                        }
                        AnimatedMemoryTitle(
                            navController = navController,
                            onMindTrainingClick = {
                                mediaPlayer?.pause()
                                navController.navigate("mind_training")
                            },
                            onReportClick = {
                                mediaPlayer?.pause()
                                navController.navigate("report")
                            },
                            onLevelsClick = {
                                mediaPlayer?.pause()
                                navController.navigate("timed_levels")
                            },
                            onThemesClick = {
                                mediaPlayer?.pause()
                                navController.navigate("themes")
                            },
                            onScreenStylesClick = {
                                mediaPlayer?.pause()
                                navController.navigate("screen_styles")
                            },
                            onMenuBackgroundsClick = {
                                mediaPlayer?.pause()
                                navController.navigate("menu_backgrounds")
                            },
                            onChallengeClick  = {
                                mediaPlayer?.pause()
                                navController.navigate("challenge_screen")
                            },
                            mediaPlayer = mediaPlayer
                        )

                    }

                    composable("mind_training") {
                        MindTrainingScreen(navController)
                    }

                    composable("report") {
                        ReportScreen(navController)
                    }
                    composable("badges") {
                        BadgesScreen(navController)
                    }
                    composable("themes") {
                        ThemeScreen(navController)
                    }
                    composable("screen_styles") {
                        ScreenStyleSettings(navController)
                    }
                    composable("menu_backgrounds") {
                        MenuBackgroundSettings(navController)
                    }
                    composable("challenge_screen") {
                        ChallengeScreen(navController)
                    }
                    composable("challenge_game") {
                        ChallengeGameScreen(navController)
                    }
                    composable("timed_levels") {
                        TimedLevelsScreen(navController)
                    }
                    composable("timed_level/{level}") { backStackEntry ->
                        val level = backStackEntry.arguments?.getString("level")?.toIntOrNull() ?: 1
                        TimedLevelScreen(navController, level)
                    }
                    composable("shop") {
                        ShopScreen(navController)
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
@Composable
fun AnimatedMemoryTitle(
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier,
    onMindTrainingClick: () -> Unit = {},
    onChallengeClick: () -> Unit = {},
    onLevelsClick: () -> Unit = {},
    onScreenStylesClick: () -> Unit = {},
    onMenuBackgroundsClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onThemesClick: () -> Unit = {},
    mediaPlayer: MediaPlayer? = null
) {
    val scope = rememberCoroutineScope()
    // odczyt wybranego tła
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    val selectedBg = prefs.getString("selected_menu_background", "bg1") ?: "bg1"

    val backgroundRes = when (selectedBg) {
        "bg2" -> R.drawable.t3ekran2
        "bg3" -> R.drawable.t4_ekran
        "bg4" -> R.drawable.t3_splash2
        "bg5" -> R.drawable.t5_ekran
        else -> R.drawable.t1_ekran
    }
    // kolor tesktu zależny od motywu
    val textGradientColors = when (selectedBg) {
        "bg2" -> listOf(
            Color(0xFFE9D8B4),
            Color(0xFFD7C093)
        )
        "bg4" -> listOf(
            Color(0xFFE1BEE7).copy(alpha = 0.95f),
            Color(0xFFCE93D8).copy(alpha = 0.85f)
        )
        "bg3" -> listOf(
            Color(0xFF81D4FA).copy(alpha = 0.95f),
            Color(0xFFFFFFFF).copy(alpha = 0.92f)
        )
        "bg5" -> listOf(
            Color(0xFFB3E5FC).copy(alpha = 0.95f),
            Color(0xFF81D4FA).copy(alpha = 0.9f)
        )
        else -> listOf(
            Color(0xFFFFE082).copy(alpha = 0.9f),
            Color(0xFFFFC107).copy(alpha = 0.7f)
        )
    }

    // gradient tła panelu menu - dopasowany do motywu
    val panelGradientColors = when (selectedBg) {
        "bg2" -> listOf(
            Color(0xCC2F1F13),
            Color(0x99513A27)
        )
        "bg4" -> listOf(
            Color(0xAAF8BBD0),
            Color(0xAABA68C8)
        )
        "bg3" -> listOf(
            Color(0xAA81D4FA),
            Color(0xAA0288D1)
        )
        "bg5" -> listOf(
            Color(0xAA0D47A1),
            Color(0x882196F3)
        )
        else -> listOf(
            Color(0xEED7B46A),
            Color(0xCC8D6E63)
        )
    }
    // wspólny styl gradientowy dla tekstów
    fun gradientStyle(shadowBlur: Float) = TextStyle(
        brush = Brush.verticalGradient(colors = textGradientColors),
        shadow = Shadow(
            color = Color(0x33FFFFFF),
            offset = Offset(0f, 0f),
            blurRadius = shadowBlur
        )
    )
    val text = "MEMORY"
    val animatedOffsets = remember { text.map { Animatable(0f) } }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var showCustomizationMenu by remember { mutableStateOf(false) }
    val transition = remember { Animatable(0f) }

    // animacja napisu MEMORY
    LaunchedEffect(Unit) {
        while (true) {
            for (i in text.indices) {
                scope.launch {
                    animatedOffsets[i].animateTo(-15f, tween(600, easing = LinearEasing))
                    animatedOffsets[i].animateTo(0f, tween(800, easing = LinearEasing))
                }
                delay(800L)
            }
            delay(1200L)
        }
    }
    val isAnyPanelOpen = showVolumeSlider || showCustomizationMenu

    Box(modifier = modifier.fillMaxSize()) {
        // rozmycie tłą przy otwrciu panela
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Background",
            modifier = Modifier
                .fillMaxSize()
                .then(if (isAnyPanelOpen) Modifier.blur(12.dp) else Modifier),
            contentScale = ContentScale.Crop
        )
        // ikona menu dopasowana do motywu
        Box(modifier = Modifier.fillMaxSize()) {
            val menuIconColor = when (selectedBg) {
                "bg2" -> Color(0xFFE9D8B4)
                "bg4" -> Color(0xFFE1BEE7)
                "bg3" -> Color(0xFFBBDEFB)
                "bg5" ->Color(0xFFB3E5FC)
                else -> Color(0xFFFFE082)
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = menuIconColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 16.dp)
                        .size(36.dp)
                        .clickable { showVolumeSlider = !showVolumeSlider }
                )
            }

        }
        // napis MEMORY
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            text.forEachIndexed { index, char ->
                Text(
                    text = char.toString(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 68.sp,
                    style = gradientStyle(shadowBlur = 50f),
                    modifier = Modifier
                        .offset(y = animatedOffsets[index].value.dp)
                        .padding(horizontal = 4.dp)
                )
            }
        }
        // przyciski główne
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 230.dp)
                .then(if (isAnyPanelOpen) Modifier.blur(10.dp) else Modifier)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onMindTrainingClick() }
            ) {
                Text(
                    text = "ćwiczenie",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    style = gradientStyle(shadowBlur = 25f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "umysłu",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    style = gradientStyle(shadowBlur = 25f)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(50.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "poziomy",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                    style = gradientStyle(shadowBlur = 0f),
                    modifier = Modifier.clickable {
                        mediaPlayer?.pause()
                        onLevelsClick()
                    }
                )
                Text(
                    text = "wyzwanie",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                    style = gradientStyle(shadowBlur = 0f),
                    modifier = Modifier.clickable {
                        mediaPlayer?.pause()
                        onChallengeClick()
                    }
                )

            }
        }

        // dolne przyciski, personalizacja i sklep
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 28.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isAnyPanelOpen) Modifier.blur(10.dp) else Modifier)
            ) {
                Text(
                    "Personalizacja",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    style = gradientStyle(15f),
                    modifier = Modifier.clickable {
                        showCustomizationMenu = true
                        scope.launch {
                            transition.animateTo(1f, tween(400, easing = LinearEasing))
                        }
                    }
                )
                Text(
                    "Sklep",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    style = gradientStyle(15f),
                    modifier = Modifier.clickable {
                        mediaPlayer?.pause()
                        navController.navigate("shop")
                    }
                )
            }
        }
        // okno głośności
        if (showVolumeSlider) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
                    .clickable { showVolumeSlider = false }
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // panel raport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .background(
                                brush = Brush.verticalGradient(colors = panelGradientColors),
                                shape = RoundedCornerShape(25.dp)
                            )
                            .padding(vertical = 14.dp)
                            .clickable {
                                showVolumeSlider = false
                                onReportClick()
                            }
                    ) {
                        Text(
                            "Ranking",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // panel odznaki
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .background(
                                brush = Brush.verticalGradient(colors = panelGradientColors),
                                shape = RoundedCornerShape(25.dp)
                            )
                            .padding(vertical = 14.dp)
                            .clickable {
                                showVolumeSlider = false
                                navController.navigate("badges")
                            }
                    ) {
                        Text(
                            "Odznaki",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // panel głośności
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.verticalGradient(colors = panelGradientColors),
                                shape = RoundedCornerShape(25.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .width(280.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Regulacja głośności",
                                color = Color(0xFFE3F2FD),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                            Text("Muzyka menu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Slider(
                                value = SoundSettings.menuVolume,
                                onValueChange = { newVolume ->
                                    SoundSettings.menuVolume = newVolume
                                    mediaPlayer?.setVolume(newVolume, newVolume)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFBBDEFB),
                                    activeTrackColor = Color(0xFFE3F2FD),
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                )
                            )
                            Text("Muzyka po wygranej", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Slider(
                                value = SoundSettings.winVolume,
                                onValueChange = { newVolume -> SoundSettings.winVolume = newVolume },
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFBBDEFB),
                                    activeTrackColor = Color(0xFFE3F2FD),
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                )
                            )
                            Text("Dźwięk startu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Slider(
                                value = SoundSettings.startVolume,
                                onValueChange = { newVolume -> SoundSettings.startVolume = newVolume },
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFBBDEFB),
                                    activeTrackColor = Color(0xFFE3F2FD),
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                )
                            )
                        }
                    }
                }
            }
        }

        // panel personalizacji
        if (showCustomizationMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
                    .clickable {
                        scope.launch {
                            transition.animateTo(0f, tween(300))
                            delay(100)
                            showCustomizationMenu = false
                        }
                    }
            )
            // panel wysuwany z dołu
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (300 * (1f - transition.value)).dp)
                    .background(
                        brush = Brush.verticalGradient(panelGradientColors),
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Personalizacja",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Divider(color = Color.White.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Motywy kart",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                showCustomizationMenu = false
                                onThemesClick()
                            }
                            .padding(vertical = 10.dp)
                    )
                    Text(
                        text = "Style ekranów",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                showCustomizationMenu = false
                                mediaPlayer?.pause()
                                onScreenStylesClick()
                            }
                            .padding(vertical = 10.dp)
                    )
                    Text(
                        text = "Tła menu",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                showCustomizationMenu = false
                                mediaPlayer?.pause()
                                onMenuBackgroundsClick()
                            }
                            .padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnimatedMemoryTitlePreview() {
    val fakeNavController = rememberNavController()
    Oliwia_Wojdalska_275804_memoryTheme {
        AnimatedMemoryTitle(navController = fakeNavController)
    }
}