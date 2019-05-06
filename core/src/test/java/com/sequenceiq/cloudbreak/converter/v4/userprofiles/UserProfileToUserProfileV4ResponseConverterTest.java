package com.sequenceiq.cloudbreak.converter.v4.userprofiles;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;

public class UserProfileToUserProfileV4ResponseConverterTest {

    private Tenant tenant;

    private User user;

    private UserProfileToUserProfileV4ResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new UserProfileToUserProfileV4ResponseConverter();
        tenant = new Tenant();
        tenant.setName("someTenant");
        user = new User();
        user.setTenant(tenant);
        user.setUserId("c1234d47-4ec4-4ad2-91x2-d44kef15d67s");
    }

    @Test
    public void testConvertPassesAllUserRelatedData() {
        UserProfile source = new UserProfile();
        source.setUser(user);
        source.setUserName("some@email.com");

        UserProfileV4Response result = underTest.convert(source);

        Assert.assertNotNull(result);
        Assert.assertEquals(source.getUserName(), result.getUsername());
        Assert.assertEquals(tenant.getName(), result.getTenant());
        Assert.assertEquals(user.getUserId(), result.getUserId());
    }

}