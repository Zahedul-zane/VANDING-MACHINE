package com.vending.ui;

import com.vending.model.Product;
import com.vending.model.VendingMachine;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.SnapshotParameters;
import javafx.embed.swing.SwingFXUtils;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.imageio.ImageIO;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.net.URL;
import java.util.ResourceBundle;

public class VendingController implements Initializable {

    @FXML private Label balanceLabel;
    @FXML private Label totalDueLabel;
    @FXML private Label mainDisplay;
    @FXML private FlowPane productPane;
    @FXML private ListView<String> selectionListView;
    @FXML private ListView<String> logListView;
    @FXML private HBox dispenserTray;
    @FXML private HBox receiptSlot;
    @FXML private StackPane receiptOverlay;
    @FXML private VBox floatingReceiptContainer;

    private VendingMachine machine;
    private final StringBuilder inputBuffer = new StringBuilder();
    private String lastReceiptText = "";
    private double lastSubtotal = 0;
    private double lastPaid = 0;
    private java.util.List<Product> lastPurchasedItems = new java.util.ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        machine = VendingMachine.getInstance();
        
        // Bind balance label
        machine.currentBalanceProperty().addListener((obs, oldVal, newVal) -> {
            balanceLabel.setText("$" + String.format("%.2f", newVal.doubleValue()));
        });
        
        // Custom cell factory for selection list
        selectionListView.setItems(javafx.collections.FXCollections.observableArrayList());
        machine.getSelection().addListener((javafx.collections.ListChangeListener<Product>) c -> {
            selectionListView.getItems().clear();
            for (Product p : machine.getSelection()) {
                selectionListView.getItems().add(p.getName() + " - $" + String.format("%.2f", p.getPrice()));
            }
            totalDueLabel.setText("$" + String.format("%.2f", machine.getGrandTotal()));
        });

        // Bind logs
        logListView.setItems(machine.getLogs());

