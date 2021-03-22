package gui;

import io.GsonHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import processes.BetterTimer;
import processes.Process;
import processes.ProcessHandler;

import javax.swing.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GUI {

    public GUI(Stage stage) {
        Pane root = new Pane();
        Scene scene = new Scene(root, 800, 600);
        //Retrieving the observable nodes object
        ObservableList<Node> nodes = root.getChildren();

        //Setting the text object as a node
//        Button hi = new Button("Hi");
//        nodes.add(hi);
//        Line line = new Line();
//        line.setStartX(50);
//        line.setStartY(100);
//        line.setEndY(100);
//        line.setEndX(900);
//        nodes.add(line);

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
            annoyUser(ProcessHandler.getDisallowedProcessesThatAreRunning());
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

        //Headings
        Text allowedHeading = new Text(75, 20, "Allowed");
        Font font = new Font("arial", 25);
        Text disallowedHeading = new Text(565, 20, "Not Allowed");
        allowedHeading.setFont(font);
        disallowedHeading.setFont(font);


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

        nodes.add(processesSelectionList);
        nodes.add(blacklisted);

//        scene.getStylesheets().add("style1/button-styles.css");

        stage.setTitle("Anti Procrastination Helper");

        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            refreshProcessListAndFindDisallowed.stop();
            try {
                GsonHelper.stopApp();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void annoyUser(List<Process> disallowedProcessesThatAreRunning) {
        disallowedProcessesThatAreRunning.forEach(proc -> {
            JOptionPane.showMessageDialog(null,
                    "The process " + proc + " is not allowed!!");
//            Path musicFile = Path.of("rsc","annoying.mp3");
//
//            Media sound = new Media(musicFile.toFile());
//            MediaPlayer mediaPlayer = new MediaPlayer(sound);
//            mediaPlayer.play();
        });
    }

    @NotNull
    private ObservableList<Process> getBlacklistedItems() {
        final Set<Process> blacklisted = new HashSet<>(ProcessHandler.blacklistedProcesses());
        return FXCollections.observableArrayList(blacklisted);
    }

}
