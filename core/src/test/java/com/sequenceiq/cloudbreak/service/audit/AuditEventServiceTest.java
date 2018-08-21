package com.sequenceiq.cloudbreak.service.audit;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
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
    private OrganizationService organizationService;

    @Mock
    private Organization testOrganization;

    @InjectMocks
    private AuditEventService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(testOrganization.getId()).thenReturn(TEST_DEFAULT_ORG_ID);
        when(organizationService.getDefaultOrganizationForCurrentUser()).thenReturn(testOrganization);
    }

    @Test
    public void testGetAuditEventWhenEventExistsAndHasPermissionToReadItThenTheExpectedEventShouldReturn() {
        AuditEvent expected = mock(AuditEvent.class);
        StructuredEventEntity repoResult = new StructuredEventEntity();
        when(conversionService.convert(repoResult, AuditEvent.class)).thenReturn(expected);
        when(structuredEventRepository.findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(repoResult);

        AuditEvent actual = underTest.getAuditEvent(TEST_AUDIT_ID);

        Assert.assertEquals(expected, actual);
        verify(conversionService, times(1)).convert(repoResult, AuditEvent.class);
        verify(structuredEventRepository, times(1)).findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(organizationService, times(1)).getDefaultOrganizationForCurrentUser();
    }

    @Test
    public void testGetAuditEventWhenThereIsNoRecordForGivenAuditIdThenNotFoundExceptionShouldCome() {
        when(structuredEventRepository.findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(null);

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(NOT_FOUND_EXCEPTION_MESSAGE);

        underTest.getAuditEvent(TEST_AUDIT_ID);

        verify(organizationService, times(1)).getDefaultOrganizationForCurrentUser();
        verify(structuredEventRepository, times(1)).findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(conversionService, times(0)).convert(any(StructuredEventEntity.class), AuditEvent.class);
    }

    @Test
    public void testGetAuditEventWhenUserHasNoRightToReadEntryThenAccessDeniedEceptionShouldCome() {
        when(structuredEventRepository.findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenThrow(new AccessDeniedException(REPO_ACCESS_DENIED_MESSAGE));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(REPO_ACCESS_DENIED_MESSAGE);

        underTest.getAuditEvent(TEST_AUDIT_ID);

        verify(organizationService, times(1)).getDefaultOrganizationForCurrentUser();
        verify(structuredEventRepository, times(1)).findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(conversionService, times(0)).convert(any(StructuredEventEntity.class), AuditEvent.class);
    }

    @Test
    public void testGetAuditEventByOrgIdWhenEventExistsAndHasPermissionToReadItThenTheExpectedEventShouldReturn() {
        AuditEvent expected = mock(AuditEvent.class);
        StructuredEventEntity repoResult = new StructuredEventEntity();
        when(conversionService.convert(repoResult, AuditEvent.class)).thenReturn(expected);
        when(structuredEventRepository.findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(repoResult);

        AuditEvent actual = underTest.getAuditEventByOrgId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);

        Assert.assertEquals(expected, actual);
        verify(conversionService, times(1)).convert(repoResult, AuditEvent.class);
        verify(structuredEventRepository, times(1)).findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
    }

    @Test
    public void testGetAuditEventByOrgIdWhenThereIsNoRecordForGivenAuditIdThenNotFoundExceptionShouldCome() {
        when(structuredEventRepository.findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenReturn(null);

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(NOT_FOUND_EXCEPTION_MESSAGE);

        underTest.getAuditEvent(TEST_AUDIT_ID);

        verify(structuredEventRepository, times(1)).findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(conversionService, times(0)).convert(any(StructuredEventEntity.class), AuditEvent.class);
    }

    @Test
    public void testGetAuditEventByOrgIdWhenUserHasNoRightToReadEntryThenAccessDeniedEceptionShouldCome() {
        when(structuredEventRepository.findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID)).thenThrow(new AccessDeniedException(REPO_ACCESS_DENIED_MESSAGE));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(REPO_ACCESS_DENIED_MESSAGE);

        underTest.getAuditEvent(TEST_AUDIT_ID);

        verify(structuredEventRepository, times(1)).findByOrgIdAndId(TEST_DEFAULT_ORG_ID, TEST_AUDIT_ID);
        verify(conversionService, times(0)).convert(any(StructuredEventEntity.class), AuditEvent.class);
    }
}