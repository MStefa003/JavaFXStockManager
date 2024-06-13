package com.example.util;

import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Utility class providing helper methods for common tasks.
 */
public class Utility {

    /**
     * Displays an alert dialog with the specified type, title, and message.
     *
     * @param alertType the type of the alert
     * @param title     the title of the alert
     * @param message   the message content of the alert
     */
    public static void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Opens the documentation file specified by the docPath.
     *
     * @param stage   the primary stage
     * @param docPath the path to the documentation file
     */
    public static void openDocumentation(Stage stage, String docPath) {
        InputStream input = Utility.class.getResourceAsStream(docPath);
        if (input == null) {
            showAlert(Alert.AlertType.ERROR, "File error", "Documentation file not found.");
            return;
        }

        try {
            File tempFile = File.createTempFile("word", ".docx");
            tempFile.deleteOnExit();
            Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Desktop.getDesktop().open(tempFile);
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "File error", "Failed to open documentation.");
            ex.printStackTrace();
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
