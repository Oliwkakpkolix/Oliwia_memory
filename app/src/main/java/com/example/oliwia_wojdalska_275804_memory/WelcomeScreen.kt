package com.example.oliwia_wojdalska_275804_memory

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun WelcomeScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    var name by remember { mutableStateOf(TextFieldValue("")) }
    //var selectedGender by remember { mutableStateOf<String?>(null) }
    var selectedGender by remember { mutableStateOf("none") }
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFC107), Color(0xFFFFE082))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .padding(24.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "Witaj w grze MEMORY!",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                color = Color(0xFF3E2723),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Podaj swoje imię lub nick:",
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = Color(0xFF5D4037)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                placeholder = { Text("Wpisz imię lub nick...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Text(
                text = "Wybierz płeć:",
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = Color(0xFF5D4037)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // pierwszy rząd kobieta  mężczyzna
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // przycisk wyświetla Kobieta, ale ustawia klucz female
                    GenderButton("Kobieta", selectedGender == "female") { selectedGender = "female" }
                    // przycisk wyświetla Mężczyzna, ale ustawia klucz male
                    GenderButton("Mężczyzna", selectedGender == "male") { selectedGender = "male" }
                }
                // drugi rząd nie chcę podawać
                // przycisk wyświetla Nie chcę podawać, ale ustawia klucz none
                GenderButton("Nie chcę podawać", selectedGender == "none") { selectedGender = "none" }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.text.isNotBlank()) {
                        prefs.edit()
                            .putString("user_name", name.text.trim())
                            .putString("user_gender", selectedGender)
                            .apply()
                        navController.navigate("menu") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                },
                enabled = name.text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black
                )
            ) {
                Text("Zapisz i rozpocznij grę", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GenderButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val background = if (isSelected) Color(0xFFFFC107) else Color.White
    val border = if (isSelected) Color(0xFFFFA000) else Color.Gray

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .border(2.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.Black, fontWeight = FontWeight.Medium)
    }
}