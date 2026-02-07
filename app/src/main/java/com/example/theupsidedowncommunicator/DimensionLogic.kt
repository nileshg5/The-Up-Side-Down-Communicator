package com.example.theupsidedowncommunicator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class EncodeMode { MORSE, COLOR, BEEP, GRID }

data class TransmissionState(
    val encryptedBinary: List<String> = emptyList(),
    val mode: EncodeMode = EncodeMode.MORSE,
    val sanity: Int = 100,
    val isPossessed: Boolean = false
)

object DimensionLogic {
    fun encryptMessage(msg: String): List<String> {
        val rot = msg.uppercase().map {
            if (it in 'A'..'Z') {
                val shifted = (((it.code - 65) + 13) % 26) + 65
                shifted.toChar()
            } else it
        }.joinToString("")

        return rot.map {
            (it.code xor 23).toString(2).padStart(8, '0')
        }
    }
}
