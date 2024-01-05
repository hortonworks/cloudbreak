package com.sequenceiq.cloudbreak.structuredevent.service.kafka;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.REPLACEMENT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.conf.StructuredEventSenderConfig;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;

@ExtendWith(MockitoExtension.class)
class KafkaStructuredEventHandlerTest {

    @Mock
    private StructuredEventSenderConfig structuredEventSenderConfig;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private LegacyKafkaStructuredEventHandler classIntest;

    @Test
    void checkEventTypeBasedTopicDistribution() throws ExecutionException, InterruptedException {
        StructuredRestCallEvent structuredEvent = createDummyStructuredRestEvent();
        Event<StructuredEvent> event = new Event<>(structuredEvent);
        CompletableFuture<SendResult<String, String>> futures = generateMockFutureWrappers();
        when(kafkaTemplate.send(eq("cbStructuredRestCallEvent"), anyString())).thenReturn(futures);

        classIntest.accept(event);

        verify(kafkaTemplate).send(eq("cbStructuredRestCallEvent"), anyString());
    }

    @Test
    void checkIfPropertiesGetFilteredWithCustomMapper() {
        StructuredRestCallEvent restEvent = createDummyStructuredRestEvent();

        classIntest.sanitizeSensitiveRestData(restEvent);
        RestRequestDetails requestDetails = restEvent.getRestCall().getRestRequest();

        assertTrue(requestDetails.getBody().contains(REPLACEMENT), "Should be sanitized from Kafka event");
        assertTrue(requestDetails.getHeaders().isEmpty(), "Should be empty because of ");
        assertTrue(requestDetails.getRequestUri().equals("/v3/clusters"), "Should be left intact");
    }

    private StructuredRestCallEvent createDummyStructuredRestEvent() {
        RestRequestDetails requestDetails = new RestRequestDetails();
        requestDetails.setBody("RequestBodyContent");
        requestDetails.setRequestUri("/v3/clusters");
        RestResponseDetails restResponseDetails = new RestResponseDetails();
        restResponseDetails.setStatusCode(200);
        restResponseDetails.setBody("BodyContent");
        restResponseDetails.setHeaders(ImmutableMap.of("content-length", "89"));
        RestCallDetails restCallDetails = new RestCallDetails();
        restCallDetails.setRestRequest(requestDetails);
        restCallDetails.setRestResponse(restResponseDetails);
        StructuredRestCallEvent restEvent = new StructuredRestCallEvent();
        restEvent.setRestCall(restCallDetails);
        restEvent.setType("StructuredRestCallEvent");
        return restEvent;
    }

    private CompletableFuture<SendResult<String, String>> generateMockFutureWrappers() throws InterruptedException, ExecutionException {
        CompletableFuture futureMock = mock(CompletableFuture.class);
        when(futureMock.get()).thenReturn((SendResult<String, String>) mock(SendResult.class));
        return (CompletableFuture<SendResult<String, String>>) futureMock;
    }

}
