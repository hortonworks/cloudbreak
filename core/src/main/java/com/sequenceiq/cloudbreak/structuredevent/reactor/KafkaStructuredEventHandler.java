package com.sequenceiq.cloudbreak.structuredevent.reactor;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

import reactor.bus.Event;

@Component
public class KafkaStructuredEventHandler<T extends StructuredEvent> implements ReactorEventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaStructuredEventHandler.class);

    @Value("${cb.kafka.structured.events.topic:StructuredEvents}")
    private String structuredEventsTopic;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public String selector() {
        return KafkaStructuredEventAsyncNotifier.KAFKA_EVENT_LOG_MESSAGE;
    }

    @Override
    public void accept(Event<T> structuredEvent) {
        try {
            ListenableFuture<SendResult<String, String>> sendResultFuture =
                    kafkaTemplate.send(structuredEventsTopic, objectMapper.writeValueAsString(structuredEvent.getData()));
            SendResult<String, String> sendResult = sendResultFuture.get();
            LOGGER.debug("Structured event sent to kafka: {}", sendResult.getProducerRecord());
        } catch (JsonProcessingException e) {
            LOGGER.error("Structured event json processing error", e);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error happened in message sending to kafka", e);
        }
    }
}