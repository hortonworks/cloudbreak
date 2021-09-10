package com.sequenceiq.datalake.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;

import com.sequenceiq.authorization.utils.EventAuthorizationUtils;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

class SdxEventControllerTest {

    public static final String EXPECTED_ZIP_HEADER_KEY = "content-disposition";

    public static final String EXPECTED_ZIP_HEADER_VALUE = "[attachment; filename = audit-environment.zip]";

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:" +
            "6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final List<StructuredEventType> TEST_EVENT_TYPES = List.of(StructuredEventType.FLOW, StructuredEventType.NOTIFICATION);

    @Mock
    private CDPStructuredEventDBService mockCdpStructuredEventDBService;

    @Mock
    private EventAuthorizationUtils mockEventAuthorizationUtils;

    @Mock
    private SdxClusterRepository mockSdxClusterRepository;

    @InjectMocks
    private SdxEventController datalakeEventController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testNoAuthorizationWhenEventsReturnedIsEmpty() {
        when(mockCdpStructuredEventDBService.getPagedEventsOfResource(eq(TEST_EVENT_TYPES), any(), any()))
                .thenReturn(Page.empty());
        List<CDPStructuredEvent> result = datalakeEventController.getAuditEvents(RESOURCE_CRN, TEST_EVENT_TYPES);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(mockEventAuthorizationUtils, never()).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    @Test
    void testGetAuditEventsWhenNotEmptyPageComingBackFromDbServiceThenAuthzHappens() {
        Page<CDPStructuredEvent> mockPage = mock(Page.class);

        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setCrn(RESOURCE_CRN);
        when(mockSdxClusterRepository.findByAccountIdAndEnvCrn(any(), any())).thenReturn(List.of(sdxCluster));

        when(mockPage.getContent()).thenReturn(List.of(createTestCDPStructuredEvent()));
        when(mockCdpStructuredEventDBService.getPagedEventsOfResource(eq(TEST_EVENT_TYPES), any(), any()))
                .thenReturn(mockPage);

        List<CDPStructuredEvent> result = datalakeEventController.getAuditEvents(RESOURCE_CRN, TEST_EVENT_TYPES);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(mockEventAuthorizationUtils, times(1)).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    private CDPStructuredEvent createTestCDPStructuredEvent() {
        return createTestCDPStructuredEvent(0L);
    }

    private CDPStructuredEvent createTestCDPStructuredEvent(Long timestamp) {
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setResourceCrn("someCrn");
        operationDetails.setResourceType("environment");
        operationDetails.setTimestamp(timestamp);

        CDPStructuredEvent cdpStructuredEvent = new CDPStructuredEvent() {
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