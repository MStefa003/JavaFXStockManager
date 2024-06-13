package com.example.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.example.util.Constants;
import com.example.util.Utility;
import com.example.util.AppLogger;
import com.example.model.DatabaseConnector;

/**
 * The UserPanel class handles the functionalities related to the user interface.
 */
public class UserPanel {
    private final App app;
    private final String username;

    /**
     * Constructor for UserPanel.
     *
     * @param app      the application instance
     * @param username the username of the user
     */
    public UserPanel(App app, String username) {
        this.app = app;
        this.username = username;
    }

    /**
     * Starts the user panel.
     *
     * @param stage the primary stage
     */
    public void start(Stage stage) {
        stage.setTitle("User Dashboard - Welcome " + username);

        ListView<String> productList = new ListView<>();
        loadProducts(productList);
        productList.getStyleClass().add("list-view");

        TextField quantityField = new TextField();
        quantityField.setPromptText("Enter quantity");
        quantityField.getStyleClass().add("text-field");

        Button buyButton = new Button("Buy Selected");
        buyButton.getStyleClass().add("button");

        Button logoutButton = new Button("Log out");
        logoutButton.getStyleClass().addAll("button", "logout-button");

        buyButton.setOnAction(event -> handleBuyAction(productList, quantityField));
        logoutButton.setOnAction(event -> handleLogoutAction(stage));

        VBox sidebar = new VBox(20, buyButton, logoutButton);
        sidebar.setPadding(new Insets(20));
        sidebar.getStyleClass().add("sidebar");

        VBox mainContent = new VBox(20, productList, quantityField);
        mainContent.setPadding(new Insets(20));
        mainContent.getStyleClass().add("main-content");
        VBox.setVgrow(productList, Priority.ALWAYS);

        HBox layout = new HBox(sidebar, mainContent);
        layout.getStyleClass().add("user-panel");
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        Image icon = new Image(getClass().getResourceAsStream(Constants.ICON_PATH));
        stage.getIcons().add(icon);

        Scene scene = new Scene(layout, 800, 600);
        scene.getStylesheets().add(getClass().getResource(Constants.USER_PANEL_STYLE_PATH).toExternalForm());

        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Handles the buy action when the buy button is clicked.
     *
     * @param productList  the list of products
     * @param quantityField the text field for entering quantity
     */
    private void handleBuyAction(ListView<String> productList, TextField quantityField) {
        ObservableList<String> allProducts = productList.getItems();
        String selectedProduct = productList.getSelectionModel().getSelectedItem();
        int quantity;

        try {
            quantity = Integer.parseInt(quantityField.getText());
        } catch (NumberFormatException nfe) {
            Utility.showAlert(Alert.AlertType.ERROR, "Input error", "Please type a valid number");
            return;
        }

        if (allProducts.isEmpty()) {
            Utility.showAlert(Alert.AlertType.ERROR, "No Products Available", "There are no products available for selection.");
        } else if (selectedProduct == null) {
            Utility.showAlert(Alert.AlertType.ERROR, "No Product Selected", "No product selected. Please select a product from the list.");
        } else if (quantity <= 0) {
            Utility.showAlert(Alert.AlertType.ERROR, "Invalid Quantity", "Invalid quantity entered. Please enter a valid quantity.");
        } else {
            purchaseProduct(selectedProduct.split(" - ")[0], quantity);
        }

        logUserAction("Buy button clicked");
    }

    /**
     * Handles the logout action when the logout button is clicked.
     *
     * @param stage the current stage
     */
    private void handleLogoutAction(Stage stage) {
        Alert confirmLogoutAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmLogoutAlert.setTitle("Confirm logout");
        confirmLogoutAlert.setHeaderText(null);
        confirmLogoutAlert.setContentText("Are you sure you want to log out?");

        Optional<ButtonType> response = confirmLogoutAlert.showAndWait();

        if (response.isPresent() && response.get() == ButtonType.OK) {
            logUserAction("Logout confirmed");
            logout(stage);
        } else {
            logUserAction("Logout cancelled");
        }
    }

    /**
     * Closes the current stage and shows the login screen.
     *
     * @param stage the current stage
     */
    private void logout(Stage stage) {
        stage.close();
        app.showLoginScreen();
    }

    /**
     * Loads the products from the database and adds them to the product list.
     *
     * @param productList the list view to display the products
     */
    private void loadProducts(ListView<String> productList) {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name, price, quantity FROM Products WHERE quantity > 0")) {
            ResultSet rs = stmt.executeQuery();
            ObservableList<String> items = FXCollections.observableArrayList();
            while (rs.next()) {
                items.add(rs.getString("name") + " - Price: $" + rs.getDouble("price") + " - Stock: " + rs.getInt("quantity"));
            }
            productList.setItems(items);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Purchases the selected product in the specified quantity.
     *
     * @param selectedProduct the name of the selected product
     * @param quantity the quantity to purchase
     */
    private void purchaseProduct(String selectedProduct, int quantity) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE Products SET quantity = quantity - ? WHERE name = ? AND quantity >= ?");
            updateStmt.setInt(1, quantity);
            updateStmt.setString(2, selectedProduct);
            updateStmt.setInt(3, quantity);

            int updatedRows = updateStmt.executeUpdate();
            if (updatedRows > 0) {
                recordSale(conn, selectedProduct, quantity);
                Platform.runLater(() -> Utility.showAlert(Alert.AlertType.INFORMATION, "Purchase Successful", "You have successfully purchased " + quantity + " units of " + selectedProduct));
            } else {
                Platform.runLater(() -> Utility.showAlert(Alert.AlertType.ERROR, "Purchase Failed", "Not enough stock available."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Records the sale in the database.
     *
     * @param conn          the database connection
     * @param productName   the name of the product
     * @param quantity      the quantity sold
     * @throws SQLException if an SQL error occurs
     */
    private void recordSale(Connection conn, String productName, int quantity) throws SQLException {
        int productId = findProductId(conn, productName);
        if (productId == -1) {
            Platform.runLater(() -> Utility.showAlert(Alert.AlertType.ERROR, "Purchase Failed", "Product not found."));
            return;
        }

        double price = findProductPrice(conn, productId);

        PreparedStatement saleStmt = conn.prepareStatement(
                "INSERT INTO Sales (product_id, quantity_sold, sale_date, total_price) VALUES (?, ?, NOW(), ?)");
        saleStmt.setInt(1, productId);
        saleStmt.setInt(2, quantity);
        saleStmt.setDouble(3, price * quantity);
        saleStmt.executeUpdate();
    }

    /**
     * Finds the product ID by its name.
     *
     * @param conn          the database connection
     * @param productName   the name of the product
     * @return the product ID, or -1 if not found
     * @throws SQLException if an SQL error occurs
     */
    private int findProductId(Connection conn, String productName) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT product_id FROM Products WHERE name = ?");
        stmt.setString(1, productName);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("product_id");
        }
        return -1;
    }

    /**
     * Finds the product price by its ID.
     *
     * @param conn       the database connection
     * @param productId  the product ID
     * @return the product price
     * @throws SQLException if an SQL error occurs
     */
    private double findProductPrice(Connection conn, int productId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT price FROM Products WHERE product_id = ?");
        stmt.setInt(1, productId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getDouble("price");
        }
        return 0;
    }

    /**
     * Logs the user action.
     *
     * @param action the action performed by the user
     */
    private void logUserAction(String action) {
        AppLogger.logInfo(action);
    }
}
