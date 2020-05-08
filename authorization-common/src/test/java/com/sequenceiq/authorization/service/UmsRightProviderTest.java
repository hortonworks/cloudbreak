package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@RunWith(MockitoJUnitRunner.class)
public class UmsRightProviderTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @InjectMocks
    private UmsRightProvider underTest;

    @Test
    public void testIfEveryActionIsInMap() {
        underTest.init();
        ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);
        when(grpcUmsClient.isAuthorizationEntitlementRegistered(any(), any())).thenReturn(Boolean.TRUE);
        assertTrue(Arrays.stream(AuthorizationResourceAction.values())
                .allMatch(action -> StringUtils.isNotBlank(underTest.getRight(action))));
    }
}
