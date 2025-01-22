package com.sequenceiq.cloudbreak.auth.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@ExtendWith(MockitoExtension.class)
public class CrnUserDetailsServiceTest {

    private String userCrn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    @Mock
    private GrpcUmsClient mockedUmsClient;

    private CrnUserDetailsService underTest;

    @BeforeEach
    public void setUp() throws Exception {
        underTest = new CrnUserDetailsService(mockedUmsClient);
    }

    @Test
    public void loadUserByCrnWhenLoadingUMSUserFailed() {
        User user = User.newBuilder().setCrn("userCrn").setEmail("dummyuser@cloudera.com").setUserId("1").build();
        when(mockedUmsClient.getUserDetails(eq(userCrn))).thenThrow(new RuntimeException("error"));
        assertThrows(RuntimeException.class, () -> underTest.getUmsUser(userCrn));
    }

}
