package com.sequenceiq.cloudbreak.service.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;

public class UserProfileHandlerTest {

    private static final String USER_ID_1 = "userId1";

    private static final String USER_CRN_1 = "userCrn1";

    private static final String TENANT_A = "tenant";

    private static final String USERNAME_1 = "username";

    private static final String EMAIL_1 = "username";

    private static final String CREDENTIAL_A = "credential-a";

    private static final String CREDENTIAL_B = "credential-b";

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private UserProfileHandler underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateProfilePreparation() {
        CloudbreakUser cloudbreakUser = createIdentityUser();
        User user = createUser();
        UserProfile userProfile = createUserProfile(Collections.emptySet());
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(userProfileService.getOrCreate(user)).thenReturn(userProfile);

        underTest.createProfilePreparation(createCredential(CREDENTIAL_A), user);

        assertThat(userProfile.getDefaultCredentials(), hasSize(1));
        assertEquals(userProfile.getDefaultCredentials().iterator().next().getName(), CREDENTIAL_A);
        verify(userProfileService).save(userProfile);
    }

    @Test
    public void testCreateProfilePreparationWhenDefaultIsPresent() {
        CloudbreakUser cloudbreakUser = createIdentityUser();
        User user = createUser();
        UserProfile userProfile = createUserProfile(Collections.singleton(createCredential(CREDENTIAL_A)));
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(userProfileService.getOrCreate(user)).thenReturn(userProfile);

        underTest.createProfilePreparation(createCredential(CREDENTIAL_B), user);

        assertThat(userProfile.getDefaultCredentials(), hasSize(1));
        assertEquals(userProfile.getDefaultCredentials().iterator().next().getName(), CREDENTIAL_A);
        verify(userProfileService, never()).save(userProfile);
    }

    private CloudbreakUser createIdentityUser() {
        return new CloudbreakUser(USER_ID_1, USER_CRN_1, USERNAME_1, EMAIL_1, TENANT_A);
    }

    private User createUser() {
        User user = new User();
        user.setUserId(USERNAME_1);
        return user;
    }

    private UserProfile createUserProfile(Set<Credential> defaultCredentials) {
        UserProfile userProfile = new UserProfile();
        userProfile.setDefaultCredentials(defaultCredentials);
        return userProfile;
    }

    private Credential createCredential(String name) {
        Credential credential = new Credential();
        credential.setName(name);
        return credential;
    }
}
