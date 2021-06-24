package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.utils.EventAuthorizationUtils;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;

@ExtendWith(MockitoExtension.class)
public class AuditEventV4ControllerTest {

    private static final Long TEST_AUDIT_ID = 1234L;

    private static final Long TEST_WORKSPACE_ID = 1L;

    private static final String TEST_RESOURCE_TYPE = "environment";

    private static final String TEST_RESOURCE_CRN = "someCrn";

    @Mock
    private AuditEventService mockAuditEventService;

    @Mock
    private CloudbreakRestRequestThreadLocalService mockThreadLocalService;

    @Mock
    private EventAuthorizationUtils mockEventAuthorizationUtils;

    @InjectMocks
    private AuditEventV4Controller underTest;

    @Test
    public void testGetAuditEventById() {
        when(mockAuditEventService.getAuditEventByWorkspaceId(TEST_WORKSPACE_ID, TEST_AUDIT_ID)).thenReturn(createAuditEventV4Response());

        underTest.getAuditEventById(TEST_WORKSPACE_ID, TEST_AUDIT_ID);

        verify(mockEventAuthorizationUtils, times(1)).checkPermissionBasedOnResourceTypeAndCrn(any(Set.class));
    }

    @Test
    public void testGetAuditEvents() {
        when(mockThreadLocalService.getRequestedWorkspaceId()).thenReturn(TEST_WORKSPACE_ID);
        when(mockAuditEventService.getAuditEventsByWorkspaceId(TEST_WORKSPACE_ID, TEST_RESOURCE_TYPE, TEST_AUDIT_ID, TEST_RESOURCE_CRN))
                .thenReturn(List.of(createAuditEventV4Response()));

        underTest.getAuditEvents(TEST_WORKSPACE_ID, TEST_RESOURCE_TYPE, TEST_AUDIT_ID, TEST_RESOURCE_CRN);

        verify(mockEventAuthorizationUtils, times(1)).checkPermissionBasedOnResourceTypeAndCrn(any(Set.class));
    }

    @Test
    public void testGetAuditEventsZip() {
        when(mockThreadLocalService.getRequestedWorkspaceId()).thenReturn(TEST_WORKSPACE_ID);
        when(mockAuditEventService.getAuditEventsByWorkspaceId(TEST_WORKSPACE_ID, TEST_RESOURCE_TYPE, TEST_AUDIT_ID, TEST_RESOURCE_CRN))
                .thenReturn(List.of(createAuditEventV4Response()));

        underTest.getAuditEventsZip(TEST_WORKSPACE_ID, TEST_RESOURCE_TYPE, TEST_AUDIT_ID, TEST_RESOURCE_CRN);

        verify(mockEventAuthorizationUtils, times(1)).checkPermissionBasedOnResourceTypeAndCrn(any(Set.class));
    }

    private AuditEventV4Response createAuditEventV4Response() {
        return new AuditEventV4Response(TEST_AUDIT_ID, createStructuredEvent());
    }

    private StructuredEvent createStructuredEvent() {
        OperationDetails operationDetails = new OperationDetails();
        operationDetails.setEventType(NOTIFICATION);
        operationDetails.setResourceId(1234L);
        operationDetails.setResourceCrn("someCrn");
        operationDetails.setResourceType(TEST_RESOURCE_TYPE);
        StructuredEvent cdpStructuredEvent = new StructuredEvent() {
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