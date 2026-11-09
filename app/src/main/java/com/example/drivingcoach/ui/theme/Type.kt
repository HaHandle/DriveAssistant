package com.example.drivingcoach.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.drivingcoach.R

private val AstaSans = FontFamily(
    Font(R.font.asta_sans_regular, FontWeight.Normal),
    Font(R.font.asta_sans_bold, FontWeight.Bold)
)
private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = AstaSans, fontWeight = weight, fontSize = size.sp,
    lineHeight = line.sp, letterSpacing = 0.sp
)

// Pleos Connect Typography hierarchy. Validate physical glyph height on target hardware.
val Typography = Typography(
    displayLarge = type(60, 80, FontWeight.Bold),
    displayMedium = type(56, 72, FontWeight.Bold),
    displaySmall = type(48, 64, FontWeight.Bold),
    headlineLarge = type(60, 80, FontWeight.Bold),
    headlineMedium = type(56, 72, FontWeight.Bold),
    headlineSmall = type(48, 64, FontWeight.Bold),
    titleLarge = type(40, 52, FontWeight.Bold),
    titleMedium = type(36, 44, FontWeight.Bold),
    titleSmall = type(32, 40, FontWeight.Bold),
    bodyLarge = type(30, 38), bodyMedium = type(28, 36), bodySmall = type(26, 34),
    labelLarge = type(24, 28), labelMedium = type(24, 28), labelSmall = type(20, 24)
)
