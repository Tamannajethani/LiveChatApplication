package com.chatapp.chatapp.controller;

import com.chatapp.chatapp.dto.MessageDTO;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    @MessageMapping("/sendMessage")
    @SendTo("/topic/messages")
    public MessageDTO sendMessage(MessageDTO message)
    {
        message.setSentAt(LocalDateTime.now());
        return message;
    }
}
