/*
 * Copyright 2017 The Android Open Source Project
 * Copyright 2019 Leonardo Salazar
 * Modified by Jahir Fiquitiva: converted to Kotlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.jahir.harmonic.colors

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlin.math.abs

/**
 * Extracts a background color and two text colors from a bitmap, the way Android's media
 * notifications do.
 *
 * Call [setBitmap] before setting a region, then [getColors]:
 * ```
 * HarmonicColorExtractor().setBitmap(bitmap).setBottomSide().getColors()
 * ```
 */
public class HarmonicColorExtractor {
    private lateinit var bitmap: Bitmap
    private var backgroundRegion: Rect? = null
    private var foregroundRegion: Rect? = null
    private var resizeBitmapArea: Int = 0
    private var filteredBackgroundHsl: FloatArray? = null

    public fun setBitmap(bitmap: Bitmap): HarmonicColorExtractor = apply {
        this.bitmap = bitmap
    }

    public fun setBackgroundRegion(left: Int, top: Int, right: Int, bottom: Int): HarmonicColorExtractor =
        apply { backgroundRegion = Rect(left, top, right, bottom) }

    public fun setForegroundRegion(left: Int, top: Int, right: Int, bottom: Int): HarmonicColorExtractor =
        apply { foregroundRegion = Rect(left, top, right, bottom) }

    @JvmOverloads
    public fun setLeftSide(textColorStartAreaFraction: Float = DEFAULT_TEXT_AREA_FRACTION): HarmonicColorExtractor =
        apply {
            backgroundRegion = Rect(0, 0, bitmap.width / 2, bitmap.height)
            foregroundRegion =
                Rect((bitmap.width * textColorStartAreaFraction).toInt(), 0, bitmap.width, bitmap.height)
        }

    @JvmOverloads
    public fun setTopSide(textColorStartAreaFraction: Float = DEFAULT_TEXT_AREA_FRACTION): HarmonicColorExtractor =
        apply {
            backgroundRegion = Rect(0, 0, bitmap.width, bitmap.height / 2)
            foregroundRegion =
                Rect(0, (bitmap.height * textColorStartAreaFraction).toInt(), bitmap.width, bitmap.height)
        }

    @JvmOverloads
    public fun setRightSide(textColorStartAreaFraction: Float = DEFAULT_TEXT_AREA_FRACTION): HarmonicColorExtractor =
        apply {
            backgroundRegion = Rect(bitmap.width / 2, 0, bitmap.width, bitmap.height)
            foregroundRegion =
                Rect(0, 0, (bitmap.width * textColorStartAreaFraction).toInt(), bitmap.height)
        }

    @JvmOverloads
    public fun setBottomSide(textColorStartAreaFraction: Float = DEFAULT_TEXT_AREA_FRACTION): HarmonicColorExtractor =
        apply {
            backgroundRegion = Rect(0, bitmap.height / 2, bitmap.width, bitmap.height)
            foregroundRegion =
                Rect(0, 0, bitmap.width, (bitmap.height * textColorStartAreaFraction).toInt())
        }

    public fun setResizeBitmapArea(resizeBitmapArea: Int): HarmonicColorExtractor = apply {
        this.resizeBitmapArea = resizeBitmapArea
    }

    public fun getColors(): HarmonicColors {
        val paletteBuilder = Palette.from(bitmap).clearFilters()
        backgroundRegion?.let { paletteBuilder.setRegion(it.left, it.top, it.right, it.bottom) }
        if (resizeBitmapArea != 0) paletteBuilder.resizeBitmapArea(resizeBitmapArea)

        val backgroundColor = findBackgroundColorAndFilter(paletteBuilder.generate())

        foregroundRegion?.let { paletteBuilder.setRegion(it.left, it.top, it.right, it.bottom) }
        filteredBackgroundHsl?.let { backgroundHsl ->
            paletteBuilder.addFilter { _, hsl ->
                // at least 10 degrees hue difference
                val diff = abs(hsl[0] - backgroundHsl[0])
                diff > 10 && diff < 350
            }
        }
        paletteBuilder.addFilter { _, hsl -> !isWhiteOrBlack(hsl) }

        val foregroundColor = selectForegroundColor(backgroundColor, paletteBuilder.generate())
        return ensureColors(backgroundColor, foregroundColor)
    }

    @ColorInt
    private fun selectForegroundColor(@ColorInt backgroundColor: Int, palette: Palette): Int =
        if (ColorUtils.calculateLuminance(backgroundColor) > 0.5) {
            selectForegroundColorForSwatches(
                moreVibrant = palette.darkVibrantSwatch,
                vibrant = palette.vibrantSwatch,
                moreMuted = palette.darkMutedSwatch,
                muted = palette.mutedSwatch,
                dominant = palette.dominantSwatch,
                fallbackColor = Color.BLACK,
            )
        } else {
            selectForegroundColorForSwatches(
                moreVibrant = palette.lightVibrantSwatch,
                vibrant = palette.vibrantSwatch,
                moreMuted = palette.lightMutedSwatch,
                muted = palette.mutedSwatch,
                dominant = palette.dominantSwatch,
                fallbackColor = Color.WHITE,
            )
        }

