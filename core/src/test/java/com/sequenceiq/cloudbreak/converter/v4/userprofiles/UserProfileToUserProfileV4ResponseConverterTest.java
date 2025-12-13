package com.sequenceiq.cloudbreak.converter.v4.userprofiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;

class UserProfileToUserProfileV4ResponseConverterTest {

    private Tenant tenant;

    private User user;

    private UserProfileToUserProfileV4ResponseConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new UserProfileToUserProfileV4ResponseConverter();
        tenant = new Tenant();
        tenant.setName("someTenant");
        user = new User();
        user.setTenant(tenant);
        user.setUserId("c1234d47-4ec4-4ad2-91x2-d44kef15d67s");
    }

    @Test
    void testConvertPassesAllUserRelatedData() {
        UserProfile source = new UserProfile();
        source.setUser(user);
        source.setUserName("some@email.com");

        UserProfileV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(source.getUserName(), result.getUsername());
        assertEquals(tenant.getName(), result.getTenant());
        assertEquals(user.getUserId(), result.getUserId());
    }

}