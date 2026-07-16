package com.example.oliwia_wojdalska_275804_memory

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
// tryb czwórki
data class CardStateQuad(
    val id: Int,
    val imageRes: Int,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false,
    val testedQuads: MutableSet<List<Int>> = mutableSetOf(),
    val seenCount: Int = 0
)
data class GameStatsQuad(
    var totalMoves: Int = 0,
    var correctQuads: Int = 0,
    override var cognitiveErrors: Int = 0,
    override var associationErrors: Int = 0,
    var geographicalErrors: Int = 0,
    override var geographicalErrorsWithAnchor: Int = 0,
    var learningMoves: Int = 0,
    var memoryTests: Int = 0,
    var hypothesisTests: Int = 0,
    override var attentionErrors: Int = 0,
    override var levelTime: Int = 0
) : BaseGameStats
fun getBestQuadGrid(cardsCount: Int): Pair<Int, Int> {
    val grids = listOf(
        4 to 2, 4 to 3, 4 to 4,
        6 to 4, 7 to 4, 8 to 4, 9 to 4,
        8 to 5, 8 to 6, 8 to 8, 9 to 8, 10 to 8
    )
    return grids.minBy { abs((it.first * it.second) - cardsCount) }
}
fun generateQuadCards(cardsCount: Int, context: Context): List<CardStateQuad> {
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    val selectedTheme = prefs.getString("selected_theme", "theme1") ?: "theme1"
    val prefix = when (selectedTheme) {
        "theme2" -> "t3_"
        "theme3" -> "t4_"
        "theme4" -> "t2_"
        "theme5" -> "t5_"

        else -> "t"
    }
    val resList = (1..32).mapNotNull { i ->
        val name = if (prefix == "t") "t$i" else "${prefix}$i"
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId != 0) resId else null
    }
    val selected = resList.shuffled().take(cardsCount / 4)
    val quadrupled = (selected + selected + selected + selected).shuffled()
    return quadrupled.mapIndexed { index, res -> CardStateQuad(id = index, imageRes = res) }
}
fun associationThresholdQuad(cardsCount: Int): Int = when (cardsCount) {
    8, 12, 16 -> 3
    24, 28, 32 -> 4
    36, 40 -> 5
    48, 64, 72, 80 -> 6
    else -> 5
}
@Composable
fun MemoryCardQuad(card: CardStateQuad, onClick: () -> Unit, size: Dp, highlight: Boolean = false) {
    Card(
        modifier = Modifier
            .size(size)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))) {
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
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
@Composable
fun MemoryGameBoardQuad(
    cardsCount: Int,
    elapsedTime: Int,
    onLevelFinished: (GameStatsQuad) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cards by remember { mutableStateOf(generateQuadCards(cardsCount, context)) }
    var flipped by remember { mutableStateOf<List<CardStateQuad>>(emptyList()) }
    var locked by remember { mutableStateOf(false) }
    var activeGeoZone by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var persistentAnchorId by remember { mutableStateOf<Int?>(null) }
    var anchorErrorAlreadyCounted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val gameStats = remember { GameStatsQuad() }
    val wrongAttemptsByQuad = remember { mutableStateMapOf<List<Int>, Int>() }
    fun quadKeyOf(a: Int, b: Int, c: Int, d: Int): List<Int> = listOf(a, b, c, d).sorted()
    val assocThreshold = remember(cardsCount) { associationThresholdQuad(cardsCount) }
    val (rows, cols) = getBestQuadGrid(cards.size)
    val spacing = 3.dp

    fun calculateGeoZone(centerId: Int): Set<Int> {
        val r = centerId / cols
        val c = centerId % cols
        val zone = mutableSetOf<Int>()
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols) zone.add(nr * cols + nc)
        }
        return zone
    }
    val prefs = context.getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
    val selectedTheme = prefs.getString("selected_theme", "theme1") ?: "theme1"

    val backgroundRes = when (selectedTheme) {
        "theme2" -> R.drawable.t3_tlo
        "theme3" -> R.drawable.t4_tlo
        "theme4" -> R.drawable.t2_tlo
        "theme5" -> R.drawable.t5_tlo
        else -> R.drawable.t1_tlo
    }
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Tło motywu",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
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
                MemoryCardQuad(
                    card = card,
                    size = cardSize,
                    highlight = activeGeoZone.contains(card.id),
                    onClick = {
                        if (locked) return@MemoryCardQuad
                        if (card.isFlipped || card.isMatched) {
                            gameStats.attentionErrors++
                            return@MemoryCardQuad
                        }
                        val isInGeoZone = activeGeoZone.contains(card.id)
                        val isSameImageAsAnyFlipped = flipped.any { it.imageRes == card.imageRes }
                        if (isInGeoZone && !isSameImageAsAnyFlipped) {
                            gameStats.geographicalErrors++
                        }
                        if (
                            activeGeoZone.contains(card.id) &&
                            persistentAnchorId != null &&
                            card.id != persistentAnchorId &&
                            !anchorErrorAlreadyCounted
                        ) {
                            gameStats.geographicalErrorsWithAnchor++
                            anchorErrorAlreadyCounted = true
                        }
                        val newCards = cards.map {
                            if (it.id == card.id)
                                it.copy(isFlipped = true, seenCount = it.seenCount + 1)
                            else it
                        }
                        cards = newCards
                        val current = newCards.filter { it.isFlipped && !it.isMatched }
                        flipped = current
                        if (current.size == 1) {
                            val firstId = current.first().id
                            if (persistentAnchorId == null || persistentAnchorId != firstId) {
                                persistentAnchorId = firstId
                                anchorErrorAlreadyCounted = false
                            }
                        }
                        val prevZone = activeGeoZone
                        activeGeoZone = when (current.size) {
                            1, 2, 3 -> {
                                val first = current[0]
                                val allSame = newCards.filter { it.imageRes == first.imageRes }
                                val knownHiddenOthers = allSame.filter {
                                    it.id != first.id && it.seenCount > 0 && !it.isFlipped && !it.isMatched
                                }
                                if (knownHiddenOthers.isNotEmpty()) {
                                    val zones = mutableSetOf<Int>()
                                    knownHiddenOthers.forEach { zones.addAll(calculateGeoZone(it.id)) }
                                    zones.removeAll(allSame.map { it.id }.toSet())
                                    zones
                                } else prevZone
                            }
                            4 -> emptySet()
                            else -> prevZone
                        }
                        if (current.size == 4) {
                            locked = true
                            activeGeoZone = emptySet()
                            val c1 = current[0]
                            val c2 = current[1]
                            val c3 = current[2]
                            val c4 = current[3]
                            val quadKey = quadKeyOf(c1.id, c2.id, c3.id, c4.id)
                            scope.launch {
                                val allCards = listOf(c1, c2, c3, c4)
                                val knownCardsCount = allCards.count { it.testedQuads.isNotEmpty() }
                                if (knownCardsCount == 0) gameStats.learningMoves++
                                else if (knownCardsCount < 4) gameStats.memoryTests++
                                else gameStats.hypothesisTests++
                                if (c1.imageRes == c2.imageRes && c2.imageRes == c3.imageRes && c3.imageRes == c4.imageRes) {
                                    delay(400)
                                    cards = cards.map {
                                        if (it.imageRes == c1.imageRes) it.copy(isMatched = true) else it
                                    }
                                    gameStats.correctQuads++
                                    wrongAttemptsByQuad.remove(quadKey)
                                    if (persistentAnchorId != null &&
                                        listOf(c1.id, c2.id, c3.id, c4.id).contains(persistentAnchorId)
                                    ) {
                                        persistentAnchorId = null
                                        anchorErrorAlreadyCounted = false
                                    }
                                } else {
                                    val curr = (wrongAttemptsByQuad[quadKey] ?: 0) + 1
                                    wrongAttemptsByQuad[quadKey] = curr
                                    var associationCounted = false
                                    val allFourKnownBySight = allCards.all { it.seenCount >= assocThreshold }
                                    if (allFourKnownBySight) {
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
                                    cards = cards.map { cardState ->
                                        if (allCards.any { it.id == cardState.id }) {
                                            cardState.copy(testedQuads = cardState.testedQuads.toMutableSet().apply {
                                                add(quadKey)
                                            })
                                        } else cardState
                                    }
                                    delay(800)
                                    cards = cards.map {
                                        if (it.id in listOf(c1.id, c2.id, c3.id, c4.id))
                                            it.copy(isFlipped = false)
                                        else it
                                    }
                                }
                                gameStats.totalMoves++
                                flipped = emptyList()
                                locked = false
                                if (cards.all { it.isMatched }) {
                                    gameStats.levelTime = elapsedTime
                                    onLevelFinished(gameStats)
                                }
                            }
                        }
                    }
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
//    Błędy lokalizacyjne (kotwica): ${gameStats.geographicalErrorsWithAnchor}
//    Uczenie się: ${gameStats.learningMoves}
//    Testy pamięci: ${gameStats.memoryTests}
//    Testy hipotezy: ${gameStats.hypothesisTests}
//    Błędy uwagi: ${gameStats.attentionErrors}
//    Ruchy razem: ${gameStats.totalMoves}
//    Kotwica ID: ${persistentAnchorId ?: "brak"}
//    Policzono błąd dla kotwicy: $anchorErrorAlreadyCounted
//    """.trimIndent(),
//                color = Color.Yellow
//            )
//        }
//    }
}