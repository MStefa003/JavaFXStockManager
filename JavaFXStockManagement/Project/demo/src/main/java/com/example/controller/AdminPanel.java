package com.example.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import com.example.util.Constants;
import com.example.util.Utility;
import com.example.util.AppLogger;
import com.example.model.DatabaseConnector;

/**
 * The AdminPanel class handles the functionalities related to the admin interface.
 */
public class AdminPanel {

    private final App app;
    private final String username;
    private ListView<String> productListView;
    private ListView<String> salesLog;
    private ListView<String> lowStockList;
    private ListView<String> salesTrendsList;
    private final Set<Integer> notifiedProducts = new HashSet<>();

    /**
     * Constructor for AdminPanel.
     *
     * @param app      the application instance
     * @param username the username of the admin
     */
    public AdminPanel(App app, String username) {
        this.app = app;
        this.username = username;
    }

    /**
     * Starts the admin panel.
     *
     * @param stage the primary stage
     */
    public void start(Stage stage) {
        stage.setTitle("Admin Dashboard - Welcome " + username);

        // Load the icon
        Image icon = new Image(getClass().getResourceAsStream(Constants.ICON_PATH));
        stage.getIcons().add(icon);

        HBox root = new HBox();
        root.setPrefSize(1000, 600);

        VBox tabButtons = createTabButtons();
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(40));
        contentBox.setStyle("-fx-background-color: #ecf0f1;");
        root.getChildren().addAll(tabButtons, contentBox);

        VBox stockContent = createStockReportPanel();
        VBox productManagementContent = createProductManagementPanel();
        VBox productDeletionContent = createProductDeletionPanel();
        VBox salesLogContent = createSalesLogPanel();
        VBox salesTrendsContent = createSalesTrendPanel();

        contentBox.getChildren().setAll(stockContent);

        tabButtons.getChildren().get(0).setOnMouseClicked(e -> contentBox.getChildren().setAll(stockContent));
        tabButtons.getChildren().get(1).setOnMouseClicked(e -> contentBox.getChildren().setAll(productManagementContent));
        tabButtons.getChildren().get(2).setOnMouseClicked(e -> contentBox.getChildren().setAll(productDeletionContent));
        tabButtons.getChildren().get(3).setOnMouseClicked(e -> contentBox.getChildren().setAll(salesLogContent));
        tabButtons.getChildren().get(4).setOnMouseClicked(e -> contentBox.getChildren().setAll(salesTrendsContent));
        tabButtons.getChildren().get(5).setOnMouseClicked(e -> confirmLogout(stage));

