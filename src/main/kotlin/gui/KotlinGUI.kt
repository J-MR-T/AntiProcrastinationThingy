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
import processes.*
import java.awt.Image
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


class KotlinGUI(colors: MyColors = MyColors.AWESOME_MAGNET) {
    private val colors: Colors = colors.getColors()

    //timers
    private val daemonTimers: MutableList<Timer> = mutableListOf()

    //options
    private val automaticallyKillDisallowed: MutableState<Boolean> = mutableStateOf(false)
    private val hideInsteadOfBlacklist: MutableState<Boolean> = mutableStateOf(false)
    private val volume: MutableState<Float> = mutableStateOf(0.1f)
    private val showConfirmDialog: MutableState<Boolean> = mutableStateOf(false);

    //process list
    private val processList: SnapshotStateList<Process>;

    //resources
    private val icon: BufferedImage = try {
        resize(ImageIO.read(File(Path.of("res", "shield-alt-solid.png").toUri())), 32, 32)
    } catch (e: IIOException) {
        BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR)
    }
    private val pathToAnnoySound = Path.of("res", "mixkit-lone-wolf-howling-1729.wav")
    private val mainAnnoySound: Clip

    companion object {
        private const val MAIN_ANNOY_SOUND_LENGTH: Long = 5000L
    }

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
    private fun defaultTheme(content: @Composable () -> Unit) = MaterialTheme(
        colors = colors,
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
            AudioSystem.getAudioInputStream(
                File(
                    pathToAnnoySound
                        .toString()
                ).absoluteFile
            )
        mainAnnoySound = AudioSystem.getClip()
        mainAnnoySound.open(audioInputStream)
        setVolumeOfClip(mainAnnoySound, volume.value)
        mainAnnoySound.setLoopPoints(0, MAIN_ANNOY_SOUND_LENGTH.toInt())
//        mainAnnoySound.loop(Integer.MAX_VALUE)
        mainAnnoySound.stop()

        processList = mutableStateListOf()
        processList.addAll(ProcessHandler.computeFilteredProcessList(true))
        daemonTimers.add(initializeProcessUpdateTimer(processList))
        daemonTimers.add(initializeAnnoyTimer(automaticallyKillDisallowed))
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
            defaultTheme {
                Box(modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
                            Column(
                                Modifier.fillMaxWidth(0.5f).padding(PaddingValues(20.dp)),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                defaultHeading("All Processes")
                                simpleLabeledCheckbox(
                                    "Hide Processes instead of Blacklisting them",
                                    hideInsteadOfBlacklist,
                                )
                                drawProcessList()
                            }
                            Column(
                                Modifier.fillMaxWidth()
                                    .padding(PaddingValues(20.dp)),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                defaultHeading("Blacklisted Processes")
                                customLabeledCheckbox("Automatically kill disallowed processes if running") {
                                    Checkbox(
                                        checked = automaticallyKillDisallowed.value,
                                        onCheckedChange = {
                                            if (automaticallyKillDisallowed.value) {
                                                showConfirmDialog.value = true
                                            } else {
                                                automaticallyKillDisallowed.value = it
                                            }
                                        },
                                    )
                                }
                                if (showConfirmDialog.value) {
                                    confirmDialog()
                                }

                                ProcessHandler.blacklistedProcesses().forEach { proc ->
                                    textBox(proc) {
                                        ProcessHandler.blacklisted.remove(proc.command())
                                    }
                                }
                            }

                        }
                        settings()
                    }
                }
            }
        }
    }

    @Composable
    private fun settings() {
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
                                text = "Volume: " + (volume.value * 100).toInt(),
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
                        labeledElevatedButton(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            onClick = ProcessHandler::resetHiddenProcesses,
                            text = "Reset hidden processes list"
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun confirmDialog() {
        Dialog(
            onDismissRequest = { showConfirmDialog.value = false },
            properties = DialogProperties(
                title = "Do you really want to deactivate this?",
                size = IntSize(400, 120),
                icon = icon,
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(PaddingValues(15.dp)),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                labeledElevatedButton(Modifier.fillMaxWidth(0.5f).fillMaxHeight(), "Really stop working") {
                    automaticallyKillDisallowed.value = false
                    showConfirmDialog.value = false
                }
                labeledElevatedButton(text = "Actually, my brain tricked me again") {
                    automaticallyKillDisallowed.value = true
                    showConfirmDialog.value = false
                }
            }
        }
    }

    @Composable
    private fun labeledElevatedButton(modifier: Modifier = Modifier.fillMaxSize(), text: String, onClick: () -> Unit) {
        TextButton(
            modifier = modifier,
            onClick = onClick,
            elevation = ButtonDefaults.elevation(),
            colors = ButtonColorsPrimary(),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.body2
            )
        }
    }

    @Composable
    private fun drawProcessList() {
        val stateLeftList = rememberScrollState(0)
        Row {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .verticalScroll(stateLeftList),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,

                ) {
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
            Spacer(Modifier.fillMaxWidth(0.1f))
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight(),
                adapter = rememberScrollbarAdapter(stateLeftList)
            )
        }
    }


    @Composable
    private fun simpleLabeledCheckbox(
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
    private fun customLabeledCheckbox(
        label: String,
        checkbox: @Composable () -> Unit,
    ) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            checkbox()
            Text(
                label,
                color = MaterialTheme.colors.secondary,
                fontWeight = FontWeight.Light,
            )
        }
    }

    fun resize(img: BufferedImage, newW: Int, newH: Int): BufferedImage {
        val tmp: Image = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH)
        val dimg = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
        val g2d = dimg.createGraphics()
        g2d.drawImage(tmp, 0, 0, null)
        g2d.dispose()
        return dimg
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
                if (!mainAnnoySound.isRunning) {
                    mainAnnoySound.start()
                    //FIXME fix the stopping of audio (and inspect remove warning)
                    daemonTimers.add(
                        timer("Stop audioclip", true, MAIN_ANNOY_SOUND_LENGTH, Long.MAX_VALUE) {
                            mainAnnoySound.stop()

                            println("test")
                            daemonTimers.remove(this)
                            this.cancel()
                        })
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
