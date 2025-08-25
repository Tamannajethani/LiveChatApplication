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


    @Override
    public MessageDTO sendMessage(MessageRequest messageRequest)
    {
        // Find sender (user must exist)
        User sender = userRepository.findById(messageRequest.getSenderId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User receiver = userRepository.findById(messageRequest.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        // Create new message
        Message message = new Message();
        message.setSender(sender.getUsername());
        message.setContent(messageRequest.getContent());
        message.setSentAt(LocalDateTime.now());

        // Save
        Message savedMessage = messageRepository.save(message);

        // Convert to DTO
        return new MessageDTO(savedMessage.getSender()
                ,savedMessage.getReceiver(),
                savedMessage.getContent(),
                savedMessage.getSentAt());
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

