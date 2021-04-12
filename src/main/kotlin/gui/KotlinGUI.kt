package gui

import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import gui.colors.ButtonColorsPrimary
import gui.colors.MyColors
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
import kotlin.concurrent.timer

class KotlinGUI {
    //timers
    private val daemonTimers: MutableList<Timer> = mutableListOf()

    //options
    private val automaticallyKillDisallowed: MutableState<Boolean> = mutableStateOf(false)
    private val hideInsteadOfBlacklist: MutableState<Boolean> = mutableStateOf(false)
    val volume: MutableState<Float> = mutableStateOf(0.1f);

    //resources
    private val icon: BufferedImage = try {
        ImageIO.read(File(Path.of("res", "shield-alt-solid.png").toUri()))
    } catch (e: IIOException) {
        BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR)
    }
    private val pathToAnnoySound = Path.of("res", "mixkit-lone-wolf-howling-1729.wav")
    private val mainAnnoySound: Clip
    private val fontFolder: Path = Path.of("res", "font")
    private val robotoFolder: Path = fontFolder.resolve("Roboto")
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
        colors = MyColors.AWESOME_MAGNET.getColors(),
        typography = Typography(fonts),
        content = content,
    )

    private val windowCloseRequest: () -> Unit = {
        daemonTimers.forEach {
            it.cancel()
            it.purge()
        }
        PersistenceHelper.stopApp()
    }

    //TODO fix audio
    init {
        val audioInputStream =
            AudioSystem.getAudioInputStream(File(pathToAnnoySound
                .toString()).absoluteFile)
        mainAnnoySound = AudioSystem.getClip()
        mainAnnoySound.open(audioInputStream)
        setVolumeOfClip(mainAnnoySound, volume.value)
    }

    private fun setVolumeOfClip(clip: Clip, volume: Float) {
        val actualVolume = if (volume in -0.05f..0.05f) 0f else (0.5f + volume * 0.4f)
        val volumeControl: FloatControl = mainAnnoySound.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val range = volumeControl.maximum - volumeControl.minimum
        volumeControl.value = (range * actualVolume) + volumeControl.minimum
    }

    fun getWindow() {
        return Window(
            title = "APT",
            size = IntSize(1280, 720),
            icon = icon,
            onDismissRequest = windowCloseRequest,
        ) {
            val processList: SnapshotStateList<Process> = mutableStateListOf()
            processList.addAll(ProcessHandler.computeFilteredProcessList(true))
            daemonTimers.add(initializeProcessUpdateTimer(processList))
            daemonTimers.add(initializeAnnoyTimer(automaticallyKillDisallowed))

            defaultTheme {
                Box(modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
                            val stateLeftList = rememberScrollState(0)
                            Column(
                                Modifier.fillMaxWidth(0.5f).padding(PaddingValues(20.dp)),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                defaultHeading("All Processes")
                                val checkboxFunction: @Composable ((Boolean) -> Unit)? = {
                                    Dialog(onDismissRequest = {},
                                        properties = DialogProperties(title = "Do you really want to deactivate this?")) {
                                        Button(onClick = {
                                            hideInsteadOfBlacklist.value = hideInsteadOfBlacklist.value.not()
                                        }) { }
                                    }
                                }
                                labeledCheckbox("Hide Processes instead of Blacklisting them",
                                    hideInsteadOfBlacklist)
                                //TODO make dialog to confirm you really want to disable the app
//                            val showDialog: MutableState<Boolean> = mutableStateOf(true);
//                            if (showDialog.value) {
//                                Dialog(onDismissRequest = {showDialog.value=false},
//                                    properties = DialogProperties(
//                                        title = "Do you really want to stop this?",
//                                        resizable = false,
//                                    )) {
//
//                                }
//                            }
                                processList.forEach { proc ->
                                    textBox(proc) {
                                        if (hideInsteadOfBlacklist.value) {
                                            ProcessHandler.hiddenProcesses.add(proc.command())
                                        } else {
                                            ProcessHandler.blacklisted.add(proc.command())
                                        }
                                    }
                                }
                            }
                            //FIXME fix scrollbar(s) (make an extra column for the textBoxes with a row which
                            // fills almost everything except the space for the scrollbar)
                            VerticalScrollbar(
                                modifier = Modifier.fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(stateLeftList))
                            Column(
                                Modifier.fillMaxWidth().padding(PaddingValues(20.dp)),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                defaultHeading("Blacklisted Processes")
                                labeledCheckbox("Automatically kill disallowed processes if running",
                                    automaticallyKillDisallowed)
                                ProcessHandler.blacklistedProcesses().forEach { proc ->
                                    textBox(proc) {
                                        ProcessHandler.blacklisted.remove(proc.command())
                                    }
                                }
                            }

                        }
                        Row {
                            Spacer(Modifier.fillMaxWidth(0.5f))

                            Column(modifier = Modifier.fillMaxSize()) {
                                defaultHeading("Settings")
                                Row {
                                    Column(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight()) {
                                        defaultHeading("Volume", MaterialTheme.typography.h5)
                                        Column {
                                            Spacer(modifier = Modifier.padding(5.dp))
                                            Text(
                                                text = "Volume: " + (volume.value*100).toInt(),
                                                style = MaterialTheme.typography.body1,
                                                fontStyle = FontStyle.Normal,
                                                fontWeight = FontWeight.Light,
                                                color = MaterialTheme.colors.primary,
                                            )

                                            Box(modifier = Modifier.fillMaxSize()) {
                                                Slider(volume.value, modifier = Modifier.align(Alignment.BottomCenter),
                                                    onValueChange = {
                                                        volume.value = it
                                                        setVolumeOfClip(mainAnnoySound, volume = volume.value)
                                                    })
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        //Reset button
                                        TextButton(
                                            onClick = ProcessHandler::resetHiddenProcesses,
                                            elevation = ButtonDefaults.elevation(),
                                            modifier = Modifier.align(Alignment.BottomEnd),
                                            colors = ButtonColorsPrimary(),
                                        ) {
                                            Text("Reset hidden processes list", style = MaterialTheme.typography.body2)
                                        }
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
    private fun labeledCheckbox(
        label: String,
        toBeAffected: MutableState<Boolean>,
    ) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Checkbox(checked = toBeAffected.value, { toBeAffected.value = it })
            Text(
                label,
                color = MaterialTheme.colors.secondary,
                fontWeight = FontWeight.Light,
            )
        }
    }

    @Composable
    private fun defaultHeading(text: String, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.h4) =
        Text(
            text = text,
            color = MaterialTheme.colors.secondary,
            style = style,
            fontWeight = FontWeight.Thin,
            textAlign = TextAlign.Center,
            modifier = Modifier.background(MaterialTheme.colors.surface),
        )


    private fun initializeProcessUpdateTimer(list: SnapshotStateList<Process>): Timer {
        return fixedRateTimer("Update", true, 1000L, 1000L) {
            val newList = ProcessHandler.computeFilteredProcessList(false)
            if (list != newList) {
                list.removeIf { !newList.contains(it) }
                newList.forEach { if (!list.contains(it)) list.add(it) }
            }
        }
    }

    private fun initializeAnnoyTimer(automaticallyKillDisallowed: MutableState<Boolean>): Timer {
        return fixedRateTimer("Annoy User", true, 0L, 1000L) {
            ProcessHandler.getDisallowedProcessesThatAreRunning().forEach {
                mainAnnoySound.start()
                //FIXME fix the stopping of audio
                timer("Stop audioclip", true, mainAnnoySound.microsecondLength, 0L) {
                    mainAnnoySound.stop()
                }
                if (automaticallyKillDisallowed.value) it.kill()
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
        maxLines: Int = 1,
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
                maxLines = maxLines,
            )

        }
    }

}
