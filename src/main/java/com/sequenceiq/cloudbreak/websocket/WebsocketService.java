package com.sequenceiq.cloudbreak.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;

@Service
public class WebsocketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketService.class);

    @Autowired
    private SimpMessageSendingOperations messageSendingOperations;

    public void sendToTopic(String destinationSuffix, Object message) {
        LOGGER.info("Sending message {} to {}/{}", message, destinationSuffix);
        messageSendingOperations.convertAndSend(String.format("/%s/%s", "topic", destinationSuffix), message);
    }

    public void sendToTopicUser(String userEmail, WebsocketEndPoint websocketEndPoint, Object message) {
        LOGGER.info("Sending message {} to {}/{}", message, websocketEndPoint.getValue());
        messageSendingOperations.convertAndSendToUser(userEmail, String.format("/%s/%s", "topic", websocketEndPoint.getValue()), message);
    }

    public void send(String destinationPrefix, String destinationSuffix, Object message) {
        LOGGER.info("Sending message {} to {}/{}", message, destinationPrefix, destinationSuffix);
        messageSendingOperations.convertAndSend(String.format("%s/%s", destinationPrefix, destinationSuffix), message);
    }

}