package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;

class CredentialDeleteServiceTest {

    private static final String ACCOUNT_ID = "1";

    private CredentialDeleteService underTest;

    @Mock
    private CredentialRetrievalService credentialRetrievalService;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private CredentialNotificationService credentialNotificationService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new CredentialDeleteService(credentialRetrievalService, credentialNotificationService,
                environmentViewService, ownerAssignmentService, credentialRepository, Set.of("AWS", "AZURE", "YARN"));
    }

    @Test
    void testMultipleIfAllTheCredentialsAreExistsAndAbleToArchiveThenExpectedCredentialsAreComingBack() {
        String firstCredentialName = "first";
        String secondCredentialName = "second";
        Credential firstCred = createCredentialWithName(firstCredentialName);
        Credential secondCred = createCredentialWithName(secondCredentialName);
        Set<String> names = Set.of(firstCredentialName, secondCredentialName);
        when(credentialRetrievalService.findByNameAndAccountId(eq(firstCredentialName), eq(ACCOUNT_ID), any(Set.class), any()))
                .thenReturn(Optional.of(firstCred));
        when(credentialRetrievalService.findByNameAndAccountId(eq(secondCredentialName), eq(ACCOUNT_ID), any(Set.class), any()))
                .thenReturn(Optional.of(secondCred));
        when(credentialRepository.save(firstCred)).thenReturn(firstCred);
        when(credentialRepository.save(secondCred)).thenReturn(secondCred);
        doNothing().when(ownerAssignmentService).notifyResourceDeleted(any());

        Set<Credential> result = underTest.deleteMultiple(names, ACCOUNT_ID, ENVIRONMENT);

        assertNotNull(result);
        assertEquals(2L, result.size());
        assertTrue(result.stream().anyMatch(credential -> credential.getName().startsWith(firstCredentialName)));
        assertTrue(result.stream().anyMatch(credential -> credential.getName().startsWith(secondCredentialName)));
        assertTrue(result.stream().allMatch(Credential::isArchived));

        verify(credentialRetrievalService, times(2)).findByNameAndAccountId(anyString(), anyString(), anyCollection(), any());
        verify(environmentViewService, times(2)).findAllByCredentialId(any());
        verify(credentialRepository, times(2)).save(any());
        verify(credentialRepository, times(1)).save(firstCred);
        verify(credentialRepository, times(1)).save(secondCred);
        verify(ownerAssignmentService, times(2)).notifyResourceDeleted(any());
    }

    @Test
    void testMultipleIfOneOfTheCredentialsIsNotExistsThenNotFoundExceptionComes() {
        when(credentialRetrievalService.findByNameAndAccountId(anyString(), anyString(), any(Set.class), any())).thenReturn(Optional.empty());
        Set<Credential> result = underTest.deleteMultiple(Set.of("someCredNameWhichDoesNotExists"), ACCOUNT_ID, ENVIRONMENT);

        verify(credentialRetrievalService, times(1)).findByNameAndAccountId(anyString(), anyString(), anyCollection(), any());
        verify(environmentViewService, times(0)).findAllByCredentialId(anyLong());
        verify(credentialRepository, times(0)).save(any());
        assertTrue(result.isEmpty());
    }

    @Test
    void testMultipleWhenEnvironmentStillUsesTheCredential2ThenBadRequestShouldComeOnSecondButTheFirstDeletionSuccess() {
        doNothing().when(ownerAssignmentService).notifyResourceDeleted(any());
        String name1 = "something1";
        Credential cred1 = createCredentialWithName(name1);
        cred1.setId(1L);
        when(credentialRetrievalService.findByNameAndAccountId(eq(name1), eq(ACCOUNT_ID), any(Set.class), any())).thenReturn(Optional.of(cred1));
        when(credentialRepository.save(any())).thenReturn(cred1);

        String name2 = "something2";
        Credential cred2 = createCredentialWithName(name2);
        cred2.setId(2L);
        when(credentialRetrievalService.findByNameAndAccountId(eq(name2), eq(ACCOUNT_ID), any(Set.class), any()))
                .thenThrow(new BadRequestException("anything can happen"));

        when(environmentViewService.findAllByCredentialId(cred1.getId())).thenReturn(Set.of());

        Set<Credential> result = underTest.deleteMultiple(Set.of(name1, name2), ACCOUNT_ID, ENVIRONMENT);

        verify(credentialRetrievalService, times(2)).findByNameAndAccountId(anyString(), anyString(), anyCollection(), any());
        verify(environmentViewService, times(1)).findAllByCredentialId(anyLong());
        verify(environmentViewService, times(1)).findAllByCredentialId(cred1.getId());
        verify(credentialRepository, times(1)).save(any());
        verify(ownerAssignmentService, times(1)).notifyResourceDeleted(any());
        assertTrue(result.size() == 1);
        assertTrue(result.iterator().next().getName().startsWith(name1));
    }

    @Test
    void testWhenCredentialDeleteIsSuccessfulThenNotificationShouldBeSent() {
        Credential cred = createCredentialWithName("first");
        when(credentialRetrievalService.findByNameAndAccountId(eq(cred.getName()), eq(ACCOUNT_ID), any(Set.class), any())).thenReturn(Optional.of(cred));
        when(credentialRepository.save(cred)).thenReturn(cred);
        doNothing().when(ownerAssignmentService).notifyResourceDeleted(any());

        underTest.deleteMultiple(Set.of(cred.getName()), ACCOUNT_ID, ENVIRONMENT);

        verify(ownerAssignmentService, times(1)).notifyResourceDeleted(any());
    }

    private Credential createCredentialWithName(String name) {
        Credential cred = new Credential();
        cred.setName(name);
        return cred;
    }

}
