package com.example.oliwia_wojdalska_275804_memory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun ShopScreen(navController: NavController) {
    val context = LocalContext.current
    //  Dodajemy 10 000 do testów
    LaunchedEffect(Unit) {
        EconomyManager.addTestMoney(context)
    }

    var balance by remember { mutableStateOf(EconomyManager.getCurrency(context)) }

    val sections = listOf(
        "Motywy kart" to listOf(
            ShopItem("theme2", "Magiczny blask", 600),
            ShopItem("theme3", "Morska głębia", 800),
            ShopItem("theme4", "Pixel Retro", 1000)
        ),
        "Style ekranów" to listOf(
            ShopItem("screen2", "Magiczny blask", 300),
            ShopItem("screen3", "Morska głębia", 400),
            ShopItem("screen4", "Pixel Retro", 600),
            ShopItem("screen5", "Gwieździsta noc", 800)
        ),
        "Tła menu" to listOf(
            ShopItem("bg2", "Magiczny blask", 400),
            ShopItem("bg3", "Morska głębia", 700),
            ShopItem("bg4", "Pixel Retro", 800),
            ShopItem("bg5", "Gwieździsta noc", 1000)
        )
    )

    val bgColor = Color(0xFF111111)
    val textColor = Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Powrót",
            tint = textColor,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 32.dp)
                .size(42.dp)
                .clickable { navController.popBackStack() }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SKLEP UMYSŁU",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = textColor
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Saldo: $balance 💠",
                color = Color(0xFFFFC107),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            sections.forEach { (title, items) ->
                Text(
                    text = title,
                    fontSize = 22.sp,
                    color = Color(0xFF81D4FA),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )

                items.forEach { item ->
                    val purchased = remember { mutableStateOf(EconomyManager.isPurchased(context, item.id)) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x22222222)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    item.name,
                                    color = textColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (purchased.value)
                                    Text("Kupione", color = Color(0xFF4CAF50), fontSize = 16.sp)
                                else
                                    Text(
                                        "Cena: ${item.price} 💠",
                                        color = Color(0xFFFFC107),
                                        fontSize = 16.sp
                                    )
                            }
                            // czy gracz ma wystarczająco dużo waluty
                            val canAfford = balance >= item.price

                            Button(
                                onClick = {
                                    if (!purchased.value && canAfford) {
                                        val ok = EconomyManager.spendCurrency(context, item.price)
                                        if (ok) {
                                            EconomyManager.setPurchased(context, item.id)
                                            purchased.value = true
                                            balance = EconomyManager.getCurrency(context)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        purchased.value -> Color.Gray
                                        !canAfford -> Color(0x55FFC107) // półprzezroczysty, jeśli nie stać
                                        else -> Color(0xFFFFC107)
                                    },
                                    contentColor = if (canAfford && !purchased.value) Color.Black else Color.DarkGray
                                ),
                                enabled = !purchased.value && canAfford // blokuje kliknięcie jeśli brak środków
                            ) {
                                Text(
                                    when {
                                        purchased.value -> "Kupione"
                                        !canAfford -> "Za mało 💠"
                                        else -> "Kup"
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(50.dp))
        }
    }
}

data class ShopItem(val id: String, val name: String, val price: Int)