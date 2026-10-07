# Harmonic Colors

Extracts a background color and two readable text colors from a bitmap, the same way Android's media notifications color themselves from the album art.

![Media notifications colored from their album art](https://i0.wp.com/9to5google.com/wp-content/uploads/sites/4/2017/06/dp3_notification_media_theme_1.jpg?resize=1600%2C842&quality=82&strip=all&ssl=1)

## Installation

The library is on Maven Central:

```kotlin
dependencies {
    implementation("dev.jahir:harmonic-colors:1.0.0")
}
```

It supports `minSdk` 21 and depends only on AndroidX (`androidx.core` and `androidx.palette`), so it doesn't need Jetifier.

## Usage

Set a bitmap with `setBitmap` and call `getColors()` at the end:

```kotlin
val colors = HarmonicColorExtractor().setBitmap(bitmap).getColors()
```

`HarmonicColors` holds the three colors extracted from the image: `backgroundColor`, `firstForegroundColor` and `secondForegroundColor`.

```kotlin
container.setBackgroundColor(colors.backgroundColor)
title.setTextColor(colors.firstForegroundColor)
subtitle.setTextColor(colors.secondForegroundColor)
```

From Java, the same calls work and the colors are read with `getBackgroundColor()`, `getFirstForegroundColor()` and `getSecondForegroundColor()`:

```java
HarmonicColors colors = new HarmonicColorExtractor().setBitmap(bitmap).getColors();
```

### Customizing the extraction

You can change how the library reads the colors:

- Set the region the background color is read from with `setBackgroundRegion(left, top, right, bottom)`.
- Set the region the text colors are read from with `setForegroundRegion(left, top, right, bottom)`.
- Change how many pixels the bitmap is scaled down to before reading colors with `setResizeBitmapArea(area)`. A smaller area is faster and less precise.

If you set no region, both colors come from the whole bitmap.

```kotlin
val colors = HarmonicColorExtractor()
    .setBitmap(bitmap)
    .setBackgroundRegion(0, 0, bitmap.width / 2, bitmap.height)
    .setForegroundRegion(bitmap.width / 2, 0, bitmap.width, bitmap.height)
    .setResizeBitmapArea(150 * 150)
    .getColors()
```

Regions are in pixels, with `left` and `top` included and `right` and `bottom` excluded, like [`Rect`](https://developer.android.com/reference/android/graphics/Rect).

### Presets

`setLeftSide()`, `setTopSide()`, `setRightSide()` and `setBottomSide()` read the background color from that half of the bitmap and the text colors from the opposite side:

```kotlin
val colors = HarmonicColorExtractor().setBitmap(bitmap).setLeftSide().getColors()
```

Each preset takes an optional fraction (0.4 by default), measured from the left or top edge: the text color region starts there for `setLeftSide` and `setTopSide`, and ends there for `setRightSide` and `setBottomSide`.

Call `setBitmap` before any preset, because the presets read the bitmap's size.

`getColors()` runs on the calling thread and does not cache results, so call it from a background thread when you extract colors for many images, like the items of a list.

## Demo

The `app` module lets you pick an image with the system photo picker and switch between regions to compare the results.

## Changes from the original

This is a fork of [HarmonicColorExtractor](https://github.com/LeonardoSM04/HarmonicColorExtractor) by Leonardo Salazar, which is based on the media notification color extraction in the Android Open Source Project. Changes in this fork:

- Converted to Kotlin, moved to the `dev.jahir.harmonic.colors` package and published to Maven Central instead of JitPack.
- Uses AndroidX instead of the support libraries.
- The extractor is a single class whose setters return itself, instead of an inner `Builder` class. `setRigtSide()` is now `setRightSide()`.
- `HarmonicColors` is a data class.
- On dark backgrounds, the text color search now returns the last color that passed the 4.5 contrast check. It used to return the last color it tried, which could fall just below 4.5.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
