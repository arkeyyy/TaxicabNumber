package org.example.taxicab_number;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class TaxicabController {
    @FXML private TextField upperLimitField;
    @FXML private TextField threadCountField;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane workerPane;
    @FXML private TextArea resultsArea;
    @FXML private Label taxicabCountLabel;
    @FXML private Label otherCountLabel;
    @FXML private Label totalCountLabel;

    private Run activeRun;

    private static final class Run {
        final List<Task<List<Integer>>> workers = new ArrayList<>();
        final int upperLimit;
        final int threadCount;
        int finished;

        Run(int upperLimit, int threadCount) {
            this.upperLimit = upperLimit;
            this.threadCount = threadCount;
        }

        String threadSummary() {
            return "Finished threads: " + finished + " of " + threadCount + ".";
        }
    }

    @FXML
    private void startSearch() {
        if (activeRun != null) {
            return;
        }
        int upperLimit = positiveInteger(upperLimitField.getText());
        int threadCount = positiveInteger(threadCountField.getText());
        if (upperLimit == 0) {
            showError("Upper limit must be a positive 32-bit integer.");
            return;
        }
        int maxThreads = Math.min(upperLimit, 64);
        if (threadCount == 0 || threadCount > maxThreads) {
            showError("Thread count must be between 1 and " + maxThreads + ".");
            return;
        }

        showError("");
        clearResults();
        workerPane.getChildren().clear();
        Run run = new Run(upperLimit, threadCount);
        activeRun = run;
        setRunning(true);
        statusLabel.setText("Running. " + run.threadSummary());
        for (TaxicabSearch.Range range : TaxicabSearch.ranges(upperLimit, threadCount)) {
            startWorker(run, range);
        }
    }

    private static int positiveInteger(String text) {
        try {
            return Math.max(0, Integer.parseInt(text.strip()));
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    private void startWorker(Run run, TaxicabSearch.Range range) {
        int number = run.workers.size() + 1;
        Task<List<Integer>> task = new Task<>() {
            @Override
            protected List<Integer> call() {
                return TaxicabSearch.search(range, this::isCancelled,
                        checked -> updateProgress(checked, range.size()));
            }
        };
        run.workers.add(task);
        Label state = new Label("Waiting");
        ProgressIndicator indicator = new ProgressIndicator(0);
        indicator.setPrefSize(42, 42);
        indicator.setMaxSize(42, 42);
        indicator.progressProperty().bind(Bindings.max(0, task.progressProperty()));
        Label bounds = new Label(range.first() + " to " + range.last());
        bounds.setMaxWidth(Double.MAX_VALUE);
        Label size = new Label(range.size() + (range.size() == 1 ? " number" : " numbers"));
        VBox card = new VBox(6, new Label("Thread " + number), indicator, bounds, size, state);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(150);
        workerPane.getChildren().add(card);

        task.setOnRunning(event -> state.setText("Running"));
        task.setOnSucceeded(event -> {
            if (activeRun == run) {
                state.setText("Done");
                workerFinished(run);
            }
        });
        task.setOnFailed(event -> {
            if (activeRun == run) {
                discardRun("Search failed.");
            }
        });
        Thread thread = new Thread(task, "taxicab-worker-" + number);
        thread.setDaemon(true);
        thread.start();
    }

    private void workerFinished(Run run) {
        run.finished++;
        if (run.finished < run.threadCount) {
            statusLabel.setText("Running. " + run.threadSummary());
            return;
        }

        StringJoiner output = new StringJoiner(", ");
        int matches = 0;
        for (Task<List<Integer>> task : run.workers) {
            List<Integer> values = task.getValue();
            matches += values.size();
            for (int value : values) {
                output.add(Integer.toString(value));
            }
        }
        resultsArea.setText(output.toString());
        setCounts(matches, run.upperLimit);
        statusLabel.setText("Complete. Checked all " + run.upperLimit + " numbers from 1 to "
                + run.upperLimit + ". " + run.threadSummary());
        activeRun = null;
        setRunning(false);
    }

    @FXML
    private void stopSearch() {
        if (activeRun != null) {
            discardRun("Stopped.");
        }
    }

    void close() {
        stopSearch();
    }

    private void discardRun(String status) {
        Run run = activeRun;
        activeRun = null;
        for (Task<?> task : run.workers) {
            task.cancel(true);
        }
        workerPane.getChildren().clear();
        clearResults();
        statusLabel.setText(status);
        setRunning(false);
    }

    private void clearResults() {
        resultsArea.clear();
        setCounts(0, 0);
    }

    private void setCounts(int matches, int checked) {
        taxicabCountLabel.setText("Taxicab numbers: " + matches);
        otherCountLabel.setText("Other numbers: " + (checked - matches));
        totalCountLabel.setText("Total checked: " + checked);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(!message.isEmpty());
        errorLabel.setVisible(!message.isEmpty());
    }

    private void setRunning(boolean running) {
        upperLimitField.setDisable(running);
        threadCountField.setDisable(running);
        startButton.setDisable(running);
        stopButton.setDisable(!running);
    }
}