    @ColorInt
    private fun selectForegroundColorForSwatches(
        moreVibrant: Palette.Swatch?,
        vibrant: Palette.Swatch?,
        moreMuted: Palette.Swatch?,
        muted: Palette.Swatch?,
        dominant: Palette.Swatch?,
        @ColorInt fallbackColor: Int,
    ): Int {
        val coloredCandidate =
            selectVibrantCandidate(moreVibrant, vibrant) ?: selectMutedCandidate(muted, moreMuted)
        return when {
            coloredCandidate != null -> when {
                dominant == null || dominant === coloredCandidate -> coloredCandidate.rgb
                coloredCandidate.population.toFloat() / dominant.population < POPULATION_FRACTION_FOR_DOMINANT &&
                    dominant.hsl[1] > MIN_SATURATION_WHEN_DECIDING -> dominant.rgb
                else -> coloredCandidate.rgb
            }
            dominant != null && hasEnoughPopulation(dominant) -> dominant.rgb
            else -> fallbackColor
        }
    }

    private fun selectMutedCandidate(first: Palette.Swatch?, second: Palette.Swatch?): Palette.Swatch? {
        val firstValid = first != null && hasEnoughPopulation(first)
        val secondValid = second != null && hasEnoughPopulation(second)
        return when {
            firstValid && secondValid -> {
                val populationFraction = first.population / second.population.toFloat()
                if (first.hsl[1] * populationFraction > second.hsl[1]) first else second
            }
            firstValid -> first
            secondValid -> second
            else -> null
        }
    }

    private fun selectVibrantCandidate(first: Palette.Swatch?, second: Palette.Swatch?): Palette.Swatch? {
        val firstValid = first != null && hasEnoughPopulation(first)
        val secondValid = second != null && hasEnoughPopulation(second)
        return when {
            firstValid && secondValid ->
                if (first.population / second.population.toFloat() < POPULATION_FRACTION_FOR_MORE_VIBRANT) second
                else first
            firstValid -> first
            secondValid -> second
            else -> null
        }
    }

    private fun hasEnoughPopulation(swatch: Palette.Swatch): Boolean =
        swatch.population / RESIZE_BITMAP_AREA.toFloat() > MINIMUM_IMAGE_FRACTION

    @ColorInt
    private fun findBackgroundColorAndFilter(palette: Palette): Int {
        // by default we use the dominant palette
        val dominant = palette.dominantSwatch
        if (dominant == null) {
            filteredBackgroundHsl = null
            return Color.WHITE
        }
        if (!isWhiteOrBlack(dominant.hsl)) {
            filteredBackgroundHsl = dominant.hsl
            return dominant.rgb
        }
        // The dominant color is black or white, so look at the most populated colored swatch
        val second = palette.swatches
            .filter { it !== dominant && !isWhiteOrBlack(it.hsl) }
            .maxByOrNull { it.population }
        if (second == null ||
            dominant.population / second.population.toFloat() > POPULATION_FRACTION_FOR_WHITE_OR_BLACK
        ) {
            filteredBackgroundHsl = null
            return dominant.rgb
        }
        filteredBackgroundHsl = second.hsl
        return second.rgb
    }

    private fun isWhiteOrBlack(hsl: FloatArray): Boolean =
        hsl[2] <= BLACK_MAX_LIGHTNESS || hsl[2] >= WHITE_MIN_LIGHTNESS

    private fun ensureColors(@ColorInt backgroundColor: Int, @ColorInt foregroundColor: Int): HarmonicColors {
        val backgroundLuminance = ColorUtils.calculateLuminance(backgroundColor)
        val foregroundLuminance = ColorUtils.calculateLuminance(foregroundColor)
        // We only respect the given colors if worst case Black or White still has contrast
        val backgroundLight =
            (backgroundLuminance > foregroundLuminance && satisfiesTextContrast(backgroundColor, Color.BLACK)) ||
                (backgroundLuminance <= foregroundLuminance && !satisfiesTextContrast(backgroundColor, Color.WHITE))

        val primaryTextColor: Int
        val secondaryTextColor: Int
        if (!satisfiesTextContrast(backgroundColor, foregroundColor)) {
            secondaryTextColor = findContrastColor(foregroundColor, backgroundColor, backgroundLight)
            primaryTextColor = changeColorLightness(secondaryTextColor, -lightnessTextDifference(backgroundLight))
        } else {
            val secondaryCandidate =
                changeColorLightness(foregroundColor, lightnessTextDifference(backgroundLight))
            if (satisfiesTextContrast(backgroundColor, secondaryCandidate)) {
                primaryTextColor = foregroundColor
                secondaryTextColor = secondaryCandidate
            } else {
                secondaryTextColor = findContrastColor(secondaryCandidate, backgroundColor, backgroundLight)
                primaryTextColor =
                    changeColorLightness(secondaryTextColor, -lightnessTextDifference(backgroundLight))
            }
        }
        return HarmonicColors(backgroundColor, primaryTextColor, secondaryTextColor)
    }

