package com.sequenceiq.cloudbreak.structuredevent.kafka;

import static com.sequenceiq.cloudbreak.structuredevent.json.AnonymizerUtil.REPLACEMENT;

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
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import reactor.bus.Event;

@Component
public class KafkaStructuredEventHandler<T extends StructuredEvent> implements ReactorEventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaStructuredEventHandler.class);

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
            StructuredEvent event = structuredEvent.getData();
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

    protected void sanitizeSensitiveRestData(StructuredEvent event) {
        if ("StructuredRestCallEvent".equals(event.getType())) {
            StructuredRestCallEvent restEvent = (StructuredRestCallEvent) event;
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