package com.chatapp.chatapp.controller;

import com.chatapp.chatapp.dto.MessageDTO;
import com.chatapp.chatapp.dto.MessageRequest;
import com.chatapp.chatapp.model.Message;
import com.chatapp.chatapp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private  MessageService messageService;

    @PostMapping
    public MessageDTO sendMessage(@RequestBody MessageRequest messageRequest){
        return messageService.sendMessage(messageRequest);
    }

    @GetMapping
    public List<MessageDTO> getAllMessages()
    {
        return messageService.getAllMessages();
    }

    @GetMapping("/sender/{sender}")
    public List<MessageDTO> getMessagesBySender(@PathVariable String sender)
    {
        return messageService.getMessageBySender(sender);
    }

    @GetMapping("/receiver/{receiver}")
    public List<MessageDTO> getMessagesByReceiver(@PathVariable String receiver)
    {
        return messageService.getMessageByReceiver(receiver);
    }

    @GetMapping("/chat")
    public List<MessageDTO> getChatHistory(@RequestParam String sender , @RequestParam String receiver)
    {
        return messageService.getChatHistory(sender,receiver);
    }

    @DeleteMapping("/{id}")
    public Map<String ,Boolean> deleteMessage(@PathVariable Long id){
        return messageService.deleteMessage(id);
    }
}

