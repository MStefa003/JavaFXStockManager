package com.example.controller;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

import com.example.util.Constants;
import com.example.util.Utility;
import com.example.util.AppLogger;
import com.example.model.DatabaseConnector;

/**
 * The App class handles the main application logic for user login and registration.
 */
public class App extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("User Login/Register");

        Image icon = new Image(getClass().getResourceAsStream(Constants.ICON_PATH));
        primaryStage.getIcons().add(icon);

        showLoginScreen();
    }

    /**
     * Displays the login screen.
     */
    public void showLoginScreen() {
        BorderPane root = new BorderPane();
        root.setPrefSize(800, 400);

        VBox tabButtons = new VBox(10);
        tabButtons.setStyle("-fx-background-color: #2c3e50;");
        tabButtons.setPadding(new Insets(20));
        tabButtons.setAlignment(Pos.TOP_CENTER);
        tabButtons.setPrefWidth(200);
        VBox.setVgrow(tabButtons, Priority.ALWAYS); // Allow tab buttons to grow

        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(40));
        contentBox.setStyle("-fx-background-color: #ffffff;");
        VBox.setVgrow(contentBox, Priority.ALWAYS); // Allow content box to grow
        HBox.setHgrow(contentBox, Priority.ALWAYS); // Allow content box to grow horizontally

        root.setLeft(tabButtons);
        root.setCenter(contentBox);

        Button registerButton = new Button("Register");
        Button loginButton = new Button("Login");
        Button aboutButton = new Button("About");
        styleTabButton(registerButton);
        styleTabButton(loginButton);
        styleTabButton(aboutButton);

        tabButtons.getChildren().addAll(registerButton, loginButton, aboutButton);

        VBox registerContent = buildSignUpForm();
        VBox loginContent = buildLoginForm();
        VBox aboutContent = buildAboutTab();

        contentBox.getChildren().setAll(registerContent);

        registerButton.setOnAction(e -> {
            contentBox.getChildren().setAll(registerContent);
            setButtonStyle(registerButton, true);
            setButtonStyle(loginButton, false);
            setButtonStyle(aboutButton, false);
        });
        loginButton.setOnAction(e -> {
            contentBox.getChildren().setAll(loginContent);
            setButtonStyle(registerButton, false);
            setButtonStyle(loginButton, true);
            setButtonStyle(aboutButton, false);
        });
        aboutButton.setOnAction(e -> {
            contentBox.getChildren().setAll(aboutContent);
            setButtonStyle(registerButton, false);
            setButtonStyle(loginButton, false);
            setButtonStyle(aboutButton, true);
        });

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource(Constants.STYLE_PATH).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Styles the tab buttons.
     *
     * @param button the button to style
     */
    private void styleTabButton(Button button) {
        button.getStyleClass().add("tab-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(40); // Set a preferred height to keep buttons uniform
        VBox.setVgrow(button, Priority.ALWAYS); // Allow button to grow vertically
    }

    /**
     * Sets the style of the tab button based on its selected state.
     *
     * @param button the button to style
     * @param selected whether the button is selected
     */
    private void setButtonStyle(Button button, boolean selected) {
        button.getStyleClass().removeAll("selected-tab-button", "unselected-tab-button");
        if (selected) {
            button.getStyleClass().add("selected-tab-button");
        } else {
            button.getStyleClass().add("unselected-tab-button");
        }
    }

    /**
     * Builds the sign-up form.
     *
     * @return the sign-up form VBox
     */
    private VBox buildSignUpForm() {
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getStyleClass().add("comboBox");
        roleComboBox.getItems().addAll("user", "admin");
        roleComboBox.setPromptText("Select Role");

        Button signUpButton = new Button("Register");
        signUpButton.setOnAction(event -> {
            if (!roleComboBox.getSelectionModel().isEmpty()) {
                registerUser(usernameField.getText(), passwordField.getText(), roleComboBox.getValue());
                AppLogger.logInfo("Register button clicked with role: " + roleComboBox.getValue());
            } else {
                Utility.showAlert(Alert.AlertType.ERROR, "Registration Error", "Please select a role.");
            }
        });

        VBox vBox = new VBox(10, new Label("Username:"), usernameField, new Label("Password:"), passwordField, roleComboBox, signUpButton);
        vBox.setPadding(new Insets(20));
        VBox.setVgrow(vBox, Priority.ALWAYS); // Allow vBox to grow vertically
        return vBox;
    }

    /**
     * Builds the login form.
     *
     * @return the login form VBox
     */
    private VBox buildLoginForm() {
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Button loginButton = new Button("Login");
        loginButton.setOnAction(event -> {
            loginUser(usernameField.getText(), passwordField.getText());
            AppLogger.logInfo("Login button clicked");
        });

        VBox vBox = new VBox(10, new Label("Username:"), usernameField, new Label("Password:"), passwordField, loginButton);
        vBox.setPadding(new Insets(20));
        VBox.setVgrow(vBox, Priority.ALWAYS); // Allow vBox to grow vertically
        return vBox;
    }

    /**
     * Builds the about tab.
     *
     * @return the about tab VBox
     */
    private VBox buildAboutTab() {
        Label aboutLabel = new Label("Developer: Marios Stefanidis\n\n" +
                                     "Project Purpose:\n" +
                                     "This JavaFX software was developed as part of an assignment for my Univercity [Metropolitan College]. " +
                                     "It serves as a stock management software, providing functionalities for user registration, login, and administrative tasks.\n\n" +
                                     "Contact Me:\n" +
                                     "For any inquiries or further information, please feel free to reach out.");
        aboutLabel.setWrapText(true);

        Hyperlink contactLink = new Hyperlink("-> Contact Me <-");
        contactLink.setOnAction(event -> {
            try {
                Desktop.getDesktop().mail(new URI("mailto:marios.stefanintis@gmail.com"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        VBox vBox = new VBox(aboutLabel, contactLink);
        vBox.setPadding(new Insets(15));
        vBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(vBox, Priority.ALWAYS); // Allow vBox to grow vertically
        return vBox;
    }

    /**
     * Registers a new user.
     *
     * @param username the username
     * @param password the password
     * @param role     the role
     */
    private void registerUser(String username, String password, String role) {
        username = username.trim();
        password = password.trim();
        if (username.isEmpty() || password.isEmpty() || role.isEmpty()) {
            Utility.showAlert(Alert.AlertType.ERROR, "Registration Error", "All fields must be filled.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            String checkUserSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement checkUserStmt = conn.prepareStatement(checkUserSql)) {
                checkUserStmt.setString(1, username);
                ResultSet resultSet = checkUserStmt.executeQuery();
                if (resultSet.next() && resultSet.getInt(1) > 0) {
                    Utility.showAlert(Alert.AlertType.ERROR, "Registration Error", "Username already exists.");
                    return;
                }
            }

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                pstmt.setString(3, role);
                pstmt.executeUpdate();
                Utility.showAlert(Alert.AlertType.INFORMATION, "Registration Successful", "User registered successfully as " + role + "!");
            }
        } catch (SQLException ex) {
            Utility.showAlert(Alert.AlertType.ERROR, "Registration Error", ex.getMessage());
            AppLogger.logError("Error registering user", ex);
        }
    }

    /**
     * Logs in a user.
     *
     * @param username the username
     * @param password the password
     */
    private void loginUser(String username, String password) {
        username = username.trim();
        password = password.trim();
        if (username.isEmpty() || password.isEmpty()) {
            Utility.showAlert(Alert.AlertType.ERROR, "Login Failed", "Username and password cannot be empty.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT password, role FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    String role = rs.getString("role");
                    if (BCrypt.checkpw(password, storedPassword)) {
                        primaryStage.close();
                        if ("admin".equals(role)) {
                            launchAdminPanel(username);
                        } else {
                            launchUserPanel(username);
                        }
                    } else {
                        Utility.showAlert(Alert.AlertType.ERROR, "Login Failed", "Incorrect username or password.");
                    }
                } else {
                    Utility.showAlert(Alert.AlertType.ERROR, "Login Failed", "Incorrect username or password.");
                }
            }
        } catch (SQLException ex) {
            AppLogger.logError("Error logging in user", ex);
            Utility.showAlert(Alert.AlertType.ERROR, "Login Error", "An error occurred while logging in: " + ex.getMessage());
        }
    }

    /**
     * Launches the admin panel.
     *
     * @param username the username of the admin
     */
    private void launchAdminPanel(String username) {
        AdminPanel adminPanel = new AdminPanel(this, username);
        adminPanel.start(new Stage());
    }

    /**
     * Launches the user panel.
     *
     * @param username the username of the user
     */
    private void launchUserPanel(String username) {
        UserPanel userPanel = new UserPanel(this, username);
        userPanel.start(new Stage());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
