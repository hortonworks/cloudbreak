package com.sequenceiq.cloudbreak.service.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.users.UserProfileRequest;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

public class UserProfileServiceTest {

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
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getShouldAddUIProps() {
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());
        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        String account = "account1";
        String owner = "123-123-123";
        userProfileService.getOrCreate(account, owner, user);
        verify(userProfileRepository).save(userProfileCaptor.capture());

        UserProfile capturedProfile = userProfileCaptor.getValue();
        assertNotNull(capturedProfile.getUiProperties());
    }

    @Test
    public void getWithoutUsername() {
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());
        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        String account = "account1";
        String owner = "123-123-123";
        UserProfile returnedUserProfile = userProfileService.getOrCreate(account, owner, user);
        verify(userProfileRepository).save(userProfileCaptor.capture());

        UserProfile capturedProfile = userProfileCaptor.getValue();
        assertEquals(account, capturedProfile.getAccount());
        assertEquals(owner, capturedProfile.getOwner());
        assertNull("username should be null", capturedProfile.getUserName());

        assertNotNull(returnedUserProfile);
    }

    @Test
    public void getWithUsername() {
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());
        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        String account = "account1";
        String owner = "123-123-123";
        String username = "test@hortonworks.com";
        UserProfile returnedUserProfile = userProfileService.getOrCreate(account, owner, username, user);
        verify(userProfileRepository).save(userProfileCaptor.capture());

        UserProfile capturedProfile = userProfileCaptor.getValue();
        assertEquals(account, capturedProfile.getAccount());
        assertEquals(owner, capturedProfile.getOwner());
        assertEquals(username, capturedProfile.getUserName());

        assertNotNull(returnedUserProfile);
    }

    @Test
    public void getFillUserNameIfEmpty() {
        String account = "account1";
        String owner = "123-123-123";
        UserProfile foundProfile = new UserProfile();
        foundProfile.setOwner(owner);
        foundProfile.setAccount(account);

        String username = "test@hortonworks.com";

        when(userProfileRepository.findOneByOwnerAndAccount(anyString(), anyString())).thenReturn(foundProfile);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());

        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        UserProfile returnedUserProfile = userProfileService.getOrCreate(account, owner, username, user);
        verify(userProfileRepository).save(userProfileCaptor.capture());

        UserProfile capturedProfile = userProfileCaptor.getValue();
        assertEquals(account, capturedProfile.getAccount());
        assertEquals(owner, capturedProfile.getOwner());
        assertEquals(username, capturedProfile.getUserName());

        assertNotNull(returnedUserProfile);
    }

    @Test
    public void getEmptyUserName() {
        String account = "account1";
        String owner = "123-123-123";
        UserProfile foundProfile = new UserProfile();
        foundProfile.setUser(user);
        foundProfile.setOwner(owner);
        foundProfile.setAccount(account);

        when(userProfileRepository.findOneByOwnerAndAccount(anyString(), anyString())).thenReturn(foundProfile);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());

        UserProfile returnedUserProfile = userProfileService.getOrCreate(account, owner, user);
        verify(userProfileRepository, never()).save(any(UserProfile.class));

        assertEquals(account, returnedUserProfile.getAccount());
        assertEquals(owner, returnedUserProfile.getOwner());
        assertNotNull(returnedUserProfile);
        assertNull(returnedUserProfile.getUserName());
    }

    @Test
    public void getShouldntDeleteUserName() {
        String account = "account1";
        String owner = "123-123-123";
        String savedUserName = "test@hortonworks.com";

        UserProfile foundProfile = new UserProfile();
        foundProfile.setUser(user);
        foundProfile.setOwner(owner);
        foundProfile.setAccount(account);
        foundProfile.setUserName(savedUserName);

        when(userProfileRepository.findOneByOwnerAndAccount(anyString(), anyString())).thenReturn(foundProfile);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());

        UserProfile returnedUserProfile = userProfileService.getOrCreate(account, owner, user);
        verify(userProfileRepository, never()).save(any(UserProfile.class));

        assertNotNull(returnedUserProfile);
        assertEquals(account, returnedUserProfile.getAccount());
        assertEquals(owner, returnedUserProfile.getOwner());
        assertEquals(savedUserName, returnedUserProfile.getUserName());
    }

    @Test
    public void testAddDefaultCredentials() {
        CloudbreakUser cloudbreakUser = new CloudbreakUser(null, null, null);
        UserProfile userProfile = new UserProfile();
        userProfile.setUser(new User());
        Workspace workspace = createWorkspace(1L);
        UserProfileRequest userProfileRequest = createUserProfileRequest(null, 1L);

        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(userProfile);
        when(credentialService.get(anyLong(), any())).thenReturn(createCredential(1L, null, workspace));

        userProfileService.put(userProfileRequest, cloudbreakUser, null, workspace);
        assertEquals(1, userProfile.getDefaultCredentials().size());

        userProfileService.put(userProfileRequest, cloudbreakUser, null, workspace);
        assertEquals(1, userProfile.getDefaultCredentials().size());

        workspace = createWorkspace(2L);
        userProfileRequest = createUserProfileRequest("cred", null);

        when(credentialService.getByNameForWorkspace(anyString(), any())).thenReturn(createCredential(2L, "cred", workspace));

        userProfileService.put(userProfileRequest, cloudbreakUser, null, workspace);
        assertEquals(2, userProfile.getDefaultCredentials().size());
    }

    private UserProfileRequest createUserProfileRequest(String credentialName, Long credentialId) {
        UserProfileRequest request = new UserProfileRequest();
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