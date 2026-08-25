package com.example.bizops.data.model

import androidx.compose.ui.graphics.Color

/**
 * Pre-defined styles and layouts for invoices.
 */
enum class InvoiceTemplateStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val isDarkHero: Boolean = false,
    val fontStyle: String = "SansSerif" // SansSerif, Serif, Monospace
) {
    MODERN_EXECUTIVE(
        id = "modern_executive",
        title = "Modern Executive",
        subtitle = "Sleek blue accent with bold headers & clean metric cards",
        tag = "Popular",
        primaryColor = Color(0xFF2563EB),
        secondaryColor = Color(0xFF0F172A),
        accentColor = Color(0xFF3B82F6),
        isDarkHero = false,
        fontStyle = "SansSerif"
    ),
    MINIMALIST_CLEAN(
        id = "minimalist_clean",
        title = "Minimalist Clean",
        subtitle = "Monochrome slate aesthetic with airy spacing & fine lines",
        tag = "Minimal",
        primaryColor = Color(0xFF334155),
        secondaryColor = Color(0xFF0F172A),
        accentColor = Color(0xFF64748B),
        isDarkHero = false,
        fontStyle = "SansSerif"
    ),
    CLASSIC_CORPORATE(
        id = "classic_corporate",
        title = "Classic Corporate",
        subtitle = "Timeless navy & burgundy with formal serif typography",
        tag = "Formal",
        primaryColor = Color(0xFF1E3A8A),
        secondaryColor = Color(0xFF881337),
        accentColor = Color(0xFF172554),
        isDarkHero = false,
        fontStyle = "Serif"
    ),
    EMERALD_GROWTH(
        id = "emerald_growth",
        title = "Emerald Growth",
        subtitle = "Fresh forest green & mint tones with rounded pill headers",
        tag = "Fintech",
        primaryColor = Color(0xFF059669),
        secondaryColor = Color(0xFF064E3B),
        accentColor = Color(0xFF10B981),
        isDarkHero = false,
        fontStyle = "SansSerif"
    ),
    TECH_DARK_HERO(
        id = "tech_dark_hero",
        title = "Tech Dark Banner",
        subtitle = "Striking slate-900 hero banner with high-contrast electric blue",
        tag = "High Contrast",
        primaryColor = Color(0xFF0F172A),
        secondaryColor = Color(0xFF38BDF8),
        accentColor = Color(0xFF0284C7),
        isDarkHero = true,
        fontStyle = "Monospace"
    ),
    CREATIVE_CORAL(
        id = "creative_coral",
        title = "Creative Coral",
        subtitle = "Vibrant sunset amber & coral accents with agency flair",
        tag = "Agency",
        primaryColor = Color(0xFFEA580C),
        secondaryColor = Color(0xFF7C2D12),
        accentColor = Color(0xFFF97316),
        isDarkHero = false,
        fontStyle = "SansSerif"
    ),
    ROYAL_ENTERPRISE(
        id = "royal_enterprise",
        title = "Royal Enterprise",
        subtitle = "Prestigious deep violet and lavender luxury borders",
        tag = "Premium",
        primaryColor = Color(0xFF6D28D9),
        secondaryColor = Color(0xFF4C1D95),
        accentColor = Color(0xFF8B5CF6),
        isDarkHero = false,
        fontStyle = "SansSerif"
    );

    companion object {
        fun fromId(id: String?): InvoiceTemplateStyle {
            return values().find { it.id.equals(id, ignoreCase = true) } ?: MODERN_EXECUTIVE
        }
    }
}
