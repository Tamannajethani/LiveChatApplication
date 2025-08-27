package com.chatapp.chatapp.service;

import com.chatapp.chatapp.dto.MessageDTO;
import com.chatapp.chatapp.dto.MessageRequest;
import com.chatapp.chatapp.model.Message;
import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.repository.MessageRepository;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

private final String uploadDir="uploads/";
    @Override
    public MessageDTO sendMessage(MessageRequest messageRequest) {
        User sender = userRepository.findByUsername(messageRequest.getSender())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User receiver = userRepository.findByUsername(messageRequest.getReceiver())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Message message = new Message();
        message.setSender(sender.getUsername());
        message.setReceiver(receiver.getUsername());
        message.setSentAt(LocalDateTime.now());

        if (messageRequest.getFileUrl() != null && !messageRequest.getFileUrl().isEmpty()) {
            // Handle media message
            message.setContent(null);
            message.setFileUrl(messageRequest.getFileUrl());
            message.setFileType(messageRequest.getFileType());
        } else {
            // Handle text message
            message.setContent(messageRequest.getContent());
        }

        Message saved = messageRepository.save(message);

        return new MessageDTO(
                saved.getSender(),
                saved.getReceiver(),
                saved.getContent(),
                saved.getSentAt(),
                saved.getFileUrl(),
                saved.getFileType()
        );
    }

    public MessageDTO sendFileMessage(String sender, String receiver, MultipartFile file) throws IOException {
        // Ensure uploads directory exists
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Generate unique file name
        String uniqueName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, uniqueName);

        // Save file locally
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Build file URL for retrieval
        String fileUrl = "/files/" + uniqueName;

        // Create and save Message entity
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(null); // no text content
        message.setFileUrl(fileUrl);
        message.setFileType(file.getContentType());
        message.setSentAt(LocalDateTime.now());

        messageRepository.save(message);

        // Return DTO
        return new MessageDTO(
                sender,
                receiver,
                null,
                message.getSentAt(),
                fileUrl,
                file.getContentType()
        );
    }

    @Override
    public List<MessageDTO> getAllMessages()
    {
        return messageRepository.findAll().stream()
                .map(msg -> new MessageDTO(
                        msg.getSender(),
                        msg.getReceiver(),
                        msg.getContent(),
                        msg.getSentAt()
                ))
                .collect(Collectors.toList());
    }
    @Override
    public List<MessageDTO> getMessageBySender(String sender)
    {
        return messageRepository.findBySender(sender).stream()
                .map(msg -> new MessageDTO(
                        msg.getSender(),
                        msg.getReceiver(),
                        msg.getContent(),
                        msg.getSentAt()
                ))
                .collect(Collectors.toList());
    }
    @Override
    public List<MessageDTO> getMessageByReceiver(String receiver)
    {
        return messageRepository.findByReceiver(receiver).stream()
                .map(msg -> new MessageDTO(
                        msg.getSender(),
                        msg.getReceiver(),
                        msg.getContent(),
                        msg.getSentAt()
                ))
                .collect(Collectors.toList());
    }
    @Override
    public List<MessageDTO> getChatHistory(String sender,String receiver)
    {
        return messageRepository.findChatHistory( sender, receiver).stream()
                .map(msg -> new MessageDTO(
                        msg.getSender(),
                        msg.getReceiver(),
                        msg.getContent(),
                        msg.getSentAt(),
                        msg.getFileUrl(),   // ✅ include files
                        msg.getFileType()
                ))
                .collect(Collectors.toList());
    }
    @Override
    public Map<String,Boolean> deleteMessage(Long id)
    {
        Message message=messageRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Message not found"));
        messageRepository.delete(message);
        Map<String,Boolean> response=new HashMap<>();
        response.put("deleted",true);
        return response;
    }

}

