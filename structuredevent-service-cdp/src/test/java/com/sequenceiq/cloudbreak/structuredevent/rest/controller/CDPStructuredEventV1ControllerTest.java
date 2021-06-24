package com.sequenceiq.cloudbreak.structuredevent.rest.controller;

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

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.sequenceiq.authorization.utils.EventAuthorizationUtils;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;

class CDPStructuredEventV1ControllerTest {

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-" +
            "8ac6-4c26-aa34-dab36f4bd243";

    private static final List<StructuredEventType> TEST_EVENT_TYPES = List.of(StructuredEventType.REST);

    private static final Integer TEST_PAGE = 1;

    private static final Integer TEST_SIZE = 1;

    private static final String EXPECTED_ZIP_HEADER_KEY = "content-disposition";

    private static final String EXPECTED_ZIP_HEADER_VALUE = "[attachment; filename = audit-environment.zip]";

    @Mock
    private CDPStructuredEventDBService mockStructuredEventDBService;

    @Mock
    private EventAuthorizationUtils mockEventAuthorizationUtils;

    @InjectMocks
    private CDPStructuredEventV1Controller underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAuditEventsWhenEmptyPageComingBackFromDbServiceThenNoAuthzHappens() {
        when(mockStructuredEventDBService.getPagedEventsOfResource(eq(TEST_EVENT_TYPES), eq(RESOURCE_CRN), any(PageRequest.class)))
                .thenReturn(Page.empty());

        List<CDPStructuredEvent> result = underTest.getAuditEvents(RESOURCE_CRN, TEST_EVENT_TYPES, TEST_PAGE, TEST_SIZE);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(mockEventAuthorizationUtils, never()).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    @Test
    void testGetAuditEventsWhenNotEmptyPageComingBackFromDbServiceThenAuthzHappens() {
        Page<CDPStructuredEvent> mockPage = mock(Page.class);
        when(mockPage.getContent()).thenReturn(List.of(createTestCDPStructuredEvent()));
        when(mockStructuredEventDBService.getPagedEventsOfResource(eq(TEST_EVENT_TYPES), eq(RESOURCE_CRN), any(PageRequest.class))).thenReturn(mockPage);

        List<CDPStructuredEvent> result = underTest.getAuditEvents(RESOURCE_CRN, TEST_EVENT_TYPES, TEST_PAGE, TEST_SIZE);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(mockEventAuthorizationUtils, times(1)).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    @Test
    void testGetAuditEventsZipWhenEmptyPageComingBackFromDbServiceThenNoAuthzHappens() {
        when(mockStructuredEventDBService.getPagedEventsOfResource(eq(TEST_EVENT_TYPES), eq(RESOURCE_CRN), any(PageRequest.class)))
                .thenReturn(Page.empty());

        underTest.getAuditEventsZip(RESOURCE_CRN, TEST_EVENT_TYPES);

        verify(mockEventAuthorizationUtils, never()).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    @Test
    void testGetAuditEventsZipWhenNotEmptyPageComingBackFromDbServiceThenAuthzHappens() {
        when(mockStructuredEventDBService.getEventsOfResource(TEST_EVENT_TYPES, RESOURCE_CRN)).thenReturn(List.of(createCDPStructuredNotificationEvent()));

        underTest.getAuditEventsZip(RESOURCE_CRN, TEST_EVENT_TYPES);

        verify(mockEventAuthorizationUtils, times(1)).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    @Test
    void testGetAuditEventsZipWhenEmptyPageComingBackFromDbServiceThenHeaderShouldNotBeInTheResponse() {
        when(mockStructuredEventDBService.getPagedEventsOfResource(eq(TEST_EVENT_TYPES), eq(RESOURCE_CRN), any(PageRequest.class)))
                .thenReturn(Page.empty());

        Response response = underTest.getAuditEventsZip(RESOURCE_CRN, TEST_EVENT_TYPES);

        assertNotNull(response);
        assertTrue(response.getHeaders().isEmpty());
    }

    @Test
    void testGetAuditEventsZipWhenNotEmptyPageComingBackFromDbServiceThenNoHeaderShouldBeInTheResponse() {
        Page<CDPStructuredEvent> mockPage = mock(Page.class);
        when(mockPage.getContent()).thenReturn(List.of(createTestCDPStructuredEvent()));
        when(mockStructuredEventDBService.getPagedEventsOfResource(eq(TEST_EVENT_TYPES), eq(RESOURCE_CRN), any(PageRequest.class))).thenReturn(mockPage);

        Response response = underTest.getAuditEventsZip(RESOURCE_CRN, TEST_EVENT_TYPES);

        assertNotNull(response);
        assertTrue(response.getHeaders().isEmpty());
    }

    private void checkZipHeader(Response response) {
        assertNotNull(response);
        assertTrue(response.getHeaders().containsKey(EXPECTED_ZIP_HEADER_KEY));
        assertEquals(EXPECTED_ZIP_HEADER_VALUE, response.getHeaders().get(EXPECTED_ZIP_HEADER_KEY).toString());
    }

    private CDPStructuredEvent createTestCDPStructuredEvent() {
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setResourceCrn("someCrn");
        operationDetails.setResourceType("environment");
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

    private CDPStructuredNotificationEvent createCDPStructuredNotificationEvent() {
        CDPStructuredNotificationEvent event = new CDPStructuredNotificationEvent();
        event.setOperation(createTestCDPStructuredEvent().getOperation());
        return event;
    }

}