        HBox.setHgrow(contentBox, Priority.ALWAYS);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource(Constants.ADMIN_PANEL_STYLE_PATH).toExternalForm());
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        Timeline checkStockTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> checkLowStock()));
        checkStockTimeline.setCycleCount(Timeline.INDEFINITE);
        checkStockTimeline.play();
    }

    /**
     * Creates the tab buttons for the admin panel.
     *
     * @return the VBox containing the tab buttons
     */
    private VBox createTabButtons() {
        VBox tabButtons = new VBox(10);
        tabButtons.setStyle("-fx-background-color: #2c3e50;");
        tabButtons.setPadding(new Insets(20));
        tabButtons.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        tabButtons.setPrefWidth(200);

        Button stockButton = new Button("Stock Reports");
        Button productManagementButton = new Button("Product Management");
        Button productDeletionButton = new Button("Delete Products");
        Button salesLogButton = new Button("Sales Log");
        Button salesTrendsButton = new Button("Sales Trends");
        Button logoutButton = new Button("Log out");

        styleTabButton(stockButton);
        styleTabButton(productManagementButton);
        styleTabButton(productDeletionButton);
        styleTabButton(salesLogButton);
        styleTabButton(salesTrendsButton);
        styleTabButton(logoutButton);

        tabButtons.getChildren().addAll(stockButton, productManagementButton, productDeletionButton, salesLogButton, salesTrendsButton, logoutButton);
        return tabButtons;
    }

    /**
     * Styles the tab buttons.
     *
     * @param button the button to style
     */
    private void styleTabButton(Button button) {
        button.getStyleClass().add("tab-button");
        button.setMaxWidth(Double.MAX_VALUE);
    }

    /**
     * Confirms the logout action.
     *
     * @param stage the primary stage
     */
    private void confirmLogout(Stage stage) {
        Alert confirmLogoutAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmLogoutAlert.setTitle("Confirm logout");
        confirmLogoutAlert.setHeaderText(null);
        confirmLogoutAlert.setContentText("Are you sure you want to log out?");

        Optional<ButtonType> response = confirmLogoutAlert.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.OK) {
            AppLogger.logInfo("Logout confirmed");
            logout(stage);
        } else {
            AppLogger.logInfo("Logout cancelled");
        }
    }

    /**
     * Logs out the user and closes the stage.
     *
     * @param stage the primary stage
     */
    private void logout(Stage stage) {
        stage.close();
        app.showLoginScreen();
    }

    /**
     * Creates the stock report panel.
     *
     * @return the VBox containing the stock report panel
     */
    private VBox createStockReportPanel() {
        VBox vBox = new VBox(10);
        Label header = new Label("Detailed Stock Report");
        lowStockList = new ListView<>();
        vBox.getChildren().addAll(header, lowStockList);
        return vBox;
    }

    /**
     * Creates the product management panel.
     *
     * @return the VBox containing the product management panel
     */
    private VBox createProductManagementPanel() {
        VBox vBox = new VBox(10);
        TextField nameField = new TextField();
        nameField.setPromptText("Product Name");
        TextField priceField = new TextField();
        priceField.setPromptText("Product Price");
        TextField quantityField = new TextField();
        quantityField.setPromptText("Initial Quantity");
        Button addButton = new Button("Add Product");

        addButton.setOnAction(e -> {
            if (nameField.getText().isEmpty() || priceField.getText().isEmpty() || quantityField.getText().isEmpty()) {
                Utility.showAlert(Alert.AlertType.ERROR, "Input Error", "Please fill in all fields to add a product.");
            } else {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    int quantity = Integer.parseInt(quantityField.getText());
                    addProduct(nameField.getText(), price, quantity);
                    Utility.showAlert(Alert.AlertType.INFORMATION, "Added", "You successfully added " + nameField.getText() + " to products.");
                    AppLogger.logInfo("Add Product button clicked with product: " + nameField.getText());
                } catch (NumberFormatException nfe) {
                    Utility.showAlert(Alert.AlertType.ERROR, "Input Error", "Price and quantity must be valid numbers.");
                }
            }
        });

        TextField productIdField = new TextField();
        productIdField.setPromptText("Product ID");
        TextField increaseAmountField = new TextField();
        increaseAmountField.setPromptText("Amount to Increase");
        Button increaseStockButton = new Button("Increase Stock");

        increaseStockButton.setOnAction(e -> {
            if (productIdField.getText().isEmpty() || increaseAmountField.getText().isEmpty()) {
                Utility.showAlert(Alert.AlertType.ERROR, "Input Error", "Please fill in all fields to increase stock.");
            } else {
                try {
                    int productId = Integer.parseInt(productIdField.getText());
                    int amount = Integer.parseInt(increaseAmountField.getText());
                    boolean success = increaseStock(productId, amount);
                    if (success) {
                        Utility.showAlert(Alert.AlertType.INFORMATION, "Succeed", "You successfully added " + amount + " stock to Product ID: " + productId);
                        AppLogger.logInfo("Increase Stock button clicked with Product ID: " + productId);
                    } else {
                        Utility.showAlert(Alert.AlertType.ERROR, "Error", "This ID does not exist.");
                    }
                } catch (NumberFormatException nfe) {
                    Utility.showAlert(Alert.AlertType.ERROR, "Input Error", "ID and amount must be valid numbers.");
                }
            }
        });

        vBox.getChildren().addAll(
            new Label("Add New Product:"),
            nameField, priceField, quantityField, addButton,
            new Label("Increase Stock for an Existing Product:"),
            productIdField, increaseAmountField, increaseStockButton
        );
        return vBox;
    }

    /**
     * Creates the product deletion panel.
     *
     * @return the VBox containing the product deletion panel
     */
    private VBox createProductDeletionPanel() {
        VBox vBox = new VBox(10);
        Label header = new Label("Delete Products");
        productListView = new ListView<>();
        loadProducts();
        Button deleteProductButton = new Button("Delete Selected Product");

        deleteProductButton.setOnAction(e -> {
            if (productListView.getItems().isEmpty()) {
                Utility.showAlert(Alert.AlertType.ERROR, "Error", "There are no products to delete");
            } else if (productListView.getSelectionModel().getSelectedItem() == null) {
                Utility.showAlert(Alert.AlertType.ERROR, "Error", "Please select a product to delete");
            } else {
                deleteSelectedProduct();
                AppLogger.logInfo("Delete Product button clicked");
            }
        });
        vBox.getChildren().addAll(header, productListView, deleteProductButton);
        return vBox;
    }

    /**
     * Creates the sales log panel.
     *
     * @return the VBox containing the sales log panel
     */
    private VBox createSalesLogPanel() {
        VBox vBox = new VBox(10);
        Label header = new Label("Recent Purchases");
        salesLog = new ListView<>();
        Button deleteButton = new Button("Delete Selected Log");

        deleteButton.setOnAction(e -> {
            if (salesLog.getItems().isEmpty()) {
                Utility.showAlert(Alert.AlertType.ERROR, "Error", "There are no logs to delete.");
            } else if (salesLog.getSelectionModel().getSelectedItem() == null) {
                Utility.showAlert(Alert.AlertType.ERROR, "Error", "Please select a log to delete.");
            } else {
                deleteSelectedLog();
                AppLogger.logInfo("Delete sales button clicked");
            }
        });

        Button clearAllButton = new Button("Clear All Logs");
        clearAllButton.setOnAction(e -> {
            if (salesLog.getItems().isEmpty()) {
                Utility.showAlert(Alert.AlertType.ERROR, "Error", "There are no logs to delete.");
            } else {
                Alert confirmLogoutAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmLogoutAlert.setTitle("Confirm clearing all logs");
                confirmLogoutAlert.setHeaderText(null);
                confirmLogoutAlert.setContentText("Are you sure you want to clear all logs?");

                Optional<ButtonType> response = confirmLogoutAlert.showAndWait();
                if (response.isPresent() && response.get() == ButtonType.OK) {
                    clearAllLogs();
                    AppLogger.logInfo("Clear All Sales Logs button clicked");
                } else {
                    AppLogger.logInfo("Clear all logs cancelled");
                }
            }
        });

        Button exportButton = new Button("Export to CSV");
        exportButton.setOnAction(e -> exportSalesLogsToCSV());

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(deleteButton, clearAllButton, exportButton);
        buttonBox.setPadding(new Insets(10, 0, 10, 0));
        buttonBox.setSpacing(10);

        loadSales(salesLog);
        vBox.getChildren().addAll(header, salesLog, buttonBox);
        return vBox;
    }

    /**
     * Creates the sales trend panel.
     *
     * @return the VBox containing the sales trend panel
     */
    private VBox createSalesTrendPanel() {
        VBox vBox = new VBox(10);
        Label header = new Label("Sales Trends Over Time");
        salesTrendsList = new ListView<>();
        loadSalesTrends(salesTrendsList);
        vBox.getChildren().addAll(header, salesTrendsList);
        return vBox;
    }

    /**
     * Loads the sales trends and populates the sales trends list.
     *
     * @param salesTrendsList the ListView to populate
     */
    private void loadSalesTrends(ListView<String> salesTrendsList) {
        ObservableList<String> salesData = FXCollections.observableArrayList();
        String query = "SELECT p.name, SUM(s.quantity_sold) AS total_sold FROM Sales s JOIN Products p ON s.product_id = p.product_id GROUP BY p.name ORDER BY total_sold DESC";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String trendInfo = String.format("Product: %s, Total Sold: %d",
                        rs.getString("name"), rs.getInt("total_sold"));
                salesData.add(trendInfo);
            }
            salesTrendsList.setItems(salesData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs user actions.
     *
     * @param action the action to log
     */
    private void logUserAction(String action) {
        Platform.runLater(() -> System.out.println("User action: " + action));
    }

    /**
     * Loads the products and populates the product list view.
     */
    private void loadProducts() {
        ObservableList<String> products = FXCollections.observableArrayList();
        String sql = "SELECT product_id, name, price, quantity FROM Products ORDER BY name";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                products.add(rs.getInt("product_id") + ": " + rs.getString("name") + " - $" + rs.getDouble("price") + " - Qty: " + rs.getInt("quantity"));
            }
            productListView.setItems(products);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes the selected product from the product list view.
     */
    private void deleteSelectedProduct() {
        String selected = productListView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isEmpty()) {
            int productId = Integer.parseInt(selected.split(":")[0]);
            try (Connection conn = DatabaseConnector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM Products WHERE product_id = ?")) {
                stmt.setInt(1, productId);
                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    loadProducts();
                    removeSalesTrend(productId);
                    loadSalesTrends(salesTrendsList);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                Utility.showAlert(Alert.AlertType.ERROR, "Delete product error", e.getMessage());
            }
        }
    }

    /**
     * Adds a new product to the database.
     *
     * @param name     the name of the product
     * @param price    the price of the product
     * @param quantity the quantity of the product
     */
    private void addProduct(String name, double price, int quantity) {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO Products (name, price, quantity) VALUES (?, ?, ?)")) {
            stmt.setString(1, name);
            stmt.setDouble(2, price);
            stmt.setInt(3, quantity);
            stmt.executeUpdate();
            loadProducts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Increases the stock of an existing product.
     *
     * @param productId the ID of the product
     * @param amount    the amount to increase
     * @return true if the stock was successfully increased, false otherwise
     */
    private boolean increaseStock(int productId, int amount) {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE Products SET quantity = quantity + ? WHERE product_id = ?")) {
            stmt.setInt(1, amount);
            stmt.setInt(2, productId);
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads the sales logs and populates the sales log list view.
     *
     * @param salesLog the ListView to populate
     */
    private void loadSales(ListView<String> salesLog) {
        ObservableList<String> salesData = FXCollections.observableArrayList();
        String query = "SELECT sale_id, p.name, s.quantity_sold, s.sale_date FROM Sales s JOIN Products p ON s.product_id = p.product_id ORDER BY s.sale_date DESC";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String saleInfo = String.format("%d: Product: %s, Quantity Sold: %d, Sale Date: %s",
                        rs.getInt("sale_id"), rs.getString("name"), rs.getInt("quantity_sold"), rs.getString("sale_date"));
                salesData.add(saleInfo);
            }
            salesLog.setItems(salesData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes the selected log from the sales log list view.
     */
    private void deleteSelectedLog() {
        String selected = salesLog.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int saleId = Integer.parseInt(selected.split(":")[0]);
            try (Connection conn = DatabaseConnector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM Sales WHERE sale_id = ?")) {
                stmt.setInt(1, saleId);
                stmt.executeUpdate();
                loadSales(salesLog);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Clears all logs from the sales log list view.
     */
    private void clearAllLogs() {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Sales")) {
            stmt.executeUpdate();
            loadSales(salesLog);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Exports the sales logs to a CSV file.
     */
    private void exportSalesLogsToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Sales Logs");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("Sale ID,Product Name,Quantity Sold,Sale Date");
                ObservableList<String> salesData = salesLog.getItems();
                for (String sale : salesData) {
                    String[] data = sale.split(", ");
                    String saleId = data[0].split(": ")[0];
                    String productName = data[0].substring(data[0].indexOf(": ") + 2);
                    String quantitySold = data[1].split(": ")[1];
                    String saleDate = data[2].split(": ")[1];
                    writer.println(saleId + "," + productName + "," + quantitySold + "," + saleDate);
                }
                Utility.showAlert(Alert.AlertType.INFORMATION, "Success", "Sales logs exported successfully to CSV.");
                System.out.println("Sales logs exported successfully to " + file.getAbsolutePath());
            } catch (IOException e) {
                Utility.showAlert(Alert.AlertType.ERROR, "Export Error", "Error occurred while exporting sales logs: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks for low stock and updates the low stock list view.
     */
    private void checkLowStock() {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT product_id, name, quantity FROM Products")) {
            ResultSet rs = stmt.executeQuery();
            Set<Integer> currentLowStockIds = new HashSet<>();

            while (rs.next()) {
                int productId = rs.getInt("product_id");
                String productName = rs.getString("name");
                int quantity = rs.getInt("quantity");

                if (quantity <= 3) {
                    currentLowStockIds.add(productId);
                    if (!notifiedProducts.contains(productId)) {
                        String message = String.format("Low stock alert: %s - Only %d left in stock!", productName, quantity);
                        Platform.runLater(() -> lowStockList.getItems().add(message));
                        notifiedProducts.add(productId);
                    }
                }
            }

            notifiedProducts.retainAll(currentLowStockIds);
            Platform.runLater(() -> lowStockList.getItems().removeIf(item -> {
                String[] parts = item.split(" - Only ");
                String namePart = parts[0].substring(parts[0].indexOf(':') + 2);
                return !currentLowStockIds.contains(getProductIdByName(namePart));
            }));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the sales trend for the specified product.
     *
     * @param productId the ID of the product
     */
    private void removeSalesTrend(int productId) {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Sales WHERE product_id = ?")) {
            stmt.setInt(1, productId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the product ID by the product name.
     *
     * @param productName the name of the product
     * @return the product ID, or -1 if not found
     */
    private int getProductIdByName(String productName) {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT product_id FROM Products WHERE name = ?")) {
            stmt.setString(1, productName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("product_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
