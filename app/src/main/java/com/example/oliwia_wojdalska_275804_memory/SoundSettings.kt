package com.example.oliwia_wojdalska_275804_memory

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object SoundSettings {

    // głośność muzyki menu
    var menuVolume by mutableStateOf(0.4f)

    // głośność muzyki po wygranej
    var winVolume by mutableStateOf(0.8f)

    // głośność dźwięku startu
    var startVolume by mutableStateOf(0.5f)
}