    private companion object {
        private const val DEFAULT_TEXT_AREA_FRACTION = 0.4f

        /** The fraction below which we select the vibrant instead of the light/dark vibrant color */
        private const val POPULATION_FRACTION_FOR_MORE_VIBRANT = 1.0f

        /** Minimum saturation that a muted color must have if deciding between two colors */
        private const val MIN_SATURATION_WHEN_DECIDING = 0.19f

        /** Minimum fraction that any color must have to be picked up as a text color */
        private const val MINIMUM_IMAGE_FRACTION = 0.002

        /** The population fraction to select the dominant color as the text color over the colored ones */
        private const val POPULATION_FRACTION_FOR_DOMINANT = 0.01f

        /** The population fraction to select a white or black color as the background over a color */
        private const val POPULATION_FRACTION_FOR_WHITE_OR_BLACK = 2.5f

        private const val BLACK_MAX_LIGHTNESS = 0.08f
        private const val WHITE_MIN_LIGHTNESS = 0.90f
        private const val RESIZE_BITMAP_AREA = 150 * 150
        private const val MIN_TEXT_CONTRAST = 4.5

        /**
         * Lightness added to the primary text color to get the secondary one. Smaller on dark
         * backgrounds because it looks better there.
         */
        private const val LIGHTNESS_TEXT_DIFFERENCE_LIGHT = 20
        private const val LIGHTNESS_TEXT_DIFFERENCE_DARK = -10

        private fun lightnessTextDifference(backgroundLight: Boolean): Int =
            if (backgroundLight) LIGHTNESS_TEXT_DIFFERENCE_LIGHT else LIGHTNESS_TEXT_DIFFERENCE_DARK

        private fun satisfiesTextContrast(@ColorInt backgroundColor: Int, @ColorInt foregroundColor: Int): Boolean =
            ColorUtils.calculateContrast(foregroundColor, backgroundColor) >= MIN_TEXT_CONTRAST

        @ColorInt
        private fun findContrastColor(@ColorInt color: Int, @ColorInt other: Int, backgroundLight: Boolean): Int =
            if (backgroundLight) findContrastColorAgainstLight(color, other)
            else findContrastColorAgainstDark(color, other)

        /** Binary search on LAB lightness for the lightest color that still has enough contrast */
        @ColorInt
        private fun findContrastColorAgainstLight(@ColorInt color: Int, @ColorInt other: Int): Int {
            if (satisfiesTextContrast(other, color)) return color
            val lab = DoubleArray(3)
            ColorUtils.colorToLAB(color, lab)
            var low = 0.0
            var high = lab[0]
            var i = 0
            while (i < 15 && high - low > 0.00001) {
                val lightness = (low + high) / 2
                if (ColorUtils.calculateContrast(ColorUtils.LABToColor(lightness, lab[1], lab[2]), other) >
                    MIN_TEXT_CONTRAST
                ) {
                    low = lightness
                } else {
                    high = lightness
                }
                i++
            }
            return ColorUtils.LABToColor(low, lab[1], lab[2])
        }

        /** Binary search on HSL lightness for the darkest color that still has enough contrast */
        @ColorInt
        private fun findContrastColorAgainstDark(@ColorInt color: Int, @ColorInt other: Int): Int {
            if (satisfiesTextContrast(other, color)) return color
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(color, hsl)
            var low = hsl[2]
            var high = 1f
            var i = 0
            while (i < 15 && high - low > 0.00001) {
                val lightness = (low + high) / 2
                hsl[2] = lightness
                if (ColorUtils.calculateContrast(ColorUtils.HSLToColor(hsl), other) > MIN_TEXT_CONTRAST) {
                    high = lightness
                } else {
                    low = lightness
                }
                i++
            }
            // AOSP returns the last color tried, which can end just below the minimum contrast
            hsl[2] = high
            return ColorUtils.HSLToColor(hsl)
        }

        @ColorInt
        private fun changeColorLightness(@ColorInt baseColor: Int, amount: Int): Int {
            val lab = DoubleArray(3)
            ColorUtils.colorToLAB(baseColor, lab)
            return ColorUtils.LABToColor((lab[0] + amount).coerceIn(0.0, 100.0), lab[1], lab[2])
        }
    }
}
