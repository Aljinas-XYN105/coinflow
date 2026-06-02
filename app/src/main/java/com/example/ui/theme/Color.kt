package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// iOS-Level Premium System Colors
val iOSBlue = Color(0xFF0A84FF)       // iOS System Blue (Dark Mode Active)
val iOSBlueLight = Color(0xFF007AFF)  // iOS System Blue (Light Mode Active)
val iOSGreen = Color(0xFF30D158)      // iOS System Green (Success/Income)
val iOSGreenLight = Color(0xFF34C759) // iOS System Green Light
val iOSRed = Color(0xFFFF453A)        // iOS System Red (Warning/Expense)
val iOSRedLight = Color(0xFFFF3B30)   // iOS System Red Light
val iOSOrange = Color(0xFFFF9F0A)     // iOS System Orange (Alert)
val iOSOrangeLight = Color(0xFFFF9500)// iOS System Orange Light

// iOS Standard System Backgrounds
val iOSDarkBg = Color(0xFF000000)          // Pure iOS Pitch Black (Primary)
val iOSDarkCard = Color(0xFF1C1C1E)        // iOS Secondary Dark Background
val iOSDarkCardSecondary = Color(0xFF2C2C2E)// iOS Tertiary Dark Background
val iOSLightBg = Color(0xFFF2F2F7)         // iOS System Grouped Background 
val iOSLightCard = Color(0xFFFFFFFF)       // iOS System Card White
val iOSLightCardSecondary = Color(0xFFE5E5EA) // Muted Gray Card

val iOSMutedTextDark = Color(0xFF8E8E93)
val iOSMutedTextLight = Color(0xFF8E8E93)
val iOSDividerDark = Color(0x26FFFFFF)      // Soft separator
val iOSDividerLight = Color(0x1F000000)

// Fallback compatibility variables for older usages
val ObsidianBg = iOSDarkBg
val ObsidianSurface = iOSDarkCard
val MintAccent = iOSBlue
val LimeHighlight = iOSGreen
val WarningRed = iOSRed
val SoftGrey = iOSMutedTextDark
val White = Color(0xFFFFFFFF)
val LightBg = iOSLightBg
val LightSurface = iOSLightCard
val MintPrimary = iOSBlueLight
val LightGrey = iOSMutedTextLight
