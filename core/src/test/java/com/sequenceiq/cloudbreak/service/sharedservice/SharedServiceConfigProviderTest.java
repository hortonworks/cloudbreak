package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT_DELETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.cluster.DatalakeResourcesRepository;
import com.sequenceiq.cloudbreak.service.cluster.KerberosConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientFactory;
import com.sequenceiq.cloudbreak.service.credential.CredentialPrerequisiteService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

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
    private LdapConfig ldapConfig;

    @Mock
    private ClusterDefinition clusterDefinition;

    @Mock
    private Stack publicStack;

    @Mock
    private Cluster publicStackCluster;

    @Mock
    private KerberosConfigProvider kerberosConfigProvider;

    @Mock
    private DatalakeResourcesRepository datalakeResourcesRepository;

    @Mock
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @Mock
    private AmbariClientFactory ambariClientFactory;

    @Mock
    private DatalakeConfigProvider datalakeConfigProvider;

    @Mock
    private StackInputs stackInputs;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConfigureClusterWhenConnectedClusterRequestIsNullThenOriginalClusterInstanceShouldReturn() {
        Cluster cluster = new Cluster();
        cluster.setStack(publicStack);
        when(publicStack.getDatalakeResourceId()).thenReturn(null);

        Cluster result = underTest.configureCluster(cluster, user, workspace);

        Assert.assertEquals(cluster, result);
        verify(datalakeResourcesRepository, times(0)).findById(anyLong());
        verify(kerberosConfigProvider, times(0)).setKerberosConfigForWorkloadCluster(any(Cluster.class), any(DatalakeResources.class));
        assertNull(cluster.getLdapConfig());
        assertNull(cluster.getKerberosConfig());
        assertNull(cluster.getRdsConfigs());
    }

    @Test
    public void testConfigureClusterWithDl() throws IOException {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setLdapConfig(ldapConfig);

        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(datalakeResourcesRepository.findById(anyLong())).thenReturn(Optional.of(datalakeResources));

        Cluster result = underTest.configureCluster(requestedCluster, user, workspace);

        Assert.assertEquals(ldapConfig, result.getLdapConfig());
        Assert.assertTrue(result.getRdsConfigs().isEmpty());
        verify(datalakeResourcesRepository, times(1)).findById(anyLong());
        verify(kerberosConfigProvider, times(1)).setKerberosConfigForWorkloadCluster(requestedCluster, datalakeResources);
    }

    @Test
    public void testConfigureClusterIfSourceClusterContainsDifferentResourceStatusThenTheDefaultOnesWouldNotBeStoredInTheReturnCluster() throws IOException {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setRdsConfigs(createRdsConfigs(DEFAULT, DEFAULT_DELETED, USER_MANAGED));

        when(stackService.getById(TEST_LONG_VALUE)).thenReturn(publicStack);
        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(datalakeResourcesRepository.findById(anyLong())).thenReturn(Optional.of(datalakeResources));

        Cluster result = underTest.configureCluster(requestedCluster, user, workspace);

        Assert.assertEquals(1L, result.getRdsConfigs().size());
        result.getRdsConfigs().forEach(rdsConfig -> Assert.assertNotEquals(DEFAULT, rdsConfig.getStatus()));
        verify(kerberosConfigProvider, times(1)).setKerberosConfigForWorkloadCluster(requestedCluster, datalakeResources);
    }

    @Test
    public void testPrepareDLConfNotAttached() {
        when(publicStack.getDatalakeResourceId()).thenReturn(null);
        when(publicStack.getEnvironment()).thenReturn(new EnvironmentView());

        Stack stack = underTest.prepareDatalakeConfigs(publicStack);

        assertNull(stack.getDatalakeResourceId());
        assertEquals(publicStack.getInputs(), stack.getInputs());
        verify(datalakeResourcesRepository, times(0)).findById(anyLong());
        verify(credentialPrerequisiteService, times(0)).isCumulusCredential(anyString());
        verify(ambariClientFactory, times(0)).getAmbariClient(any(Stack.class), any(Cluster.class));
    }

    @Test
    public void testPrepareDLConfCumulus() throws IOException {
        Stack stackIn = new Stack();
        stackIn.setDatalakeResourceId(1L);
        Credential credential = new Credential();
        credential.setAttributes("attr");
        stackIn.setCredential(credential);
        DatalakeResources datalakeResources = new DatalakeResources();
        when(datalakeResourcesRepository.findById(anyLong())).thenReturn(Optional.of(datalakeResources));
        when(credentialPrerequisiteService.isCumulusCredential(anyString())).thenReturn(Boolean.TRUE);
        AmbariClient ambariClient = new AmbariClient();
        when(credentialPrerequisiteService.createCumulusAmbariClient(anyString())).thenReturn(ambariClient);
        when(datalakeConfigProvider.getAdditionalParameters(stackIn, datalakeResources)).thenReturn(Collections.singletonMap("test", "data"));
        when(datalakeConfigProvider.getBlueprintConfigParameters(datalakeResources, stackIn, ambariClient))
                .thenReturn(Collections.singletonMap("test", "data"));
        when(stackService.save(stackIn)).thenReturn(stackIn);
        stackIn.setInputs(new Json(stackInputs));

        Stack stack = underTest.prepareDatalakeConfigs(stackIn);

        verify(stackService, times(0)).getById(anyLong());

        StackInputs stackInputs = stack.getInputs().get(StackInputs.class);

        assertEquals(1L, stackInputs.getDatalakeInputs().size());
        assertEquals(1L, stackInputs.getFixInputs().size());
    }

    @Test
    public void testPrepareDLConfWithCloudDL() throws IOException {
        Stack stackIn = new Stack();
        stackIn.setDatalakeResourceId(1L);
        Credential credential = new Credential();
        credential.setAttributes("attr");
        stackIn.setCredential(credential);
        DatalakeResources datalakeResources = new DatalakeResources();
        long datalakeStackId = 11L;
        datalakeResources.setDatalakeStackId(datalakeStackId);
        when(datalakeResourcesRepository.findById(anyLong())).thenReturn(Optional.of(datalakeResources));
        when(credentialPrerequisiteService.isCumulusCredential(anyString())).thenReturn(Boolean.FALSE);
        AmbariClient ambariClient = new AmbariClient();
        when(datalakeConfigProvider.getAdditionalParameters(stackIn, datalakeResources)).thenReturn(Collections.singletonMap("test", "data"));
        when(datalakeConfigProvider.getBlueprintConfigParameters(datalakeResources, stackIn, ambariClient))
                .thenReturn(Collections.singletonMap("test", "data"));
        when(stackService.save(stackIn)).thenReturn(stackIn);
        Stack dlStack = new Stack();
        dlStack.setId(datalakeStackId);
        dlStack.setCluster(createBarelyConfiguredRequestedCluster());
        when(stackService.getById(dlStack.getId())).thenReturn(dlStack);
        when(ambariClientFactory.getAmbariClient(dlStack, dlStack.getCluster())).thenReturn(ambariClient);
        stackIn.setInputs(new Json(stackInputs));

        Stack stack = underTest.prepareDatalakeConfigs(stackIn);

        verify(stackService, times(1)).getById(datalakeResources.getDatalakeStackId());

        StackInputs stackInputs = stack.getInputs().get(StackInputs.class);
        assertEquals(1L, stackInputs.getDatalakeInputs().size());
        assertEquals(1L, stackInputs.getFixInputs().size());
    }

    private Cluster createBarelyConfiguredRequestedCluster() {
        Cluster requestedCluster = new Cluster();
        requestedCluster.setRdsConfigs(new LinkedHashSet<>());
        requestedCluster.setClusterDefinition(clusterDefinition);
        requestedCluster.setStack(publicStack);
        return requestedCluster;
    }

    private Set<RDSConfig> createRdsConfigs(ResourceStatus... statuses) {
        Set<RDSConfig> configs = new LinkedHashSet<>(statuses.length);
        long id = 0;
        for (ResourceStatus status : statuses) {
            RDSConfig config = new RDSConfig();
            config.setId(id++);
            config.setStatus(status);
            configs.add(config);
        }
        return configs;
    }

}