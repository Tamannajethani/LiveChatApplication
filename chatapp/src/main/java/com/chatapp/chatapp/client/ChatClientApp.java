package com.chatapp.chatapp.client;

import com.chatapp.chatapp.dto.MessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Application;
import javafx.geometry.Insets;package com.chatapp.chatapp.client;

import com.chatapp.chatapp.dto.MessageDTO;
import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.security.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Hyperlink;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import java.io.File;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;


public class ChatClientApp extends Application {
    private VBox chatBox;
    private StompSession stompSession;
    private String loggedInUser; // current logged in username

    // --- Backend endpoints (adjust if yours differ)
    private static final String BASE_HTTP = "http://localhost:8080";
    private static final String LOGIN_URL = BASE_HTTP + "/users/login";
    private static final String REGISTER_URL = BASE_HTTP + "/users/register";
    private static final String FILE_UPLOAD_URL = BASE_HTTP + "/files/upload"; // returns public URL (String/JSON)
    private static final String WS_URL = "ws://localhost:8080/chat/websocket";
    private static final String WS_APP_DEST = "/app/sendMessage";
    private static final String WS_TOPIC = "/topic/messages";

    private Node renderMessage(MessageDTO msg) {
        VBox msgBox = new VBox();
        msgBox.setSpacing(5);

        Label senderLabel = new Label(msg.getSender() + " (" + msg.getSentAt() + ")");
        senderLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");

        Node contentNode;

        // Case 1: File message
        if (msg.getFileUrl() != null && !msg.getFileUrl().isEmpty()) {
            // Ensure absolute URL
            String tmpUrl = msg.getFileUrl();
            if (!tmpUrl.startsWith("http")) {
                tmpUrl = "http://localhost:8080" + tmpUrl;
            }
            final String url = tmpUrl; // ✅ effectively final for lambdas

            String type = msg.getFileType();

            if (type != null) {
                if (type.startsWith("image/")) {
                    // Show image
                    Image image = new Image(url, 200, 200, true, true);
                    ImageView imageView = new ImageView(image);
                    imageView.setPreserveRatio(true);
                    contentNode = imageView;

                } else if (type.startsWith("video/")) {
                    // Show video player
                    Media media = new Media(url);
                    MediaPlayer player = new MediaPlayer(media);
                    MediaView mediaView = new MediaView(player);
                    mediaView.setFitWidth(300);
                    mediaView.setFitHeight(200);
                    Button playBtn = new Button("▶ Play");
                    playBtn.setOnAction(e -> player.play());
                    VBox videoBox = new VBox(mediaView, playBtn);
                    videoBox.setSpacing(5);
                    contentNode = videoBox;

                } else {
                    // Generic file → download link
                    Hyperlink link = new Hyperlink("📎 " +
                            (msg.getContent() != null ? msg.getContent() : "Download File"));
                    link.setOnAction(e -> getHostServices().showDocument(url));
                    contentNode = link;
                }
            } else {
                // fallback if no type
                Hyperlink link = new Hyperlink("📎 File");
                link.setOnAction(e -> getHostServices().showDocument(url));
                contentNode = link;
            }

        } else {
            // Case 2: Normal text message
            Label textLabel = new Label(msg.getContent());
            textLabel.setWrapText(true);
            textLabel.setStyle("-fx-font-size: 14px;");
            contentNode = textLabel;
        }

        msgBox.getChildren().addAll(senderLabel, contentNode);
        return msgBox;
    }

    @Override
    public void start(Stage stage) {
        showLoginScreen(stage);
    }

