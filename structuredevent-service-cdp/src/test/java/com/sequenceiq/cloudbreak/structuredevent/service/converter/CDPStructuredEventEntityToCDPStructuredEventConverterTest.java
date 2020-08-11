package com.sequenceiq.cloudbreak.structuredevent.service.converter;

import java.io.Serializable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

@ExtendWith(MockitoExtension.class)
public class CDPStructuredEventEntityToCDPStructuredEventConverterTest {

    @InjectMocks
    private CDPStructuredEventEntityToCDPStructuredEventConverter underTest;

    @BeforeEach
    public void setup() {
        underTest.init();
    }

    @Test
    public void testConvertWhenSuccess() {
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setAccountId("accountId");
        FlowDetails flowDetails = new FlowDetails();
        Serializable payload = "payload";
        CDPStructuredEvent event = new CDPStructuredFlowEvent(CDPStructuredFlowEvent.class.getSimpleName(), operationDetails, flowDetails, payload);

        CDPStructuredEventEntity eventEntity = new CDPStructuredEventEntity();
        eventEntity.setEventType(StructuredEventType.FLOW);
        eventEntity.setStructuredEventJson(new Json(event));
        CDPStructuredFlowEvent<String> actual = (CDPStructuredFlowEvent<String>) underTest.convert(eventEntity);
        Assertions.assertEquals("accountId", operationDetails.getAccountId());
        Assertions.assertEquals("payload", actual.getPayload());
    }

    @Test
    public void testConvertWhenFail() {
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setAccountId("accountId");
        FlowDetails flowDetails = new FlowDetails();
        Serializable payload = "payload";
        CDPStructuredEvent event = new CDPStructuredFlowEvent("AnyName", operationDetails, flowDetails, payload);

        CDPStructuredEventEntity eventEntity = new CDPStructuredEventEntity();
        eventEntity.setEventType(StructuredEventType.FLOW);
        eventEntity.setStructuredEventJson(new Json(event));
        CloudbreakServiceException actualException = Assertions.assertThrows(CloudbreakServiceException.class, () -> underTest.convert(eventEntity));
        Assertions.assertEquals("Cannot convert structured event entity to type: 'FLOW (AnyName)' structured event.", actualException.getMessage());

    }
}
