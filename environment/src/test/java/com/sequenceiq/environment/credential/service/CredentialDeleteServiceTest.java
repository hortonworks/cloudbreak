package com.sequenceiq.environment.credential.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;
import com.sequenceiq.notification.Notification;
import com.sequenceiq.notification.NotificationSender;

public class CredentialDeleteServiceTest {

    private static final String ACCOUNT_ID = "1";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private CredentialDeleteService underTest;

    @Mock
    private CredentialRepository repository;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Before
    public void setUp() {
            MockitoAnnotations.initMocks(this);
            underTest = new CredentialDeleteService(repository, notificationSender, messagesService, environmentViewService, Set.of("AWS", "AZURE", "YARN"));
    }

    @Test
    public void testMultipleIfAllTheCredentialsAreExistsAndAbleToArchiveThenExpectedCredentialsAreComingBack() {
        String firstCredentialName = "first";
        String secondCredentialName = "second";
        Credential firstCred = createCredentialWithName(firstCredentialName);
        Credential secondCred = createCredentialWithName(secondCredentialName);
        Set<String> names = Set.of(firstCredentialName, secondCredentialName);
        when(repository.findByNameAndAccountId(eq(firstCredentialName), eq(ACCOUNT_ID), any(Set.class))).thenReturn(Optional.of(firstCred));
        when(repository.findByNameAndAccountId(eq(secondCredentialName), eq(ACCOUNT_ID), any(Set.class))).thenReturn(Optional.of(secondCred));
        when(repository.save(firstCred)).thenReturn(firstCred);
        when(repository.save(secondCred)).thenReturn(secondCred);

        Set<Credential> result = underTest.deleteMultiple(names, ACCOUNT_ID);

        assertNotNull(result);
        assertEquals(2L, result.size());
        assertTrue(result.stream().anyMatch(credential -> credential.getName().startsWith(firstCredentialName)));
        assertTrue(result.stream().anyMatch(credential -> credential.getName().startsWith(secondCredentialName)));
        assertTrue(result.stream().allMatch(Credential::isArchived));

        verify(repository, times(2)).findByNameAndAccountId(anyString(), anyString(), anyCollection());
        verify(environmentViewService, times(2)).findAllByCredentialId(any());
        verify(repository, times(2)).save(any());
        verify(repository, times(1)).save(firstCred);
        verify(repository, times(1)).save(secondCred);
    }

    @Test
    public void testMultipleIfOneOfTheCredentialsIsNotExistsThenNotFoundExceptionComes() {
        when(repository.findByNameAndAccountId(anyString(), anyString(), any(Set.class))).thenReturn(Optional.empty());
        thrown.expect(NotFoundException.class);
        underTest.deleteMultiple(Set.of("someCredNameWhichDoesNotExists"), ACCOUNT_ID);

        verify(repository, times(1)).findByNameAndAccountId(anyString(), anyString(), anyCollection());
        verify(environmentViewService, times(0)).findAllByCredentialId(anyLong());
        verify(repository, times(0)).save(any());
    }

    @Test
    public void testMultipleWhenEnvironmentStillUsesTheCredentialThenBadRequestShouldCome() {
        String name = "something";
        Credential cred = createCredentialWithName(name);
        cred.setId(1L);
        when(repository.findByNameAndAccountId(eq(name), eq(ACCOUNT_ID), any(Set.class))).thenReturn(Optional.of(cred));
        when(environmentViewService.findAllByCredentialId(cred.getId())).thenReturn(Set.of(new EnvironmentView()));

        thrown.expect(BadRequestException.class);

        underTest.deleteMultiple(Set.of(name), ACCOUNT_ID);

        verify(repository, times(1)).findByNameAndAccountId(anyString(), anyString(), anyCollection());
        verify(environmentViewService, times(1)).findAllByCredentialId(anyLong());
        verify(environmentViewService, times(1)).findAllByCredentialId(cred.getId());
        verify(repository, times(0)).save(any());
    }

    @Test
    public void testWhenCredentialDeleteIsSuccessfulThenNotificationShouldBeSent() {
        Credential cred = createCredentialWithName("first");
        when(repository.findByNameAndAccountId(eq(cred.getName()), eq(ACCOUNT_ID), any(Set.class))).thenReturn(Optional.of(cred));
        when(repository.save(cred)).thenReturn(cred);

        underTest.deleteMultiple(Set.of(cred.getName()), ACCOUNT_ID);

        notificationSender.send(any(Notification.class), any(List.class), any(Client.class));
    }

    private Credential createCredentialWithName(String name) {
        Credential cred = new Credential();
        cred.setName(name);
        return cred;
    }

}
