package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.polling.PollingResult;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerCommissionerTest extends BaseClouderaManagerCommDecommTest {

    private static final String STACK_NAME = "stack";

    private static final String DELETED_INSTANCE_FQDN = "deletedInstance";

    private static final String RUNNING_INSTANCE_FQDN = "runningInstance";

    private static final String RUNNING_INSTANCE_FQDN_BASE = "runningInstance-";

    private static final String HOSTGROUP_NAME_COMPUTE = "compute";

    @InjectMocks
    private ClouderaManagerCommissioner underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient client;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private HostsResourceApi hostsResourceApi;

    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private CommandsResourceApi commandsResourceApi;

    @Mock
    private ClouderaManagerPollingServiceProvider pollingServiceProvider;

    @Test
    public void testCollectHostsToCommissionAllFound() throws ApiException {
        Set<String> hostNames = createHostnames(3);
        ApiHostList apiHostList = createGoodHealthApiHostList(hostNames);
        mockListClusterHosts(apiHostList);
        HostGroup hostGroup = createHostGroup(hostNames);

        Map<String, InstanceMetaData> result = underTest.collectHostsToCommission(getStack(), hostGroup, hostNames, client);

        assertEquals(3, result.size());
        assertEquals(hostNames, result.keySet());
    }

    @Test
    public void testCollectHostsToCommissionMissingInCB() throws ApiException {
        Set<String> hostNames = createHostnames(3);
        ApiHostList apiHostList = createGoodHealthApiHostList(hostNames);
        mockListClusterHosts(apiHostList);

        Set<String> hostnamesKnownToCb = new HashSet<>(hostNames);
        hostnamesKnownToCb.remove(hostNames.iterator().next());
        HostGroup hostGroup = createHostGroup(hostnamesKnownToCb);

        Map<String, InstanceMetaData> result = underTest.collectHostsToCommission(getStack(), hostGroup, hostNames, client);

        assertEquals(2, result.size());
        assertEquals(hostnamesKnownToCb, result.keySet());
    }

    @Test
    public void testCollectHostsToCommissionMissingInCM() throws ApiException {
        Set<String> hostNames = createHostnames(3);

        Set<String> hostnamesKnownToCm = new HashSet<>(hostNames);
        hostnamesKnownToCm.remove(hostNames.iterator().next());
        ApiHostList apiHostList = createGoodHealthApiHostList(hostnamesKnownToCm);
        mockListClusterHosts(apiHostList);

        HostGroup hostGroup = createHostGroup(hostNames);

        Map<String, InstanceMetaData> result = underTest.collectHostsToCommission(getStack(), hostGroup, hostNames, client);

        assertEquals(2, result.size());
        assertEquals(hostnamesKnownToCm, result.keySet());
    }

    @Test
    public void testCollectHostsToCommissionMissingInCBAndCM() throws ApiException {
        Set<String> hostNames = createHostnames(3);

        Set<String> hostnamesKnownToCm = new HashSet<>(hostNames);
        hostnamesKnownToCm.remove(hostNames.iterator().next());
        ApiHostList apiHostList = createGoodHealthApiHostList(hostnamesKnownToCm);
        mockListClusterHosts(apiHostList);

        Set<String> hostnamesKnownToCb = new HashSet<>(hostNames);
        hostnamesKnownToCb.remove(hostnamesKnownToCm.iterator().next());
        HostGroup hostGroup = createHostGroup(hostnamesKnownToCb);

        Map<String, InstanceMetaData> result = underTest.collectHostsToCommission(getStack(), hostGroup, hostNames, client);

        assertEquals(1, result.size());
    }

    @Test
    public void testRecommissionNodesSuccess() throws ApiException {
        Set<String> hostNames = createHostnames(3);
        ApiHostList apiHostList = createGoodHealthApiHostList(hostNames);
        mockListClusterHosts(apiHostList);

        Map<String, InstanceMetaData> hostsToDecommission;
        Set<InstanceMetaData> instanceMetaDataSet = createRunningInstanceMetadata(hostNames);
        Map<String, InstanceMetaData> hostsToCommission = instanceMetaDataSet.stream().collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, e -> e));

        mockCommissionAndExitMaintenanceMode(PollingResult.SUCCESS);
        Set<String> result = underTest.recommissionNodes(getStack(), hostsToCommission, client);

        assertEquals(3, result.size());
        assertEquals(hostNames, result);
    }

    @Test
    public void testRecommissionNodesSuccessMissingNodesInCm() throws ApiException {
        Set<String> hostNames = createHostnames(3);
        Set<String> hostnamesKnownToCm = new HashSet<>(hostNames);
        hostnamesKnownToCm.remove(hostNames.iterator().next());
        ApiHostList apiHostList = createGoodHealthApiHostList(hostnamesKnownToCm);
        mockListClusterHosts(apiHostList);

        Map<String, InstanceMetaData> hostsToDecommission;
        Set<InstanceMetaData> instanceMetaDataSet = createRunningInstanceMetadata(hostNames);
        Map<String, InstanceMetaData> hostsToCommission = instanceMetaDataSet.stream().collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, e -> e));

        mockCommissionAndExitMaintenanceMode(PollingResult.SUCCESS);
        Set<String> result = underTest.recommissionNodes(getStack(), hostsToCommission, client);

        assertEquals(2, result.size());
        assertEquals(hostnamesKnownToCm, result);
    }

    @Test
    public void testRecommissionNodesTimeout() throws ApiException {
        Set<String> hostNames = createHostnames(3);
        ApiHostList apiHostList = createGoodHealthApiHostList(hostNames);
        mockListClusterHosts(apiHostList);

        Map<String, InstanceMetaData> hostsToDecommission;
        Set<InstanceMetaData> instanceMetaDataSet = createRunningInstanceMetadata(hostNames);
        Map<String, InstanceMetaData> hostsToCommission = instanceMetaDataSet.stream().collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, e -> e));

        // TODO CB-15132: What are the other PollingResults that CM can return. How about entities like FAILURE?.
        //  Is an explicit check for SUCCESS required?
        mockCommissionAndExitMaintenanceMode(PollingResult.TIMEOUT);
        mockAbortCommission();

        assertThrows(CloudbreakServiceException.class, () -> underTest.recommissionNodes(getStack(), hostsToCommission, client));
        verify(commandsResourceApi).abortCommand(eq(BigDecimal.valueOf(1)));
    }

    // TODO CB-15132: As error handling is improved, add tests to include
    //  1. hosts which are not in the desired state.
    //  2. Exceptions when communicating with CM endpoints.

    private Set<String> createHostnames(int runningCount) {
        Set<String> hostnames = new HashSet<>();
        for (int i = 0; i < runningCount; i++) {
            hostnames.add(RUNNING_INSTANCE_FQDN_BASE + i);
        }
        return hostnames;
    }

    private void mockListClusterHosts(ApiHostList apiHostList) throws ApiException {
        mockListClusterHosts(apiHostList, hostsResourceApi, clouderaManagerApiFactory, client);
    }

    private void mockCommissionAndExitMaintenanceMode(PollingResult pollingResult) throws ApiException {
        ApiCommand apiCommand = getApiCommand(BigDecimal.valueOf(1));
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(eq(client))).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.hostsRecommissionAndExitMaintenanceModeCommand(any(), any())).thenReturn(apiCommand);
        when(pollingServiceProvider.startPollingCmHostsRecommission(any(), eq(client), eq(apiCommand.getId()))).thenReturn(pollingResult);
    }

    private void mockAbortCommission() {
        when(clouderaManagerApiFactory.getCommandsResourceApi(eq(client))).thenReturn(commandsResourceApi);
    }

    private Set<InstanceMetaData> createRunningInstanceMetadata(Set<String> hosts) {
        return hosts.stream().map(
                h -> createRunningInstanceMetadata(h, HOSTGROUP_NAME_COMPUTE)).collect(Collectors.toUnmodifiableSet());
    }

    private HostGroup createHostGroup(Set<String> runningHostNames) {
        HostGroup hostGroup = new HostGroup();
        InstanceGroup instanceGroup = new InstanceGroup();

        Set<InstanceMetaData> instanceMetaData = createRunningInstanceMetadata(runningHostNames);

        instanceGroup.setInstanceMetaData(instanceMetaData);
        instanceGroup.setGroupName(HOSTGROUP_NAME_COMPUTE);
        hostGroup.setInstanceGroup(instanceGroup);
        hostGroup.setName(HOSTGROUP_NAME_COMPUTE);
        return hostGroup;
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setName(STACK_NAME);
        return stack;
    }
}
