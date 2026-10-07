package dev.jahir.harmonic.colors

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class HarmonicColorExtractorTest {

    @Test
    fun keepsVibrantForegroundThatIsReadableOnDarkBackground() {
        val navy = Color.rgb(0x1A, 0x23, 0x7E)
        val orange = Color.rgb(0xFF, 0x6D, 0x00)

        val colors = HarmonicColorExtractor()
            .setBitmap(twoColorBitmap(top = orange, bottom = navy))
            .setBottomSide()
            .getColors()

        assertSameHue(navy, colors.backgroundColor)
        assertSameHue(orange, colors.firstForegroundColor)
        assertReadable(colors)
    }

    @Test
    fun darkensLowContrastForegroundOnLightBackground() {
        val lightYellow = Color.rgb(0xFF, 0xF5, 0x9D)
        val lightCyan = Color.rgb(0x80, 0xDE, 0xEA)

        val colors = HarmonicColorExtractor()
            .setBitmap(twoColorBitmap(top = lightCyan, bottom = lightYellow))
            .setBottomSide()
            .getColors()

        assertSameHue(lightYellow, colors.backgroundColor)
        assertSameHue(lightCyan, colors.firstForegroundColor)
        assertTrue(
            "foreground should be darker than the original cyan",
            ColorUtils.calculateLuminance(colors.firstForegroundColor) < ColorUtils.calculateLuminance(lightCyan),
        )
        assertReadable(colors)
    }

    /** Top 40% is [top], the rest is [bottom], matching the regions [HarmonicColorExtractor.setBottomSide] reads */
    private fun twoColorBitmap(@ColorInt top: Int, @ColorInt bottom: Int): Bitmap =
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).apply {
                drawColor(bottom)
                clipRect(0, 0, 100, 40)
                drawColor(top)
            }
        }

    private fun assertReadable(colors: HarmonicColors) {
        for (textColor in listOf(colors.firstForegroundColor, colors.secondForegroundColor)) {
            val contrast = ColorUtils.calculateContrast(textColor, colors.backgroundColor)
            assertTrue("contrast $contrast should be at least 4.5", contrast >= 4.5)
        }
    }

    /** Palette quantizes colors and the contrast search changes lightness, so only the hue is stable */
    private fun assertSameHue(@ColorInt expected: Int, @ColorInt actual: Int) {
        val expectedHue = FloatArray(3).also { ColorUtils.colorToHSL(expected, it) }[0]
        val actualHue = FloatArray(3).also { ColorUtils.colorToHSL(actual, it) }[0]
        val diff = abs(expectedHue - actualHue)
        assertEquals(
            "hue of #${Integer.toHexString(actual)}",
            0f,
            min(diff, 360 - diff),
            HUE_TOLERANCE,
        )
    }

    private companion object {
        const val HUE_TOLERANCE = 10f
    }
}
