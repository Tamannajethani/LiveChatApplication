package com.chatapp.chatapp.controller;

import com.chatapp.chatapp.dto.MessageDTO;
import com.chatapp.chatapp.dto.MessageRequest;
import com.chatapp.chatapp.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Controller
public class ChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    // ✅ Constructor injection for both dependencies
    public ChatController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/sendMessage")
    @SendTo("/topic/messages")
    public MessageDTO sendMessage(MessageDTO message) {
        message.setSentAt(LocalDateTime.now());

        MessageRequest req = new MessageRequest(
                message.getSender(),
                message.getReceiver(),
                message.getContent()
        );

        // Carry over file info if present
        req.setFileUrl(message.getFileUrl());
        req.setFileType(message.getFileType());

        return messageService.sendMessage(req);
    }

    @PostMapping("/messages/sendFile")
    @ResponseBody
    public ResponseEntity<MessageDTO> sendFile(
            @RequestParam("sender") String sender,
            @RequestParam("receiver") String receiver,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        MessageDTO saved = messageService.sendFileMessage(sender, receiver, file);

        // broadcast to all subscribers of /topic/messages
        messagingTemplate.convertAndSend("/topic/messages", saved);

        return ResponseEntity.ok(saved);
    }
}
