package com.sequenceiq.cloudbreak.service.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.v4.audit.StructuredEventEntityToAuditEventV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.db.LegacyStructuredEventDBService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    private static final Long TEST_AUDIT_ID = 1L;

    private static final Long TEST_DEFAULT_ORG_ID = 2L;

    private static final String REPO_ACCESS_DENIED_MESSAGE = "You have no access for this resource.";

    private static final String NOT_FOUND_EXCEPTION_MESSAGE = String.format("StructuredEvent '%d' not found", TEST_AUDIT_ID);

    @Mock
    private LegacyStructuredEventDBService legacyStructuredEventDBService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private Workspace testWorkspace;

    @Mock
    private UserService userService;

    @Mock
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private User user;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private StructuredEventEntityToAuditEventV4ResponseConverter structuredEventEntityToAuditEventV4ResponseConverter;

    @InjectMocks
    private AuditEventService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(testWorkspace.getId()).thenReturn(TEST_DEFAULT_ORG_ID);
        lenient().when(legacyRestRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        lenient().when(userService.getOrCreate(cloudbreakUser)).thenReturn(user);
        lenient().when(workspaceService.getDefaultWorkspaceForUser(user)).thenReturn(testWorkspace);
    }

    @Test
    void testGetAuditEventWhenEventExistsAndHasPermissionToReadItThenTheExpectedEventShouldReturn() {
        AuditEventV4Response expected = mock(AuditEventV4Response.class);
        StructuredEventEntity repoResult = new StructuredEventEntity();
        when(structuredEventEntityToAuditEventV4ResponseConverter.convert(repoResult)).thenReturn(expected);
        when(legacyStructuredEventDBService.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(repoResult);

        AuditEventV4Response actual = underTest.getAuditEvent(TEST_AUDIT_ID);

        assertEquals(expected, actual);
        verify(structuredEventEntityToAuditEventV4ResponseConverter, times(1)).convert(repoResult);
        verify(legacyStructuredEventDBService, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
    }

    @Test
    void testGetAuditEventWhenThereIsNoRecordForGivenAuditIdThenNotFoundExceptionShouldCome() {
        when(legacyStructuredEventDBService.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> underTest.getAuditEvent(TEST_AUDIT_ID), NOT_FOUND_EXCEPTION_MESSAGE);

        verify(legacyStructuredEventDBService, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(structuredEventEntityToAuditEventV4ResponseConverter, times(0)).convert(any(StructuredEventEntity.class));
    }

    @Test
    void testGetAuditEventWhenUserHasNoRightToReadEntryThenAccessDeniedEceptionShouldCome() {
        when(legacyStructuredEventDBService.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID))
                .thenThrow(new ForbiddenException(REPO_ACCESS_DENIED_MESSAGE));

        assertThrows(ForbiddenException.class, () -> underTest.getAuditEvent(TEST_AUDIT_ID), REPO_ACCESS_DENIED_MESSAGE);

        verify(legacyStructuredEventDBService, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(structuredEventEntityToAuditEventV4ResponseConverter, times(0)).convert(any(StructuredEventEntity.class));
    }

    @Test
    void testGetAuditEventByWorkspaceIdWhenEventExistsAndHasPermissionToReadItThenTheExpectedEventShouldReturn() {
        AuditEventV4Response expected = mock(AuditEventV4Response.class);
        StructuredEventEntity repoResult = new StructuredEventEntity();
        when(structuredEventEntityToAuditEventV4ResponseConverter.convert(repoResult)).thenReturn(expected);
        when(legacyStructuredEventDBService.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(repoResult);

        AuditEventV4Response actual = underTest.getAuditEventByWorkspaceId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);

        assertEquals(expected, actual);
        verify(structuredEventEntityToAuditEventV4ResponseConverter, times(1)).convert(repoResult);
        verify(legacyStructuredEventDBService, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
    }

    @Test
    void testGetAuditEventByWorkspaceIdWhenThereIsNoRecordForGivenAuditIdThenNotFoundExceptionShouldCome() {
        when(legacyStructuredEventDBService.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> underTest.getAuditEvent(TEST_AUDIT_ID), NOT_FOUND_EXCEPTION_MESSAGE);

        verify(legacyStructuredEventDBService, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(structuredEventEntityToAuditEventV4ResponseConverter, times(0)).convert(any(StructuredEventEntity.class));
    }

    @Test
    void testGetAuditEventByWorkspaceIdWhenUserHasNoRightToReadEntryThenAccessDeniedEceptionShouldCome() {
        when(legacyStructuredEventDBService.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID))
                .thenThrow(new ForbiddenException(REPO_ACCESS_DENIED_MESSAGE));

        assertThrows(ForbiddenException.class, () -> underTest.getAuditEvent(TEST_AUDIT_ID), REPO_ACCESS_DENIED_MESSAGE);

        verify(legacyStructuredEventDBService, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(structuredEventEntityToAuditEventV4ResponseConverter, times(0)).convert(any(StructuredEventEntity.class));
    }

    @Test
    void testGetEventsForUserWithTypeAndResourceIdByWorkspaceWhenResourceCrnIsNull() {
        Workspace workspace = new Workspace();
        long resourceId = 0L;

        when(legacyStructuredEventDBService.findByWorkspaceAndResourceTypeAndResourceId(workspace, null, resourceId)).thenReturn(Collections.emptyList());

        underTest.getEventsForUserWithTypeAndResourceIdByWorkspace(workspace, null, resourceId, null);

        verify(legacyStructuredEventDBService).findByWorkspaceAndResourceTypeAndResourceId(workspace, null, resourceId);
    }

    @Test
    void testGetEventsForUserWithTypeAndResourceIdByWorkspaceWhenResourceCrnIsEmpty() {
        Workspace workspace = new Workspace();
        long resourceId = 0L;

        when(legacyStructuredEventDBService.findByWorkspaceAndResourceTypeAndResourceId(workspace, null, resourceId)).thenReturn(Collections.emptyList());

        underTest.getEventsForUserWithTypeAndResourceIdByWorkspace(workspace, null, resourceId, "");

        verify(legacyStructuredEventDBService).findByWorkspaceAndResourceTypeAndResourceId(workspace, null, resourceId);
    }

    @Test
    void testGetEventsForUserWithTypeAndResourceIdByWorkspaceWhenResourceCrnIsNotEmpty() {
        Workspace workspace = new Workspace();
        long resourceId = 0L;
        String resourceCrn = "crn";

        when(legacyStructuredEventDBService.findByWorkspaceAndResourceTypeAndResourceCrn(workspace, resourceCrn)).thenReturn(Collections.emptyList());

        underTest.getEventsForUserWithTypeAndResourceIdByWorkspace(workspace, null, resourceId, resourceCrn);

        verify(legacyStructuredEventDBService, never()).findByWorkspaceAndResourceTypeAndResourceId(workspace, null, resourceId);
        verify(legacyStructuredEventDBService).findByWorkspaceAndResourceTypeAndResourceCrn(workspace, resourceCrn);
    }
}
