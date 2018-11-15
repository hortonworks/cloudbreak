package com.sequenceiq.cloudbreak.structuredevent.kafka;

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

import com.sequenceiq.cloudbreak.conf.StructuredEventSenderConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
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
    public void sendingValidRestCallToDifferentTopic() throws ExecutionException, InterruptedException {
        StructuredRestCallEvent structuredEvent = generateValidRestEvent();
        Event<StructuredEvent> event = new Event<>(structuredEvent);
        ListenableFuture<SendResult<String, String>> futures = generateMockFutureWrappers();
        when(kafkaTemplate.send(eq("cbStructuredRestCallEvent"), anyString())).thenReturn(futures);

        classIntest.accept(event);

        verify(kafkaTemplate).send(eq("cbStructuredRestCallEvent"), anyString());
    }

    private ListenableFuture<SendResult<String, String>> generateMockFutureWrappers() throws InterruptedException, ExecutionException {
        ListenableFuture futureMock = mock(ListenableFuture.class);
        when(futureMock.get()).thenReturn((SendResult<String, String>) mock(SendResult.class));
        return (ListenableFuture<SendResult<String, String>>) futureMock;
    }

    private StructuredRestCallEvent generateValidRestEvent() {
        OperationDetails opDetails = new OperationDetails();
        opDetails.setTimestamp(1542641796L);
        RestCallDetails details = new RestCallDetails();
        details.setDuration(15L);
        StructuredRestCallEvent structuredEvent = new StructuredRestCallEvent(opDetails, details);
        RestResponseDetails restResponseDetails = new RestResponseDetails();
        restResponseDetails.setStatusCode(200);
        details.setRestResponse(restResponseDetails);
        structuredEvent.setType("StructuredRestCallEvent");
        return structuredEvent;
    }
}