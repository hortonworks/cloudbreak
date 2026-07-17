package com.sequenceiq.datalake.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.datalake.service.SdxEventsService;
import com.sequenceiq.datalake.service.SdxEventsZipService;

@ExtendWith(MockitoExtension.class)
class SdxEventControllerTest {

    private static final Integer TEST_PAGE = 1;

    private static final Integer TEST_SIZE = 10;

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:" +
            "6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final List<StructuredEventType> TEST_EVENT_TYPES = List.of(StructuredEventType.FLOW, StructuredEventType.NOTIFICATION);

    @Mock
    private SdxEventsService sdxEventsService;

    @Mock
    private SdxEventsZipService sdxEventsZipService;

    @InjectMocks
    private SdxEventController datalakeEventController;

    @Test
    void testNoAuthorizationWhenEventsReturnedIsEmpty() {
        when(sdxEventsService.getPagedDatalakeAuditEvents(any(), eq(TEST_EVENT_TYPES), any(), any()))
                .thenReturn(Collections.emptyList());
        List<CDPStructuredEvent> result = datalakeEventController.getAuditEvents(RESOURCE_CRN, TEST_EVENT_TYPES, TEST_PAGE, TEST_SIZE);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testNoAuthorizationWhenEventsReturnedIsNotEmpty() {
        when(sdxEventsService.getPagedDatalakeAuditEvents(any(), eq(TEST_EVENT_TYPES), any(), any()))
                .thenReturn(Collections.singletonList(createCDPStructuredFlowEvent(1L)));
        List<CDPStructuredEvent> result = datalakeEventController.getAuditEvents(RESOURCE_CRN, TEST_EVENT_TYPES, TEST_PAGE, TEST_SIZE);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    private CDPStructuredEvent createCDPStructuredFlowEvent(Long timestamp) {
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setResourceCrn("someCrn");
        operationDetails.setResourceType("datalake");
        operationDetails.setTimestamp(timestamp);
        operationDetails.setEventType(StructuredEventType.FLOW);

        CDPStructuredFlowEvent cdpStructuredEvent = new CDPStructuredFlowEvent() {
            @Override
            public String getStatus() {
                return SENT;
            }

            @Override
            public Long getDuration() {
                return 1L;
            }
        };
        cdpStructuredEvent.setOperation(operationDetails);
        return cdpStructuredEvent;
    }
}
