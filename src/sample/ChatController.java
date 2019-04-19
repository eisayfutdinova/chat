package sample;


import com.sun.istack.internal.NotNull;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

public class ChatController {

    private static final Logger log = Logger.getLogger(ChatController.class);

    @FXML
    public Button sendButton;

    @FXML
    public TextField message;

    @FXML
    public ListView<String> messageList;

    private Client client;

    public ChatController() {
        UserPreferences user = getUserPreferences();

        while (user.userPort != ChatConstants.getDefaultPort()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Dialog");
            alert.setHeaderText("Look, a Confirmation Dialog");
            alert.setContentText("Нет сервера на текущем порте!");

            Optional<ButtonType> alertButton = alert.showAndWait().filter(buttonType -> buttonType == ButtonType.OK);
            if (alertButton.isPresent()) {
                user = getUserPreferences();
            } else {
                System.exit(0);
            }

        }

        client = new Client(user.userName, this, user.userPort);
        log.info("User - " + client.clientName + " connected to chat");
        try {
            client.makeConnection();
        } catch (IOException ignored) {

        }
    }


    @NotNull
    private UserPreferences getUserPreferences() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("User Preferences Dialog");
        dialog.setHeaderText("To join the chat, enter your\nname and connecting port!");

        // Set the icon
        dialog.setGraphic(new ImageView(this.getClass().getResource("login.png").toString()));

        // Set the button types
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField port = new PasswordField();
        port.setPromptText(String.valueOf(ChatConstants.getDefaultPort()));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Port:"), 0, 1);
        grid.add(port, 1, 1);


        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation using lambda
        username.textProperty().addListener((observable, oldValue, newValue) ->
                loginButton.setDisable(newValue.trim().isEmpty()));

        port.textProperty().addListener((observable, oldValue, newValue) ->
                loginButton.setDisable(!newValue.trim().matches("([0-9])*") || !username.getText().isEmpty()));
        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(username::requestFocus);

        // Convert the result to a username-port-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), port.getText());
            }

            if (dialogButton == ButtonType.CANCEL || dialogButton == ButtonType.CLOSE)
                System.exit(0);

            return null;
        });


        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(usernamePassword -> log.info("User " + usernamePassword.getKey() + " in PORT = " + ChatConstants.getDefaultPort() + result.get().getValue() + " created. "));

        if (result.isPresent() && result.get().getValue().isEmpty())
            return new UserPreferences(result.get().getKey(), ChatConstants.getDefaultPort());

        return new UserPreferences(result.get().getKey(), Integer.parseInt(result.get().getValue()));
    }

    @FXML
    public void onEnter() {
        messageSender();
    }

    @FXML
    public void sendMessage() {
        messageSender();
    }

    private void messageSender() {
        if (!message.getText().isEmpty()) {
            String sendTime = " " + LocalDateTime.now().getHour() + ":" + LocalDateTime.now().getMinute() + ":";
            String seconds = String.valueOf(LocalDateTime.now().getSecond());
            if (seconds.length() < 2)
                sendTime += 0 + seconds + "  ";
            else sendTime += seconds + "  ";
            client.sendMessage(sendTime + "[" + client.clientName + "]" + ": " + message.getText());
            message.clear();
        }
    }

    void messageReceiver(String message) {
        messageList.getItems().add(message);
    }


    private class UserPreferences {
        final String userName;
        final int userPort;

        UserPreferences(String userName, int userPort) {
            this.userName = userName;
            this.userPort = userPort;
        }

        @Override
        public String toString() {
            return "User name - " + userName + "\nUser port - " + userPort;
        }
    }

}

