package com.sequenceiq.cloudbreak.service.audit;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.db.StructuredEventRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuditEventServiceTest {

    private static final Long TEST_AUDIT_ID = 1L;

    private static final Long TEST_DEFAULT_ORG_ID = 2L;

    private static final String REPO_ACCESS_DENIED_MESSAGE = "You have no access for this resource.";

    private static final String NOT_FOUND_EXCEPTION_MESSAGE = String.format("StructuredEvent '%d' not found", TEST_AUDIT_ID);

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ConversionService conversionService;

    @Mock
    private StructuredEventRepository structuredEventRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private Workspace testWorkspace;

    @Mock
    private UserService userService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private User user;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @InjectMocks
    private AuditEventService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(testWorkspace.getId()).thenReturn(TEST_DEFAULT_ORG_ID);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(userService.getOrCreate(cloudbreakUser)).thenReturn(user);
        when(workspaceService.getDefaultWorkspaceForUser(user)).thenReturn(testWorkspace);
    }

    @Test
    public void testGetAuditEventWhenEventExistsAndHasPermissionToReadItThenTheExpectedEventShouldReturn() {
        AuditEventV4Response expected = mock(AuditEventV4Response.class);
        StructuredEventEntity repoResult = new StructuredEventEntity();
        when(conversionService.convert(repoResult, AuditEventV4Response.class)).thenReturn(expected);
        when(structuredEventRepository.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(repoResult);

        AuditEventV4Response actual = underTest.getAuditEvent(TEST_AUDIT_ID);

        Assert.assertEquals(expected, actual);
        verify(conversionService, times(1)).convert(repoResult, AuditEventV4Response.class);
        verify(structuredEventRepository, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
    }

    @Test
    public void testGetAuditEventWhenThereIsNoRecordForGivenAuditIdThenNotFoundExceptionShouldCome() {
        when(structuredEventRepository.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(null);

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(NOT_FOUND_EXCEPTION_MESSAGE);

        underTest.getAuditEvent(TEST_AUDIT_ID);

        verify(structuredEventRepository, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(conversionService, times(0)).convert(any(StructuredEventEntity.class), AuditEventV4Response.class);
    }

    @Test
    public void testGetAuditEventWhenUserHasNoRightToReadEntryThenAccessDeniedEceptionShouldCome() {
        when(structuredEventRepository.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID))
                .thenThrow(new AccessDeniedException(REPO_ACCESS_DENIED_MESSAGE));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(REPO_ACCESS_DENIED_MESSAGE);

        underTest.getAuditEvent(TEST_AUDIT_ID);

        verify(structuredEventRepository, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(conversionService, times(0)).convert(any(StructuredEventEntity.class), AuditEventV4Response.class);
    }

    @Test
    public void testGetAuditEventByWorkspaceIdWhenEventExistsAndHasPermissionToReadItThenTheExpectedEventShouldReturn() {
        AuditEventV4Response expected = mock(AuditEventV4Response.class);
        StructuredEventEntity repoResult = new StructuredEventEntity();
        when(conversionService.convert(repoResult, AuditEventV4Response.class)).thenReturn(expected);
        when(structuredEventRepository.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(repoResult);

        AuditEventV4Response actual = underTest.getAuditEventByWorkspaceId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);

        Assert.assertEquals(expected, actual);
        verify(conversionService, times(1)).convert(repoResult, AuditEventV4Response.class);
        verify(structuredEventRepository, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
    }

    @Test
    public void testGetAuditEventByWorkspaceIdWhenThereIsNoRecordForGivenAuditIdThenNotFoundExceptionShouldCome() {
        when(structuredEventRepository.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(null);

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(NOT_FOUND_EXCEPTION_MESSAGE);

        underTest.getAuditEvent(TEST_AUDIT_ID);

        verify(structuredEventRepository, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(conversionService, times(0)).convert(any(StructuredEventEntity.class), AuditEventV4Response.class);
    }

    @Test
    public void testGetAuditEventByWorkspaceIdWhenUserHasNoRightToReadEntryThenAccessDeniedEceptionShouldCome() {
        when(structuredEventRepository.findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID))
                .thenThrow(new AccessDeniedException(REPO_ACCESS_DENIED_MESSAGE));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(REPO_ACCESS_DENIED_MESSAGE);

        underTest.getAuditEvent(TEST_AUDIT_ID);

        verify(structuredEventRepository, times(1)).findByWorkspaceIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(conversionService, times(0)).convert(any(StructuredEventEntity.class), AuditEventV4Response.class);
    }
}