        // Load products
        loadProducts();
    }

    private void loadProducts() {
        productPane.getChildren().clear();
        for (Product product : machine.getInventory().getProducts()) {
            productPane.getChildren().add(createProductCard(product));
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(160);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setCursor(javafx.scene.Cursor.HAND);

        // Click to add to selection
        card.setOnMouseClicked(e -> {
            try {
                machine.addToSelection(product);
                animateSelect(card);
            } catch (com.vending.model.OutOfStockException ex) {
                mainDisplay.setText("OUT OF STOCK");
            }
        });

        Label codeBadge = new Label(product.getCode());
        codeBadge.setStyle("-fx-background-color: #ffcc33; -fx-text-fill: #1a1400; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 5;");

        ImageView imageView = new ImageView();
        try {
            Image img = new Image(getClass().getResourceAsStream("/com/vending/images/" + product.getIconName()));
            imageView.setImage(img);
            imageView.setFitWidth(100);
            imageView.setFitHeight(100);
            imageView.setPreserveRatio(true);
            imageView.setEffect(new javafx.scene.effect.DropShadow(10, javafx.scene.paint.Color.BLACK));
        } catch (Exception ex) {
            System.err.println("Could not load image: " + product.getIconName());
        }

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");

        Label priceLabel = new Label("$" + String.format("%.2f", product.getPrice()));
        priceLabel.getStyleClass().add("product-price");

        Label stockLabel = new Label("Stock: " + machine.getInventory().getQuantity(product));
        stockLabel.setStyle("-fx-text-fill: #666;");

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.setPrefSize(120, 120);
        imageContainer.setMinSize(120, 120);
        imageContainer.setMaxSize(120, 120);
        imageContainer.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(codeBadge, imageContainer, nameLabel, priceLabel, stockLabel);
        return card;
    }

    private void animateSelect(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(0.95); st.setToY(0.95);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }

    @FXML
    private void handleNumKey(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        if (inputBuffer.length() < 5) { // Limit input length
            inputBuffer.append(btn.getText());
            updateDisplayWithInput();
        }
    }

    private void updateDisplayWithInput() {
        if (inputBuffer.length() == 0) {
            mainDisplay.setText("ENTER AMOUNT");
        } else {
            mainDisplay.setText("$" + inputBuffer.toString());
        }
    }

    @FXML
    private void handleNumClear() {
        inputBuffer.setLength(0);
        updateDisplayWithInput();
    }

    @FXML
    private void handleNumOk() {
        if (inputBuffer.length() > 0) {
            try {
                double amount = Double.parseDouble(inputBuffer.toString());
                machine.insertMoney(amount);
                mainDisplay.setText("ACCEPTED: $" + String.format("%.2f", amount));
                inputBuffer.setLength(0);
            } catch (NumberFormatException e) {
                mainDisplay.setText("INVALID AMOUNT");
            }
        }
    }

    @FXML
    private void handleCheckout() {
        if (machine.getSelection().isEmpty()) {
            mainDisplay.setText("SELECT ITEMS FIRST");
            return;
        }

        double subtotal = machine.getTotalPrice();
        double grandTotal = machine.getGrandTotal();
        double balance = machine.currentBalanceProperty().get();

        if (balance < grandTotal) {
            mainDisplay.setText("NEED $" + String.format("%.2f", grandTotal - balance));
            return;
        }

        try {
            if (machine.processCheckout()) {
                // Store purchase data before clearing selection
                this.lastSubtotal = subtotal;
                this.lastPaid = balance;
                this.lastPurchasedItems = new java.util.ArrayList<>(machine.getSelection());
                
                exportReceiptAsImage(subtotal, balance, lastPurchasedItems);
                generateBill(subtotal, balance, lastPurchasedItems);
                dispenseAll();
                machine.clearSelection();
                loadProducts();
                mainDisplay.setText("THANK YOU!");
            }
        } catch (com.vending.model.InsufficientFundsException e) {
            mainDisplay.setText("NEED " + VendingMachine.formatCurrency(e.getMissingAmount()));
        } catch (com.vending.model.OutOfStockException e) {
            mainDisplay.setText("OUT OF STOCK");
            loadProducts();
        } catch (com.vending.model.VendingException e) {
            mainDisplay.setText("ERROR: " + e.getMessage());
        }
    }

    private void generateBill(double subtotal, double paid, java.util.List<Product> items) {
        double vat = subtotal * VendingMachine.VAT_RATE;
        double grandTotal = subtotal + vat;
        
        machine.getLogs().add(0, "----------------------------");
        machine.getLogs().add(0, "CHANGE: $" + String.format("%.2f", paid - grandTotal));
        machine.getLogs().add(0, "DEBIT (Charge): $" + String.format("%.2f", grandTotal));
        machine.getLogs().add(0, "CREDIT (Paid): $" + String.format("%.2f", paid));
        machine.getLogs().add(0, "TOTAL BILL: $" + String.format("%.2f", grandTotal));
        machine.getLogs().add(0, "----------------------------");
        
        // Add items to log (Full Data)
        for (int i = items.size() - 1; i >= 0; i--) {
            Product p = items.get(i);
            machine.getLogs().add(0, "> [" + p.getCode() + "] " + p.getName() + ": $" + String.format("%.2f", p.getPrice()));
        }
        
        machine.getLogs().add(0, "VAT (5%): $" + String.format("%.2f", vat));
        machine.getLogs().add(0, "SUBTOTAL: $" + String.format("%.2f", subtotal));
        machine.getLogs().add(0, "*** DIGITAL RECEIPT ***");
        machine.getLogs().add(0, "----------------------------");
    }

    private void dispenseAll() {
        dispenserTray.getChildren().clear();
        for (Product p : machine.getSelection()) {
            ImageView item = new ImageView();
            try {
                item.setImage(new Image(getClass().getResourceAsStream("/com/vending/images/" + p.getIconName())));
                item.setFitWidth(40);
                item.setFitHeight(40);
                item.setPreserveRatio(true);
            } catch (Exception e) {}
            
            dispenserTray.getChildren().add(item);
            
            ScaleTransition st = new ScaleTransition(Duration.millis(500), item);
            st.setFromX(0); st.setFromY(0);
            st.setToX(1); st.setToY(1);
            st.play();
        }
    }

    private void exportReceiptAsImage(double total, double paid, java.util.List<Product> items) {
        VBox receipt = createReceiptNode(total, paid, items);
        
        // Capture snapshot on FX thread

        // Capture snapshot on FX thread
        Platform.runLater(() -> {
            try {
                // Critical: Create a Scene to trigger CSS and layout calculations
                new javafx.scene.Scene(receipt);
                receipt.applyCss();
                receipt.layout();
                
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(javafx.scene.paint.Color.web("#050a08"));
                WritableImage image = receipt.snapshot(params, null);
                
                // Save image on a background thread to keep UI responsive
                new Thread(() -> {
                    try {
                        File dir = new File("receipts");
                        if (!dir.exists()) dir.mkdir();
                        
                        String fileName = "ORION_Receipt_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png";
                        File file = new File(dir, fileName);
                        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                        System.out.println("Receipt exported successfully: " + file.getAbsolutePath());
                        
                        Platform.runLater(() -> mainDisplay.setText("RECEIPT EXPORTED"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> mainDisplay.setText("EXPORT ERROR"));
                    }
                }).start();
                
                // Show physical receipt in slot (FX Thread)
                receiptSlot.getChildren().clear();
                VBox paper = new VBox(5);
                paper.setPrefSize(60, 70);
                paper.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                paper.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 2; -fx-border-color: #ccc; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 5);");
                
                Label paperTitle = new Label("RECEIPT");
                paperTitle.setStyle("-fx-text-fill: #333; -fx-font-size: 8; -fx-font-weight: bold;");
                
                VBox lines = new VBox(3);
                lines.setAlignment(javafx.geometry.Pos.CENTER);
                for (int i = 0; i < 4; i++) {
                    Region line = new Region();
                    line.setPrefHeight(2);
                    line.setPrefWidth(40);
                    line.setStyle("-fx-background-color: #ddd;");
                    lines.getChildren().add(line);
                }
                
                paper.getChildren().addAll(paperTitle, lines);
                receiptSlot.getChildren().add(paper);
                
                TranslateTransition tt = new TranslateTransition(Duration.millis(800), paper);
                tt.setFromY(-80);
                tt.setToY(0);
                tt.play();
                
            } catch (Exception e) {
                e.printStackTrace();
                mainDisplay.setText("SNAPSHOT ERROR");
            }
        });
    }

    @FXML
    private void handleShowFloatingReceipt() {
        // We use the stored purchase data
        VBox displayReceipt = createReceiptNode(lastSubtotal, lastPaid, lastPurchasedItems);
        
        floatingReceiptContainer.getChildren().clear();
        floatingReceiptContainer.getChildren().add(0, displayReceipt);
        
        receiptOverlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), receiptOverlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
        
        ScaleTransition st = new ScaleTransition(Duration.millis(300), displayReceipt);
        st.setFromX(0.5); st.setFromY(0.5);
        st.setToX(1); st.setToY(1);
        st.play();
    }

    @FXML
    private void handleCloseFloatingReceipt() {
        FadeTransition ft = new FadeTransition(Duration.millis(300), receiptOverlay);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> receiptOverlay.setVisible(false));
        ft.play();
    }

    private VBox createReceiptNode(double subtotal, double paid, java.util.List<Product> items) {
        double vatRate = VendingMachine.VAT_RATE;
        double vatAmount = subtotal * vatRate;
        double grandTotal = subtotal + vatAmount;

        StringBuilder textReceipt = new StringBuilder();
        textReceipt.append("ORION VENDING - DIGITAL RECEIPT\n");
        textReceipt.append("----------------------------\n");
        
        VBox receipt = new VBox(20);
        receipt.setStyle("-fx-background-color: #050a08; -fx-padding: 50; -fx-border-color: #ffcc33; -fx-border-width: 3; -fx-background-radius: 5; -fx-border-radius: 5;");
        receipt.setPrefWidth(450);
        receipt.setAlignment(javafx.geometry.Pos.CENTER);

        VBox header = new VBox(5);
        header.setAlignment(javafx.geometry.Pos.CENTER);
        Label brand = new Label("ORION VENDING");
        brand.setStyle("-fx-text-fill: #ffcc33; -fx-font-size: 32; -fx-font-weight: bold; -fx-font-family: 'Segoe UI Bold';");
        Label slogan = new Label("PREMIUM REFRESHMENT SYSTEMS");
        slogan.setStyle("-fx-text-fill: #555; -fx-font-size: 10; -fx-letter-spacing: 2;");
        header.getChildren().addAll(brand, slogan);

        Separator topSep = new Separator();
        topSep.setStyle("-fx-background-color: #ffcc33; -fx-opacity: 0.2;");

        VBox infoBox = new VBox(15);
        Label receiptId = new Label("RECEIPT #: " + System.currentTimeMillis() / 1000);
        receiptId.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        
        VBox itemsBox = new VBox(8);
        for (Product p : items) {
            HBox row = new HBox(10);
            Label code = new Label("[" + p.getCode() + "]");
            code.setStyle("-fx-text-fill: #ffcc33; -fx-font-size: 13; -fx-font-family: 'JetBrains Mono';");
            Label name = new Label(p.getName());
            name.setStyle("-fx-text-fill: #eee; -fx-font-size: 15;");
            Label price = new Label("$" + String.format("%.2f", p.getPrice()));
            price.setStyle("-fx-text-fill: #ffcc33; -fx-font-size: 15; -fx-font-family: 'JetBrains Mono';");
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            row.getChildren().addAll(code, name, spacer, price);
            itemsBox.getChildren().add(row);
            textReceipt.append("[").append(p.getCode()).append("] ").append(p.getName()).append(" - $").append(String.format("%.2f", p.getPrice())).append("\n");
        }
        textReceipt.append("----------------------------\n");
        textReceipt.append("SUBTOTAL: $").append(String.format("%.2f", subtotal)).append("\n");
        textReceipt.append("VAT (5%): $").append(String.format("%.2f", vatAmount)).append("\n");
        textReceipt.append("TOTAL BILL: $").append(String.format("%.2f", grandTotal)).append("\n");
        textReceipt.append("CREDIT: $").append(String.format("%.2f", paid)).append("\n");
        textReceipt.append("DEBIT: $").append(String.format("%.2f", grandTotal)).append("\n");
        textReceipt.append("CHANGE: $").append(String.format("%.2f", paid - grandTotal)).append("\n");
        textReceipt.append("----------------------------\n");
        textReceipt.append("THANK YOU FOR YOUR PATRONAGE\n");
        textReceipt.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        this.lastReceiptText = textReceipt.toString();
        infoBox.getChildren().addAll(receiptId, itemsBox);

        VBox totalsBox = new VBox(10);
        totalsBox.setStyle("-fx-background-color: rgba(255, 204, 51, 0.05); -fx-padding: 15; -fx-background-radius: 10;");
        totalsBox.getChildren().addAll(
            createReceiptRow("SUBTOTAL", subtotal, "#aaa", 14),
            createReceiptRow("VAT (5%)", vatAmount, "#ff5555", 14),
            new Separator(),
            createReceiptRow("TOTAL BILL", grandTotal, "#ffcc33", 22),
            createReceiptRow("CREDIT", paid, "#eee", 14),
            createReceiptRow("DEBIT", grandTotal, "#ff5555", 14),
            createReceiptRow("CHANGE DUE", paid - grandTotal, "#00ffa3", 18)
        );

        VBox footer = new VBox(10);
        footer.setAlignment(javafx.geometry.Pos.CENTER);
        Label thankYou = new Label("THANK YOU FOR YOUR PATRONAGE");
        thankYou.setStyle("-fx-text-fill: #ffcc33; -fx-font-size: 12; -fx-font-weight: bold;");
        Label dateLabel = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy HH:mm")));
        dateLabel.setStyle("-fx-text-fill: #444; -fx-font-size: 10;");
        footer.getChildren().addAll(thankYou, dateLabel);

        receipt.getChildren().addAll(header, topSep, infoBox, totalsBox, footer);
        return receipt;
    }

    private HBox createReceiptRow(String label, double value, String color, int size) {
        HBox row = new HBox();
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        Label v = new Label("$" + String.format("%.2f", value));
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        row.getChildren().addAll(l, spacer, v);
        row.setAlignment(javafx.geometry.Pos.CENTER);
        return row;
    }

    @FXML
    private void handleCopyReceipt() {
        if (lastReceiptText == null || lastReceiptText.isEmpty()) {
            mainDisplay.setText("NO RECEIPT TO COPY");
            return;
        }
        
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(lastReceiptText);
        clipboard.setContent(content);
        
        mainDisplay.setText("COPIED TO CLIPBOARD");
    }

    @FXML private void handleRefund() { 
        machine.refund(); 
        dispenserTray.getChildren().clear();
        receiptSlot.getChildren().clear();
        handleNumClear();
        mainDisplay.setText("WELCOME");
    }
}




