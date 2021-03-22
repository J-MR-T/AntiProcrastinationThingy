package gui;

import io.GsonHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import processes.BetterTimer;
import processes.Process;
import processes.ProcessHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GUI {

    public GUI(Stage stage) {
        Pane root = new Pane();
        Scene mainScene = new Scene(root, 800, 600);
        MediaView mediaView = new MediaView();
        Slider volumeSlider = new Slider(0, 1, 0.1);
        setMedia(mediaView, volumeSlider);
        //Retrieving the observable nodes object
        ObservableList<Node> nodes = root.getChildren();

        //List of all Processes
        //"Visual" wrapper:
        ListView<Process> processesSelectionList = new ListView<>();
        //Actual item list
        final ObservableList<Process> originalProcessItems =
                FXCollections.observableArrayList(ProcessHandler.computeReducedProcessList());
        processesSelectionList.setItems(originalProcessItems);
        //This listener refreshes the list every 500ms
        BetterTimer refreshProcessListAndFindDisallowed = new BetterTimer(() -> {
            //Refresh Process list
            final List<Process> processList = ProcessHandler.computeReducedProcessList();
            ObservableList<Process> processItems =
                    FXCollections.observableArrayList(processList);
            if (!processesSelectionList.getItems().equals(processItems)) {
                processesSelectionList.setItems(processItems);
            }
            //Find Disallowed
            annoyUser(ProcessHandler.getDisallowedProcessesThatAreRunning(), mediaView, volumeSlider);
        }, 500);
        processesSelectionList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        processesSelectionList.setTranslateX(0);
        processesSelectionList.setTranslateY(100);

        //List of blacklistes Processes
        ListView<Process> blacklisted = new ListView<>();
        final ObservableList<Process> originalBlacklistedItems = getBlacklistedItems();
        blacklisted.setItems(originalBlacklistedItems);
        blacklisted.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        blacklisted.setTranslateX(500);
        blacklisted.setTranslateY(100);

        //Texts
        //Headings
        Font arial25 = new Font("arial", 25);
        Text allowedHeading = new Text(75, 20, "Allowed");
        Text disallowedHeading = new Text(565, 20, "Not Allowed");
        allowedHeading.setFont(arial25);
        disallowedHeading.setFont(arial25);

        Text volume = new Text(275, 275, "Volume");
        volume.setFont(arial25);

        volumeSlider.setTranslateX(275);
        volumeSlider.setTranslateY(300);
        volumeSlider.valueChangingProperty().addListener((obs, wasChanging, isNowChanging) -> {
            if (!isNowChanging) {
                mediaView.getMediaPlayer().setVolume(volumeSlider.getValue());
            }
        });

        Button blacklistSelected = new Button("Blacklist Selected");
        blacklistSelected.setTranslateX(275);
        blacklistSelected.setTranslateY(185);
        blacklistSelected.setOnAction(event -> {
            ProcessHandler.reducedProcessList.stream()
                    .filter(proc -> processesSelectionList.getSelectionModel().getSelectedItems().contains(proc))
                    .forEach(proc -> ProcessHandler.blacklisted.add(proc.command()));
            //TODO maybe do this in the platform.runLater if it throws exceptions?
            blacklisted.setItems(getBlacklistedItems());
        });

        Button removeSelectedFromBlacklist = new Button("Remove Selected From Blacklist");
        removeSelectedFromBlacklist.setTranslateX(275);
        removeSelectedFromBlacklist.setTranslateY(215);
        removeSelectedFromBlacklist.setOnAction(event -> {
            blacklisted.getSelectionModel().getSelectedItems()
                    .forEach(proc -> ProcessHandler.blacklisted.remove(proc.command()));
            blacklisted.setItems(getBlacklistedItems());
        });

        Button removeSelectedFromVisibleList = new Button("Don't Show Selected Anymore");
        //Add all selected processes command representation to the blacklist
        //TODO think about Process::command vs Process::toString here
        removeSelectedFromVisibleList.setOnAction(event -> {
            ProcessHandler.currentBlacklist.addAll(processesSelectionList.getSelectionModel().getSelectedItems()
                    .stream().map(Process::command).collect(Collectors.toSet()));
            ObservableList<Process> processItems =
                    FXCollections.observableArrayList(ProcessHandler.computeReducedProcessList(true));
            if (!processesSelectionList.getItems().equals(processItems)) {
                processesSelectionList.setItems(processItems);
            }
        });
        removeSelectedFromVisibleList.setTranslateX(0);
        removeSelectedFromVisibleList.setTranslateY(500);


        nodes.add(blacklistSelected);
        nodes.add(removeSelectedFromBlacklist);
        nodes.add(removeSelectedFromVisibleList);

        nodes.add(allowedHeading);
        nodes.add(disallowedHeading);

        nodes.add(volume);

        nodes.add(processesSelectionList);
        nodes.add(blacklisted);

        nodes.add(mediaView);
        nodes.add(volumeSlider);

//        mainScene.getStylesheets().add("style1/button-styles.css");

        stage.setTitle("Anti Procrastination Helper");

        stage.setScene(mainScene);
        stage.setOnCloseRequest(event -> {
            refreshProcessListAndFindDisallowed.stop();
            try {
                GsonHelper.stopApp();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void setMedia(MediaView mediaView, Slider volumeSlider) {
        setMedia("mixkit-lone-wolf-howling-1729.wav", mediaView, volumeSlider);
    }

    private void setMedia(String fileName, MediaView mediaView, Slider volumeSlider) {
        Path musicFile = Path.of("rsc", fileName);
        Media sound = new Media(musicFile.toUri().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setVolume(volumeSlider.getValue());
        mediaView.setMediaPlayer(mediaPlayer);
    }

    private void annoyUser(List<Process> disallowedProcessesThatAreRunning, MediaView mediaView, Slider volumeSlider) {
        disallowedProcessesThatAreRunning.forEach(proc -> {
            if (mediaView.getMediaPlayer().statusProperty().get() != null &&
                    (mediaView.getMediaPlayer().statusProperty().get().equals(MediaPlayer.Status.READY)
                            || mediaView.getMediaPlayer().statusProperty().get().equals(MediaPlayer.Status.STALLED)
                            || mediaView.getMediaPlayer().statusProperty().get().equals(MediaPlayer.Status.STOPPED))) {
                Thread t = new Thread(() -> {
                    mediaView.getMediaPlayer().play();
                    try {
                        Thread.sleep((long) mediaView.getMediaPlayer().getTotalDuration().toMillis());
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted! " + e.getMessage());
                    }
                    mediaView.getMediaPlayer().stop();
                });
                t.start();
//                JOptionPane.showMessageDialog(null,
//                        "The process " + proc + " is not allowed!!");
            }
        });
    }

    @NotNull
    private ObservableList<Process> getBlacklistedItems() {
        final Set<Process> blacklisted = new HashSet<>(ProcessHandler.blacklistedProcesses());
        return FXCollections.observableArrayList(blacklisted);
    }

}
