package com.sequenceiq.cloudbreak.service.credential;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;

@RunWith(MockitoJUnitRunner.class)
public class CredentialServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private AccountPreferencesService accountPreferencesService;

    @InjectMocks
    private CredentialService credentialService = new CredentialService();

    @Test
    public void testRetrieveAccountCredentialsWhenUserIsAdmin() {

        IdentityUser user = mock(IdentityUser.class);
        Set<String> platforms = Sets.newHashSet("AWS");
        Credential credential = new Credential();
        credential.setCloudPlatform("AWS");

        when(user.getRoles()).thenReturn(Collections.singletonList(IdentityUserRole.fromString("ADMIN")));
        when(user.getAccount()).thenReturn("account");
        when(accountPreferencesService.enabledPlatforms()).thenReturn(platforms);
        when(credentialRepository.findAllInAccountAndFilterByPlatforms(user.getAccount(), platforms)).thenReturn(Sets.newHashSet(credential));

        Set<Credential> actual = credentialService.retrieveAccountCredentials(user);

        assertEquals("AWS", actual.stream().findFirst().get().cloudPlatform());

        verify(credentialRepository, times(1)).findAllInAccountAndFilterByPlatforms("account", platforms);
    }

    @Test
    public void testRetrieveAccountCredentialsWhenUserIsNotAdmin() {

        IdentityUser user = mock(IdentityUser.class);
        Set<String> platforms = Sets.newHashSet("AWS");
        Credential credential = new Credential();
        credential.setCloudPlatform("AWS");

        when(user.getRoles()).thenReturn(Collections.singletonList(IdentityUserRole.fromString("USER")));
        when(user.getAccount()).thenReturn("account");
        when(user.getUserId()).thenReturn("userId");
        when(accountPreferencesService.enabledPlatforms()).thenReturn(platforms);
        when(credentialRepository.findPublicInAccountForUserFilterByPlatforms("userId", "account", platforms)).thenReturn(Sets.newHashSet(credential));

        Set<Credential> actual = credentialService.retrieveAccountCredentials(user);

        assertEquals("AWS", actual.stream().findFirst().get().cloudPlatform());

        verify(credentialRepository, times(1)).findPublicInAccountForUserFilterByPlatforms("userId", "account", platforms);
    }

}