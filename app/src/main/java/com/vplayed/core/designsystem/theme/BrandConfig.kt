package com.vplayed.core.designsystem.theme

import com.vplayed.core.designsystem.color.BrandPalette
import com.vplayed.core.designsystem.color.BrandPalettes
import com.vplayed.core.designsystem.typography.AppFont
import com.vplayed.core.designsystem.typography.AppFonts

/**
 * App-wide brand defaults: which palettes and fonts exist and which are used out of the box.
 * Re-branding (white-label) starts here; feature code never references a palette or font directly.
 */
object BrandConfig {
    val palettes: List<BrandPalette> = listOf(BrandPalettes.Vplayed, BrandPalettes.Monochrome)
    val defaultPalette: BrandPalette = BrandPalettes.Vplayed

    val fonts: List<AppFont> = AppFonts.all
    val defaultFont: AppFont = AppFonts.Inter

    /** Brand colors are shown unless the user opts into wallpaper colors. */
    const val dynamicColorByDefault: Boolean = false

    fun palette(id: String): BrandPalette = palettes.firstOrNull { it.id == id } ?: defaultPalette

    fun font(id: String): AppFont = fonts.firstOrNull { it.id == id } ?: defaultFont
}
