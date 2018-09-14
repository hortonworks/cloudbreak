package com.sequenceiq.cloudbreak.service.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;

public class UserProfileHandlerTest {

    private static final String USER_ID_1 = "userId1";

    private static final String ACCOUNT_A = "account";

    private static final String USERNAME_1 = "username";

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
        IdentityUser identityUser = createIdentityUser();
        User user = createUser();
        UserProfile userProfile = createUserProfile(Collections.emptySet());
        when(restRequestThreadLocalService.getIdentityUser()).thenReturn(identityUser);
        when(userProfileService.getOrCreate(ACCOUNT_A, USER_ID_1, user)).thenReturn(userProfile);

        underTest.createProfilePreparation(createCredential(CREDENTIAL_A), user);

        assertThat(userProfile.getDefaultCredentials(), hasSize(1));
        assertEquals(userProfile.getDefaultCredentials().iterator().next().getName(), CREDENTIAL_A);
        verify(userProfileService).save(userProfile);
    }

    @Test
    public void testCreateProfilePreparationWhenDefaultIsPresent() {
        IdentityUser identityUser = createIdentityUser();
        User user = createUser();
        UserProfile userProfile = createUserProfile(Collections.singleton(createCredential(CREDENTIAL_A)));
        when(restRequestThreadLocalService.getIdentityUser()).thenReturn(identityUser);
        when(userProfileService.getOrCreate(ACCOUNT_A, USER_ID_1, user)).thenReturn(userProfile);

        underTest.createProfilePreparation(createCredential(CREDENTIAL_B), user);

        assertThat(userProfile.getDefaultCredentials(), hasSize(1));
        assertEquals(userProfile.getDefaultCredentials().iterator().next().getName(), CREDENTIAL_A);
        verify(userProfileService, never()).save(userProfile);
    }

    private IdentityUser createIdentityUser() {
        return new IdentityUser(USER_ID_1, USERNAME_1, ACCOUNT_A, Collections.singletonList(IdentityUserRole.USER), "givenName", "familyName", new Date());
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
