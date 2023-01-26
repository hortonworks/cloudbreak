package com.sequenceiq.periscope.service.security;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.Clustered;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;

@ExtendWith(MockitoExtension.class)
public class TenantBasedPermissionEvaluatorTest {

    @InjectMocks
    private TenantBasedPermissionEvaluator underTest;

    @Mock
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private CloudbreakAuthorizationService cloudbreakAuthorizationService;

    @Mock
    private Authentication authentication;

    @Mock
    private Clustered clusteredTarget;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private Cluster cluster;

    @Mock
    private ClusterPertain clusterPertain;

    @BeforeEach
    public void init() {
        when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(cloudbreakUser.getTenant()).thenReturn("tenant");
        lenient().when(clusterPertain.getTenant()).thenReturn("tenant");
        lenient().when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        lenient().when(cluster.getClusterPertain()).thenReturn(clusterPertain);
        lenient().when(clusteredTarget.getCluster()).thenReturn(cluster);
    }

    @Test
    public void testRequestIsNotAuthorized() {
        when(authentication.isAuthenticated()).thenReturn(false);

        boolean result = underTest.hasPermission(authentication, null, "");

        Assertions.assertTrue(result);
    }

    @Test
    public void testTargetIsNull() {
        boolean result = underTest.hasPermission(authentication, null, "");

        Assertions.assertFalse(result);
    }

    @Test
    public void testTargetNotInstanceOfClustered() {
        boolean result = underTest.hasPermission(authentication, new Object(), "");

        Assertions.assertTrue(result);
    }

    @Test
    public void testClusterIsNull() {
        when(clusteredTarget.getCluster()).thenReturn(null);

        boolean result = underTest.hasPermission(authentication, clusteredTarget, "");

        Assertions.assertFalse(result);
    }

    @Test
    public void testTenantNotMatch() {
        when(clusterPertain.getTenant()).thenReturn("tenant_a");

        boolean result = underTest.hasPermission(authentication, clusteredTarget, "");

        Assertions.assertFalse(result);
    }

    @Test
    public void testHasAccess() {
        boolean result = underTest.hasPermission(authentication, clusteredTarget, "");

        Assertions.assertTrue(result);
    }

    @Test
    public void testIsOptional() {
        boolean result = underTest.hasPermission(authentication, Optional.of(clusteredTarget), "");

        Assertions.assertTrue(result);
    }

    @Test
    public void testIsCollection() {
        boolean result = underTest.hasPermission(authentication, Collections.singleton(clusteredTarget), "");

        Assertions.assertTrue(result);
    }
}
