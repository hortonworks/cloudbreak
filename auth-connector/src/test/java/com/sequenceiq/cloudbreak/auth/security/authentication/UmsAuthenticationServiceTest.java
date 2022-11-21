package com.sequenceiq.cloudbreak.auth.security.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsAuthenticationException;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@ExtendWith(MockitoExtension.class)
public class UmsAuthenticationServiceTest {

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private UmsAuthenticationService underTest;

    @BeforeEach
    public void before() {
        underTest = new UmsAuthenticationService(grpcUmsClient, regionAwareInternalCrnGeneratorFactory);
    }

    @Test
    public void testInvalidCrnNull() {
        UmsAuthenticationException exception = assertThrows(UmsAuthenticationException.class, () -> {
            underTest.getCloudbreakUser(null, "principal");
        });

        assertEquals("Invalid CRN has been provided: null", exception.getMessage());
    }

    @Test
    public void testInvalidCrnDueToPattern() {
        String crn = "crsdfadsfdsf sadasf3-df81ae585e10";

        UmsAuthenticationException exception = assertThrows(UmsAuthenticationException.class, () -> {
            underTest.getCloudbreakUser(crn, "principal");
        });

        assertEquals("Invalid CRN has been provided: " + crn, exception.getMessage());
    }

    @Test
    public void testInvalidCrnDueToParse() {
        String crn = "crn:cdp:cookie:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:qaas/b8a64902-7765-4ddd-a4f3-df81ae585e10";

        UmsAuthenticationException exception = assertThrows(UmsAuthenticationException.class, () -> {
            underTest.getCloudbreakUser(crn, "principal");
        });

        assertEquals("Invalid CRN has been provided: " + crn, exception.getMessage());
    }

    @Test
    public void testInvalidTypeCrn() {
        String crn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:cluster:qaas/b8a64902-7765-4ddd-a4f3-df81ae585e10";

        UmsAuthenticationException exception = assertThrows(UmsAuthenticationException.class, () -> {
            underTest.getCloudbreakUser(crn, "principal");
        });

        assertEquals("Authentication is supported only with User and MachineUser CRN: " + crn, exception.getMessage());
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
