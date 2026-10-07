package dev.jahir.harmonic.colors.demo

import androidx.annotation.IdRes
import dev.jahir.harmonic.colors.HarmonicColorExtractor

/** Part of the image the background color is read from, one per radio button */
internal enum class Region(@param:IdRes val buttonId: Int) {
    Bottom(R.id.region_bottom),
    Top(R.id.region_top),
    Left(R.id.region_left),
    Right(R.id.region_right),
    Whole(R.id.region_whole);

    fun applyTo(extractor: HarmonicColorExtractor): HarmonicColorExtractor = when (this) {
        Bottom -> extractor.setBottomSide()
        Top -> extractor.setTopSide()
        Left -> extractor.setLeftSide()
        Right -> extractor.setRightSide()
        Whole -> extractor
    }

    companion object {
        fun fromButtonId(@IdRes buttonId: Int): Region = entries.first { it.buttonId == buttonId }
    }
}
