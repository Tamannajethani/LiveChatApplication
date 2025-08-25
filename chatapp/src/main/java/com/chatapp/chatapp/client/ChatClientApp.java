package com.chatapp.chatapp.client;

import com.chatapp.chatapp.dto.MessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Application;
import javafx.geometry.Insets;
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
