package com.probusiness.intranet.ui.support

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.probusiness.intranet.ui.theme.Amber200
import com.probusiness.intranet.ui.theme.Amber600
import com.probusiness.intranet.ui.theme.Amber900
import com.probusiness.intranet.ui.theme.Emerald200
import com.probusiness.intranet.ui.theme.Emerald800
import com.probusiness.intranet.ui.theme.Green200
import com.probusiness.intranet.ui.theme.Green600
import com.probusiness.intranet.ui.theme.Orange200
import com.probusiness.intranet.ui.theme.Orange800
import com.probusiness.intranet.ui.theme.Red200
import com.probusiness.intranet.ui.theme.Red800
import com.probusiness.intranet.ui.theme.Slate200
import com.probusiness.intranet.ui.theme.Slate300
import com.probusiness.intranet.ui.theme.Slate600
import com.probusiness.intranet.ui.theme.Slate700
import com.probusiness.intranet.ui.theme.Sky200
import com.probusiness.intranet.ui.theme.Sky500
import com.probusiness.intranet.ui.theme.Sky800
import com.probusiness.intranet.ui.theme.Teal200
import com.probusiness.intranet.ui.theme.Teal800
import com.probusiness.intranet.ui.theme.Violet200
import com.probusiness.intranet.ui.theme.Violet800

/** Réplica de los colores por estado/prioridad de constants/soporteTiColores.ts del frontend web. */
data class BadgeStyle(val background: Color, val foreground: Color, val border: Color)

@Composable
fun estadoBadgeStyle(estadoCodigo: String?): BadgeStyle {
    val dark = isSystemInDarkTheme()
    return when (estadoCodigo) {
        "en_maqueta" -> if (dark) BadgeStyle(Violet800, Violet200, Violet800) else BadgeStyle(com.probusiness.intranet.ui.theme.Violet50, Violet800, Violet200)
        "en_progreso" -> if (dark) BadgeStyle(Sky800, Sky200, Sky800) else BadgeStyle(com.probusiness.intranet.ui.theme.Sky50, Sky800, Sky200)
        "hecho" -> if (dark) BadgeStyle(Teal800, Teal200, Teal800) else BadgeStyle(com.probusiness.intranet.ui.theme.Teal50, Teal800, Teal200)
        "desplegado" -> if (dark) BadgeStyle(Amber900, Amber200, Amber900) else BadgeStyle(com.probusiness.intranet.ui.theme.Amber50, Amber900, Amber200)
        "observado" -> if (dark) BadgeStyle(Orange800, Orange200, Orange800) else BadgeStyle(com.probusiness.intranet.ui.theme.Orange50, Orange800, Orange200)
        "operativo" -> if (dark) BadgeStyle(Emerald800, Emerald200, Emerald800) else BadgeStyle(com.probusiness.intranet.ui.theme.Emerald50, Emerald800, Emerald200)
        else -> if (dark) BadgeStyle(Slate700, Slate300, Slate700) else BadgeStyle(com.probusiness.intranet.ui.theme.Slate50, Slate700, Slate200)
    }
}

@Composable
fun prioridadBadgeStyle(prioridad: Int?): BadgeStyle {
    val dark = isSystemInDarkTheme()
    return when (prioridad) {
        1 -> if (dark) BadgeStyle(Red800, Red200, Red800) else BadgeStyle(com.probusiness.intranet.ui.theme.Red50, Red800, Red200)
        2 -> if (dark) BadgeStyle(Amber900, Amber200, Amber900) else BadgeStyle(com.probusiness.intranet.ui.theme.Amber50, Amber900, Amber200)
        else -> if (dark) BadgeStyle(Slate700, Slate300, Slate700) else BadgeStyle(com.probusiness.intranet.ui.theme.Slate50, Slate700, Slate200)
    }
}

@Composable
fun statsCardAccentColor(kind: StatsKind): Color {
    val dark = isSystemInDarkTheme()
    return when (kind) {
        StatsKind.TOTAL -> if (dark) Slate300 else Slate600
        StatsKind.PENDIENTES -> if (dark) Amber200 else Amber600
        StatsKind.EN_PROGRESO -> if (dark) Sky200 else Sky500
        StatsKind.COMPLETADAS -> if (dark) Green200 else Green600
    }
}

enum class StatsKind { TOTAL, PENDIENTES, EN_PROGRESO, COMPLETADAS }
