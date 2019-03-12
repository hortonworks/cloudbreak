package com.sequenceiq.cloudbreak.structuredevent.kafka;

import static com.sequenceiq.cloudbreak.structuredevent.json.AnonymizerUtil.REPLACEMENT;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.conf.StructuredEventSenderConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;

import reactor.bus.Event;

@RunWith(MockitoJUnitRunner.class)
public class KafkaStructuredEventHandlerTest {

    @Mock
    private StructuredEventSenderConfig structuredEventSenderConfig;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaStructuredEventHandler classIntest;

    @Test
    public void checkEventTypeBasedTopicDistribution() throws ExecutionException, InterruptedException {
        StructuredRestCallEvent structuredEvent = createDummyStructuredRestEvent();
        Event<StructuredEvent> event = new Event<>(structuredEvent);
        ListenableFuture<SendResult<String, String>> futures = generateMockFutureWrappers();
        when(kafkaTemplate.send(eq("cbStructuredRestCallEvent"), anyString())).thenReturn(futures);

        classIntest.accept(event);

        verify(kafkaTemplate).send(eq("cbStructuredRestCallEvent"), anyString());
    }

    @Test
    public void checkIfPropertiesGetFilteredWithCustomMapper() {
        StructuredRestCallEvent restEvent = createDummyStructuredRestEvent();

        classIntest.sanitizeSensitiveRestData(restEvent);
        RestRequestDetails requestDetails = restEvent.getRestCall().getRestRequest();

        assertTrue("Should be sanitized from Kafka event", requestDetails.getBody().contains(REPLACEMENT));
        assertTrue("Should be empty because of ", requestDetails.getHeaders().isEmpty());
        assertTrue("Should be left intact", requestDetails.getRequestUri().equals("/v3/clusters"));
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

    private ListenableFuture<SendResult<String, String>> generateMockFutureWrappers() throws InterruptedException, ExecutionException {
        ListenableFuture futureMock = mock(ListenableFuture.class);
        when(futureMock.get()).thenReturn((SendResult<String, String>) mock(SendResult.class));
        return (ListenableFuture<SendResult<String, String>>) futureMock;
    }

}