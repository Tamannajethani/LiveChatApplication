package com.chatapp.chatapp.repository;

import com.chatapp.chatapp.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

public interface MessageRepository extends JpaRepository <Message,Long>{

    List<Message> findBySender(String sender);

    List<Message> findByReceiver(String receiver);

    List<Message> findBySenderAndReceiver(String sender,String receiver);
}
