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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
    public void testDecommissionForNodesOnlyUnKnownByCM() throws ApiException {
        HostTemplatesResourceApi hostTemplatesResourceApi = mock(HostTemplatesResourceApi.class);
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        apiHostTemplateList.setItems(new ArrayList<>());
        when(hostTemplatesResourceApi.readHostTemplates(any())).thenReturn(apiHostTemplateList);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com", ApiHealthSummary.GOOD));
        apiHostList.addItemsItem(createApiHostRef("host2.example.com", ApiHealthSummary.BAD));
        apiHostList.addItemsItem(createApiHostRef("host5.example.com", ApiHealthSummary.GOOD));
        when(hostsResourceApi.readHosts(any(), any(), any())).thenReturn(apiHostList);

        InstanceMetaData healthy1 = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, "host1.example.com", "compute");
        InstanceMetaData bad1 = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, "host2.example.com", "compute");
        InstanceMetaData unknown1 = createInstanceMetadata(InstanceStatus.ORCHESTRATION_FAILED, "host3.example.com", "compute");
        InstanceMetaData unknown2 = createInstanceMetadata(InstanceStatus.ORCHESTRATION_FAILED, "host4.example.com", "compute");
        InstanceMetaData healthy2 = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, "host5.example.com", "compute");

        Set<InstanceMetadataView> instanceMetaDataSet = Set.of(unknown1, unknown2, healthy1, healthy2, bad1);
        StackDtoDelegate stack = getStack();

        Set<InstanceMetadataView> removableInstances = underTest.collectDownscaleCandidates(client, stack, "compute", 2, instanceMetaDataSet);
        assertEquals(2, removableInstances.size());
        assertTrue(removableInstances.contains(unknown1));
        assertTrue(removableInstances.contains(unknown2));
    }

    @Test
    public void testDecommissionForNodesUnKnownByCMAndUnhealthy() throws ApiException {
        HostTemplatesResourceApi hostTemplatesResourceApi = mock(HostTemplatesResourceApi.class);
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        apiHostTemplateList.setItems(new ArrayList<>());
        when(hostTemplatesResourceApi.readHostTemplates(any())).thenReturn(apiHostTemplateList);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com", ApiHealthSummary.GOOD));
        apiHostList.addItemsItem(createApiHostRef("host2.example.com", ApiHealthSummary.BAD));
        apiHostList.addItemsItem(createApiHostRef("host5.example.com", ApiHealthSummary.GOOD));
        when(hostsResourceApi.readHosts(any(), any(), any())).thenReturn(apiHostList);

        InstanceMetaData healthy1 = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, "host1.example.com", "compute");
        InstanceMetaData bad1 = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, "host2.example.com", "compute");
        InstanceMetaData unknown1 = createInstanceMetadata(InstanceStatus.ORCHESTRATION_FAILED, "host3.example.com", "compute");
        InstanceMetaData unknown2 = createInstanceMetadata(InstanceStatus.ORCHESTRATION_FAILED, "host4.example.com", "compute");
        InstanceMetaData healthy2 = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, "host5.example.com", "compute");

        Set<InstanceMetadataView> instanceMetaDataSet = Set.of(unknown1, unknown2, healthy1, healthy2, bad1);
        StackDtoDelegate stack = getStack();

        Set<InstanceMetadataView> removableInstances = underTest.collectDownscaleCandidates(client, stack, "compute", 3, instanceMetaDataSet);
        assertEquals(3, removableInstances.size());
        assertTrue(removableInstances.contains(unknown1));
        assertTrue(removableInstances.contains(unknown2));
        assertTrue(removableInstances.contains(bad1));
    }

    @Test
    public void testDecommissionForNodesUnKnownByCMAndUnHealthyAndHealthy() throws ApiException {
        HostTemplatesResourceApi hostTemplatesResourceApi = mock(HostTemplatesResourceApi.class);
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        apiHostTemplateList.setItems(new ArrayList<>());
        when(hostTemplatesResourceApi.readHostTemplates(any())).thenReturn(apiHostTemplateList);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com", ApiHealthSummary.GOOD));
        apiHostList.addItemsItem(createApiHostRef("host2.example.com", ApiHealthSummary.BAD));
        apiHostList.addItemsItem(createApiHostRef("host5.example.com", ApiHealthSummary.GOOD));
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

    static Object[][] testDataMultiAz() {
        return new Object[][]{
                {
                        "collectDownscaleCandidatesMultiAz_NotKnownInCmOnly",
                        List.of(List.of("host1.example.com", "BAD", "1"),
                                List.of("host2.example.com", "NA", "1"),
                                List.of("host3.example.com", "BAD", "2"),
                                List.of("host4.example.com", "GOOD", "2"),
                                List.of("host5.example.com", "NA", "3")),
                        2,
                        List.of("host5.example.com", "host2.example.com")
                },
                {
                        "collectDownscaleCandidatesMultiAz_UnHealthyOnly",
                        List.of(List.of("host1.example.com", "GOOD", "1"),
                                List.of("host2.example.com", "BAD", "1"),
                                List.of("host3.example.com", "BAD", "2"),
                                List.of("host4.example.com", "GOOD", "2"),
                                List.of("host5.example.com", "BAD", "3")),
                        2,
                        List.of("host2.example.com", "host3.example.com")
                },
                {
                        "collectDownscaleCandidatesMultiAz_NotKnownInCmAndUnHealthy",
                        List.of(List.of("host1.example.com", "GOOD", "1"),
                                List.of("host2.example.com", "NA", "1"),
                                List.of("host3.example.com", "BAD", "2"),
                                List.of("host4.example.com", "GOOD", "2"),
                                List.of("host5.example.com", "BAD", "3")),
                        2,
                        List.of("host2.example.com", "host3.example.com")
                },
                {
                        "collectDownscaleCandidatesMultiAz_NotKnownInCmAndUnHealthyMultiZones",
                        List.of(List.of("host1.example.com", "GOOD", "1"),
                                List.of("host2.example.com", "NA", "1"),
                                List.of("host3.example.com", "BAD", "2"),
                                List.of("host4.example.com", "GOOD", "2"),
                                List.of("host5.example.com", "BAD", "3"),
                                List.of("host6.example.com", "GOOD", "3")),
                        3,
                        List.of("host2.example.com", "host3.example.com", "host5.example.com")
                },
                {
                        "collectDownscaleCandidatesMultiAz_NotKnownInCmAndHealthyMultiZones",
                        List.of(List.of("host1.example.com", "GOOD", "1"),
                                List.of("host2.example.com", "NA", "1"),
                                List.of("host3.example.com", "GOOD", "2"),
                                List.of("host4.example.com", "GOOD", "2"),
                                List.of("host5.example.com", "GOOD", "3")),
                        2,
                        List.of("host2.example.com", "host4.example.com")
                },
                {
                        "collectDownscaleCandidatesMultiAz_HealthyNodesMultiAzs",
                        List.of(List.of("host1.example.com", "GOOD", "1"),
                                List.of("host2.example.com", "GOOD", "1"),
                                List.of("host3.example.com", "GOOD", "2"),
                                List.of("host4.example.com", "GOOD", "2"),
                                List.of("host5.example.com", "GOOD", "3")),
                        2,
                        List.of("host2.example.com", "host4.example.com")
                },
                {
                        "collectDownscaleCandidatesMultiAz_AllNodes",
                        List.of(List.of("host1.example.com", "GOOD", "1"),
                                List.of("host2.example.com", "GOOD", "1"),
                                List.of("host3.example.com", "GOOD", "2"),
                                List.of("host4.example.com", "GOOD", "2"),
                                List.of("host5.example.com", "GOOD", "3")),
                        5,
                        List.of("host1.example.com", "host2.example.com", "host3.example.com", "host4.example.com", "host5.example.com")
                },
                {
                        "collectDownscaleCandidatesMultiAz_NotKnownInCmAndHealthyAndUnHealthyInSameZone",
                        List.of(List.of("host1.example.com", "GOOD", "1"),
                                List.of("host2.example.com", "NA", "1"),
                                List.of("host3.example.com", "BAD", "1"),
                                List.of("host4.example.com", "GOOD", "1"),
                                List.of("host5.example.com", "GOOD", "2"),
                                List.of("host4.example.com", "GOOD", "3")),
                        3,
                        List.of("host2.example.com", "host3.example.com", "host4.example.com")
                },
                {
                        "collectDownscaleCandidatesMultiAz_NotKnownInCmAndHealthyAndUnHealthyWithZonesNotPopulated",
                        List.of(List.of("host1.example.com", "GOOD", "NA"),
                                List.of("host2.example.com", "NA", "NA"),
                                List.of("host3.example.com", "BAD", "NA"),
                                List.of("host4.example.com", "GOOD", "NA"),
                                List.of("host5.example.com", "GOOD", "NA")),
                        3,
                        List.of("host2.example.com", "host3.example.com", "host5.example.com")
                },
                {
                        "collectDownscaleCandidatesMultiAz_HealthyFromAnyZone",
                        List.of(List.of("host1.example.com", "GOOD", "1"),
                                List.of("host2.example.com", "GOOD", "1"),
                                List.of("host3.example.com", "GOOD", "2"),
                                List.of("host4.example.com", "GOOD", "3")),
                        2,
                        List.of("host2.example.com", "host1.example.com:host3.example.com:host4.example.com")
                }
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testDataMultiAz")
    public void collectDownscaleCandidatesMultiAz(String testName, List<List<String>> input, Integer scalingAdjustment, List<String> expectedHosts)
            throws ApiException {
        HostTemplatesResourceApi hostTemplatesResourceApi = mock(HostTemplatesResourceApi.class);
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        apiHostTemplateList.setItems(new ArrayList<>());
        when(hostTemplatesResourceApi.readHostTemplates(any())).thenReturn(apiHostTemplateList);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        for (List<String> hostInfo : input) {
            if (!hostInfo.get(1).equals("NA")) {
                apiHostList.addItemsItem(createApiHostRef(hostInfo.get(0), ApiHealthSummary.fromValue(hostInfo.get(1))));
            }
        }
        when(hostsResourceApi.readHosts(any(), any(), any())).thenReturn(apiHostList);

        Set<InstanceMetadataView> instanceMetaDataSet = new HashSet<>();

        for (int i = 0; i < input.size(); i++) {
            instanceMetaDataSet.add(createInstanceMetadata("instance" + i,
                    InstanceStatus.SERVICES_HEALTHY, input.get(i).get(0), "compute", input.get(i).get(2)));
        }

        StackDtoDelegate stack = getStack(true);
        Set<InstanceMetadataView> removableInstances = underTest.collectDownscaleCandidates(client, stack, "compute",
                scalingAdjustment, instanceMetaDataSet);
        assertEquals(expectedHosts.size(), removableInstances.size());

        Set<String> removableHosts = removableInstances.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toSet());

        assertTrue(matchExpectedHosts(removableHosts, expectedHosts), () -> String.format("removableHosts: %s, expectedHosts: %s",
                removableHosts, expectedHosts));
    }

    private boolean matchExpectedHosts(Set<String> removableHosts, List<String> expectedHosts) {
        for (String host : expectedHosts) {
            String [] anyHosts = host.split(":");
            boolean anyHostMatch = false;
            for (String anyHost : anyHosts) {
                if (removableHosts.contains(anyHost)) {
                    if (anyHostMatch) {
                        return false;
                    } else {
                        anyHostMatch = true;
                    }
                }
            }
            if (!anyHostMatch) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testDecommissionComparisonMethodViolatesShouldNotBeThrown() throws ApiException {
        HostTemplatesResourceApi hostTemplatesResourceApi = mock(HostTemplatesResourceApi.class);
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        apiHostTemplateList.setItems(new ArrayList<>());
        when(hostTemplatesResourceApi.readHostTemplates(any())).thenReturn(apiHostTemplateList);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        Set<InstanceMetadataView> instanceMetaDataSet = new HashSet<>();

        // we need this size (as I observed >= 201) to have "java.lang.IllegalArgumentException: Comparison method violates its general contract!"
        int nodeCount = 500;

        for (int i = 0; i < nodeCount; i++) {
            apiHostList.addItemsItem(createApiHostRef("host" + i + ".example.com", i % 2 == 0 ? ApiHealthSummary.BAD : ApiHealthSummary.GOOD));
            instanceMetaDataSet.add(createInstanceMetadata(InstanceStatus.SERVICES_UNHEALTHY, "host" + i + ".example.com", "compute"));
        }
        when(hostsResourceApi.readHosts(any(), any(), any())).thenReturn(apiHostList);

        StackDtoDelegate stack = getStack();

        Set<InstanceMetadataView> removableInstances = underTest.collectDownscaleCandidates(client, stack, "compute", nodeCount, instanceMetaDataSet);
        assertEquals(nodeCount, removableInstances.size());
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

    @Test
    public void testEnterMaintenanceMode() throws ApiException {
        Set<String> hostList = new HashSet<>(Arrays.asList("host1", "host2", "host3"));
        ApiHost host1 = new ApiHost().hostname("host1").hostId("host1Id");
        ApiHost host2 = new ApiHost().hostname("host2").hostId("host2Id");
        List<ApiHost> apiHostList = Arrays.asList(host1, host2);
        ApiHostList hostListResponse = new ApiHostList().items(apiHostList);

        when(clouderaManagerApiFactory.getHostsResourceApi(any())).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenReturn(hostListResponse);
        when(hostsResourceApi.enterMaintenanceMode(any())).thenReturn(null);

        underTest.enterMaintenanceMode(hostList, client);

        verify(hostsResourceApi, times(2)).enterMaintenanceMode(any());
    }

    @Test
    public void testEnterMaintenanceModeWhenNull() throws ApiException {
        Set<String> hostList = new HashSet<>(Arrays.asList("host1", "host2", "host3"));
        ApiHostList hostListResponse = new ApiHostList().items(null);

        when(clouderaManagerApiFactory.getHostsResourceApi(any())).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenReturn(hostListResponse);

        underTest.enterMaintenanceMode(hostList, client);

        verify(hostsResourceApi, times(0)).enterMaintenanceMode(any());
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
        return createInstanceMetadata(null, servicesHealthy, runningInstanceFqdn, instanceGroupName, null);
    }

    private InstanceMetaData createInstanceMetadata(String instanceId, InstanceStatus servicesHealthy, String runningInstanceFqdn,
            String instanceGroupName, String availabilityZone) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(instanceId);
        instanceMetaData.setInstanceStatus(servicesHealthy);
        instanceMetaData.setDiscoveryFQDN(runningInstanceFqdn);
        if (!"NA".equals(availabilityZone)) {
            instanceMetaData.setAvailabilityZone(availabilityZone);
        }
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
        return getStack(false);
    }

    private Stack getStack(boolean multiAz) {
        Stack stack = new Stack();
        stack.setName(STACK_NAME);
        stack.setPlatformVariant("AWS");
        stack.setMultiAz(multiAz);
        return stack;
    }
}