    // ================= LOGIN SCREEN =================
    private void showLoginScreen(Stage stage) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        Button registerButton = new Button("Register");
        Label statusLabel = new Label();

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (authenticateUser(username, password)) {
                loggedInUser = username;
                showChatScreen(stage);
                connectToWebSocket();
            } else {
                statusLabel.setText("❌ Invalid login. Try again.");
            }
        });

        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (registerUser(username, password)) {
                statusLabel.setText("✅ Registered successfully! Please log in.");
            } else {
                statusLabel.setText("❌ Registration failed.");
            }
        });

        VBox layout = new VBox(10, usernameField, passwordField, loginButton, registerButton, statusLabel);
        layout.setPadding(new Insets(10));

        stage.setScene(new Scene(layout, 300, 220));
        stage.setTitle("Login");
        stage.show();
    }

    // ================= CHAT SCREEN =================
    private void showChatScreen(Stage stage) {
        chatBox = new VBox(10);
        ScrollPane scrollPane = new ScrollPane(chatBox);
        scrollPane.setFitToWidth(true);

        // Who are we chatting with?
        TextField receiverField = new TextField();
        receiverField.setPromptText("Receiver username (or leave as 'All')");
        receiverField.setText("All");

        TextField inputField = new TextField();
        inputField.setPromptText("Type your message...");

        Button sendButton = new Button("Send");
        Button sendFileButton = new Button("Send File");

        sendButton.setOnAction(e -> {
            String message = inputField.getText().trim();
            String receiver = receiverField.getText().trim();
            if (receiver.isEmpty()) receiver = "All";
            if (!message.isEmpty()) {
                sendTextMessage(message, receiver);
                inputField.clear();
            }
        });

        sendFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose File to Send");
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                String receiver = receiverField.getText().trim();
                if (receiver.isEmpty()) receiver = "All";
                sendFileMessage(file, receiver);
            }
        });

        HBox topBar = new HBox(10, new Label("To:"), receiverField);
        HBox bottomBar = new HBox(10, inputField, sendButton, sendFileButton);

        VBox layout = new VBox(10, topBar, scrollPane, bottomBar);
        layout.setPadding(new Insets(10));

        stage.setScene(new Scene(layout, 520, 360));
        stage.setTitle("Chat - Logged in as " + loggedInUser);
        stage.show();
        receiverField.setOnAction(e -> {
            String receiver = receiverField.getText().trim();
            if (receiver.isEmpty()) receiver = "All";
            loadChatHistory(loggedInUser, receiver);
        });

    }

    // ================== BACKEND AUTH ==================
    private boolean authenticateUser(String username, String password) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            User loginRequest = new User(username, password);
            ResponseEntity<User> response = restTemplate.postForEntity(LOGIN_URL, loginRequest, User.class);
            return response.getStatusCode() == HttpStatus.OK && response.getBody() != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean registerUser(String username, String password) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            User registerRequest = new User(username, password);
            ResponseEntity<User> response = restTemplate.postForEntity(REGISTER_URL, registerRequest, User.class);
            return response.getStatusCode() == HttpStatus.OK && response.getBody() != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================== WEBSOCKET ==================
    private void connectToWebSocket() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converter.setObjectMapper(mapper);
        stompClient.setMessageConverter(converter);

        try {
            WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
            StompHeaders connectHeaders = new StompHeaders();

            stompSession = stompClient.connectAsync(
                    WS_URL,
                    httpHeaders,
                    connectHeaders,
                    new StompSessionHandlerAdapter() {
                        @Override
                        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                            System.out.println("Connected to WebSocket Server!!");
                        }
                    }).get();

            // ✅ Subscribe and handle messages
            stompSession.subscribe(WS_TOPIC, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return MessageDTO.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    MessageDTO msg = (MessageDTO) payload;

                    // Only decrypt if it's a text message
                    if (msg.getContent() != null && (msg.getFileUrl() == null || msg.getFileUrl().isEmpty())) {
                        try {
                            String decrypted = EncryptionUtil.decrypt(msg.getContent());
                            msg.setContent(decrypted);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    javafx.application.Platform.runLater(() -> {
                        Node messageNode = renderMessage(msg);
                        chatBox.getChildren().add(messageNode);
                    });
                }

            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================== SENDING MESSAGES ==================
    private void sendTextMessage(String content, String receiver) {
        if (stompSession != null && stompSession.isConnected()) {
            String encryptedContent = EncryptionUtil.encrypt(content);
            MessageDTO message = new MessageDTO();
            message.setSender(loggedInUser);
            message.setReceiver(receiver);
            message.setContent(encryptedContent);
            message.setSentAt(LocalDateTime.now());
            // file fields remain null
            stompSession.send(WS_APP_DEST, message);

               }
    }

    private void sendFileMessage(File file, String receiver) {
        try {
            // 1) Upload the file via REST (multipart)
            String fileUrl = uploadFile(file);
            if (fileUrl == null || fileUrl.isEmpty()) {
                Label errorLabel = new Label("⚠️ File upload failed.");
                chatBox.getChildren().add(errorLabel);
                return;
            }

            // 2) Send a message with fileUrl + fileType (no encryption on fileUrl)
            if (stompSession != null && stompSession.isConnected()) {
                String fileType = guessMimeType(file.getName());

                MessageDTO message = new MessageDTO();
                message.setSender(loggedInUser);
                message.setReceiver(receiver);
                message.setContent(null); // content optional for file messages
                message.setFileUrl(fileUrl);
                message.setFileType(fileType);
                message.setSentAt(LocalDateTime.now());

                stompSession.send(WS_APP_DEST, message);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Label errorLabel = new Label("⚠️ File upload failed.");
            chatBox.getChildren().add(errorLabel);        }
    }

    // ================== HELPERS ==================
    /**
     * Uploads a file to the backend. Expects backend to return either:
     *  - plain text URL, or
     *  - JSON like {"url":"http://..."}
     */
    private String uploadFile(File file) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:8080/files/upload",
                     requestEntity, String.class);


            String resp = response.getBody().trim();

            // If server returns JSON like {"url":"..."}
            if ((resp.startsWith("{") && resp.endsWith("}")) || (resp.startsWith("[") && resp.endsWith("]"))) {
                ObjectMapper om = new ObjectMapper();
                try {
                    // very small mapper for field "url"
                    @SuppressWarnings("unchecked")
                    var map = om.readValue(resp, java.util.Map.class);
                    Object url = map.get("url");
                    return url != null ? url.toString() : null;
                } catch (Exception ignore) {
                    // fall through to return raw string
                }
            }

            // Otherwise assume it's a plain URL string
            return resp.replace("\"", "");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String guessMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
    private void loadChatHistory(String sender, String receiver) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://localhost:8080/messages/chat?sender=" + sender + "&receiver=" + receiver;

            ResponseEntity<MessageDTO[]> response = restTemplate.getForEntity(url, MessageDTO[].class);
            MessageDTO[] messages = response.getBody();

            if (messages != null) {
                Platform.runLater(() -> {
                    chatBox.getChildren().clear(); // clear previous messages
                    for (MessageDTO msg : messages) {

                        // ✅ Decrypt only text messages
                        if (msg.getContent() != null && (msg.getFileUrl() == null || msg.getFileUrl().isEmpty())) {
                            try {
                                String decrypted = EncryptionUtil.decrypt(msg.getContent());
                                msg.setContent(decrypted);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }

                        // ✅ Always use common renderer
                        Node msgNode = renderMessage(msg);
                        chatBox.getChildren().add(msgNode);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Label error = new Label("⚠️ Failed to load chat history");
                chatBox.getChildren().add(error);
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.w3c.dom.Text;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

public class ChatClientApp extends Application {
    private TextArea chatArea;
    private StompSession stompSession;

    @Override
    public void start(Stage stage)
    {
        chatArea= new TextArea();
        chatArea.setEditable(false);

        TextField inputField =new TextField();
        inputField.setPromptText("Type your message...");

        Button sendButton =new Button("Send");

        sendButton.setOnAction(e->{
            String message = inputField.getText().trim();
            if(!message.isEmpty())
            {
                sendMessage(message);
                inputField.clear();
            }
        });

        VBox layout =new VBox(10,chatArea,inputField,sendButton);
        layout.setPadding(new Insets(10));

        stage.setScene(new Scene(layout,400,300));
        stage.setTitle("Chat Client");
        stage.show();

        connectToWebSocket();
    }
    private void connectToWebSocket() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());   // 👈 this line is important
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converter.setObjectMapper(mapper);

        stompClient.setMessageConverter(converter);

        try {
            WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
            StompHeaders connectHeaders = new StompHeaders();

            stompSession = stompClient.connectAsync(
                    "ws://localhost:8080/chat",
                    httpHeaders,
                    connectHeaders,
                    new StompSessionHandlerAdapter() {
                        @Override
                        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                            System.out.println("Connected to WebSocket Server!!");
                        }
                    }).get();

            stompSession.subscribe("/topic/messages", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return MessageDTO.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    MessageDTO msg = (MessageDTO) payload;
                    chatArea.appendText(msg.getSender() + ": " + msg.getContent() + "\n");
                }
            });

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }


    private void sendMessage(String content)
    {
        if(stompSession !=null && stompSession.isConnected()) {
            MessageDTO message = new MessageDTO("JavaFxUser", "All", content, LocalDateTime.now());
            stompSession.send("/app/sendMessage", message);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
