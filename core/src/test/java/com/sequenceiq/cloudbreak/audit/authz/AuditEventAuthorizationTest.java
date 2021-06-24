package com.sequenceiq.cloudbreak.audit.authz;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.utils.EventAuthorizationUtils;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;

@ExtendWith(MockitoExtension.class)
public class AuditEventAuthorizationTest {

    @Mock
    private EventAuthorizationUtils mockEventAuthorizationUtils;

    private AuditEventAuthorization underTest;

    @BeforeEach
    void setUp() {
        underTest = new AuditEventAuthorization(mockEventAuthorizationUtils);
    }

    @Test
    public void testCheckPermissionsWithSingleEvent() {
        OperationDetails operationDetails = new OperationDetails();
        operationDetails.setResourceCrn("testCrn");
        operationDetails.setResourceType("Environment");
        StructuredFlowEvent flowEvent = new StructuredFlowEvent();
        flowEvent.setOperation(operationDetails);
        AuditEventV4Response auditEventV4Response = new AuditEventV4Response(1L, flowEvent);

        Set<AuditEventV4Response> auditEvents = Collections.singleton(auditEventV4Response);

        underTest.checkPermissions(auditEvents, operationDetails.getResourceType());

        verify(mockEventAuthorizationUtils).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    @Test
    public void testCheckPermissionsWithMultipleEvents() {
        Set<AuditEventV4Response> auditEvents = new HashSet<>();
        for (long i = 0; i < 5; i++) {
            OperationDetails operationDetails = new OperationDetails();
            operationDetails.setResourceCrn("testCrn" + i);
            operationDetails.setResourceType("testResourceType" + i);
            StructuredFlowEvent flowEvent = new StructuredFlowEvent();
            flowEvent.setOperation(operationDetails);
            AuditEventV4Response auditEventV4Response = new AuditEventV4Response(i, flowEvent);
            auditEvents.add(auditEventV4Response);
        }

        underTest.checkPermissions(auditEvents, "testResourceType");

        verify(mockEventAuthorizationUtils, times(1)).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    @Test
    public void testCheckPermissionsWithNoEvents() {
        Set<AuditEventV4Response> auditEvents = Collections.emptySet();

        underTest.checkPermissions(auditEvents, "testResourceType");

        verify(mockEventAuthorizationUtils, times(0)).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    @Test
    public void testCheckPermissionsWithNullEvents() {
        underTest.checkPermissions(null, "testResourceType");

        verify(mockEventAuthorizationUtils, times(0)).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    @Test
    public void testCheckPermissionsWithDifferentResourceTypes() {
        Set<AuditEventV4Response> auditEvents = new HashSet<>();
        for (long i = 0; i < 5; i++) {
            OperationDetails operationDetails = new OperationDetails();
            operationDetails.setResourceCrn("testCrn");
            operationDetails.setResourceType("testResourceType" + i);
            StructuredFlowEvent flowEvent = new StructuredFlowEvent();
            flowEvent.setOperation(operationDetails);
            AuditEventV4Response auditEventV4Response = new AuditEventV4Response(i, flowEvent);
            auditEvents.add(auditEventV4Response);
        }

        underTest.checkPermissions(auditEvents, "testResourceType");

        verify(mockEventAuthorizationUtils, times(1)).checkPermissionBasedOnResourceTypeAndCrn(any());
    }

    private enum TestResourceTypes {
        ENVIRONMENT,
        FLOW,

    }

}