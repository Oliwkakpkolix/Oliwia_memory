package com.example.oliwia_wojdalska_275804_memory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateMapOf
import kotlin.math.abs
import androidx.compose.material3.Text
import android.content.Context
import androidx.compose.ui.platform.LocalContext
// tryb dwójki
data class CardState(
    val id: Int,                                        // unikalny identyfikator karty
    val imageRes: Int,                                  // id drawable
    val isFlipped: Boolean = false,                     // czy odsłonięta
    val isMatched: Boolean = false,                     // czy dopasowana
    val testedPairs: MutableSet<Int> = mutableSetOf(),  // zestaw sprawdzonych par
    val seenCount: Int = 0                              // ile razy karta była odsłonięta
)

data class GameStats(
    var totalMoves: Int = 0,                            // wszystkie liczby ruchów
    var correctPairs: Int = 0,                          // prawidłowe pary
    override var cognitiveErrors: Int = 0,              // błędy: poznawcze
    override var associationErrors: Int = 0,            // asocjacyjne
    var geographicalErrors: Int = 0,                    // lokalizacyjne (stary algorytm)
    override var geographicalErrorsWithAnchor: Int = 0, // lokalizacyjne z kotwicą
    var learningMoves: Int = 0,                         // ruchy uczące
    var memoryTests: Int = 0,                           // testy pamięci
    var hypothesisTests: Int = 0,                       // testy hipotez
    override var attentionErrors: Int = 0,              // błędy uwagi
    override var levelTime: Int = 0                     // czas poziomu
) : BaseGameStats
// układ siatki dla liczby kart
fun getBestGrid(cardsCount: Int): Pair<Int, Int> {
    val grids = listOf(
        2 to 2, 3 to 2, 4 to 2,
        4 to 3, 4 to 4,
        5 to 4, 6 to 4, 7 to 4, 8 to 4,
        6 to 6, 8 to 5, 8 to 6, 8 to 7
    )
    return grids.minBy { abs((it.first * it.second) - cardsCount) }
}
// generowanie kart
fun generateCards(cardsCount: Int, context: Context): List<CardState> {
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    val selectedTheme = prefs.getString("selected_theme", "theme1") ?: "theme1"

    // zależne od motywu
    val prefix = when (selectedTheme) {
        "theme2" -> "t3_"
        "theme3" -> "t4_"
        "theme4" -> "t2_"
        "theme5" -> "t5_"
        else -> "t"
    }
    // lista id obrazków 1 - 32
    val resList = (1..32).mapNotNull { i ->
        val name = if (prefix == "t") "t$i" else "${prefix}$i"
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId != 0) resId else null
    }
    // losujemy połowę kart i tworzymy pary
    val selected = resList.shuffled().take(cardsCount / 2)
    val paired = (selected + selected).shuffled()
    // cardstate z unikalnym id
    return paired.mapIndexed { index, res ->
        CardState(id = index, imageRes = res)
    }
}
// próg błędu asocjacyjnego zależna od liczby kart
fun associationThreshold(cardsCount: Int): Int = when (cardsCount) {
    4, 6, 8 -> 2
    12, 16, 20 -> 3
    24, 28, 32, 36 -> 4
    40 -> 5
    48, 56 -> 6
    else -> 4
}
@Composable
fun MemoryCard(card: CardState, onClick: () -> Unit, size: Dp, highlight: Boolean = false) {
    Card(
        modifier = Modifier
            .size(size)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        // wybór obrazka, przód/tył
        val image = if (card.isFlipped || card.isMatched)
            painterResource(card.imageRes)
        else {
            val prefs = LocalContext.current.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
            val selectedTheme = prefs.getString("selected_theme", "theme1") ?: "theme1"
            val backRes = when (selectedTheme) {
                "theme2" -> R.drawable.t3_tylkart
                "theme3" -> R.drawable.t4_tylkart
                "theme4" -> R.drawable.t2_tylkart
                "theme5" -> R.drawable.t5_tylkart
                else -> R.drawable.t1_tylkart
            }
            painterResource(backRes)
        }
        Image(
            painter = image,
            contentDescription = "Card",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
        )
    }
}
// logika gry
@Composable
fun MemoryGameBoardStay(
    cardsCount: Int,
    elapsedTime: Int,
    setOfCardsSize: Int,
    onLevelFinished: (GameStats) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cards by remember { mutableStateOf(generateCards(cardsCount, context)) }
    var flipped by remember { mutableStateOf<List<CardState>>(emptyList()) }
    var locked by remember { mutableStateOf(false) }
    var activeGeoZone by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // stare zmienne (dla poprzedniego algorytmu)
    var lastAnchorId by remember { mutableStateOf<Int?>(null) }
    var geoErrorInCurrentAttempt by remember { mutableStateOf(false) }

    // aktualne zmienne dla ulepszonego algorytmu z kotwicą trwałą
    var persistentAnchorId by remember { mutableStateOf<Int?>(null) } // trwała kotwica
    var anchorErrorAlreadyCounted by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val gameStats = remember { GameStats() } // statystki gry
    val wrongAttemptsByIds = remember { mutableStateMapOf<Pair<Int, Int>, Int>() } // licznik prób
    val assocThreshold = remember(cardsCount) { associationThreshold(cardsCount) } // próg błędu asocjacyjngeo
    val (rows, cols) = getBestGrid(cards.size)
    val spacing = 3.dp // odstęp między kartami

    // obliczanie strefy błędu lokalizacyjnego
    fun calculateGeoZone(centerId: Int): Set<Int> {
        val r = centerId / cols
        val c = centerId % cols
        val zone = mutableSetOf<Int>()
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols) {
                    zone.add(nr * cols + nc)
                }
            }
        }
        return zone
    }
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
        val selectedTheme = prefs.getString("selected_theme", "theme1") ?: "theme1"
        val backgroundRes = when (selectedTheme) {
            "theme2" -> R.drawable.t3_tlo
            "theme3" -> R.drawable.t4_tlo
            "theme4" -> R.drawable.t2_tlo
            "theme5" -> R.drawable.t5_tlo
            else -> R.drawable.t1_tlo
        }
        // tło zależne od motywu
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Tło motywu",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        // rozmiar karty
        val cardSize = minOf(
            (maxWidth - spacing * (cols - 1)) / cols,
            (maxHeight - spacing * (rows - 1)) / rows
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items(cards, key = { it.id }) { card ->
                MemoryCard(
                    card = card,
                    onClick = {
                        if (locked) return@MemoryCard // blokada kliknięcia
                        if (card.isFlipped || card.isMatched) {
                            gameStats.attentionErrors++ // błąd uwagi
                            return@MemoryCard
                        }

                        // stary licznik błędów lokalizacyjnych
                        if (activeGeoZone.contains(card.id)) {
                            gameStats.geographicalErrors++
                        }
                        // aktualny algorytm lokalizacyjny z kotwicą trwałą
                        if (
                            activeGeoZone.contains(card.id) &&
                            persistentAnchorId != null &&
                            card.id != persistentAnchorId &&
                            !anchorErrorAlreadyCounted
                        ) {
                            gameStats.geographicalErrorsWithAnchor++
                            anchorErrorAlreadyCounted = true
                        }
                        // odsłonięcie karty
                        val newCards = cards.map {
                            if (it.id == card.id)
                                it.copy(isFlipped = true, seenCount = it.seenCount + 1)
                            else it
                        }
                        cards = newCards
                        // lista kart odsłoniętych
                        val current = newCards.filter { it.isFlipped && !it.isMatched }
                        flipped = current
                        // ustawienie / zmiana kotwicy trwałej
                        if (current.size == 1) {
                            val firstId = current.first().id
                            if (persistentAnchorId == null) {
                                persistentAnchorId = firstId
                                anchorErrorAlreadyCounted = false
                            } else if (persistentAnchorId != firstId) {
                                persistentAnchorId = firstId
                                anchorErrorAlreadyCounted = false
                            }
                        }
                        // wyznaczenie strefy błędu dla odsłoniętej karty
                        if (current.size == 1) {
                            val first = current.first()
                            val mate = newCards.firstOrNull {
                                it.imageRes == first.imageRes && it.id != first.id && !it.isMatched && !it.isFlipped
                            }
                            activeGeoZone = if (
                                mate != null && first.seenCount > 0 && mate.seenCount > 0
                            ) calculateGeoZone(mate.id) else emptySet()
                        }
                        // sprawdzenie dwóch kart
                        if (current.size == 2) {
                            locked = true
                            activeGeoZone = emptySet()
                            val first = current[0]
                            val second = current[1]
                            val idA = minOf(first.id, second.id)
                            val idB = maxOf(first.id, second.id)
                            val pairKey = idA to idB // numer pary
                            scope.launch {
                                // liczenie ruchów uczących, pamięć, hipoteza
                                if (first.testedPairs.isEmpty() && second.testedPairs.isEmpty())
                                    gameStats.learningMoves++
                                else if ((first.testedPairs.isEmpty() && second.testedPairs.isNotEmpty()) ||
                                    (second.testedPairs.isEmpty() && first.testedPairs.isNotEmpty()))
                                    gameStats.memoryTests++
                                else if (first.testedPairs.isNotEmpty() && second.testedPairs.isNotEmpty() &&
                                    !first.testedPairs.contains(second.id) && !second.testedPairs.contains(first.id))
                                    gameStats.hypothesisTests++
                                // para dopasowana
                                if (first.imageRes == second.imageRes) {
                                    delay(400)
                                    cards = cards.map {
                                        if (it.imageRes == first.imageRes) it.copy(isMatched = true) else it
                                    }
                                    gameStats.correctPairs++
                                    wrongAttemptsByIds.remove(pairKey)
                                    // jśli trafiona para - reset kotwicy
                                    if (persistentAnchorId != null &&
                                        (first.id == persistentAnchorId || second.id == persistentAnchorId)
                                    ) {
                                        persistentAnchorId = null
                                        anchorErrorAlreadyCounted = false
                                    }
                                } else {
                                    // curr - ile razy była widziana para
                                    // seenCount - ile razy była widziana pojedyncza karta
                                    val curr = (wrongAttemptsByIds[pairKey] ?: 0) + 1
                                    wrongAttemptsByIds[pairKey] = curr
                                    var associationCounted = false
                                    if (first.seenCount >= assocThreshold && second.seenCount >= assocThreshold) {
                                        gameStats.associationErrors++
                                        associationCounted = true
                                    }
                                    if (!associationCounted && curr > assocThreshold) {
                                        gameStats.associationErrors++
                                        associationCounted = true
                                    }
                                    if (!associationCounted && curr > 1) {
                                        gameStats.cognitiveErrors++
                                    }
                                    // dodanie kart do zestawu sprawdzonych par
                                    first.testedPairs.add(second.id)
                                    second.testedPairs.add(first.id)
                                    // odwrócenie kart po złym dopasowaniu
                                    delay(800)
                                    cards = cards.map {
                                        if (it.id == first.id || it.id == second.id)
                                            it.copy(isFlipped = false)
                                        else it
                                    }
                                }
                                gameStats.totalMoves++
                                flipped = emptyList()
                                locked = false
                                // reset starej logiki (dla porównania)
                                lastAnchorId = null
                                geoErrorInCurrentAttempt = false
                                // koniec pozimu
                                if (cards.all { it.isMatched }) {
                                    gameStats.levelTime = elapsedTime
                                    onLevelFinished(gameStats)
                                }
                            }
                        }
                    },
                    size = cardSize,
                    highlight = activeGeoZone.contains(card.id)
                )
            }
        }
    }

//    // statystyki
//    Box(modifier = Modifier.fillMaxSize()) {
//        Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
//            Text(
//                text = """
//    Błędy poznawcze: ${gameStats.cognitiveErrors}
//    Błędy asocjacyjne: ${gameStats.associationErrors}
//    Błędy lokalizacyjne (stary algorytm): ${gameStats.geographicalErrors}
//    Błędy lokalizacyjne (kotwica trwała): ${gameStats.geographicalErrorsWithAnchor}
//    Testy pamięci: ${gameStats.memoryTests}
//    Uczenie się: ${gameStats.learningMoves}
//    Testy hipotezy: ${gameStats.hypothesisTests}
//    Błędy uwagi: ${gameStats.attentionErrors}
//    Ruchy razem: ${gameStats.totalMoves}
//    Kotwica ID (stara): ${lastAnchorId ?: "brak"}
//    Flaga błędu (stara): $geoErrorInCurrentAttempt
//    Kotwica trwała: ${persistentAnchorId ?: "brak"}
//    Policzono błąd dla tej kotwicy: $anchorErrorAlreadyCounted
//    """.trimIndent(),
//                color = Color.Yellow
//            )
//        }
//    }
}