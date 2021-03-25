package gui;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.jfoenix.controls.*;
import io.GsonHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processes.Process;
import processes.ProcessHandler;
import timers.BetterTimer;
import timers.BetterTimerExecuteOnce;
import timers.BetterTimerFixedRate;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class GUI {
    public static ActionListener TRAY_EXIT_LISTENER = null;

    public GUI(Stage stage) {
        Pane root = new Pane();
        Scene mainScene = new Scene(root, 800, 600);
        mainScene.getStylesheets().add("file:darkmode-style.css");
        List<BetterTimer> timers = new ArrayList<>();
        MediaView mediaView = new MediaView();
        Slider volumeSlider = new JFXSlider(0, 100, 10);
        try {
            GsonHelper.startApp(volumeSlider);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setMedia(mediaView, volumeSlider);
        //Retrieving the observable nodes object
        ObservableList<Node> nodes = root.getChildren();
        final Image icon = new Image(Path.of("rsc", "shield-alt-solid.png").toUri().toString());

        final FXTrayIcon trayIcon;
        URL url = null;
        try {
            url = Path.of("rsc", "shield-alt-solid.png").toUri().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();

        }
        if (FXTrayIcon.isSupported() && url != null) {
            trayIcon = new FXTrayIcon(stage, url);
        } else {
            trayIcon = null;
        }
        TRAY_EXIT_LISTENER = event -> {
            exitAppUnconditional(volumeSlider, mediaView, timers, trayIcon);
        };

        JFXColorPicker primaryColorPicker = new JFXColorPicker(Color.valueOf("#393e46"));
        JFXColorPicker secondaryColorPicker = new JFXColorPicker(Color.valueOf("#00adb5"));
        primaryColorPicker.setTranslateX(700);
        primaryColorPicker.setTranslateY(525);
        secondaryColorPicker.setTranslateX(700);
        secondaryColorPicker.setTranslateY(575);

        //Toggle Buttons
        ToggleButton autoKillProcesses = new JFXToggleButton();
        autoKillProcesses.setText("""
                Automatically kill
                disallowed Processes?
                WARNING: USE AT
                YOUR OWN RISK""");
        autoKillProcesses.setTranslateX(275);
        autoKillProcesses.setTranslateY(350);
        autoKillProcesses.styleProperty().bindBidirectional(primaryColorPicker.valueProperty(),
                new ColorStringBijection("jfx-checked-color"));

        ToggleButton toggleCloseToTray = new JFXToggleButton();
        toggleCloseToTray.setTranslateX(275);
        toggleCloseToTray.setTranslateY(500);
        toggleCloseToTray.setText("Enable close to tray");
        if (trayIcon != null) {
            toggleCloseToTray.setOnAction(event -> {
                if (trayIcon.isShowing()) {
                    toggleCloseToTray.setDisable(true);
                    Platform.runLater(() -> disableTrayIcon(trayIcon));
                    new BetterTimerExecuteOnce(() -> toggleCloseToTray.setDisable(false), 200L);
                } else {
                    toggleCloseToTray.setDisable(true);
                    Platform.runLater(() -> enableTrayIcon(trayIcon));
                    new BetterTimerExecuteOnce(() -> toggleCloseToTray.setDisable(false), 200L);
                }
            });
        }

        //List of all Processes
        //"Visual" wrapper:
        ListView<Process> processesSelectionList = new JFXListView<>();
        //Actual item list
        final ObservableList<Process> originalProcessItems = getProcessItems(false);
        processesSelectionList.setItems(originalProcessItems);
        //This listener refreshes the list every 500ms
        BetterTimerFixedRate refreshProcessListAndFindDisallowedAndKillDisallowedIfAskedTo =
                new BetterTimerFixedRate(() -> {
                    //Refresh Process list
                    updateProcessItems(processesSelectionList, false);
                    //Find Disallowed
                    final List<Process> disallowedProcessesThatAreRunning =
                            ProcessHandler.getDisallowedProcessesThatAreRunning();
                    annoyUser(disallowedProcessesThatAreRunning, mediaView, trayIcon);
                    //Kill if asked to do so
                    if (autoKillProcesses.isSelected()) {
                        disallowedProcessesThatAreRunning.forEach(Process::kill);
                    }
                }, 500);
        timers.add(refreshProcessListAndFindDisallowedAndKillDisallowedIfAskedTo);
        processesSelectionList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        processesSelectionList.setTranslateX(0);
        processesSelectionList.setTranslateY(100);
        processesSelectionList.styleProperty().bindBidirectional(primaryColorPicker.valueProperty(),
                new ColorStringBijection());

        //List of blacklisted Processes
        ListView<Process> blacklisted = new JFXListView<>();
        final ObservableList<Process> originalBlacklistedItems = getBlacklistedItems();
        blacklisted.setItems(originalBlacklistedItems);
        blacklisted.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        blacklisted.setTranslateX(500);
        blacklisted.setTranslateY(100);
        blacklisted.styleProperty().bindBidirectional(primaryColorPicker.valueProperty(),
                new ColorStringBijection());


        //Texts
        //Headings
        Font arial25 = new Font("arial", 25);
        Text allowedHeading = new Text(75, 20, "Allowed");
        Text disallowedHeading = new Text(565, 20, "Not Allowed");
        allowedHeading.setFont(arial25);
        disallowedHeading.setFont(arial25);
        allowedHeading.setFill(Paint.valueOf("00adb5"));
        disallowedHeading.setFill(Paint.valueOf("00adb5"));

        Text volumeHeading = new Text(275, 275, "Volume");
        volumeHeading.setFont(arial25);
        volumeHeading.setFill(Paint.valueOf("00adb5"));

        Text primaryColorHeading = new Text(700, 515, "Primary Color");
        Text secondaryColorHeading = new Text(700, 565, "Secondary Color");

        volumeSlider.setTranslateX(275);
        volumeSlider.setTranslateY(300);
        volumeSlider.valueChangingProperty().addListener(
                (obs, wasChanging, isNowChanging) -> mediaView.getMediaPlayer()
                        .setVolume(volumeSlider.getValue() / 100));
        volumeSlider.setOnMouseClicked(event -> mediaView.getMediaPlayer().setVolume(volumeSlider.getValue() / 100));
        volumeSlider.styleProperty().bindBidirectional(primaryColorPicker.valueProperty(),
                new ColorStringBijection("jfx-default-thumb"));

        Button blacklistSelected = new JFXButton("Blacklist Selected");
        blacklistSelected.setTranslateX(275);
        blacklistSelected.setTranslateY(185);
        blacklistSelected.setOnAction(event -> {
            ProcessHandler.reducedProcessList.stream()
                    .filter(proc -> processesSelectionList.getSelectionModel().getSelectedItems().contains(proc))
                    .forEach(proc -> ProcessHandler.blacklisted.add(proc.command()));
            blacklisted.setItems(getBlacklistedItems());
        });
        blacklistSelected.styleProperty().bindBidirectional(primaryColorPicker.valueProperty(),
                new ColorStringBijection());

        Button removeSelectedFromBlacklist = new JFXButton("Remove Selected From Blacklist");
        removeSelectedFromBlacklist.setTranslateX(275);
        removeSelectedFromBlacklist.setTranslateY(215);
        removeSelectedFromBlacklist.setOnAction(event -> {
            blacklisted.getSelectionModel().getSelectedItems()
                    .forEach(proc -> ProcessHandler.blacklisted.remove(proc.command()));
            blacklisted.setItems(getBlacklistedItems());
        });
        removeSelectedFromBlacklist.styleProperty().bindBidirectional(primaryColorPicker.valueProperty(),
                new ColorStringBijection());

        Button removeSelectedFromVisibleList = new JFXButton("Don't Show Selected Anymore");
        //Add all selected processes command representation to the blacklist
        //We explicitly add the **command** here, because we can be sure only that process is actually excluded
        removeSelectedFromVisibleList.setOnAction(event -> {
            ProcessHandler.hiddenProcesses.addAll(processesSelectionList.getSelectionModel().getSelectedItems()
                    .stream().map(Process::command).collect(Collectors.toSet()));
            updateProcessItems(processesSelectionList, true);
        });
        removeSelectedFromVisibleList.setTranslateX(0);
        removeSelectedFromVisibleList.setTranslateY(505);
        removeSelectedFromVisibleList.styleProperty().bindBidirectional(primaryColorPicker.valueProperty(),
                new ColorStringBijection());

        Button resetHiddenProcesses = new JFXButton("Reset the List of Hidden Processes");
        resetHiddenProcesses.setOnAction(event -> {
            ProcessHandler.resetHiddenProcesses();
            updateProcessItems(processesSelectionList, true);
        });
        resetHiddenProcesses.setTranslateX(0);
        resetHiddenProcesses.setTranslateY(535);
        resetHiddenProcesses.styleProperty().bindBidirectional(primaryColorPicker.valueProperty(),
                new ColorStringBijection());


        nodes.add(blacklistSelected);
        nodes.add(removeSelectedFromBlacklist);
        nodes.add(removeSelectedFromVisibleList);
        nodes.add(resetHiddenProcesses);

        nodes.add(toggleCloseToTray);

        nodes.add(allowedHeading);
        nodes.add(disallowedHeading);
        nodes.add(volumeHeading);
        nodes.add(primaryColorHeading);
        nodes.add(secondaryColorHeading);

        nodes.add(autoKillProcesses);

        nodes.add(primaryColorPicker);
        nodes.add(secondaryColorPicker);

        nodes.add(processesSelectionList);
        nodes.add(blacklisted);

        nodes.add(mediaView);
        nodes.add(volumeSlider);

        stage.setTitle("Anti Procrastination Helper");
        stage.getIcons().add(icon);

        stage.setScene(mainScene);


        //This is called even if the tray Icon exists so we have to check for that separately
        stage.setOnCloseRequest(event -> {
            exitAppConditional(volumeSlider, mediaView,
                    List.of(refreshProcessListAndFindDisallowedAndKillDisallowedIfAskedTo),
                    trayIcon);
        });
    }

    /**
     * Replaces the ActionListener of the specified trayIcon with TRAY_EXIT_LISTENER
     *
     * @param trayIcon the tray icon in question
     */
    private void replaceTrayIconActionListener(FXTrayIcon trayIcon) {
        try {
            final java.awt.MenuItem originalExitItem = trayIcon.getMenuItem(1);
            Arrays.stream(originalExitItem.getActionListeners()).forEach(originalExitItem::removeActionListener);
            originalExitItem.addActionListener(TRAY_EXIT_LISTENER);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Couldn't replace tray icon action listener");
        }
    }

    /**
     * Exit the app if the trayIcon allows it or if its forced.
     *
     * @param volumeSlider The volume slider which contains the value to be saved
     * @param mediaView    the media view which contains the primary media player
     * @param timers       all running timers that should be stopped
     * @param trayIcon     The tray icon which should be disabled to close the application
     */
    private void exitAppUnconditional(Slider volumeSlider, MediaView mediaView, List<BetterTimer> timers,
            FXTrayIcon trayIcon) {
        exitAppConditional(volumeSlider, mediaView, timers, trayIcon, true);
    }

    /**
     * Exit the app if the trayIcon allows it or if its forced.
     *
     * @param volumeSlider The volume slider which contains the value to be saved
     * @param mediaView    the media view which contains the primary media player
     * @param timers       all running timers that should be stopped
     * @param trayIcon     the tray icon which decides if the app should be exited and should be disabled to close the application
     */
    private void exitAppConditional(Slider volumeSlider, MediaView mediaView, List<BetterTimer> timers,
            FXTrayIcon trayIcon) {
        exitAppConditional(volumeSlider, mediaView, timers, trayIcon, false);
    }

    /**
     * Exit the app if the trayIcon allows it or if its forced.
     *
     * @param force        Ignore the condition of the tray Icon and exit in any case
     * @param volumeSlider The volume slider which contains the value to be saved
     * @param mediaView    the media view which contains the primary media player
     * @param timers       all running timers that should be stopped
     * @param trayIcon     the tray icon which decides if the app should be exited and should be disabled to close the application
     */
    private void exitAppConditional(Slider volumeSlider, MediaView mediaView, List<BetterTimer> timers,
            FXTrayIcon trayIcon, boolean force) {
        if (force || trayIcon == null || !trayIcon.isShowing()) {
            timers.forEach(BetterTimer::stop);
            try{
                mediaView.getMediaPlayer().stop();
            }catch (NullPointerException wtfWhyIsThisExceptionEvenThrown){

            }
            mediaView.getMediaPlayer().dispose();
            try {
                GsonHelper.stopApp(volumeSlider.getValue() / 100);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (trayIcon != null) {
                trayIcon.clear();
                trayIcon.hide();
            }
            Platform.exit();
        }
    }

    private void enableTrayIcon(FXTrayIcon trayIcon) {
        trayIcon.show();
        SwingUtilities.invokeLater(() -> replaceTrayIconActionListener(trayIcon));
    }

    private void disableTrayIcon(FXTrayIcon trayIcon) {
        SwingUtilities.invokeLater(() -> {
            trayIcon.clear();
            trayIcon.hide();
        });
    }

    /**
     * Update the parameter processesSelectionList's content using the getProcessItems method
     *
     * @param processesSelectionList
     * @param force                  Force the update. Use this to update the list after the hiddenList has been updated
     * @see gui.GUI
     */
    private void updateProcessItems(ListView<Process> processesSelectionList, boolean force) {
        ObservableList<Process> processItems = getProcessItems(ProcessHandler.blacklisted, force);
        if (!processesSelectionList.getItems().equals(processItems)) {
            processesSelectionList.setItems(processItems);
        }
    }

    @NotNull
    private ObservableList<Process> getProcessItems(Set<String> exclude, boolean force) {
        final List<Process> list = ProcessHandler.computeReducedProcessList(force);
        list.removeIf(proc -> exclude.contains(proc.toString()) || exclude.contains(proc.command()));
        return FXCollections.observableArrayList(list);
    }

    @NotNull
    private ObservableList<Process> getProcessItems(boolean force) {
        return FXCollections.observableArrayList(ProcessHandler.computeReducedProcessList(force));
    }

    /**
     * Default sound to be played to <b>annoyUser</b>
     *
     * @param mediaView    the mediaView which has the appropriate mediaPlayer to be affected
     * @param volumeSlider guess what
     */
    private void setMedia(MediaView mediaView, Slider volumeSlider) {
        setMedia("mixkit-lone-wolf-howling-1729.wav", mediaView, volumeSlider);
    }

    /**
     * Set sound to be played to <b>annoyUser</b>
     *
     * @param fileName     guess what
     * @param mediaView    the mediaView which has the appropriate mediaPlayer to be affected
     * @param volumeSlider guess what
     */
    private void setMedia(String fileName, MediaView mediaView, Slider volumeSlider) {
        Path musicFile = Path.of("rsc", fileName);
        Media sound = new Media(musicFile.toUri().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setVolume(volumeSlider.getValue() / 100);
        mediaView.setMediaPlayer(mediaPlayer);
    }

    /**
     * hehe
     *
     * @param disallowedProcessesThatAreRunning list of processes to annoy the User about
     * @param mediaView                         the media view to play the annoying sound
     */
    private void annoyUser(List<Process> disallowedProcessesThatAreRunning, MediaView mediaView) {
        annoyUser(disallowedProcessesThatAreRunning, mediaView, null);
    }

    /**
     * hehe
     *
     * @param disallowedProcessesThatAreRunning list of processes to annoy the User about
     * @param mediaView                         the media view to play the annoying sound
     * @param trayIcon                          the tray Icon for messaging
     */
    private void annoyUser(List<Process> disallowedProcessesThatAreRunning, MediaView mediaView,
            @Nullable FXTrayIcon trayIcon) {
        disallowedProcessesThatAreRunning.forEach(proc -> {
            if (mediaView.getMediaPlayer().statusProperty().isNotNull().get() &&
                    (mediaView.getMediaPlayer().statusProperty().get().equals(MediaPlayer.Status.READY)
                            || mediaView.getMediaPlayer().statusProperty().get().equals(MediaPlayer.Status.STALLED)
                            || mediaView.getMediaPlayer().statusProperty().get().equals(MediaPlayer.Status.STOPPED))) {
                if (trayIcon != null) {
                    boolean wasShowing = trayIcon.isShowing();
                    if (!wasShowing) {
                        enableTrayIcon(trayIcon);
                    }
                    trayIcon.showErrorMessage(proc.toString(), "Der Prozess " + proc + " ist nicht erlaubt!");
                    if (!wasShowing) {
                        trayIcon.hide();
                        trayIcon.clear();
                    }
                }
                new BetterTimerExecuteOnce(() -> mediaView.getMediaPlayer().play());
                new BetterTimerExecuteOnce(() -> mediaView.getMediaPlayer().stop(),
                        (long) mediaView.getMediaPlayer().getTotalDuration().toMillis());
            }
        });
    }

    @NotNull
    private ObservableList<Process> getBlacklistedItems() {
        final Set<Process> blacklisted = new HashSet<>(ProcessHandler.blacklistedProcesses());
        return FXCollections.observableArrayList(blacklisted);
    }

}
