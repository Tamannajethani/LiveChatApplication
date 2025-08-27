package com.chatapp.chatapp.repository;

import com.chatapp.chatapp.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

public interface MessageRepository extends JpaRepository <Message,Long> {

    List<Message> findBySender(String sender);

    List<Message> findByReceiver(String receiver);

    List<Message> findBySenderAndReceiver(String sender, String receiver);

    @Query("SELECT m FROM Message m " +
            "WHERE (LOWER(m.sender) = LOWER(:sender) AND LOWER(m.receiver) = LOWER(:receiver)) " +
            "   OR (LOWER(m.sender) = LOWER(:receiver) AND LOWER(m.receiver) = LOWER(:sender)) " +
            "ORDER BY m.sentAt ASC")
    List<Message> findChatHistory(@Param("sender") String sender,
                                  @Param("receiver") String receiver);
}
