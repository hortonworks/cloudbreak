package com.sequenceiq.cloudbreak.cm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostNameList;
import com.cloudera.api.swagger.model.ApiHostTemplateList;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerDecomissionerTest {

    private static final String STACK_NAME = "stack";

    private static final String DELETED_INSTANCE_FQDN = "deletedInstance";

    private static final String RUNNING_INSTANCE_FQDN = "runningInstance";

    @InjectMocks
    private ClouderaManagerDecomissioner underTest;

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

    @Mock
    private FlowMessageService flowMessageService;

    @Test
    public void testDecommissionForLostNodesIfFirstDecommissionSucceeded() throws ApiException {
        mockListClusterHosts();
        mockDecommission(Pair.of(BigDecimal.ONE, new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build()));
        mockAbortCommand(BigDecimal.ONE);
        InstanceMetaData deletedInstanceMetadata = createDeletedInstanceMetadata();

        underTest.decommissionNodes(getStack(), Map.of(deletedInstanceMetadata.getDiscoveryFQDN(), deletedInstanceMetadata), client);

        verifyNoInteractions(commandsResourceApi);
        verify(clouderaManagerResourceApi, times(1)).hostsDecommissionCommand(any());
    }

    @Test
    public void testDecommissionForLostNodesIfSecondDecommissionSucceeded() throws ApiException {
        mockListClusterHosts();
        mockDecommission(Pair.of(BigDecimal.ONE, new ExtendedPollingResult.ExtendedPollingResultBuilder().timeout().build()),
                Pair.of(BigDecimal.TEN, new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build()));
        mockAbortCommand(BigDecimal.ONE);
        InstanceMetaData deletedInstanceMetadata = createDeletedInstanceMetadata();

        underTest.decommissionNodes(getStack(), Map.of(deletedInstanceMetadata.getDiscoveryFQDN(), deletedInstanceMetadata), client);

        verify(commandsResourceApi, times(1)).abortCommand(eq(BigDecimal.ONE));
        verify(clouderaManagerResourceApi, times(2)).hostsDecommissionCommand(any());
    }

    @Test
    public void testDecommissionForLostNodesIfBothDecommissionFails() throws ApiException {
        mockListClusterHosts();
        mockDecommission(Pair.of(BigDecimal.ONE, new ExtendedPollingResult.ExtendedPollingResultBuilder().timeout().build()),
                Pair.of(BigDecimal.TEN, new ExtendedPollingResult.ExtendedPollingResultBuilder().timeout().build()));
        mockAbortCommand(BigDecimal.ONE, BigDecimal.TEN);
        doNothing().when(flowMessageService).fireInstanceGroupEventAndLog(any(), any(), any(), any(), any());
        InstanceMetaData deletedInstanceMetadata = createDeletedInstanceMetadata();

        underTest.decommissionNodes(getStack(), Map.of(deletedInstanceMetadata.getDiscoveryFQDN(), deletedInstanceMetadata), client);

        verify(commandsResourceApi, times(1)).abortCommand(eq(BigDecimal.ONE));
        verify(commandsResourceApi, times(1)).abortCommand(eq(BigDecimal.TEN));
        verify(clouderaManagerResourceApi, times(2)).hostsDecommissionCommand(any());
    }

    @Test
    public void collectHostsToRemoveShouldCollectDeletedOnProviderSideNodes() throws Exception {
        mockListClusterHosts();
        Set<String> hostNames = Set.of(DELETED_INSTANCE_FQDN, RUNNING_INSTANCE_FQDN);

        Stack stack = getStack();
        InstanceGroup instanceGroup = createInstanceGroup();
        String groupName = "hgName";
        instanceGroup.setGroupName(groupName);
        stack.setInstanceGroups(Set.of(instanceGroup));
        Map<String, InstanceMetadataView> result = underTest.collectHostsToRemove(stack, groupName, hostNames, client);

        assertThat(result.keySet())
                .contains(createDeletedInstanceMetadata().getDiscoveryFQDN(),
                        createRunningInstanceMetadata().getDiscoveryFQDN());
    }

    @Test
    public void testDecommissionForNodesNowKnownByCM() throws ApiException {
        HostTemplatesResourceApi hostTemplatesResourceApi = mock(HostTemplatesResourceApi.class);
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        apiHostTemplateList.setItems(new ArrayList<>());
        when(hostTemplatesResourceApi.readHostTemplates(any())).thenReturn(apiHostTemplateList);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host2.example.com", ApiHealthSummary.BAD));
        apiHostList.addItemsItem(createApiHostRef("host5.example.com"));
        when(hostsResourceApi.readHosts(any(), any(), any())).thenReturn(apiHostList);

        InstanceMetaData healthy1 = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, "host1.example.com", "compute");
        InstanceMetaData bad1 = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, "host2.example.com", "compute");
        InstanceMetaData failed1 = createInstanceMetadata(InstanceStatus.ORCHESTRATION_FAILED, "host3.example.com", "compute");
        InstanceMetaData failed2 = createInstanceMetadata(InstanceStatus.ORCHESTRATION_FAILED, "host4.example.com", "compute");
        InstanceMetaData healthy2 = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, "host5.example.com", "compute");

        Set<InstanceMetadataView> instanceMetaDataSet = Set.of(failed1, failed2, healthy1, healthy2, bad1);
        StackDtoDelegate stack = getStack();

        Set<InstanceMetadataView> removableInstances = underTest.collectDownscaleCandidates(client, stack, "compute", 4, instanceMetaDataSet);
        assertEquals(4, removableInstances.size());
        assertTrue(removableInstances.contains(failed1));
        assertTrue(removableInstances.contains(failed2));
        assertTrue(removableInstances.contains(healthy2));
        assertTrue(removableInstances.contains(bad1));
    }

    @Test
    public void testDeleteHostWithoutFqdn() throws ApiException {
        StackDtoDelegate stack = getStack();
        InstanceMetaData instanceMetaData = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, null, "compute");
        ServicesResourceApi servicesResourceApi = mock(ServicesResourceApi.class);
        when(clouderaManagerApiFactory.getServicesResourceApi(eq(client))).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(stack.getName()), any())).thenReturn(new ApiServiceList().items(new ArrayList<>()));
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com"));
        when(hostsResourceApi.readHosts(any(), any(), any())).thenReturn(apiHostList);

        underTest.deleteHost(stack, instanceMetaData, client);

        verify(hostsResourceApi, times(1)).readHosts(any(), any(), any());
        verify(hostsResourceApi, never()).deleteHost(any());
    }

    @Test
    public void testStopRolesOnHosts() throws CloudbreakException, ApiException {
        StackDtoDelegate stack = getStack();
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host2.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host3.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host4.example.com"));
        when(hostsResourceApi.readHosts(isNull(), isNull(), any())).thenReturn(apiHostList);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(eq(client))).thenReturn(clouderaManagerResourceApi);
        ArgumentCaptor<ApiHostNameList> apiHostNameListArgumentCaptor = ArgumentCaptor.forClass(ApiHostNameList.class);
        BigDecimal apiCommandId = BigDecimal.ONE;
        when(clouderaManagerResourceApi.hostsStopRolesCommand(apiHostNameListArgumentCaptor.capture())).thenReturn(getApiCommand(apiCommandId));
        ExtendedPollingResult extendedPollingResult = mock(ExtendedPollingResult.class);
        when(pollingServiceProvider.startPollingStopRolesCommand(any(), eq(client), eq(apiCommandId))).thenReturn(extendedPollingResult);

        underTest.stopRolesOnHosts(stack, client, Set.of("host1.example.com", "host2.example.com"));
        verify(clouderaManagerResourceApi, times(1)).hostsStopRolesCommand(any());
        assertThat(apiHostNameListArgumentCaptor.getValue().getItems()).containsOnly("host1.example.com", "host2.example.com");
    }

    @Test
    public void testStopRolesOnHostsBadNodeFilteredOut() throws CloudbreakException, ApiException {
        StackDtoDelegate stack = getStack();
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host2.example.com", ApiHealthSummary.BAD));
        apiHostList.addItemsItem(createApiHostRef("host3.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host4.example.com"));
        when(hostsResourceApi.readHosts(isNull(), isNull(), any())).thenReturn(apiHostList);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(eq(client))).thenReturn(clouderaManagerResourceApi);
        ArgumentCaptor<ApiHostNameList> apiHostNameListArgumentCaptor = ArgumentCaptor.forClass(ApiHostNameList.class);
        BigDecimal apiCommandId = BigDecimal.ONE;
        when(clouderaManagerResourceApi.hostsStopRolesCommand(apiHostNameListArgumentCaptor.capture())).thenReturn(getApiCommand(apiCommandId));
        ExtendedPollingResult extendedPollingResult = mock(ExtendedPollingResult.class);
        when(pollingServiceProvider.startPollingStopRolesCommand(any(), eq(client), eq(apiCommandId))).thenReturn(extendedPollingResult);

        underTest.stopRolesOnHosts(stack, client, Set.of("host1.example.com"));
        verify(clouderaManagerResourceApi, times(1)).hostsStopRolesCommand(any());
        assertThat(apiHostNameListArgumentCaptor.getValue().getItems()).containsOnly("host1.example.com");
    }

    @Test
    public void testStopRolesOnHostsOneNodeFilteredOut() throws CloudbreakException, ApiException {
        StackDtoDelegate stack = getStack();
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host3.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host4.example.com"));
        when(hostsResourceApi.readHosts(isNull(), isNull(), any())).thenReturn(apiHostList);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(eq(client))).thenReturn(clouderaManagerResourceApi);
        ArgumentCaptor<ApiHostNameList> apiHostNameListArgumentCaptor = ArgumentCaptor.forClass(ApiHostNameList.class);
        BigDecimal apiCommandId = BigDecimal.ONE;
        when(clouderaManagerResourceApi.hostsStopRolesCommand(apiHostNameListArgumentCaptor.capture())).thenReturn(getApiCommand(apiCommandId));
        ExtendedPollingResult extendedPollingResult = mock(ExtendedPollingResult.class);
        when(pollingServiceProvider.startPollingStopRolesCommand(any(), eq(client), eq(apiCommandId))).thenReturn(extendedPollingResult);

        underTest.stopRolesOnHosts(stack, client, Set.of("host1.example.com", "host2.example.com"));
        verify(clouderaManagerResourceApi, times(1)).hostsStopRolesCommand(any());
        assertThat(apiHostNameListArgumentCaptor.getValue().getItems()).containsOnly("host1.example.com");
    }

    @Test
    public void testStopRolesOnHostsAllNodeFilteredOut() throws CloudbreakException, ApiException {
        StackDtoDelegate stack = getStack();
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host3.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host4.example.com"));
        when(hostsResourceApi.readHosts(isNull(), isNull(), any())).thenReturn(apiHostList);

        underTest.stopRolesOnHosts(stack, client, Set.of("host1.example.com", "host2.example.com"));
        verify(clouderaManagerResourceApi, times(0)).hostsStopRolesCommand(any());
    }

    @Test
    public void testStopRolesOnHostsAllNodeFilteredOutBecauseOfBadState() throws CloudbreakException, ApiException {
        StackDtoDelegate stack = getStack();
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com", ApiHealthSummary.BAD));
        apiHostList.addItemsItem(createApiHostRef("host2.example.com", ApiHealthSummary.BAD));
        when(hostsResourceApi.readHosts(isNull(), isNull(), any())).thenReturn(apiHostList);

        underTest.stopRolesOnHosts(stack, client, Set.of("host1.example.com", "host2.example.com"));
        verify(clouderaManagerResourceApi, times(0)).hostsStopRolesCommand(any());
    }

    private InstanceGroup createInstanceGroup() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(createDeletedInstanceMetadata(), createRunningInstanceMetadata()));
        return instanceGroup;
    }

    private void mockDecommission(Pair<BigDecimal, ExtendedPollingResult> resultPair, Pair<BigDecimal, ExtendedPollingResult>... resultPairs)
            throws ApiException {
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(eq(client))).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.hostsDecommissionCommand(any())).thenReturn(getApiCommand(resultPair.getLeft()),
                Arrays.stream(resultPairs).map(resultPairItem -> getApiCommand(resultPairItem.getLeft())).toArray(ApiCommand[]::new));
        when(pollingServiceProvider.startPollingCmHostDecommissioning(any(), eq(client), any(), anyBoolean(), anyInt())).thenReturn(resultPair.getRight(),
                Arrays.stream(resultPairs).map(resultPairItem -> resultPairItem.getRight()).toArray(ExtendedPollingResult[]::new));
    }

    private void mockAbortCommand(BigDecimal commandId, BigDecimal... commandIds) throws ApiException {
        lenient().when(clouderaManagerApiFactory.getCommandsResourceApi(eq(client))).thenReturn(commandsResourceApi);
        lenient().when(commandsResourceApi.abortCommand(any())).thenReturn(getApiCommand(commandId),
                Arrays.stream(commandIds).map(commandIdItem -> getApiCommand(commandIdItem)).toArray(ApiCommand[]::new));
    }

    private void mockListClusterHosts() throws ApiException {
        ApiHostList apiHostRefList = new ApiHostList();
        apiHostRefList.setItems(List.of(createApiHostRef(DELETED_INSTANCE_FQDN), createApiHostRef(RUNNING_INSTANCE_FQDN)));
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenReturn(apiHostRefList);
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
    }

    private InstanceMetaData createRunningInstanceMetadata() {
        return createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, RUNNING_INSTANCE_FQDN, "compute");
    }

    private InstanceMetaData createDeletedInstanceMetadata() {
        return createInstanceMetadata(InstanceStatus.DELETED_ON_PROVIDER_SIDE, DELETED_INSTANCE_FQDN, "compute");
    }

    private InstanceMetaData createInstanceMetadata(InstanceStatus servicesHealthy, String runningInstanceFqdn, String instanceGroupName) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(servicesHealthy);
        instanceMetaData.setDiscoveryFQDN(runningInstanceFqdn);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(instanceGroupName);
        instanceMetaData.setInstanceGroup(instanceGroup);
        return instanceMetaData;
    }

    private ApiHost createApiHostRef(String instanceFqd) {
        return createApiHostRef(instanceFqd, ApiHealthSummary.GOOD);
    }

    private ApiHost createApiHostRef(String instanceFqd, ApiHealthSummary healthSummary) {
        ApiHost instanceHostRef = new ApiHost();
        instanceHostRef.setHostname(instanceFqd);
        instanceHostRef.setHostId(instanceFqd);
        instanceHostRef.setHealthSummary(healthSummary);
        return instanceHostRef;
    }

    private ApiCommand getApiCommand(BigDecimal commandId) {
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setId(commandId);
        return apiCommand;
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setName(STACK_NAME);
        stack.setPlatformVariant("AWS");
        return stack;
    }
}
