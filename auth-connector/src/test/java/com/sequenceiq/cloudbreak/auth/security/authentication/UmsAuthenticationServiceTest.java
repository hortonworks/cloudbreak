package com.sequenceiq.cloudbreak.auth.security.authentication;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsAuthenticationException;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@RunWith(MockitoJUnitRunner.class)
public class UmsAuthenticationServiceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private UmsAuthenticationService underTest;

    @Before
    public void before() {
        underTest = new UmsAuthenticationService(grpcUmsClient, regionAwareInternalCrnGeneratorFactory);
    }

    @Test
    public void testInvalidCrnNull() {
        thrown.expect(UmsAuthenticationException.class);

        try {
            underTest.getCloudbreakUser(null, "principal");
        } catch (UmsAuthenticationException e) {
            assertEquals("Invalid CRN has been provided: null", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testInvalidCrnDueToPattern() {
        thrown.expect(UmsAuthenticationException.class);

        String crn = "crsdfadsfdsf sadasf3-df81ae585e10";
        try {
            underTest.getCloudbreakUser(crn, "principal");
        } catch (UmsAuthenticationException e) {
            assertEquals("Invalid CRN has been provided: " + crn, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testInvalidCrnDueToParse() {
        thrown.expect(UmsAuthenticationException.class);

        String crn = "crn:cdp:cookie:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:qaas/b8a64902-7765-4ddd-a4f3-df81ae585e10";
        try {
            underTest.getCloudbreakUser(crn, "principal");
        } catch (UmsAuthenticationException e) {
            assertEquals("Invalid CRN has been provided: " + crn, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testInvalidTypeCrn() {
        thrown.expect(UmsAuthenticationException.class);

        String crn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:cluster:qaas/b8a64902-7765-4ddd-a4f3-df81ae585e10";
        try {
            underTest.getCloudbreakUser(crn, "principal");
        } catch (UmsAuthenticationException e) {
            assertEquals("Authentication is supported only with User and MachineUser CRN: " + crn, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testUserCrn() {
        String crn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:qaas/b8a64902-7765-4ddd-a4f3-df81ae585e10";
        UserManagementProto.User user = UserManagementProto.User.newBuilder()
                .setUserId("userId")
                .setEmail("e@mail.com")
                .setCrn(crn)
                .build();

        when(grpcUmsClient.getUserDetails(anyString(), any())).thenReturn(user);
        CloudbreakUser cloudbreakUser = underTest.getCloudbreakUser(crn, null);

        assertEquals("userId", cloudbreakUser.getUserId());
        assertEquals("e@mail.com", cloudbreakUser.getUsername());
        assertEquals(crn, cloudbreakUser.getUserCrn());
    }

    @Test
    public void testMachineUserCrn() {
        String crn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:machineUser:qaas/b8a64902-7765-4ddd-a4f3-df81ae585e10";
        UserManagementProto.MachineUser machineUser = UserManagementProto.MachineUser.newBuilder()
                .setMachineUserId("machineUserId")
                .setCrn(crn)
                .build();

        when(grpcUmsClient.getMachineUserDetails(anyString(), anyString(), any())).thenReturn(machineUser);
        CloudbreakUser cloudbreakUser = underTest.getCloudbreakUser(crn, "principal");

        assertEquals("machineUserId", cloudbreakUser.getUserId());
        assertEquals("principal", cloudbreakUser.getUsername());
        assertEquals(crn, cloudbreakUser.getUserCrn());
    }
}
