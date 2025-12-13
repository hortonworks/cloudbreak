package com.sequenceiq.cloudbreak.structuredevent.service.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

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
        CDPStructuredEvent event = new CDPStructuredFlowEvent(operationDetails, flowDetails, payload, null, null);

        CDPStructuredEventEntity eventEntity = new CDPStructuredEventEntity();
        eventEntity.setEventType(StructuredEventType.FLOW);
        eventEntity.setStructuredEventJson(new Json(event));
        CDPStructuredFlowEvent<String> actual = (CDPStructuredFlowEvent<String>) underTest.convert(eventEntity);
        assertEquals("accountId", operationDetails.getAccountId());
        assertEquals("payload", actual.getPayload());
    }
}
