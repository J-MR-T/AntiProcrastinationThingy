@file:Suppress("FunctionName", "unused")

package gui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.desktop.AppFrame
import androidx.compose.desktop.AppManager
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import gui.colors.ButtonColorsError
import gui.colors.ButtonColorsPrimary
import gui.colors.ButtonColorsSecondary
import io.commandline.ArgParser
import io.serialization.PersistenceHelper
import processes.Process
import processes.ProcessHandler
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
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timer
import kotlin.system.exitProcess


object KotlinGUI {
    //cmd options
    internal var options: ArgParser.CmdOptions = ArgParser.CmdOptions()
        set(options) {
            this.volume.value = options.volume.toFloat()
            setVolumeOfClip(mainAnnoySound, options.volume.toFloat())
            audio.value = options.audio
            unclosable.value = options.unclosable
            field = options
        }

    //notifier
    private val notifier = Notifier()

    //timers
    private val daemonTimers: MutableList<Pair<TimerTask, Timer>> = Collections.synchronizedList(mutableListOf())

    //options
    private val automaticallyKillDisallowed: MutableState<Boolean> = mutableStateOf(false)
    private val hideInsteadOfBlacklist: MutableState<Boolean> = mutableStateOf(false)
    private var volume: MutableState<Float> = mutableStateOf(0.1f)
    private val showConfirmDialog: MutableState<Boolean> = mutableStateOf(false)
    private val audio: MutableState<Boolean> = mutableStateOf(options.audio)
    private val unclosable: MutableState<Boolean> = mutableStateOf(options.unclosable)

    //close to tray stuff
    /**
     * DO NOT ADDRESS ON ITS ON, USE [setCloseToTray] instead
     */
    private val closeToTray: MutableState<Boolean> = mutableStateOf(false)
    private var tray: Tray? = null

    private fun setCloseToTray(b: Boolean) {
        closeToTray.value = b
        if (closeToTray.value || unclosable.value) {
            SwingUtilities.invokeLater {
                AppManager.focusedWindow?.window?.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
            }
        } else {
            SwingUtilities.invokeLater {
                AppManager.focusedWindow?.window?.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            }
        }
    }

    //process list
    private val processList: SnapshotStateList<Process>

