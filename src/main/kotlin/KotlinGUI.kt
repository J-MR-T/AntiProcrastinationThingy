import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.PersistenceHelper
import processes.Process
import processes.ProcessHandler
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import java.util.*
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.concurrent.fixedRateTimer

@ExperimentalFoundationApi
class KotlinGUI {
    private val icon: BufferedImage = try {
        ImageIO.read(File(Path.of("res", "shield-alt-solid.png").toUri()))
    } catch (e: IIOException) {
        BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR)
    }
    private val mainAnnoySound: Clip
    private val fontFolder: Path = Path.of("res", "font")
    private val robotoFolder: Path = fontFolder.resolve("Roboto")
    private val daemonTimers: MutableList<Timer> = mutableListOf()
    private val fonts: FontFamily =
        FontFamily(
            Font(robotoFolder.resolve("Roboto-Light.ttf").toFile(), FontWeight.Light),
            Font(robotoFolder.resolve("Roboto-LightItalic.ttf").toFile(), FontWeight.Light, FontStyle.Italic),
            Font(robotoFolder.resolve("Roboto-Bold.ttf").toFile(), FontWeight.Bold),
            Font(robotoFolder.resolve("Roboto-BoldItalic.ttf").toFile(), FontWeight.Bold, FontStyle.Italic),
            Font(robotoFolder.resolve("Roboto-Black.ttf").toFile(), FontWeight.Black),
            Font(robotoFolder.resolve("Roboto-BlackItalic.ttf").toFile(), FontWeight.Black, FontStyle.Italic),
            Font(robotoFolder.resolve("Roboto-Regular.ttf").toFile(), FontWeight.Normal),
            Font(robotoFolder.resolve("Roboto-Italic.ttf").toFile(), FontWeight.Normal, FontStyle.Italic),
            Font(robotoFolder.resolve("Roboto-Medium.ttf").toFile(), FontWeight.Medium),
            Font(robotoFolder.resolve("Roboto-MediumItalic.ttf").toFile(), FontWeight.Medium, FontStyle.Italic),
            Font(robotoFolder.resolve("Roboto-Thin.ttf").toFile(), FontWeight.Thin),
            Font(robotoFolder.resolve("Roboto-ThinItalic.ttf").toFile(), FontWeight.Thin, FontStyle.Italic),
        )

    @Composable
    private fun defaultTheme(content: @Composable() () -> Unit) = MaterialTheme(
        colors = darkColors(),
        typography = Typography(fonts),
        content = content,
    )

    private val windowCloseRequest: () -> Unit = {
        daemonTimers.forEach {
            it.cancel()
            it.purge()
        }
        PersistenceHelper.writeBlacklistSet(ProcessHandler.blacklisted)
        PersistenceHelper.writeHiddenProcesses(ProcessHandler.hiddenProcesses)
    }

    init {
        val audioInputStream =
            AudioSystem.getAudioInputStream(File(Path.of("res", "mixkit-lone-wolf-howling-1729.wav")
                .toString()).absoluteFile)
        mainAnnoySound = AudioSystem.getClip()
        mainAnnoySound.open(audioInputStream)
        var volume = 0.6f
        val volumeControl: FloatControl = mainAnnoySound.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val range = volumeControl.maximum - volumeControl.minimum
        volumeControl.value = (range * volume) + volumeControl.minimum
//        mainAnnoySound.start()
    }

    fun getWindow() {
        return Window(
            title = "APT",
            size = IntSize(1280,720),
            icon = icon,
            onDismissRequest = windowCloseRequest,
        ) {
            val automaticallyKillDisallowed = remember { mutableStateOf(false) }
            val processList: SnapshotStateList<Process> = mutableStateListOf()
            processList.addAll(ProcessHandler.computeFilteredProcessList(true))
            daemonTimers.add(initializeProcessUpdateTimer(processList))
            daemonTimers.add(initializeAnnoyTimer(automaticallyKillDisallowed.value))

            //TODO fix audio

            defaultTheme {
                Box(modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(1f)) {
                        LazyColumn(
                            Modifier.fillMaxWidth(0.5f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(10.dp),
                        ) {
                            stickyHeader {
                                defaultHeader("All Processes")
                            }
                            items(processList) { proc ->
                                textBox(proc) {
                                    ProcessHandler.blacklisted.add(proc.command())
                                }
                            }
                        }
                        LazyColumn(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(10.dp),
                        ) {
                            stickyHeader {
                                defaultHeader("Blacklisted Processes")
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Checkbox(automaticallyKillDisallowed.value,
                                        onCheckedChange = { automaticallyKillDisallowed.value = it }
                                    )
                                    Text(
                                        "Automatically kill disallowed processes if running",
                                        color = MaterialTheme.colors.secondary,
                                        fontWeight = FontWeight.Light,
                                    )
                                }
                            }
                            ProcessHandler.blacklistedProcesses().forEach { proc ->
                                item {
                                    textBox(proc) {
                                        ProcessHandler.blacklisted.remove(proc.command())
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    @Composable
    private fun defaultHeader(text: String ) {
        Text(
            text = text,
            color = MaterialTheme.colors.secondary,
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Thin,
            textAlign = TextAlign.Center,
            modifier = Modifier.background(MaterialTheme.colors.surface),
        )
    }

    private fun initializeProcessUpdateTimer(list: SnapshotStateList<Process>): Timer {
        return fixedRateTimer("Update", true, 1000L, 1000L) {
            val newList = ProcessHandler.computeFilteredProcessList(false)
            if (list != newList) {
                list.removeIf { !newList.contains(it) }
                newList.forEach { if (!list.contains(it)) list.add(it) }
            }
        }
    }

    private fun initializeAnnoyTimer(automaticallyKillDisallowed: Boolean): Timer {
        return fixedRateTimer("Annoy User", true, 0L, 1000L) {
            ProcessHandler.getDisallowedProcessesThatAreRunning().forEach {
                if (automaticallyKillDisallowed) it.kill()
            }
        }
    }

    @Composable
    fun textBox(
        text: Any = "Item",
        fillFraction: Float = 1f,
        fontWeight: FontWeight = FontWeight.Light,
        fontStyle: FontStyle = FontStyle.Normal,
        alignment: Alignment = Alignment.Center,
        onClick: (() -> Unit)? = null,
    ) {
        return Box(
            modifier = Modifier
                .fillMaxWidth(fillFraction)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colors.primary)
                .clickable(true) {
                    onClick?.invoke()
                },
            contentAlignment = alignment,
        ) {
            Text(
                text = text.toString(),
                style = MaterialTheme.typography.body1,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                color = MaterialTheme.colors.onPrimary,
            )

        }
    }

}
