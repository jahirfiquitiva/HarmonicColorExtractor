/*
 * Copyright 2019 Leonardo Salazar
 * Copyright 2026 Jahir Fiquitiva
 *
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

import androidx.annotation.ColorInt

/** Colors extracted by [HarmonicColorExtractor]: a background color and two text colors for it */
public data class HarmonicColors(
    @get:ColorInt public val backgroundColor: Int,
    @get:ColorInt public val firstForegroundColor: Int,
    @get:ColorInt public val secondForegroundColor: Int,
)
