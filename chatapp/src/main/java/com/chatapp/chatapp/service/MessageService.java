package com.chatapp.chatapp.service;

import com.chatapp.chatapp.dto.MessageDTO;
import com.chatapp.chatapp.dto.MessageRequest;

import java.util.List;
import java.util.Map;

public interface MessageService {
    MessageDTO sendMessage(MessageRequest message);
    List<MessageDTO> getAllMessages();
    List<MessageDTO> getMessageBySender(String sender);
    List<MessageDTO> getMessageByReceiver (String receiver);
    List<MessageDTO> getChatHistory(String sender, String receiver);
    Map<String,Boolean> deleteMessage(Long id);
}
