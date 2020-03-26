package com.sequenceiq.environment.credential.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;
import com.sequenceiq.notification.Notification;
import com.sequenceiq.notification.NotificationSender;

class CredentialDeleteServiceTest {

    private static final String ACCOUNT_ID = "1";

    private CredentialDeleteService underTest;

    @Mock
    private CredentialService credentialService;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new CredentialDeleteService(credentialService, notificationSender, messagesService,
                environmentViewService, grpcUmsClient, Set.of("AWS", "AZURE", "YARN"));
    }

    @Test
    void testMultipleIfAllTheCredentialsAreExistsAndAbleToArchiveThenExpectedCredentialsAreComingBack() {
        String firstCredentialName = "first";
        String secondCredentialName = "second";
        Credential firstCred = createCredentialWithName(firstCredentialName);
        Credential secondCred = createCredentialWithName(secondCredentialName);
        Set<String> names = Set.of(firstCredentialName, secondCredentialName);
        when(credentialService.findByNameAndAccountId(eq(firstCredentialName), eq(ACCOUNT_ID), any(Set.class))).thenReturn(Optional.of(firstCred));
        when(credentialService.findByNameAndAccountId(eq(secondCredentialName), eq(ACCOUNT_ID), any(Set.class))).thenReturn(Optional.of(secondCred));
        when(credentialService.save(firstCred)).thenReturn(firstCred);
        when(credentialService.save(secondCred)).thenReturn(secondCred);
        doNothing().when(grpcUmsClient).notifyResourceDeleted(any(), any(), any());

        Set<Credential> result = underTest.deleteMultiple(names, ACCOUNT_ID);

        assertNotNull(result);
        assertEquals(2L, result.size());
        assertTrue(result.stream().anyMatch(credential -> credential.getName().startsWith(firstCredentialName)));
        assertTrue(result.stream().anyMatch(credential -> credential.getName().startsWith(secondCredentialName)));
        assertTrue(result.stream().allMatch(Credential::isArchived));

        verify(credentialService, times(2)).findByNameAndAccountId(anyString(), anyString(), anyCollection());
        verify(environmentViewService, times(2)).findAllByCredentialId(any());
        verify(credentialService, times(2)).save(any());
        verify(credentialService, times(1)).save(firstCred);
        verify(credentialService, times(1)).save(secondCred);
        verify(grpcUmsClient, times(2)).notifyResourceDeleted(any(), any(), any());
    }

    @Test
    void testMultipleIfOneOfTheCredentialsIsNotExistsThenNotFoundExceptionComes() {
        when(credentialService.findByNameAndAccountId(anyString(), anyString(), any(Set.class))).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTest.deleteMultiple(Set.of("someCredNameWhichDoesNotExists"), ACCOUNT_ID));

        verify(credentialService, times(1)).findByNameAndAccountId(anyString(), anyString(), anyCollection());
        verify(environmentViewService, times(0)).findAllByCredentialId(anyLong());
        verify(credentialService, times(0)).save(any());
    }

    @Test
    void testMultipleWhenEnvironmentStillUsesTheCredentialThenBadRequestShouldCome() {
        String name = "something";
        Credential cred = createCredentialWithName(name);
        cred.setId(1L);
        when(credentialService.findByNameAndAccountId(eq(name), eq(ACCOUNT_ID), any(Set.class))).thenReturn(Optional.of(cred));
        when(environmentViewService.findAllByCredentialId(cred.getId())).thenReturn(Set.of(new EnvironmentView()));

        assertThrows(BadRequestException.class, () -> underTest.deleteMultiple(Set.of(name), ACCOUNT_ID));

        verify(credentialService, times(1)).findByNameAndAccountId(anyString(), anyString(), anyCollection());
        verify(environmentViewService, times(1)).findAllByCredentialId(anyLong());
        verify(environmentViewService, times(1)).findAllByCredentialId(cred.getId());
        verify(credentialService, times(0)).save(any());
    }

    @Test
    void testWhenCredentialDeleteIsSuccessfulThenNotificationShouldBeSent() {
        Credential cred = createCredentialWithName("first");
        when(credentialService.findByNameAndAccountId(eq(cred.getName()), eq(ACCOUNT_ID), any(Set.class))).thenReturn(Optional.of(cred));
        when(credentialService.save(cred)).thenReturn(cred);
        doNothing().when(grpcUmsClient).notifyResourceDeleted(any(), any(), any());

        underTest.deleteMultiple(Set.of(cred.getName()), ACCOUNT_ID);

        notificationSender.send(any(Notification.class), any(List.class), any(Client.class));

        verify(grpcUmsClient, times(1)).notifyResourceDeleted(any(), any(), any());
    }

    private Credential createCredentialWithName(String name) {
        Credential cred = new Credential();
        cred.setName(name);
        return cred;
    }

}
