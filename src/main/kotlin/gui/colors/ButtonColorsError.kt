package gui.colors

import androidx.compose.material.ButtonColors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

class ButtonColorsError:ButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return mutableStateOf(MaterialTheme.colors.error)
    }
    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return mutableStateOf(MaterialTheme.colors.onError)
    }
}