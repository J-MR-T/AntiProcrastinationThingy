import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import processes.ProcessHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun main() = Window(title = "Compose for Desktop", size = IntSize(800, 600)) {
    val count = remember { mutableStateOf(0) }
    MaterialTheme {
        Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
            Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    count.value++
                }) {
                Text(if (count.value == 0) "Hello World" else "Clicked ${count.value}!")
            }
            Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    count.value = 0
                }) {
                Text("Reset")
            }
        }
        Box(contentAlignment = Alignment.CenterStart) {
            Column(Modifier.fillMaxSize(), Arrangement.spacedBy(3.dp)) {
                ProcessHandler.computeReducedProcessList().asSequence().forEach { proc -> textBox(proc.toString()) };
            }
        }
    }
}

@Composable
fun textBox(
    text: String = "Item",
    width: Int = 400,
    height: Int = 32,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.height(height.dp)
            .width(width.dp)
            .background(color = Color(200, 0, 0, 20))
            .padding(start = 10.dp)
//            .clickable(true,onClick = onClick?:(()))
        ,
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text)
    }
}