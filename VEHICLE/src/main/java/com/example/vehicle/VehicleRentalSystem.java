package com.example.vehicle;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class VehicleRentalSystem extends Application {

    private Stage primaryStage;
    private Scene mainScene, dashboardScene, vehicleScene, customerScene, bookingScene, paymentScene, reportScene;

    private ComboBox<String> roleDropdown, paymentMethodComboBox;
    private TextField usernameField, vehicleIdField, brandModelField, rentalPriceField;
    private TextField customerNameField, contactInfoField, licenseNumberField;
    private TextField bookingVehicleIdField, bookingCustomerIdField;
    private CheckBox availabilityCheckBox;
    private PasswordField passwordField;

    private ObservableList<String> vehicleCategories = FXCollections.observableArrayList("Car", "Bike", "Van", "Truck");
    private ObservableList<Vehicle> vehicleList = FXCollections.observableArrayList();
    private ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private ObservableList<Booking> bookingList = FXCollections.observableArrayList();

    private String currentRole = "";
    private ListView<Vehicle> vehicleListView;
    private ListView<Customer> customerListView;
    private ListView<Booking> bookingListView;
    private DbConnector dbConnector = new DbConnector();

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        createMainScene();
        createVehicleScene();
        createCustomerScene();
        createBookingScene();
        createPaymentScene();
        createReportScene();
        primaryStage.setTitle("Vehicle Rental System");
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    // Main scene setup
    private void createMainScene() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        Label title = new Label("Register or Login");
        usernameField = new TextField();
        usernameField.setPromptText("Enter Username");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");

        roleDropdown = new ComboBox<>();
        roleDropdown.getItems().addAll("Admin", "Employee");
        roleDropdown.setPromptText("Select Role");

        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> handleRegister());

        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> handleLogin());

        layout.getChildren().addAll(title, roleDropdown, usernameField, passwordField, registerButton, loginButton);
        mainScene = new Scene(layout, 300, 250);

        mainScene.getStylesheets().add(getClass().getResource("/main.css").toExternalForm());

    }

    // User registration handling
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String selectedRole = roleDropdown.getValue();

        if (username.isEmpty() || password.isEmpty() || selectedRole == null) {
            showAlert("Please fill in all fields and select a role.");
            return;
        }

        if (dbConnector.isUsernameAvailable(username)) {
            dbConnector.registerUser(username, password, selectedRole);
            saveToCSV("User Registration", username, selectedRole);
            showAlert("Registration successful! You can log in now.");
        } else {
            showAlert("Username is already taken.");
        }
    }

    // User login handling
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String selectedRole = roleDropdown.getValue();

        if (username.isEmpty() || password.isEmpty() || selectedRole == null) {
            showAlert("Please fill in all fields and select a role.");
            return;
        }

        if (dbConnector.validateUser(username, password)) {
            currentRole = selectedRole;
            createDashboardScene();
            primaryStage.setScene(dashboardScene);
        } else {
            showAlert("Login failed: Incorrect username or password.");
        }
    }

    // Dashboard setup
    private void createDashboardScene() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        Label title = new Label("Dashboard - " + currentRole);

        if ("Admin".equals(currentRole)) {
            Button vehicleButton = new Button("Vehicle Management");
            vehicleButton.setOnAction(e -> {
                refreshVehicleList();
                primaryStage.setScene(vehicleScene);
            });

            Button customerButton = new Button("Customer Management");
            customerButton.setOnAction(e -> {
                refreshCustomerList();
                primaryStage.setScene(customerScene);
            });

            Button paymentButton = new Button("Payments & Billing");
            paymentButton.setOnAction(e -> primaryStage.setScene(paymentScene));

            Button reportButton = new Button("Reports & Data");
            reportButton.setOnAction(e -> primaryStage.setScene(reportScene));

            layout.getChildren().addAll(title, vehicleButton, customerButton, paymentButton, reportButton);
        } else if ("Employee".equals(currentRole)) {
            Button bookingButton = new Button("Booking System");
            bookingButton.setOnAction(e -> {
                refreshBookingList();
                primaryStage.setScene(bookingScene);
            });

            Button paymentButton = new Button("Payments & Billing");
            paymentButton.setOnAction(e -> primaryStage.setScene(paymentScene));

            layout.getChildren().addAll(title, bookingButton, paymentButton);
        }

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> {
            currentRole = "";
            createMainScene();
            primaryStage.setScene(mainScene);
        });

        layout.getChildren().add(logoutButton);
        dashboardScene = new Scene(layout, 350, 400);
        dashboardScene.getStylesheets().add(getClass().getResource("/dashboard.css").toExternalForm());

    }

    // Vehicle management scene
    private void createVehicleScene() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        Label title = new Label("Vehicle Management");

        vehicleIdField = new TextField();
        vehicleIdField.setPromptText("Enter Vehicle ID (if updating)");
        brandModelField = new TextField();
        brandModelField.setPromptText("Enter Brand & Model");

        ComboBox<String> categoryComboBox = new ComboBox<>();
        categoryComboBox.getItems().addAll(vehicleCategories);
        categoryComboBox.setPromptText("Select Category");

        rentalPriceField = new TextField();
        rentalPriceField.setPromptText("Enter Rental Price per Day");
        availabilityCheckBox = new CheckBox("Available");

        Button addButton = new Button("Add Vehicle");
        addButton.setOnAction(e -> {
            try {
                String brandModel = brandModelField.getText().trim();
                String rentalPriceString = rentalPriceField.getText().trim();
                String category = categoryComboBox.getValue();

                if (!brandModel.isEmpty() && !rentalPriceString.isEmpty() && category != null) {
                    double pricePerDay = Double.parseDouble(rentalPriceString);
                    boolean availability = availabilityCheckBox.isSelected();

                    dbConnector.addVehicle(brandModel, pricePerDay, availability, category);
                    saveToCSV("Vehicle Added", brandModel, String.valueOf(pricePerDay), String.valueOf(availability), category);
                    clearVehicleFields();
                    refreshVehicleList();
                    showAlert("Vehicle added successfully!");
                } else {
                    showAlert("Please fill all fields.");
                }
            } catch (NumberFormatException ex) {
                showAlert("Rental price must be a valid number.");
            }
        });

        vehicleListView = new ListView<>();
        vehicleListView.setItems(vehicleList);
        vehicleListView.setOnMouseClicked(e -> {
            Vehicle selectedVehicle = vehicleListView.getSelectionModel().getSelectedItem();
            if (selectedVehicle != null) {
                fillVehicleForm(selectedVehicle);
            }
        });

        Button updateButton = new Button("Update Vehicle");
        updateButton.setOnAction(e -> {
            Vehicle selectedVehicle = vehicleListView.getSelectionModel().getSelectedItem();
            if (selectedVehicle != null) {
                try {
                    String brandModel = brandModelField.getText().trim();
                    String rentalPriceString = rentalPriceField.getText().trim();
                    String category = categoryComboBox.getValue();

                    if (!brandModel.isEmpty() && !rentalPriceString.isEmpty() && category != null) {
                        double pricePerDay = Double.parseDouble(rentalPriceString);
                        boolean availability = availabilityCheckBox.isSelected();

                        // Correctly referencing the ID
                        dbConnector.updateVehicle(selectedVehicle.getId(), brandModel, pricePerDay, availability, category);
                        clearVehicleFields();
                        refreshVehicleList();
                        showAlert("Vehicle updated successfully!");
                    } else {
                        showAlert("Please fill all fields.");
                    }
                } catch (NumberFormatException ex) {
                    showAlert("Rental price must be a valid number.");
                }
            } else {
                showAlert("Please select a vehicle to update.");
            }
        });

        Button deleteButton = new Button("Delete Vehicle");
        deleteButton.setOnAction(e -> {
            Vehicle selectedVehicle = vehicleListView.getSelectionModel().getSelectedItem();
            if (selectedVehicle != null) {
                dbConnector.deleteVehicle(selectedVehicle);
                clearVehicleFields();
                refreshVehicleList();
                showAlert("Vehicle deleted successfully!");
            } else {
                showAlert("Please select a vehicle to delete.");
            }
        });

        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(e -> primaryStage.setScene(dashboardScene));

        layout.getChildren().addAll(title, vehicleIdField, brandModelField, categoryComboBox, rentalPriceField, availabilityCheckBox, addButton, updateButton, deleteButton, vehicleListView, backButton);
        vehicleScene = new Scene(layout, 400, 450);

        vehicleScene.getStylesheets().add(getClass().getResource("/vehicle.css").toExternalForm());

    }

    private void fillVehicleForm(Vehicle vehicle) {
        vehicleIdField.setText(String.valueOf(vehicle.getId()));
        brandModelField.setText(vehicle.getBrandModel());
        rentalPriceField.setText(String.valueOf(vehicle.getRentalPrice()));
        availabilityCheckBox.setSelected(vehicle.isAvailable());
    }

    private void clearVehicleFields() {
        vehicleIdField.clear();
        brandModelField.clear();
        rentalPriceField.clear();
    }

    // Customer management scene
    private void createCustomerScene() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        Label title = new Label("Customer Management");

        customerNameField = new TextField();
        customerNameField.setPromptText("Enter Customer Name");
        contactInfoField = new TextField();
        contactInfoField.setPromptText("Enter Contact Information");
        licenseNumberField = new TextField();
        licenseNumberField.setPromptText("Enter Driving License Number");

        Button addCustomerButton = new Button("Register Customer");
        addCustomerButton.setOnAction(e -> {
            if (!customerNameField.getText().isEmpty() &&
                    !contactInfoField.getText().isEmpty() &&
                    !licenseNumberField.getText().isEmpty()) {
                dbConnector.addCustomer(customerNameField.getText(), contactInfoField.getText(), licenseNumberField.getText());
                saveToCSV("Customer Added", customerNameField.getText(), contactInfoField.getText(), licenseNumberField.getText());
                clearCustomerFields();
                refreshCustomerList();
            } else {
                showAlert("Please fill all fields.");
            }
        });

        Button updateCustomerButton = new Button("Update Customer");
        updateCustomerButton.setOnAction(e -> {
            Customer selectedCustomer = customerListView.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                dbConnector.updateCustomer(selectedCustomer.getCustomerId(),
                        customerNameField.getText(),
                        contactInfoField.getText(),
                        licenseNumberField.getText());
                refreshCustomerList();
                clearCustomerFields();
            } else {
                showAlert("Please select a customer to update.");
            }
        });

        customerListView = new ListView<>();
        customerListView.setItems(customerList);
        customerListView.setOnMouseClicked(e -> {
            Customer selectedCustomer = customerListView.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                customerNameField.setText(selectedCustomer.getCustomerName());
                contactInfoField.setText(selectedCustomer.getContactInfo());
                licenseNumberField.setText(selectedCustomer.getLicenseNumber());
            }
        });

        Button deleteCustomerButton = new Button("Delete Customer");
        deleteCustomerButton.setOnAction(e -> {
            Customer selectedCustomer = customerListView.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                dbConnector.deleteCustomer(selectedCustomer);
                refreshCustomerList();
            } else {
                showAlert("Please select a customer to delete.");
            }
        });

        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(e -> primaryStage.setScene(dashboardScene));

        layout.getChildren().addAll(title, customerNameField, contactInfoField,
                licenseNumberField, addCustomerButton,
                updateCustomerButton, customerListView, deleteCustomerButton, backButton);

        customerScene = new Scene(layout, 400, 500);

        customerScene.getStylesheets().add(getClass().getResource("/customer.css").toExternalForm());

    }

    private void clearCustomerFields() {
        customerNameField.clear();
        contactInfoField.clear();
        licenseNumberField.clear();
    }

    // Booking management scene
    private void createBookingScene() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        Label title = new Label("Booking System");

        bookingVehicleIdField = new TextField();
        bookingVehicleIdField.setPromptText("Enter Vehicle ID");
        bookingCustomerIdField = new TextField();
        bookingCustomerIdField.setPromptText("Enter Customer ID");

        Label startDateLabel = new Label("Rental Start Date:");
        DatePicker startDatePicker = new DatePicker();
        Label endDateLabel = new Label("Rental End Date:");
        DatePicker endDatePicker = new DatePicker();

        Button bookButton = new Button("Book Vehicle");
        bookButton.setOnAction(e -> {
            try {
                String vehicleId = bookingVehicleIdField.getText().trim();
                String customerId = bookingCustomerIdField.getText().trim();
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                if (!vehicleId.isEmpty() && !customerId.isEmpty() && startDate != null && endDate != null) {
                    if (endDate.isAfter(startDate)) {
                        dbConnector.bookVehicle(vehicleId, customerId, startDate, endDate);
                        saveToCSV("Vehicle Booked", vehicleId, customerId, startDate.toString(), endDate.toString());
                        showAlert("Vehicle booked successfully!");
                        clearBookingFields(startDatePicker, endDatePicker);
                        refreshBookingList();
                    } else {
                        showAlert("End date must be after the start date.");
                    }
                } else {
                    showAlert("Please enter both Vehicle ID, Customer ID, and select the dates.");
                }
            } catch (Exception ex) {
                showAlert("An error occurred while booking the vehicle: " + ex.getMessage());
            }
        });

        bookingListView = new ListView<>();
        bookingListView.setItems(bookingList);
        bookingListView.setOnMouseClicked(e -> {
            Booking selectedBooking = bookingListView.getSelectionModel().getSelectedItem();
            if (selectedBooking != null) {
                bookingVehicleIdField.setText(selectedBooking.getVehicleId());
                bookingCustomerIdField.setText(selectedBooking.getCustomerId());
                startDatePicker.setValue(selectedBooking.getStartDate());
                endDatePicker.setValue(selectedBooking.getEndDate());
            }
        });

        Button updateBookingButton = new Button("Update Booking");
        updateBookingButton.setOnAction(e -> {
            Booking selectedBooking = bookingListView.getSelectionModel().getSelectedItem();
            if (selectedBooking != null) {
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                if (endDate.isAfter(startDate)) {
                    dbConnector.updateBooking(selectedBooking.getBookingId(), startDate, endDate);
                    showAlert("Booking updated successfully!");
                    refreshBookingList();
                } else {
                    showAlert("End date must be after the start date.");
                }
            } else {
                showAlert("Please select a booking to update.");
            }
        });

        Button deleteBookingButton = new Button("Cancel Booking");
        deleteBookingButton.setOnAction(e -> {
            Booking selectedBooking = bookingListView.getSelectionModel().getSelectedItem();
            if (selectedBooking != null) {
                dbConnector.deleteBooking(selectedBooking);
                refreshBookingList();
            } else {
                showAlert("Please select a booking to cancel.");
            }
        });

        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(e -> primaryStage.setScene(dashboardScene));

        layout.getChildren().addAll(title, bookingVehicleIdField, bookingCustomerIdField,
                startDateLabel, startDatePicker, endDateLabel, endDatePicker,
                bookButton, bookingListView, updateBookingButton, deleteBookingButton, backButton);

        bookingScene = new Scene(layout, 400, 600);

        bookingScene.getStylesheets().add(getClass().getResource("/booking.css").toExternalForm());

    }

    // Clear booking fields
    private void clearBookingFields(DatePicker startDatePicker, DatePicker endDatePicker) {
        bookingVehicleIdField.clear();
        bookingCustomerIdField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
    }

    // Payment scene setup
    private void createPaymentScene() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        Label title = new Label("Payments & Billing");

        TextField paymentBookingIdField = new TextField();
        paymentBookingIdField.setPromptText("Enter Booking ID");
        TextField paymentAmountField = new TextField();
        paymentAmountField.setPromptText("Enter Amount");

        paymentMethodComboBox = new ComboBox<>();
        paymentMethodComboBox.getItems().addAll("Cash", "Credit Card", "Online");
        paymentMethodComboBox.setPromptText("Select Payment Method");

        // Additional services and late fees checkboxes
        CheckBox additionalService1CheckBox = new CheckBox("GPS Rental (R50)");
        CheckBox additionalService2CheckBox = new CheckBox("Child Seat (R30)");
        CheckBox lateFeeCheckBox = new CheckBox("Late Fee (R100)");

        Button payButton = new Button("Process Payment");
        payButton.setOnAction(e -> {
            String bookingId = paymentBookingIdField.getText().trim();
            String amountString = paymentAmountField.getText().trim();
            String paymentMethod = paymentMethodComboBox.getValue();

            if (!bookingId.isEmpty() && !amountString.isEmpty() && paymentMethod != null) {
                try {
                    double baseAmount = Double.parseDouble(amountString);
                    double additionalServicesTotal = 0;

                    // Calculate additional services and late fees
                    if (additionalService1CheckBox.isSelected()) {
                        additionalServicesTotal += 50; // Cost for GPS
                    }
                    if (additionalService2CheckBox.isSelected()) {
                        additionalServicesTotal += 30; // Cost for Child Seat
                    }
                    if (lateFeeCheckBox.isSelected()) {
                        additionalServicesTotal += 100; // Late fee
                    }

                    double totalAmount = baseAmount + additionalServicesTotal; // Total amount to pay
                    dbConnector.processPayment(bookingId, totalAmount);
                    generateInvoice(bookingId, totalAmount, paymentMethod, additionalServicesTotal);
                    paymentBookingIdField.clear();
                    paymentAmountField.clear();
                    paymentMethodComboBox.setValue(null);
                    additionalService1CheckBox.setSelected(false);
                    additionalService2CheckBox.setSelected(false);
                    lateFeeCheckBox.setSelected(false);
                    showAlert("Payment processed successfully!");
                } catch (NumberFormatException ex) {
                    showAlert("Amount must be a valid number.");
                }
            } else {
                showAlert("Please enter Booking ID, Amount, and select a payment method.");
            }
        });

        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(e -> primaryStage.setScene(dashboardScene));

        layout.getChildren().addAll(title, paymentBookingIdField, paymentAmountField, paymentMethodComboBox,
                additionalService1CheckBox, additionalService2CheckBox, lateFeeCheckBox, payButton, backButton);
        paymentScene = new Scene(layout, 400, 400);

        paymentScene.getStylesheets().add(getClass().getResource("/payment.css").toExternalForm());

    }

    // Generate invoice for payments
    private void generateInvoice(String bookingId, double amount, String paymentMethod, double additionalServicesTotal) {
        String invoice = "Invoice\n---------\n";
        invoice += "Booking ID: " + bookingId + "\n";
        invoice += "Base Amount: R" + amount + "\n";
        invoice += "Additional Services Total: R" + additionalServicesTotal + "\n";
        invoice += "Total Amount Due: R" + (amount + additionalServicesTotal) + "\n";
        invoice += "Payment Method: " + paymentMethod + "\n";
        invoice += "Thank you for your payment!\n";

        TextArea invoiceTextArea = new TextArea(invoice);
        invoiceTextArea.setEditable(false);

        // Add a print button (basic implementation)
        Button printButton = new Button("Print Invoice");
        printButton.setOnAction(e -> {
            System.out.println("Printing Invoice..."); // Placeholder for actual print functionality
        });

        VBox invoiceLayout = new VBox(10, invoiceTextArea, printButton);
        Stage invoiceStage = new Stage();
        invoiceStage.setTitle("Invoice");
        Scene invoiceScene = new Scene(invoiceLayout, 300, 300);
        invoiceStage.setScene(invoiceScene);
        invoiceStage.show();
    }

    // Report scene setup
    private void createReportScene() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        Label title = new Label("Reports");

        Button availableVehiclesButton = new Button("Available Vehicles Report");
        availableVehiclesButton.setOnAction(e -> generateAvailableVehiclesReport());

        Button customerHistoryButton = new Button("Customer Rental History");
        customerHistoryButton.setOnAction(e -> generateCustomerRentalHistory());

        Button revenueReportButton = new Button("Revenue Report");
        revenueReportButton.setOnAction(e -> generateRevenueReport());

        Button exportCSVButton = new Button("Export Report to CSV");
        exportCSVButton.setOnAction(e -> {
            try {
                exportReportToCSV();
            } catch (IOException ex) {
                showAlert("Failed to export report.");
                ex.printStackTrace();
            }
        });

        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(e -> primaryStage.setScene(dashboardScene));

        layout.getChildren().addAll(title, availableVehiclesButton, customerHistoryButton,
                revenueReportButton, exportCSVButton, backButton);

        reportScene = new Scene(layout, 600, 400);

        reportScene.getStylesheets().add(getClass().getResource("/report.css").toExternalForm());

    }

    // Generate available vehicles report
    private void generateAvailableVehiclesReport() {
        ObservableList<Vehicle> availableVehicles = dbConnector.getAvailableVehicles();

        // Create pie chart
        PieChart pieChart = createPieChart();
        Stage pieStage = new Stage();
        pieStage.setTitle("Available Vehicles Report");
        pieStage.setScene(new Scene(pieChart, 600, 400));
        pieStage.show();

        StringBuilder report = new StringBuilder("Available Vehicles:\n");
        for (Vehicle vehicle : availableVehicles) {
            report.append(vehicle.toString()).append("\n");
        }

        // Show report in alert
        showAlert(report.toString());
    }

    // Generate customer rental history report
    private void generateCustomerRentalHistory() {
        ObservableList<Booking> allBookings = dbConnector.getAllBookings();

        if (allBookings.isEmpty()) {
            showAlert("No booking history available.");
            return;
        }

        // Create bar chart
        BarChart<String, Number> barChart = createCustomerRentalHistoryChart();
        Stage barStage = new Stage();
        barStage.setTitle("Customer Rental History");
        barStage.setScene(new Scene(barChart, 800, 600));
        barStage.show();
    }

    // Create bar chart for customer rental history
    private BarChart<String, Number> createCustomerRentalHistoryChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        xAxis.setLabel("Customer");
        yAxis.setLabel("Number of Rentals");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rentals by Customer");

        for (Customer customer : customerList) {
            int rentalCount = (int) bookingList.stream()
                    .filter(b -> b.getCustomerId().equals(String.valueOf(customer.getCustomerId()))).count();
            series.getData().add(new XYChart.Data<>(customer.getCustomerName(), rentalCount));
        }

        barChart.getData().add(series);
        return barChart;
    }

    // Generate revenue report
    private void generateRevenueReport() {
        double totalRevenue = 0.0;
        for (Booking booking : bookingList) {
            Vehicle vehicle = dbConnector.getVehicleById(Integer.parseInt(booking.getVehicleId()));
            if (vehicle != null) {
                long duration = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
                totalRevenue += vehicle.getRentalPrice() * duration;
            }
        }

        // Create line chart
        LineChart<Number, Number> lineChart = createRevenueLineChart();
        Stage lineStage = new Stage();
        lineStage.setTitle("Revenue Report");
        lineStage.setScene(new Scene(lineChart, 800, 600));
        lineStage.show();

        showAlert("Total Revenue: R" + totalRevenue);
    }

    // Create line chart for revenue visualization
    private LineChart<Number, Number> createRevenueLineChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        xAxis.setLabel("Rental Period (Month)");
        yAxis.setLabel("Total Revenue (R)");

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Monthly Revenue");

        for (int month = 1; month <= 12; month++) {
            double monthlyRevenue = 0;

            for (Booking booking : bookingList) {
                if (booking.getStartDate().getMonthValue() == month) {
                    Vehicle vehicle = dbConnector.getVehicleById(Integer.parseInt(booking.getVehicleId()));
                    long duration = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
                    if (vehicle != null) {
                        monthlyRevenue += vehicle.getRentalPrice() * duration;
                    }
                }
            }
            series.getData().add(new XYChart.Data<>(month, monthlyRevenue));
        }

        lineChart.getData().add(series);
        return lineChart;
    }

    // Export report to CSV
    private void exportReportToCSV() throws IOException {
        String csvFile = "report.csv";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            writer.write("Report,Details\n");
            for (Vehicle vehicle : vehicleList) {
                writer.write("Vehicle Added," + vehicle.toString() + "\n");
            }
            for (Customer customer : customerList) {
                writer.write("Customer Added," + customer.toString() + "\n");
            }
            for (Booking booking : bookingList) {
                writer.write("Vehicle Booked," + booking.toString() + "\n");
            }
            showAlert("Report exported to " + csvFile);
        }
    }

    private void refreshVehicleList() {
        vehicleList.clear();
        vehicleList.addAll(dbConnector.getAllVehicles());
    }

    private void refreshCustomerList() {
        customerList.clear();
        customerList.addAll(dbConnector.getAllCustomers());
    }

    private void refreshBookingList() {
        bookingList.clear();
        bookingList.addAll(dbConnector.getAllBookings());
    }

    // Create pie chart for available vehicles
    private PieChart createPieChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        ObservableList<Vehicle> availableVehicles = dbConnector.getAvailableVehicles();

        for (Vehicle vehicle : availableVehicles) {
            pieChartData.add(new PieChart.Data(vehicle.getBrandModel(), 1)); // Assuming each vehicle counts as 1
        }

        PieChart pieChart = new PieChart(pieChartData);
        pieChart.setTitle("Available Vehicles");
        return pieChart;
    }

    // Show alert messages
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Method to log actions in the CSV file
    private void saveToCSV(String action, String... details) {
        String csvFile = "actions_log.csv";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, true))) {
            writer.write(action + "," + String.join(",", details) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Main method to launch the application
    public static void main(String[] args) {
        launch(args);
    }
}

/// Database connector class for database operations
class DbConnector {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/vehicle_rental_system?useSSL=false";
    private static final String DB_USERNAME = "root"; // Update with your DB username
    private static final String DB_PASSWORD = "Katleho@0210"; // Update with your DB password

    // Initializes the database and creates the necessary tables
    public void initializeDatabase() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS vehicle_rental_system");
            stmt.executeUpdate("USE vehicle_rental_system");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL, role ENUM('Admin', 'Employee') NOT NULL)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS vehicles (vehicle_id INT AUTO_INCREMENT PRIMARY KEY, brand VARCHAR(50) NOT NULL, model VARCHAR(50) NOT NULL, category VARCHAR(50) NOT NULL, rental_price DECIMAL(10, 2) NOT NULL, availability_status ENUM('Available', 'Not Available') NOT NULL)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS customers (customer_id INT AUTO_INCREMENT PRIMARY KEY, customer_name VARCHAR(100) NOT NULL, contact_info VARCHAR(150) NOT NULL, license_number VARCHAR(50) NOT NULL UNIQUE)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS bookings (booking_id INT AUTO_INCREMENT PRIMARY KEY, vehicle_id INT NOT NULL, customer_id INT NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL, booking_date DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE, FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS payments (payment_id INT AUTO_INCREMENT PRIMARY KEY, booking_id INT NOT NULL, amount DECIMAL(10, 2) NOT NULL, payment_date DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }

    public boolean isUsernameAvailable(String username) {
        String query = "SELECT * FROM users WHERE username = ?";
        try (Connection connection = connect(); PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return !rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void registerUser(String username, String password, String role) {
        String query = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean validateUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection connection = connect(); PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void addVehicle(String brandModel, double rentalPrice, boolean availability, String category) {
        try (Connection connection = connect()) {
            String[] parts = brandModel.split(" ", 2);
            String query = "INSERT INTO vehicles (brand, model, category, rental_price, availability_status) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, parts[0]);
                stmt.setString(2, parts.length > 1 ? parts[1] : "");
                stmt.setString(3, category); // Set the category here
                stmt.setDouble(4, rentalPrice);
                stmt.setString(5, availability ? "Available" : "Not Available");
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void updateVehicle(int vehicleId, String brandModel, double rentalPrice, boolean availability, String category) {
        try (Connection connection = connect()) {
            String[] parts = brandModel.split(" ", 2);
            String query = "UPDATE vehicles SET brand = ?, model = ?, category = ?, rental_price = ?, availability_status = ? WHERE vehicle_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, parts[0]);
                stmt.setString(2, parts.length > 1 ? parts[1] : "");
                stmt.setString(3, category); // Include category
                stmt.setDouble(4, rentalPrice);
                stmt.setString(5, availability ? "Available" : "Not Available");
                stmt.setInt(6, vehicleId); // Correctly setting vehicleId here
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public Vehicle getVehicleById(int id) {
        try (Connection connection = connect()) {
            String query = "SELECT * FROM vehicles WHERE vehicle_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new Vehicle(
                            rs.getInt("vehicle_id"),
                            rs.getString("brand"),
                            rs.getString("model"),
                            rs.getString("category"),
                            rs.getDouble("rental_price"),
                            rs.getString("availability_status")
                    );
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void deleteVehicle(Vehicle vehicle) {
        try (Connection connection = connect()) {
            String query = "DELETE FROM vehicles WHERE vehicle_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, vehicle.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void addCustomer(String customerName, String contactInfo, String licenseNumber) {
        try (Connection connection = connect()) {
            String query = "INSERT INTO customers (customer_name, contact_info, license_number) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, customerName);
                stmt.setString(2, contactInfo);
                stmt.setString(3, licenseNumber);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void updateCustomer(int customerId, String customerName, String contactInfo, String licenseNumber) {
        try (Connection connection = connect()) {
            String query = "UPDATE customers SET customer_name = ?, contact_info = ?, license_number = ? WHERE customer_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, customerName);
                stmt.setString(2, contactInfo);
                stmt.setString(3, licenseNumber);
                stmt.setInt(4, customerId);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteCustomer(Customer customer) {
        try (Connection connection = connect()) {
            String query = "DELETE FROM customers WHERE customer_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, customer.getCustomerId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void bookVehicle(String vehicleId, String customerId, LocalDate startDate, LocalDate endDate) {
        try (Connection connection = connect()) {
            String query = "INSERT INTO bookings (vehicle_id, customer_id, start_date, end_date, booking_date) VALUES (?, ?, ?, ?, NOW())";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, vehicleId);
                stmt.setString(2, customerId);
                stmt.setDate(3, Date.valueOf(startDate));
                stmt.setDate(4, Date.valueOf(endDate));
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void updateBooking(String bookingId, LocalDate startDate, LocalDate endDate) {
        try (Connection connection = connect()) {
            String query = "UPDATE bookings SET start_date = ?, end_date = ? WHERE booking_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setDate(1, Date.valueOf(startDate));
                stmt.setDate(2, Date.valueOf(endDate));
                stmt.setString(3, bookingId);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteBooking(Booking booking) {
        try (Connection connection = connect()) {
            String query = "DELETE FROM bookings WHERE booking_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, booking.getBookingId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void processPayment(String bookingId, double amount) {
        try (Connection connection = connect()) {
            String query = "INSERT INTO payments (booking_id, amount, payment_date) VALUES (?, ?, NOW())";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, bookingId);
                stmt.setDouble(2, amount);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public ObservableList<Vehicle> getAllVehicles() {
        ObservableList<Vehicle> vehicleList = FXCollections.observableArrayList();
        try (Connection connection = connect()) {
            String query = "SELECT * FROM vehicles";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Vehicle vehicle = new Vehicle(
                            rs.getInt("vehicle_id"),
                            rs.getString("brand"),
                            rs.getString("model"),
                            rs.getString("category"),
                            rs.getDouble("rental_price"),
                            rs.getString("availability_status")
                    );
                    vehicleList.add(vehicle);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return vehicleList;
    }

    public ObservableList<Customer> getAllCustomers() {
        ObservableList<Customer> customerList = FXCollections.observableArrayList();
        try (Connection connection = connect()) {
            String query = "SELECT * FROM customers";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Customer customer = new Customer(
                            rs.getInt("customer_id"),
                            rs.getString("customer_name"),
                            rs.getString("contact_info"),
                            rs.getString("license_number")
                    );
                    customerList.add(customer);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return customerList;
    }

    public ObservableList<Booking> getAllBookings() {
        ObservableList<Booking> bookingList = FXCollections.observableArrayList();
        try (Connection connection = connect()) {
            String query = "SELECT * FROM bookings";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Booking booking = new Booking(
                            rs.getString("booking_id"),
                            rs.getString("vehicle_id"),
                            rs.getString("customer_id"),
                            rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate(),
                            rs.getDate("booking_date")
                    );
                    bookingList.add(booking);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return bookingList;
    }

    public ObservableList<Vehicle> getAvailableVehicles() {
        ObservableList<Vehicle> availableVehicles = FXCollections.observableArrayList();
        try (Connection connection = connect()) {
            String query = "SELECT * FROM vehicles WHERE availability_status = 'Available'";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Vehicle vehicle = new Vehicle(
                            rs.getInt("vehicle_id"),
                            rs.getString("brand"),
                            rs.getString("model"),
                            rs.getString("category"),
                            rs.getDouble("rental_price"),
                            rs.getString("availability_status")
                    );
                    availableVehicles.add(vehicle);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return availableVehicles;
    }
}

// Vehicle class
class Vehicle {
    private int vehicle_id;
    private String brand;
    private String model;
    private String category; // Ensure category is included here
    private double rentalPrice;
    private boolean available;

    public Vehicle(int id, String brand, String model, String category, double rentalPrice, String availabilityStatus) {
        this.vehicle_id = id;
        this.brand = brand;
        this.model = model;
        this.category = category; // Initialize category
        this.rentalPrice = rentalPrice;
        this.available = "Available".equals(availabilityStatus);
    }

    public int getId() {
        return vehicle_id;
    }

    public String getBrandModel() {
        return brand + " " + model;
    }

    public double getRentalPrice() {
        return rentalPrice;
    }

    public String getCategory() {  // Added getter for category
        return category;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public String toString() {
        return vehicle_id + " - " + getBrandModel() + " - " + category + " - R" + rentalPrice + (available ? " (Available)" : " (Not Available)");
    }
}

// Customer class
class Customer {
    private int customerId;
    private String customerName;
    private String contactInfo;
    private String licenseNumber;

    public Customer(int customerId, String customerName, String contactInfo, String licenseNumber) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.contactInfo = contactInfo;
        this.licenseNumber = licenseNumber;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    @Override
    public String toString() {
        return customerName + " - " + contactInfo + " (License: " + licenseNumber + ")";
    }
}

// Booking class
class Booking {
    private String bookingId;
    private String vehicleId;
    private String customerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Date bookingDate;

    public Booking(String bookingId, String vehicleId, String customerId, LocalDate startDate, LocalDate endDate, Date bookingDate) {
        this.bookingId = bookingId;
        this.vehicleId = vehicleId;
        this.customerId = customerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.bookingDate = bookingDate;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Date getBookingDate() {
        return bookingDate;
    }

    @Override
    public String toString() {
        return "Booking ID: " + bookingId + ", Vehicle ID: " + vehicleId + ", Customer ID: " + customerId +
                ", Dates: " + startDate + " to " + endDate + ", Booking Date: " + bookingDate;
    }
}