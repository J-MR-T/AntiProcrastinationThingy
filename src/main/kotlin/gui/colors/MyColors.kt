package gui.colors

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

enum class MyColors(private val colors: Colors) {
    AWESOME_MAGNET(Colors(
        primary = c(0xff5454),
        primaryVariant = c(0xff4138),
        secondary = c(0x6363f7),
        secondaryVariant = c(0x9c91ff),
        background = c(0x263238),
        surface = c(0x263238),
        error = c(0xff5131),
        onPrimary = c(0x000000),
        onSecondary = c(0xffffff),
        onBackground = c(0xffffff),
        onSurface = c(0x000000),
        onError = c(0x000000),
        isLight = false
    )),
    DARK(darkColors()),
    LIGHT(lightColors()),
    ;

    fun getColors(): Colors {
        return colors
    }

}

private fun c(hex: Int): Color {
    return Color(hex+0xff000000)
}