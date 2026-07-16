package com.example.oliwia_wojdalska_275804_memory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun TimedLevelScreen(navController: NavController, level: Int) {
    var timeLeft by remember { mutableStateOf(getTimeLimitForLevel(level)) }
    var finished by remember { mutableStateOf(false) }

    // odliczanie czasu
    LaunchedEffect(finished) {
        if (!finished) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            if (timeLeft <= 0) finished = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010)),
        contentAlignment = Alignment.Center
    ) {
        if (!finished) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Poziom $level",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Czas: ${timeLeft}s",
                    color = Color.Cyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(40.dp))
                Button(onClick = { finished = true }) {
                    Text("Ukończ poziom")
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Poziom ukończony!",
                    color = Color(0xFF4CAF50),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))
                if (level < 32) {
                    Button(onClick = { navController.navigate("timed_level/${level + 1}") }) {
                        Text("Następny poziom")
                    }
                } else {
                    Text("Gratulacje! Ukończyłeś wszystkie poziomy!", color = Color.Yellow)
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Powrót do menu")
                    }
                }
            }
        }
    }
}

// czas limitu dla poziomu
fun getTimeLimitForLevel(level: Int): Int = when (level) {
    in 1..5 -> 25
    in 6..10 -> 20
    in 11..20 -> 15
    in 21..26 -> 12
    in 27..32 -> 10
    else -> 30
}