    //resources
    private val icon: BufferedImage = try {
        resize(ImageIO.read(File(Path.of("res", "shield-alt-solid.png").toUri())), 32, 32)
    } catch (e: IIOException) {
        BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR)
    }
    private val pathToAnnoySound = Path.of("res", "mixkit-lone-wolf-howling-1729.wav")
    private lateinit var mainAnnoySound: Clip

    private const val MAIN_ANNOY_SOUND_LENGTH: Long = 5000L

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
    private fun defaultTheme(content: @Composable () -> Unit) =
        MaterialTheme(
            colors = options.colors.getColors(),
            typography = Typography(fonts),
        ) {
            CompositionLocalProvider(
                ScrollbarStyleAmbient provides defaultScrollbarStyle().copy(
                    hoverDurationMillis = 250,
                    hoverColor = MaterialTheme.colors.primaryVariant,
                    shape = MaterialTheme.shapes.medium,
                )
            ) {
                content()
            }
        }


    private val windowCloseRequest: () -> Unit = {
        if (!closeToTray.value) {
            exit
        }
    }

    private val exit = {
        if (!unclosable.value) {
            close()
            AppManager.windows.forEach { window ->
                window.window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            }
            AppManager.windows.forEach(AppFrame::close)
            AppManager.exit()
            SwingUtilities.invokeLater {
                exitProcess(0)
            }
        }
    }

    private val close: () -> Unit = {
        daemonTimers.forEach { (_, timer) ->
            timer.cancel()
            timer.purge()
        }
        PersistenceHelper.stopApp(volume.value.toDouble())
    }

    init {
        resetClip()

        processList = mutableStateListOf()
        processList.addAll(ProcessHandler.computeFilteredProcessList())
        daemonTimers.add(initializeProcessUpdateTimer(processList))
        daemonTimers.add(initializeAnnoyTimer(automaticallyKillDisallowed))

        //Ensure correct loading and saving before and after the app is started/closed
        AppManager.setEvents(
            onAppStart = { PersistenceHelper.startApp() }, // Invoked before the first window is created
            onAppExit = exit // Invoked after all windows are closed
        )
    }

    private fun resetClip() {
        val audioInputStream = AudioSystem.getAudioInputStream(File(pathToAnnoySound.toString()).absoluteFile)
        mainAnnoySound = AudioSystem.getClip()
        mainAnnoySound.open(audioInputStream)
        setVolumeOfClip(mainAnnoySound, volume.value)
    }

    fun show() {
        getWindow()
        setCloseToTray(closeToTray.value)
    }

    private fun setVolumeOfClip(clip: Clip, volume: Float) =
        try {
            val actualVolume = if (volume in -0.05f..0.05f) 0f else (0.5f + volume * 0.4f)
            val volumeControl: FloatControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val range = volumeControl.maximum - volumeControl.minimum
            volumeControl.value = (range * actualVolume) + volumeControl.minimum
        } catch (ignored: IllegalArgumentException) {

        }


    @OptIn(ExperimentalAnimationApi::class)
    private fun getWindow() {
        return Window(
            title = "APT",
            size = IntSize(options.width, options.height),
            icon = icon,
            onDismissRequest = windowCloseRequest,
        ) {
            tray()
            defaultTheme {
                Box(modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f)) {
                            Column(
                                Modifier.fillMaxWidth(0.5f).padding(PaddingValues(20.dp)),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                defaultHeading("All Processes")
                                simpleLabeledCheckbox(
                                    text = "Hide Processes instead of Blacklisting them",
                                    toBeAffected = hideInsteadOfBlacklist,
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
                                AnimatedVisibility(showConfirmDialog.value) {
                                    confirmDialog()
                                }
                                val scrollState: ScrollState = rememberScrollState(0)
                                scrollableColumn(scrollState) {
                                    drawBlacklist()
                                }
                            }

                        }
                        //At 75% height
                        Row {
                            Spacer(Modifier.fillMaxWidth(0.5f))
                            settings()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun drawBlacklist() {
        ProcessHandler.blacklisted.forEach { cmd ->
            textBox(cmd.stringRepresentation) {
                ProcessHandler.blacklisted.remove(cmd)
            }
        }
    }

    @Composable
    private fun tray() {
        if (tray == null) {
            DisposableEffect(Unit) {
                tray = Tray(icon).apply {
                    menu(
                        MenuItem(
                            name = "Show Again",
                            onClick = {
                                getWindow()
                                setCloseToTray(closeToTray.value)
                            }
                        ),
                        MenuItem(
                            name = "Enable kill (can only be${System.lineSeparator()}disabled in the app)",
                            onClick = { automaticallyKillDisallowed.value = true }
                        ),
                        MenuItem(
                            name = "Exit",
                            onClick = exit
                        ),
                    )
                }
                onDispose {
                    tray?.remove()
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun settings() {
        Column(modifier = Modifier.fillMaxSize()) {
            defaultHeading("Settings")
            Row {
                Column(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight()) {
                    defaultHeading("Audio", MaterialTheme.typography.h5)
                    Column {
                        simpleLabeledCheckbox(text = "Enable Audio Feedback", toBeAffected = audio)
                        Spacer(modifier = Modifier.padding(2.dp))
                        AnimatedVisibility(audio.value) {
                            Column {
                                Text(
                                    text = "Volume: " + (volume.value * 100).toInt(),
                                    style = MaterialTheme.typography.body1,
                                    fontStyle = FontStyle.Normal,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colors.onBackground,
                                )
//                                Spacer(modifier = Modifier.padding(1.dp))
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Slider(
                                        volume.value,
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        onValueChange = {
                                            volume.value = it
                                            setVolumeOfClip(mainAnnoySound, volume = volume.value)
                                        },
                                        colors = SliderDefaults.colors(
                                            MaterialTheme.colors.secondary,
                                            MaterialTheme.colors.error,
                                            MaterialTheme.colors.secondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                }
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!unclosable.value) {
                        simpleLabeledCheckbox(
                            Modifier.padding(10.dp).align(Alignment.TopEnd),
                            "Close to tray",
                            closeToTray
                        ) {
                            setCloseToTray(it)
                        }
                    } else {
                        defaultHeading(
                            "UNCLOSABLE",
                            MaterialTheme.typography.h5,
                            Modifier.padding(10.dp).align(Alignment.TopEnd)
                        )
                    }
                    //Reset button
                    labeledElevatedButton(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        text = "Reset hidden processes list",
                        onClick = ProcessHandler::resetHiddenProcesses,
                        buttonColors = ButtonColorsSecondary()
                    )
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
                labeledElevatedButton(
                    Modifier.fillMaxWidth(0.5f).fillMaxHeight(),
                    "Really stop working",
                    ButtonColorsError(),
                ) {
                    automaticallyKillDisallowed.value = false
                    showConfirmDialog.value = false
                }
                labeledElevatedButton(
                    text = "Actually, my brain tricked me again",
                    buttonColors = ButtonColorsSecondary(),
                ) {
                    automaticallyKillDisallowed.value = true
                    showConfirmDialog.value = false
                }
            }
        }
    }

    @Composable
    private fun labeledElevatedButton(
        modifier: Modifier = Modifier.fillMaxSize(),
        text: String,
        buttonColors: ButtonColors = ButtonColorsPrimary(),
        onClick: () -> Unit,
    ) {
        TextButton(
            modifier = modifier,
            onClick = onClick,
            elevation = ButtonDefaults.elevation(),
            colors = buttonColors,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.body2
            )
        }
    }

    @Composable
    private fun drawProcessList() {
        val stateLeftList = rememberScrollState(1)

        scrollableColumn(stateLeftList) {
            processList.forEach { proc ->
                textBox(
                    proc,
                    backgroundColor = if (hideInsteadOfBlacklist.value) MaterialTheme.colors.secondary
                    else MaterialTheme.colors.primary,
                    textColor = if (hideInsteadOfBlacklist.value) MaterialTheme.colors.onSecondary
                    else MaterialTheme.colors.onPrimary,
                ) {
                    if (hideInsteadOfBlacklist.value) {
                        ProcessHandler.hiddenProcesses.add(proc)
                    } else {
                        ProcessHandler.blacklisted.add(proc)
                    }
//                    daemonTimers.refresh()
                }
            }
        }
    }

    @Composable
    private fun scrollableColumn(
        scrollState: ScrollState,
        modifier: Modifier = Modifier.fillMaxSize(),
        contentRatio: Float = 0.975f,
        spaceRatio: Float = 0.333f,
        verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(3.dp),
        horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        Row(modifier) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(contentRatio)
                    .verticalScroll(scrollState),
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                content = content
            )
            Spacer(Modifier.fillMaxWidth(spaceRatio))
            VerticalScrollbar(
                modifier = Modifier.fillMaxSize(),
                adapter = rememberScrollbarAdapter(scrollState),
            )
        }
    }


    @Composable
    private fun simpleLabeledCheckbox(
        modifier: Modifier = Modifier,
        text: String,
        toBeAffected: MutableState<Boolean>,
        onCheckedChange: ((Boolean) -> Unit)? = null,
    ) {
        Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
            Checkbox(checked = toBeAffected.value, onCheckedChange ?: { toBeAffected.value = it })
            Text(
                text,
                color = MaterialTheme.colors.onBackground,
                fontWeight = FontWeight.Light,
            )
        }
    }

    @Composable
    private inline fun customLabeledCheckbox(
        label: String,
        checkbox: @Composable () -> Unit,
    ) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            checkbox()
            Text(
                label,
                color = MaterialTheme.colors.onBackground,
                fontWeight = FontWeight.Light,
            )
        }
    }


    @Composable
    private fun defaultHeading(
        text: String,
        style: TextStyle = MaterialTheme.typography.h4,
        modifier: Modifier = Modifier,
    ) =
        Text(
            modifier = modifier,
            text = text,
            color = MaterialTheme.colors.onBackground,
            style = style,
            fontWeight = FontWeight.Thin,
            textAlign = TextAlign.Center,
        )

    private fun resize(img: BufferedImage, newW: Int, newH: Int): BufferedImage {
        val tmp: Image = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH)
        val dimg = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
        val g2d = dimg.createGraphics()
        g2d.drawImage(tmp, 0, 0, null)
        object : TimerTask() {
            override fun run() {
            }
        }
        g2d.dispose()
        return dimg
    }

    private fun initializeProcessUpdateTimer(list: SnapshotStateList<Process>): Pair<TimerTask, Timer> {
        val task = object : TimerTask() {
            override fun run() {
                val newList = ProcessHandler.computeFilteredProcessList()
                if (list != newList) {
                    synchronized(list) {
                        list.removeIf { !newList.contains(it) }
                        newList.forEach { if (!list.contains(it)) list.add(it) }
                    }
                }
            }
        }
        return task to fixedRateTimer("Update", true, 1000L, 1000L) { task.run() }
    }

    private fun initializeAnnoyTimer(automaticallyKillDisallowed: MutableState<Boolean>): Pair<TimerTask, Timer> {
        val task = object : TimerTask() {
            override fun run() {
                annoyUser(automaticallyKillDisallowed)
            }
        }
        return task to fixedRateTimer("Annoy User", true, 0L, 1000L) { task.run() }
    }

    private fun annoyUser(automaticallyKillDisallowed: MutableState<Boolean>) {
        ProcessHandler.disallowedProcessesThatAreRunning.forEach {
            if (!mainAnnoySound.isRunning && audio.value) {
                resetClip()
                mainAnnoySound.start()
                val task = object : TimerTask() {
                    override fun run() {
                        synchronized(mainAnnoySound) {
                            mainAnnoySound.stop()
                            this.cancel()
                        }
                    }
                }
                task to timer("Stop audioclip", true, MAIN_ANNOY_SOUND_LENGTH, Long.MAX_VALUE) { task.run() }
            }
            notifier.warn("!!!Disallowed Process!!!", "$it is disallowed!")
            if (automaticallyKillDisallowed.value) it.kill()
        }
    }

    //FIXME Animation, probably doesnt work because the recomposition is blocked by the process filtering calls which are too slow
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun textBox(
        text: Any = "Item",
        fillFraction: Float = 1f,
        fontWeight: FontWeight = FontWeight.Light,
        fontStyle: FontStyle = FontStyle.Normal,
        alignment: Alignment = Alignment.Center,
        maxLines: Int = 1,
        backgroundColor: Color = MaterialTheme.colors.primary,
        textColor: Color = MaterialTheme.colors.onPrimary,
        onClick: (() -> Unit)? = null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fillFraction)
                .animateContentSize()
                .clip(RoundedCornerShape(3.dp))
                .background(backgroundColor)
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
                color = textColor,
                maxLines = maxLines,
            )
        }
    }

    private fun MutableList<Pair<TimerTask, Timer>>.refresh() {
        synchronized(this) {
            forEach { (task, _) ->
                task.run()
            }
        }
    }
}