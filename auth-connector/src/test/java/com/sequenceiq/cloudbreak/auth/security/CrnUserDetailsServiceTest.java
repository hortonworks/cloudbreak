package com.sequenceiq.cloudbreak.auth.security;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetails;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

@RunWith(MockitoJUnitRunner.class)
public class CrnUserDetailsServiceTest {

    private String userCrn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    @Mock
    private GrpcUmsClient mockedUmsClient;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private CrnUserDetailsService underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new CrnUserDetailsService(mockedUmsClient, regionAwareInternalCrnGeneratorFactory);
    }

    @Test
    public void loadUserByInternalCrn() {
        UserDetails userDetails = underTest.loadUserByUsername("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        assertTrue(userDetails.getAuthorities().iterator().next().getAuthority().equals("ROLE_INTERNAL"));
    }

    @Test
    public void loadUserByInternalCrnWithAutoscale() {
        UserDetails userDetails = underTest.loadUserByUsername("crn:cdp:autoscale:us-west-1:altus:user:__internal__actor__");
        assertTrue(userDetails.getAuthorities().iterator().next().getAuthority().equals("ROLE_AUTOSCALE"));
    }

    @Test
    public void loadUserByCrn() {
        User user = User.newBuilder().setCrn("userCrn").setEmail("dummyuser@cloudera.com").setUserId("1").build();
        when(mockedUmsClient.getUserDetails(eq(userCrn), any())).thenReturn(user);
        UserDetails userDetails = underTest.loadUserByUsername(userCrn);
        assertTrue(userDetails.getAuthorities().iterator().next().getAuthority().equals("CRN_USER"));
    }

}
