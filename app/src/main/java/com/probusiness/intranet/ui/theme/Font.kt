package com.probusiness.intranet.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.probusiness.intranet.R

/** Epilogue (variable font) — la misma tipografía que usa el frontend web de la intranet. */
@OptIn(ExperimentalTextApi::class)
val Epilogue = FontFamily(
    Font(R.font.epilogue, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.epilogue, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.epilogue, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.epilogue, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.epilogue, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.epilogue, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)
