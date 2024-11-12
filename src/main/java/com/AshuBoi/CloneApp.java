package com.AshuBoi;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class CloneApp extends Application {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final int PORT = 8080;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter clientOut;
    private Robot robot;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Remote VNC Connect Application");
        showHomePage(primaryStage);
    }

    private void showHomePage(Stage primaryStage) {
        Label welcomeLabel = new Label("Welcome to Remote VNC Connect");
        Button hostSessionButton = new Button("Host a Session");
        Button joinSessionButton = new Button("Join a Session");

        VBox homeLayout = new VBox(15, welcomeLabel, hostSessionButton, joinSessionButton);
        homeLayout.setAlignment(Pos.CENTER);
        homeLayout.setPadding(new Insets(20));

        Scene homeScene = new Scene(homeLayout, 400, 300);
        primaryStage.setScene(homeScene);
        primaryStage.show();

        hostSessionButton.setOnAction(e -> startHosting(primaryStage));
        joinSessionButton.setOnAction(e -> startClient(primaryStage));
    }

    private void startHosting(Stage primaryStage) {
        TextArea statusArea = new TextArea();
        statusArea.setEditable(false);
        statusArea.appendText("Registering with broker server...\n");

        VBox hostLayout = new VBox(15, statusArea);
        hostLayout.setAlignment(Pos.CENTER);
        hostLayout.setPadding(new Insets(20));

        Scene hostScene = new Scene(hostLayout, 400, 300);
        primaryStage.setScene(hostScene);
        primaryStage.show();

        executorService.execute(() -> {
            try {
                String sessionCode = registerWithBroker();
                Platform.runLater(() -> statusArea.appendText("Session Code: " + sessionCode + "\nShare this code with the client.\nWaiting for connection...\n"));

                serverSocket = new ServerSocket(PORT);
                clientSocket = serverSocket.accept();
                clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                robot = new Robot();
                Platform.runLater(() -> statusArea.appendText("Client connected!\n"));
                startScreenSharing();
                listenForClientCommands();
            } catch (IOException | AWTException e) {
                Platform.runLater(() -> statusArea.appendText("Error while hosting: " + e.getMessage() + "\n"));
            }
        });
    }

    private String registerWithBroker() throws IOException {
        // Make HTTP POST request to broker server to register
        URL url = new URL("http://localhost:3000/registerHost");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Send JSON data (e.g., IP and port)
        String jsonInputString = "{\"port\": " + PORT + "}";
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Read response (session code)
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            // Assuming the response contains the session code
            return response.toString();
        }
    }

    private void startClient(Stage primaryStage) {
        Label hostLabel = new Label("Enter Session Code:");
        TextField sessionCodeField = new TextField();
        Button connectButton = new Button("Connect");
        Button backButton = new Button("Back");
        TextArea statusArea = new TextArea();
        statusArea.setEditable(false);

        VBox connectLayout = new VBox(15, hostLabel, sessionCodeField, connectButton, backButton, statusArea);
        connectLayout.setAlignment(Pos.CENTER);
        connectLayout.setPadding(new Insets(20));

        Scene connectScene = new Scene(connectLayout, 400, 500);
        primaryStage.setScene(connectScene);
        primaryStage.show();

        backButton.setOnAction(e -> showHomePage(primaryStage));

        connectButton.setOnAction(e -> {
            String sessionCode = sessionCodeField.getText();
            if (!sessionCode.isEmpty()) {
                statusArea.appendText("Connecting using session code " + sessionCode + "...\n");
                executorService.execute(() -> connectToHost(sessionCode, statusArea, primaryStage));
            } else {
                statusArea.appendText("Please enter a valid session code.\n");
            }
        });
    }

    private void connectToHost(String sessionCode, TextArea statusArea, Stage primaryStage) {
        executorService.execute(() -> {
            try {
                String hostDetails = getHostDetailsFromBroker(sessionCode);
                String[] details = hostDetails.split(":");
                String host = details[0];
                int port = Integer.parseInt(details[1]);

                clientSocket = new Socket(host, port);
                Platform.runLater(() -> {
                    statusArea.appendText("Connected to host!\n");
                    showRemoteScreen(primaryStage);
                });
            } catch (IOException e) {
                Platform.runLater(() -> statusArea.appendText("Error while connecting: " + e.getMessage() + "\n"));
            }
        });
    }

    private String getHostDetailsFromBroker(String sessionCode) throws IOException {
        // Make HTTP GET request to broker server to get host details
        URL url = new URL("https://your-broker-server.com/connectClient?sessionCode=" + sessionCode);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            // Assuming the response contains the host IP and port
            return response.toString();
        }
    }

    private void startScreenSharing() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    BufferedImage screenCapture = captureScreen();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(screenCapture, "jpg", baos);
                    String encodedImage = Base64.getEncoder().encodeToString(baos.toByteArray());
                    clientOut.println(encodedImage);
                }
            } catch (IOException | AWTException e) {
                Platform.runLater(() -> System.err.println("Error during screen sharing: " + e.getMessage()));
            }
        }, 0, 100, TimeUnit.MILLISECONDS);  // Adjust the rate as needed
    }


    private BufferedImage captureScreen() throws AWTException {
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        return robot.createScreenCapture(screenRect);
    }

    private void showRemoteScreen(Stage primaryStage) {
        ImageView screenView = new ImageView();
        screenView.setFitWidth(600);
        screenView.setFitHeight(400);

        VBox screenLayout = new VBox(15, screenView);
        screenLayout.setAlignment(Pos.CENTER);
        screenLayout.setPadding(new Insets(20));

        Scene screenScene = new Scene(screenLayout, 600, 500);
        primaryStage.setScene(screenScene);
        primaryStage.show();

        executorService.execute(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    final String encodedImage = line;
                    Platform.runLater(() -> {
                        try {
                            byte[] imageBytes = Base64.getDecoder().decode(encodedImage);
                            InputStream is = new ByteArrayInputStream(imageBytes);
                            Image image = new Image(is);
                            screenView.setImage(image);
                        } catch (IllegalArgumentException e) {
                            System.err.println("Error decoding image: " + e.getMessage());
                        }
                    });
                }
            } catch (IOException e) {
                System.err.println("Error while receiving screen data: " + e.getMessage());
            }
        });
    }

    private void listenForClientCommands() {
        executorService.execute(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String command;
                while ((command = in.readLine()) != null) {
                    if (command.startsWith("MOUSE")) {
                        String[] parts = command.split(" ");
                        int x = (int) Double.parseDouble(parts[1]);
                        int y = (int) Double.parseDouble(parts[2]);
                        robot.mouseMove(x, y);
                        if (parts[3].equalsIgnoreCase("PRIMARY")) {
                            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                        }
                    } else if (command.startsWith("KEY")) {
                        String[] parts = command.split(" ");
                        int keyCode = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(parts[1].charAt(0));
                        robot.keyPress(keyCode);
                        robot.keyRelease(keyCode);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error while listening for client commands: " + e.getMessage());
            }
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        executorService.shutdownNow();
    }
}
