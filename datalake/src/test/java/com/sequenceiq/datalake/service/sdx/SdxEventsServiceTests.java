package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.SdxEventsService;

public class SdxEventsServiceTests {

    private static final Integer TEST_PAGE = 1;

    private static final Integer TEST_SIZE = 10;

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:" +
            "6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String DATALAKE_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:" +
            "6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final List<StructuredEventType> TEST_EVENT_TYPES = List.of(StructuredEventType.FLOW, StructuredEventType.NOTIFICATION);

    @Mock
    private CDPStructuredEventDBService mockCdpStructuredEventDBService;

    @Mock
    private EventV4Endpoint eventV4Endpoint;

    @Mock
    private SdxClusterRepository mockSdxClusterRepository;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxEventsService sdxEventsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setCrn(DATALAKE_CRN);
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        when(mockSdxClusterRepository.findByAccountIdAndEnvCrn(any(), any())).thenReturn(List.of(sdxCluster));
        when(sdxService.listSdxByEnvCrn(any())).thenReturn(List.of(sdxCluster));
    }

    @Test
    void testGetAuditEventsWhenNotEmptyPageComingBackFromDbServiceThenAuthzHappens() {
        Page<CDPStructuredEvent> mockPage = mock(Page.class);

        when(mockPage.getContent()).thenReturn(List.of(createTestCDPStructuredEvent(StructuredEventType.NOTIFICATION),
                createTestCDPStructuredEvent(StructuredEventType.FLOW)));
        when(mockCdpStructuredEventDBService.getPagedEventsOfResources(eq(TEST_EVENT_TYPES), any(), any()))
                .thenReturn(mockPage);

        List<CDPStructuredEvent> result = sdxEventsService.getPagedDatalakeAuditEvents(DATALAKE_CRN, TEST_EVENT_TYPES, TEST_PAGE, TEST_SIZE);

        assertNotNull(result);
        assertEquals(2, result.size());

    }

    @Test
    void testGetAuditEventsWhenTypeIsProvided() {
        Page<CDPStructuredEvent> mockPage = mock(Page.class);

        when(mockPage.getContent()).thenReturn(List.of(createTestCDPStructuredEvent(StructuredEventType.NOTIFICATION)));
        when(mockCdpStructuredEventDBService.getPagedEventsOfResources(eq(List.of(StructuredEventType.NOTIFICATION)), any(), any()))
                .thenReturn(mockPage);
        when(eventV4Endpoint.getPagedCloudbreakEventListByStack(any(), any(), any(), any())).thenReturn(Collections.EMPTY_LIST);

        List<CDPStructuredEvent> result = sdxEventsService.getPagedDatalakeAuditEvents(DATALAKE_CRN,
                Collections.singletonList(StructuredEventType.NOTIFICATION), TEST_PAGE, TEST_SIZE);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(result.get(0).getOperation().getEventType(), StructuredEventType.NOTIFICATION);
    }

    @Test
    void testGetAuditEventsWithCBAndDlEvents() {
        Page<CDPStructuredEvent> mockPage = mock(Page.class);

        when(mockPage.getContent()).thenReturn(List.of(createTestCDPStructuredEvent(StructuredEventType.NOTIFICATION)));
        when(mockCdpStructuredEventDBService.getPagedEventsOfResources(eq(List.of(StructuredEventType.NOTIFICATION)), any(), any()))
                .thenReturn(mockPage);
        when(eventV4Endpoint.getPagedCloudbreakEventListByCrn(any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(createCloudbreakEventV4Response()));

        List<CDPStructuredEvent> result = sdxEventsService.getPagedDatalakeAuditEvents(DATALAKE_CRN,
                Collections.singletonList(StructuredEventType.NOTIFICATION), TEST_PAGE, TEST_SIZE);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.stream().filter(notification -> notification.getOperation().getEventType().equals(StructuredEventType.NOTIFICATION)).count());
    }

    @Test
    void testGetAuditEventsWithCBAndDlEventsSort() {
        Page<CDPStructuredEvent> mockPage = mock(Page.class);

        when(mockPage.getContent()).thenReturn(List.of(createTestCDPStructuredEvent(StructuredEventType.NOTIFICATION, 5L),
                createTestCDPStructuredEvent(StructuredEventType.NOTIFICATION, 2L)));
        when(mockCdpStructuredEventDBService.getPagedEventsOfResources(eq(List.of(StructuredEventType.NOTIFICATION)), any(), any()))
                .thenReturn(mockPage);
        when(eventV4Endpoint.getPagedCloudbreakEventListByCrn(any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(createCloudbreakEventV4Response(1L)));

        List<CDPStructuredEvent> result = sdxEventsService.getPagedDatalakeAuditEvents(DATALAKE_CRN,
                Collections.singletonList(StructuredEventType.NOTIFICATION), TEST_PAGE, TEST_SIZE);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(3, result.stream().filter(notification -> notification.getOperation().getEventType().equals(StructuredEventType.NOTIFICATION)).count());
        assertTrue(result.get(0).getOperation().getTimestamp() < result.get(1).getOperation().getTimestamp());
        assertTrue(result.get(1).getOperation().getTimestamp() < result.get(2).getOperation().getTimestamp());
    }

    private CloudbreakEventV4Response createCloudbreakEventV4Response() {
        return createCloudbreakEventV4Response(1L);
    }

    private CloudbreakEventV4Response createCloudbreakEventV4Response(Long timestamp) {
        CloudbreakEventV4Response cloudbreakEventV4Response = new CloudbreakEventV4Response();
        cloudbreakEventV4Response.setEventTimestamp(timestamp);
        cloudbreakEventV4Response.setClusterName("somename");
        cloudbreakEventV4Response.setEventMessage("testing");
        return cloudbreakEventV4Response;
    }

    private CDPStructuredEvent createTestCDPStructuredEvent(StructuredEventType type) {
        if (type.equals(StructuredEventType.NOTIFICATION)) {
            return createCDPStructuredNotificationEvent(0L);
        } else {
            return createCDPStructuredFlowEvent(0L);
        }

    }

    private CDPStructuredEvent createTestCDPStructuredEvent(StructuredEventType type, Long timestamp) {
        if (type.equals(StructuredEventType.NOTIFICATION)) {
            return createCDPStructuredNotificationEvent(timestamp);
        } else {
            return createCDPStructuredFlowEvent(timestamp);
        }

    }

    private CDPStructuredEvent createCDPStructuredNotificationEvent(Long timestamp) {
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setResourceCrn("someCrn");
        operationDetails.setResourceType("datalake");
        operationDetails.setTimestamp(timestamp);
        operationDetails.setEventType(StructuredEventType.NOTIFICATION);

        CDPStructuredNotificationEvent cdpStructuredEvent = new CDPStructuredNotificationEvent() {
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
