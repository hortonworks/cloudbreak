package com.sequenceiq.cloudbreak.service.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests.UserProfileV4Request;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

public class UserProfileServiceTest {

    private static final String USERNAME = "test@hortonworks.com";

    @Mock
    private CredentialService credentialService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserService userService;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private UserProfileService userProfileService;

    private final User user = new User();

    @Before
    public void before() {
        user.setId(1L);
        user.setUserName(USERNAME);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getShouldAddUIProps() {
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());
        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        userProfileService.getOrCreate(user);
        verify(userProfileRepository).save(userProfileCaptor.capture());

        UserProfile capturedProfile = userProfileCaptor.getValue();
        assertNotNull(capturedProfile.getUiProperties());
    }

    @Test
    public void getWithUsername() {
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());
        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        UserProfile returnedUserProfile = userProfileService.getOrCreate(user);
        verify(userProfileRepository).save(userProfileCaptor.capture());

        UserProfile capturedProfile = userProfileCaptor.getValue();
        assertEquals(USERNAME, capturedProfile.getUserName());

        assertNotNull(returnedUserProfile);
    }

    @Test
    public void getShouldntDeleteUserName() {
        UserProfile foundProfile = new UserProfile();
        foundProfile.setUser(user);
        foundProfile.setUserName(USERNAME);

        when(userProfileRepository.findOneByUser(anyLong())).thenReturn(Optional.of(foundProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());

        UserProfile returnedUserProfile = userProfileService.getOrCreate(user);
        verify(userProfileRepository, never()).save(any(UserProfile.class));

        assertNotNull(returnedUserProfile);
        assertEquals(USERNAME, returnedUserProfile.getUserName());
    }

    @Test
    public void testAddDefaultCredentials() {
        User user = new User();
        UserProfile userProfile = new UserProfile();
        userProfile.setUser(new User());
        Workspace workspace = createWorkspace(1L);
        UserProfileV4Request userProfileV4Request = createUserProfileRequest(null, 1L);

        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(userProfile);
        when(credentialService.get(anyLong(), any())).thenReturn(createCredential(1L, null, workspace));

        userProfileService.put(userProfileV4Request, user, workspace);
        assertEquals(1, userProfile.getDefaultCredentials().size());

        userProfileService.put(userProfileV4Request, user, workspace);
        assertEquals(1, userProfile.getDefaultCredentials().size());

        workspace = createWorkspace(2L);
        userProfileV4Request = createUserProfileRequest("cred", null);

        when(credentialService.getByNameForWorkspace(anyString(), any())).thenReturn(createCredential(2L, "cred", workspace));

        userProfileService.put(userProfileV4Request, user, workspace);
        assertEquals(2, userProfile.getDefaultCredentials().size());
    }

    public void testGetShowTerminatedClustersPreferences() {
        User user = new User();
        when(userService.getOrCreate(any())).thenReturn(user);

    }

    private UserProfileV4Request createUserProfileRequest(String credentialName, Long credentialId) {
        UserProfileV4Request request = new UserProfileV4Request();
        request.setCredentialId(credentialId);
        request.setCredentialName(credentialName);
        return request;
    }

    private Credential createCredential(Long id, String name, Workspace workspace) {
        Credential credential = new Credential();
        credential.setId(id);
        credential.setName(name);
        credential.setWorkspace(workspace);
        return credential;
    }

    private Workspace createWorkspace(Long id) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        return workspace;
    }
}