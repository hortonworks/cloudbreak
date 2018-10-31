package com.sequenceiq.periscope.service.security;

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.Clustered;

@RunWith(MockitoJUnitRunner.class)
public class TenantBasedPermissionEvaluatorTest {

    @InjectMocks
    private TenantBasedPermissionEvaluator underTest;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

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

    @Before
    public void init() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(cloudbreakUser.getTenant()).thenReturn("tenant");
        when(clusterPertain.getTenant()).thenReturn("tenant");
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(cluster.getClusterPertain()).thenReturn(clusterPertain);
        when(clusteredTarget.getCluster()).thenReturn(cluster);
    }

    @Test
    public void testRequestIsNotAuthorized() {
        when(authentication.isAuthenticated()).thenReturn(false);

        boolean result = underTest.hasPermission(authentication, null, "");

        Assert.assertTrue(result);
    }

    @Test
    public void testTargetIsNull() {
        boolean result = underTest.hasPermission(authentication, null, "");

        Assert.assertFalse(result);
    }

    @Test
    public void testTargetNotInstanceOfClustered() {
        boolean result = underTest.hasPermission(authentication, new Object(), "");

        Assert.assertTrue(result);
    }

    @Test
    public void testClusterIsNull() {
        when(clusteredTarget.getCluster()).thenReturn(null);

        boolean result = underTest.hasPermission(authentication, clusteredTarget, "");

        Assert.assertFalse(result);
    }

    @Test
    public void testTenantNotMatch() {
        when(clusterPertain.getTenant()).thenReturn("tenant_a");

        boolean result = underTest.hasPermission(authentication, clusteredTarget, "");

        Assert.assertFalse(result);
    }

    @Test
    public void testHasAccess() {
        boolean result = underTest.hasPermission(authentication, clusteredTarget, "");

        Assert.assertTrue(result);
    }

    @Test
    public void testIsOptional() {
        boolean result = underTest.hasPermission(authentication, Optional.of(clusteredTarget), "");

        Assert.assertTrue(result);
    }

    @Test
    public void testIsCollection() {
        boolean result = underTest.hasPermission(authentication, Collections.singleton(clusteredTarget), "");

        Assert.assertTrue(result);
    }
}
