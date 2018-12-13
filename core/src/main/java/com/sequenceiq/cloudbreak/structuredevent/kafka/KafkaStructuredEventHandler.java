package com.sequenceiq.cloudbreak.structuredevent.kafka;

import static com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails.KAFKA_PROPERTY_FILTER_NAME;

import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.kafka.common.errors.InvalidTopicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

import reactor.bus.Event;

@Component
public class KafkaStructuredEventHandler<T extends StructuredEvent> implements ReactorEventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaStructuredEventHandler.class);

    private ObjectMapper objectMapper;

    @Inject
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public String selector() {
        return AsyncKafkaStructuredEventSender.KAFKA_EVENT_LOG_MESSAGE;
    }

    @Override
    public void accept(Event<T> structuredEvent) {
        String topicByType = getTopicNameForEvent(structuredEvent);
        try {
            ListenableFuture<SendResult<String, String>> sendResultFuture =
                    kafkaTemplate.send(topicByType, objectMapper.writeValueAsString(structuredEvent.getData()));
            SendResult<String, String> sendResult = sendResultFuture.get();
            LOGGER.trace("Structured event sent to kafka with topic {}: {}", topicByType, sendResult.getProducerRecord());
        } catch (InvalidTopicException e) {
            LOGGER.error("Structured event invalid topic name {}", topicByType, e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Structured event json processing error", e);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error happened in message sending to kafka", e);
        }
    }

    @PostConstruct
    protected void init() {
        objectMapper = createObjectMapper();
    }

    protected ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(KAFKA_PROPERTY_FILTER_NAME, SimpleBeanPropertyFilter.serializeAllExcept("body", "cookies"));
        mapper.setFilterProvider(filterProvider);
        return mapper;
    }

    private String getTopicNameForEvent(Event<T> event) {
        return "cb" + event.getData().getType();
    }
}