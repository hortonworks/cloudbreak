package com.sequenceiq.cloudbreak.websocket;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.repository.UserRepository;

@Service
public class WebsocketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketService.class);

    @Autowired
    private SimpMessageSendingOperations messageSendingOperations;

    @Autowired
    private UserRepository userRepository;

    public void sendToTopic(Long userId, String destinationSuffix, Object message) {
        LOGGER.info("Sending message {} to {}/{}/{}", message, userId, destinationSuffix);
        messageSendingOperations.convertAndSend(String.format("/%s/%s/%s", "topic", userId, destinationSuffix), message);
    }

    public void sendToTopicUser(Long userId, String destinationSuffix, Object message) {
        LOGGER.info("Sending message {} to {}/{}/{}", message, userId, destinationSuffix);
        messageSendingOperations.convertAndSendToUser(userRepository.findOne(userId).getEmail(), String.format("/%s/%s/%s", "topic", userId, destinationSuffix), message);
    }

    public void send(Long userId, String destinationPrefix, String destinationSuffix, Object message) {
        LOGGER.info("Sending message {} to {}/{}/{}", message, destinationPrefix, userId, destinationSuffix);
        messageSendingOperations.convertAndSend(String.format("%s/%s/%s", destinationPrefix, userId, destinationSuffix), message);
    }

}