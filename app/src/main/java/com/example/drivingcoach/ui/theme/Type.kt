package com.example.drivingcoach.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.drivingcoach.R

val CoachFontFamily = FontFamily(
    Font(R.font.asta_sans, FontWeight.Normal),
    Font(R.font.asta_sans, FontWeight.Medium),
    Font(R.font.asta_sans, FontWeight.SemiBold),
    Font(R.font.asta_sans, FontWeight.Bold),
)

private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = CoachFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
    letterSpacing = 0.sp,
)

val Typography = Typography(
    displayLarge = type(64, 72, FontWeight.Medium),
    displayMedium = type(48, 56, FontWeight.Medium),
    headlineLarge = type(34, 44, FontWeight.SemiBold),
    headlineMedium = type(28, 38, FontWeight.SemiBold),
    titleLarge = type(24, 34, FontWeight.SemiBold),
    titleMedium = type(20, 28, FontWeight.Medium),
    titleSmall = type(18, 26, FontWeight.Medium),
    bodyLarge = type(20, 30),
    bodyMedium = type(18, 28),
    bodySmall = type(18, 26),
    labelLarge = type(20, 28, FontWeight.SemiBold),
    labelMedium = type(18, 26, FontWeight.Medium),
    labelSmall = type(18, 24),
)
