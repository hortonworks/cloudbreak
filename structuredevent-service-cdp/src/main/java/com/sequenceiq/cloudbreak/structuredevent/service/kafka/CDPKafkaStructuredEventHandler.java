package com.sequenceiq.cloudbreak.structuredevent.service.kafka;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.REPLACEMENT;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.kafka.common.errors.InvalidTopicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;

@Component
public class CDPKafkaStructuredEventHandler<T extends CDPStructuredEvent> implements EventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPKafkaStructuredEventHandler.class);

    @Inject
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public String selector() {
        return CDPAsyncKafkaStructuredEventSender.KAFKA_EVENT_LOG_MESSAGE;
    }

    @Override
    public void accept(Event<T> structuredEvent) {
        String topicByType = getTopicNameForEvent(structuredEvent);
        try {
            CDPStructuredEvent event = structuredEvent.getData();
            sanitizeSensitiveRestData(event);
            ListenableFuture<SendResult<String, String>> sendResultFuture =
                    kafkaTemplate.send(topicByType, JsonUtil.writeValueAsString(structuredEvent.getData()));
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

    protected void sanitizeSensitiveRestData(CDPStructuredEvent event) {
        if ("StructuredRestCallEvent".equals(event.getType())) {
            CDPStructuredRestCallEvent restEvent = (CDPStructuredRestCallEvent) event;
            RestRequestDetails restRequestDetails = restEvent.getRestCall().getRestRequest();
            restRequestDetails.setBody(REPLACEMENT);
            restRequestDetails.setHeaders(new HashMap());
            RestResponseDetails restResponseDetails = restEvent.getRestCall().getRestResponse();
            restResponseDetails.setBody(REPLACEMENT);
            restResponseDetails.setHeaders(new HashMap<>());
        }
    }

    private String getTopicNameForEvent(Event<T> event) {
        return "cb" + event.getData().getType();
    }
}