package com.sequenceiq.cloudbreak.service.cluster.flow;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfigurationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;

import groovyx.net.http.HttpResponseException;
import reactor.bus.EventBus;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterConnectorTest {

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private EventBus reactor;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private HttpClientConfig httpClientConfig;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private HadoopConfigurationService hadoopConfigurationService;

    @Mock
    private PollingService<AmbariHostsCheckerContext> hostsPollingService;

    @Mock
    private AmbariHostsStatusCheckerTask ambariHostsStatusCheckerTask;

    @Mock
    private HostGroupRepository hostGroupRepository;

    @Mock
    private AmbariOperationService ambariOperationService;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @Mock
    private HostGroup hostGroup;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private BlueprintProcessor blueprintProcessor;

    @InjectMocks
    @Spy
    private AmbariClusterConnector underTest = new AmbariClusterConnector();

    private Stack stack;

    private Cluster cluster;

    private Blueprint blueprint;

    @Before
    public void setUp() throws CloudbreakSecuritySetupException, HttpResponseException, InvalidHostGroupHostAssociation {
        stack = TestUtil.stack();
        blueprint = TestUtil.blueprint();
        cluster = TestUtil.cluster(blueprint, stack, 1L);
        stack.setCluster(cluster);
        cluster.setHostGroups(new HashSet<>());
        cluster.setConfigStrategy(ConfigStrategy.NEVER_APPLY);
        when(tlsSecurityService.buildTLSClientConfig(anyLong(), anyString())).thenReturn(httpClientConfig);
        when(ambariClient.extendBlueprintGlobalConfiguration(anyString(), anyMap())).thenReturn("");
        when(hostMetadataRepository.findHostsInCluster(anyLong())).thenReturn(new HashSet<>());
        when(ambariClient.extendBlueprintHostGroupConfiguration(anyString(), anyMap())).thenReturn(blueprint.getBlueprintText());
        when(ambariClient.addBlueprint(anyString())).thenReturn("");
        when(hadoopConfigurationService.getHostGroupConfiguration(any(Cluster.class))).thenReturn(new HashMap<>());
        when(ambariClientProvider.getAmbariClient(any(HttpClientConfig.class), anyInt(), anyString(), anyString())).thenReturn(ambariClient);
        when(ambariClientProvider.getDefaultAmbariClient(any(HttpClientConfig.class), anyInt())).thenReturn(ambariClient);
        when(hostsPollingService.pollWithTimeoutSingleFailure(any(AmbariHostsStatusCheckerTask.class), any(AmbariHostsCheckerContext.class), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS);
        when(hostGroupRepository.findHostGroupsInCluster(anyLong())).thenReturn(cluster.getHostGroups());
        when(ambariOperationService.waitForOperations(any(Stack.class), any(AmbariClient.class), anyMap(), any(AmbariOperationType.class)))
                .thenReturn(PollingResult.SUCCESS);
        when(ambariOperationService.waitForOperations(any(Stack.class), any(AmbariClient.class), any(StatusCheckerTask.class), anyMap(),
                any(AmbariOperationType.class))).thenReturn(PollingResult.SUCCESS);
        when(clusterRepository.save(any(Cluster.class))).thenReturn(cluster);
        when(instanceMetadataRepository.save(anyCollection())).thenReturn(stack.getRunningInstanceMetaData());
        when(ambariClient.recommendAssignments(anyString())).thenReturn(createStringListMap());
        when(ambariClient.deleteUser(anyString())).thenReturn("");
        when(ambariClient.createUser(anyString(), anyString(), anyBoolean())).thenReturn("");
        when(ambariClient.changePassword(anyString(), anyString(), anyString(), anyBoolean())).thenReturn("");
        when(ambariClientProvider.getSecureAmbariClient(any(HttpClientConfig.class), anyInt(), any(Cluster.class))).thenReturn(ambariClient);
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(clusterRepository.findOneWithLists(anyLong())).thenReturn(cluster);
    }

    @Test(expected = AmbariOperationFailedException.class)
    public void testInstallAmbariWhenExceptionOccursShouldInstallationFailed() throws Exception {
        doThrow(new IllegalArgumentException()).when(ambariClient).createCluster(anyString(), anyString(), anyMap(), anyString(), anyString());
        underTest.buildAmbariCluster(stack);
    }

    @Test(expected = AmbariOperationFailedException.class)
    public void testInstallAmbariWhenReachedMaxPollingEventsShouldInstallationFailed() throws Exception {
        when(ambariOperationService.waitForOperations(any(Stack.class), any(AmbariClient.class), anyMap(), any(AmbariOperationType.class)))
                .thenReturn(PollingResult.TIMEOUT);
        underTest.buildAmbariCluster(stack);
    }

    @Test
    public void testChangeAmbariCredentialsWhenUserIsTheSameThenModifyUser() throws Exception {
        underTest.credentialChangeAmbariCluster(stack.getId(), "admin", "admin1");
        verify(ambariClient, times(1)).changePassword(anyString(), anyString(), anyString(), anyBoolean());
        verify(ambariClient, times(0)).deleteUser(anyString());
        verify(ambariClient, times(0)).createUser(anyString(), anyString(), anyBoolean());
    }

    @Test
    public void testChangeAmbariCredentialsWhenUserDifferentThanExistThenCreateNewUserDeleteOldOne() throws Exception {
        underTest.credentialChangeAmbariCluster(stack.getId(), "admin123", "admin1");
        verify(ambariClient, times(0)).changePassword(anyString(), anyString(), anyString(), anyBoolean());
        verify(ambariClient, times(1)).deleteUser(anyString());
        verify(ambariClient, times(1)).createUser(anyString(), anyString(), anyBoolean());
    }

    private Map<String, List<String>> createStringListMap() {
        Map<String, List<String>> stringListMap = new HashMap<>();
        stringListMap.put("a1", Arrays.asList("assignment1", "assignment2"));
        return stringListMap;
    }
}
