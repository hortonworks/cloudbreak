package com.sequenceiq.cloudbreak.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests.UserProfileV4Request;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.workspace.model.User;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final String USERNAME = "test@hortonworks.com";

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserService userService;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private UserProfileService userProfileService;

    private final User user = new User();

    @BeforeEach
    public void before() {
        user.setId(1L);
        user.setUserName(USERNAME);
    }

    @Test
    void getShouldAddUIProps() {
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());
        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        userProfileService.getOrCreate(user);
        verify(userProfileRepository).save(userProfileCaptor.capture());

        UserProfile capturedProfile = userProfileCaptor.getValue();
        assertNotNull(capturedProfile.getUiProperties());
    }

    @Test
    void getWithUsername() {
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());
        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        UserProfile returnedUserProfile = userProfileService.getOrCreate(user);
        verify(userProfileRepository).save(userProfileCaptor.capture());

        UserProfile capturedProfile = userProfileCaptor.getValue();
        assertEquals(USERNAME, capturedProfile.getUserName());

        assertNotNull(returnedUserProfile);
    }

    @Test
    void getShouldntDeleteUserName() {
        UserProfile foundProfile = new UserProfile();
        foundProfile.setUser(user);
        foundProfile.setUserName(USERNAME);

        when(userProfileRepository.findOneByUser(anyLong())).thenReturn(Optional.of(foundProfile));

        UserProfile returnedUserProfile = userProfileService.getOrCreate(user);
        verify(userProfileRepository, never()).save(any(UserProfile.class));

        assertNotNull(returnedUserProfile);
        assertEquals(USERNAME, returnedUserProfile.getUserName());
    }

    private UserProfileV4Request createUserProfileRequest(String credentialName, Long credentialId) {
        UserProfileV4Request request = new UserProfileV4Request();
        request.setCredentialId(credentialId);
        request.setCredentialName(credentialName);
        return request;
    }

}