package com.chatapp.chatapp.service;

import com.chatapp.chatapp.dto.MessageDTO;
import com.chatapp.chatapp.dto.MessageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MessageService {
    MessageDTO sendMessage(MessageRequest message);
    List<MessageDTO> getAllMessages();
    List<MessageDTO> getMessageBySender(String sender);
    List<MessageDTO> getMessageByReceiver (String receiver);
    MessageDTO sendFileMessage(String sender, String receiver, MultipartFile file)throws IOException;
    List<MessageDTO> getChatHistory(String sender, String receiver);
    Map<String,Boolean> deleteMessage(Long id);
}
