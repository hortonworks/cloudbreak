package com.sequenceiq.cloudbreak.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;

@Service
public class WebsocketService {

    @Autowired
    private SimpMessageSendingOperations messageSendingOperations;

    public void sendToTopicUser(String userEmail, WebsocketEndPoint websocketEndPoint, Object message) {
        //LOGGER.info("Sending message {} to {}{}", message, "topic", websocketEndPoint.getValue());
        messageSendingOperations.convertAndSendToUser(userEmail, String.format("/%s/%s", "topic", websocketEndPoint.getValue()), message);
    }
}