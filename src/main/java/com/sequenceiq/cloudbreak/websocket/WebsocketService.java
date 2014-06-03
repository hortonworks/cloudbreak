package com.sequenceiq.cloudbreak.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
public class WebsocketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketService.class);

    @Autowired
    private SimpMessageSendingOperations messageSendingOperations;

    public void send(String destination, Object message) {
        LOGGER.info("Sending message {} to {}", message, destination);
        messageSendingOperations.convertAndSend(destination, message);
    }
}