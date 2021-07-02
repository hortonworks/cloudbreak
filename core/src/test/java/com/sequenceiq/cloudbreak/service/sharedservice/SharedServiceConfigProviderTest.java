package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cluster.api.DatalakeConfigApi;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public class SharedServiceConfigProviderTest {

    private static final Long TEST_LONG_VALUE = 1L;

    @InjectMocks
    private SharedServiceConfigProvider underTest;

    @Mock
    private StackService stackService;

    @Mock
    private User user;

    @Mock
    private Workspace workspace;

    @Mock
    private Blueprint blueprint;

    @Mock
    private Stack publicStack;

    @Mock
    private Cluster publicStackCluster;

    @Mock
    private StackInputs stackInputs;

    @Mock
    private RemoteDataContextWorkaroundService remoteDataContextWorkaroundService;

    @Mock
    private DatalakeConfigApi datalakeConfigApi;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConfigureClusterWhenConnectedClusterRequestIsNullThenOriginalClusterInstanceShouldReturn() {
        Cluster cluster = new Cluster();
        cluster.setStack(publicStack);

        Cluster result = underTest.configureCluster(cluster, user, workspace);

        Assert.assertEquals(cluster, result);
        assertNull(cluster.getRdsConfigs());
    }

    @Test
    public void testConfigureClusterWithDl() {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();

        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);

        Cluster result = underTest.configureCluster(requestedCluster, user, workspace);

        Assert.assertTrue(result.getRdsConfigs().isEmpty());
    }

    @Test
    public void testConfigureClusterIfSourceClusterContainsDifferentResourceStatusThenTheDefaultOnesWouldNotBeStoredInTheReturnCluster() {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();


        when(stackService.getById(TEST_LONG_VALUE)).thenReturn(publicStack);
        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);

        Cluster result = underTest.configureCluster(requestedCluster, user, workspace);

        Assert.assertEquals(0L, result.getRdsConfigs().size());
        result.getRdsConfigs().forEach(rdsConfig -> Assert.assertNotEquals(DEFAULT, rdsConfig.getStatus()));
    }

    private Cluster createBarelyConfiguredRequestedCluster() {
        Cluster requestedCluster = new Cluster();
        requestedCluster.setRdsConfigs(new LinkedHashSet<>());
        requestedCluster.setBlueprint(blueprint);
        requestedCluster.setStack(publicStack);
        return requestedCluster;
    }
}