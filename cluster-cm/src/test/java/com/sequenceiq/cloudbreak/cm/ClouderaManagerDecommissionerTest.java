package com.sequenceiq.cloudbreak.cm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.Optional;
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
import org.mockito.Mockito;
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
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostNameList;
import com.cloudera.api.swagger.model.ApiHostTemplate;
import com.cloudera.api.swagger.model.ApiHostTemplateList;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupRef;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.service.NodeIsBusyException;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerDecommissionerTest {

    private static final String STACK_NAME = "stack";

    private static final String DELETED_INSTANCE_FQDN = "deletedInstance";

    private static final String RUNNING_INSTANCE_FQDN = "runningInstance";

    @Mock
    private ClouderaManagerPollingServiceProvider pollingServiceProvider;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private HostTemplatesResourceApi hostTemplatesResourceApi;

    @Mock
    private HostsResourceApi hostsResourceApi;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private CommandsResourceApi commandsResourceApi;

    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private ApiHostList apiHostList;

    @InjectMocks
    private ClouderaManagerDecommissioner underTest;

    @Mock
    private StackDto stack;

    @Mock
    private ClusterView cluster;

    @Mock
    private ApiClient v51Client;

    @Mock
    private ApiClient v53Client;

    @Mock
    private FlowMessageService flowMessageService;

    @Test
    public void testVerifyNodesAreRemovable() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newNoneRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(1, 6);
        ApiHostTemplateList hostTemplates = createEmptyHostTemplates();
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        when(resourceAttributeUtil.getTypedAttributes(Mockito.any(Resource.class), Mockito.any(Class.class)))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(clouderaManagerApiFactory.getHostsResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq(null), eq(null), anyString())).thenReturn(apiHostList);
        when(apiHostList.getItems()).thenReturn(List.of());
        // WHEN
        InstanceGroupDto firstInstanceGroup = instanceGroups.iterator().next();
        underTest.verifyNodesAreRemovable(stack, new ArrayList<>(firstInstanceGroup.getInstanceMetadataViews()), new ApiClient());
        // THEN no exception
    }

    @Test
    public void testVerifyNodesAreBusy() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newNoneRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(1, 6);
        ApiHostTemplateList hostTemplates = createEmptyHostTemplates();
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);

        when(clouderaManagerApiFactory.getHostsResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq(null), eq(null), anyString())).thenReturn(apiHostList);
        when(apiHostList.getItems()).thenReturn(List.of(getBusyHost()));

        when(resourceAttributeUtil.getTypedAttributes(Mockito.any(Resource.class), Mockito.any(Class.class)))
                .thenReturn(Optional.of(volumeSetAttributes));
        // WHEN
        InstanceGroupDto firstInstanceGroup = instanceGroups.iterator().next();

        NodeIsBusyException e = assertThrows(NodeIsBusyException.class,
                () -> underTest.verifyNodesAreRemovable(stack,
                        firstInstanceGroup.getInstanceMetadataViews(),
                        new ApiClient()));
        assertEquals("Node is in 'busy' state, cannot be decommissioned right now. " +
                "Please try to remove the node later. Busy hosts: [hg0-host-1]", e.getMessage());
        // THEN exception
    }

    private ApiHost getBusyHost() {
        ApiHost apihost = new ApiHost();
        apihost.setHostname("hg0-host-1");
        ApiRoleRef apiroleref = new ApiRoleRef();
        apiroleref.setRoleStatus(ApiRoleState.BUSY);
        apihost.addRoleRefsItem(apiroleref);
        return apihost;
    }

    @Test
    public void testNotAvailableNodeShouldBeDeletedWhenRunningNodesFulfillTheReplicationNo() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(1, 2, InstanceStatus.SERVICES_HEALTHY);
        InstanceGroupDto deletedOnProviderInstanceGroups = createTestInstanceGroup(2L, "hg1", 2, InstanceStatus.DELETED_BY_PROVIDER);
        instanceGroups.add(deletedOnProviderInstanceGroups);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes(instanceGroups.stream().findFirst().get().getInstanceGroup().getGroupName());

        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        when(clouderaManagerApiFactory.getHostsResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq(null), eq(null), anyString())).thenReturn(apiHostList);
        when(apiHostList.getItems()).thenReturn(List.of());
        when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        // WHEN
        InstanceMetadataView firstInstanceMetaData = deletedOnProviderInstanceGroups.getInstanceMetadataViews().stream().findFirst().get();
        Set<InstanceMetadataView> removableInstances = Set.of(firstInstanceMetaData);
        // WHEN
        underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient());
        // THEN there is no exception
    }

    @Test
    public void testVerifyNodesAreRemovableWithoutRepairWithReplicationAndTooMuchRemovableNodes() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(2, 5);
        ApiServiceConfig apiServiceConfig = createApiServiceConfigWithReplication("3", true);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes(instanceGroups.stream().findFirst().get().getInstanceGroup().getGroupName());
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(clouderaManagerApiFactory.getServicesResourceApi(Mockito.any(ApiClient.class))).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServiceConfig(stack.getName(), "hdfs", "full")).thenReturn(apiServiceConfig);
        // WHEN
        InstanceGroupDto firstInstanceGroup = instanceGroups.iterator().next();
        List<InstanceMetadataView> firstInstanceGroupInstances = firstInstanceGroup.getInstanceMetadataViews();
        Set<InstanceMetadataView> removableInstances = firstInstanceGroupInstances.stream().limit(5).collect(Collectors.toSet());
        assertThrows(NotEnoughNodeException.class,
                () -> underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient()));
        // THEN no exception
    }

    @Test
    public void dataNodeHostGroupDownscaleIsAllowedBelowReplicationFactorWhenThereAreEnoughDataNodesInOtherHostGroup() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(2, 3);
        ApiServiceConfig apiServiceConfig = createApiServiceConfigWithReplication("3", true);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes("hg0", "hg1");
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(clouderaManagerApiFactory.getServicesResourceApi(Mockito.any(ApiClient.class))).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getHostsResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq(null), eq(null), anyString())).thenReturn(apiHostList);
        when(apiHostList.getItems()).thenReturn(List.of());
        when(servicesResourceApi.readServiceConfig(stack.getName(), "hdfs", "full")).thenReturn(apiServiceConfig);
        // WHEN
        InstanceGroupDto firstInstanceGroup = instanceGroups.iterator().next();
        List<InstanceMetadataView> firstInstanceGroupInstances = firstInstanceGroup.getInstanceMetadataViews();
        Set<InstanceMetadataView> removableInstances = firstInstanceGroupInstances.stream().limit(2).collect(Collectors.toSet());
        underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient());
        // THEN no exception
    }

    @Test
    public void dataNodeHostGroupDownscaleIsNotAllowedBelowReplicationFactorWhenThereAreNotEnoughDataNodesInOtherHostGroup() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(2, 2);
        ApiServiceConfig apiServiceConfig = createApiServiceConfigWithReplication("3", true);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes("hg0", "hg1");
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(clouderaManagerApiFactory.getServicesResourceApi(Mockito.any(ApiClient.class))).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServiceConfig(stack.getName(), "hdfs", "full")).thenReturn(apiServiceConfig);
        // WHEN
        InstanceGroupDto firstInstanceGroup = instanceGroups.iterator().next();
        List<InstanceMetadataView> firstInstanceGroupInstances = firstInstanceGroup.getInstanceMetadataViews();
        Set<InstanceMetadataView> removableInstances = firstInstanceGroupInstances.stream().limit(2).collect(Collectors.toSet());
        assertThrows(NotEnoughNodeException.class,
                () -> underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient()));
        // THEN no exception
    }

    @Test
    public void dataNodeHostGroupDownscaleIsAllowedBelowReplicationFactorWhenThereAreNotEnoughDataNodesInOtherHostGroupButRepairIsInProgress()
            throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newNoneRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(2, 2);
        ApiServiceConfig apiServiceConfig = createApiServiceConfigWithReplication("3", true);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes("hg0", "hg1");
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(clouderaManagerApiFactory.getHostsResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq(null), eq(null), anyString())).thenReturn(apiHostList);
        when(apiHostList.getItems()).thenReturn(List.of());
        // WHEN
        InstanceGroupDto firstInstanceGroup = instanceGroups.iterator().next();
        List<InstanceMetadataView> firstInstanceGroupInstances = firstInstanceGroup.getInstanceMetadataViews();
        Set<InstanceMetadataView> removableInstances = firstInstanceGroupInstances.stream().limit(2).collect(Collectors.toSet());
        underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient());
        // THEN no exception
    }

    @Test
    public void testVerifyNodesAreRemovableWithoutRepairAndReplication() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(2, 5);
        ApiServiceConfig apiServiceConfig = createApiServiceConfigWithReplication("3", true);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes(instanceGroups.stream().findFirst().get().getInstanceGroup().getGroupName());
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(clouderaManagerApiFactory.getServicesResourceApi(Mockito.any(ApiClient.class))).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServiceConfig(stack.getName(), "hdfs", "full")).thenReturn(apiServiceConfig);
        when(clouderaManagerApiFactory.getHostsResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq(null), eq(null), anyString())).thenReturn(apiHostList);
        when(apiHostList.getItems()).thenReturn(List.of());
        // WHEN
        InstanceGroupDto firstInstanceGroup = instanceGroups.iterator().next();
        List<InstanceMetadataView> firstInstanceGroupInstances = firstInstanceGroup.getInstanceMetadataViews();
        Set<InstanceMetadataView> removableInstances = firstInstanceGroupInstances.stream().limit(2).collect(Collectors.toSet());
        underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient());
        // THEN no exception
    }

    @Test
    public void testVerifyNodesAreRemovableWithRepairAndReplication() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newNoneRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(2, 5);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes(instanceGroups.stream().findFirst().get().getInstanceGroup().getGroupName());
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);

        when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(clouderaManagerApiFactory.getHostsResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq(null), eq(null), anyString())).thenReturn(apiHostList);
        when(apiHostList.getItems()).thenReturn(List.of());
        // WHEN
        InstanceGroupDto firstInstanceGroup = instanceGroups.iterator().next();
        List<InstanceMetadataView> firstInstanceGroupInstances = firstInstanceGroup.getInstanceMetadataViews();
        Set<InstanceMetadataView> removableInstances = firstInstanceGroupInstances.stream().limit(5).collect(Collectors.toSet());
        underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient());
        // THEN no exception
    }

    @Test
    public void testCollectDownscaleCandidatesWhenEveryHostHasHostname() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newNoneRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(1, 6);
        InstanceGroupDto downscaledHostGroup = instanceGroups.iterator().next();
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
        HostTemplatesResourceApi hostTemplatesResourceApi = mock(HostTemplatesResourceApi.class);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        ApiHostTemplateList hostTemplates = createEmptyHostTemplates();
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        ApiHostList apiHostRefList = new ApiHostList();
        List<ApiHost> apiHosts = new ArrayList<>();
        instanceGroups.stream()
                .flatMap(hostGroup -> hostGroup.getInstanceMetadataViews().stream())
                .map(im -> {
                    InstanceGroup instanceGroup = new InstanceGroup();
                    instanceGroup.setGroupName("hgName");
                    ((InstanceMetaData) im).setInstanceGroup(instanceGroup);
                    return im.getDiscoveryFQDN();
                })
                .forEach(hostName -> {
                    ApiHost apiHostRef = new ApiHost();
                    apiHostRef.setHostname(hostName);
                    apiHostRef.setHostId(hostName);
                    apiHostRef.setHealthSummary(ApiHealthSummary.GOOD);
                    apiHosts.add(apiHostRef);
                });
        apiHostRefList.setItems(apiHosts);
        when(hostsResourceApi.readHosts(any(), any(), any())).thenReturn(apiHostRefList);
        Set<InstanceMetadataView> downscaleCandidates = underTest.collectDownscaleCandidates(mock(ApiClient.class), stack, "hgName", -2,
                new HashSet<>(downscaledHostGroup.getInstanceMetadataViews()));
        assertEquals(2, downscaleCandidates.size());
        assertTrue(downscaleCandidates.stream().anyMatch(instanceMetaData -> "hg0-instanceid-4".equals(instanceMetaData.getInstanceId())),
                "Assert if downscaleCandidates contains hg0-instanceid-4");
        assertTrue(downscaleCandidates.stream().anyMatch(instanceMetaData -> "hg0-instanceid-5".equals(instanceMetaData.getInstanceId())),
                "Assert if downscaleCandidates contains hg0-instanceid-4");
    }

    @Test
    public void testCollectDownscaleCandidatesWhenEveryHostHasHostnameButNotEnoughNodesToDownscale() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newNoneRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(1, 6);
        InstanceGroupDto downscaledHostGroup = instanceGroups.iterator().next();
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
        HostTemplatesResourceApi hostTemplatesResourceApi = mock(HostTemplatesResourceApi.class);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        ApiHostTemplateList hostTemplates = createEmptyHostTemplates();
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        assertThrows(NotEnoughNodeException.class, () -> underTest.collectDownscaleCandidates(mock(ApiClient.class), stack, "hgName", -8,
                new HashSet<>(downscaledHostGroup.getInstanceMetadataViews())));
    }

    @Test
    public void testCollectDownscaleCandidatesWhenOneHostDoesNotHaveFQDN() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = newNoneRemovableVolume();
        List<InstanceGroupDto> instanceGroups = createTestInstanceGroups(1, 6);
        InstanceGroupDto downscaledHostGroup = instanceGroups.iterator().next();
        Optional<InstanceMetadataView> hgHost2 = downscaledHostGroup.getInstanceMetadataViews().stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN().equals("hg0-host-2"))
                .findFirst();
        hgHost2.ifPresent(instanceMetaData -> ((InstanceMetaData) instanceMetaData).setDiscoveryFQDN(null));
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(stack.getDiskResources()).thenReturn(resources(volumeSetAttributes));
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
        HostTemplatesResourceApi hostTemplatesResourceApi = mock(HostTemplatesResourceApi.class);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        ApiHostTemplateList hostTemplates = createEmptyHostTemplates();
        when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        ApiHostList apiHostRefList = new ApiHostList();
        List<ApiHost> apiHosts = new ArrayList<>();
        instanceGroups.stream()
                .flatMap(ig -> ig.getInstanceMetadataViews().stream())
                .map(im -> {
                    InstanceGroup instanceGroup = new InstanceGroup();
                    instanceGroup.setGroupName("hgName");
                    ((InstanceMetaData) im).setInstanceGroup(instanceGroup);
                    return im.getDiscoveryFQDN();
                })
                .forEach(hostName -> {
                    ApiHost apiHostRef = new ApiHost();
                    apiHostRef.setHostname(hostName);
                    apiHostRef.setHostId(hostName);
                    apiHostRef.setHealthSummary(ApiHealthSummary.GOOD);
                    apiHosts.add(apiHostRef);
                });
        apiHostRefList.setItems(apiHosts);
        when(hostsResourceApi.readHosts(any(), any(), any())).thenReturn(apiHostRefList);
        Set<InstanceMetadataView> downscaleCandidates = underTest.collectDownscaleCandidates(mock(ApiClient.class), stack, "hgName", -2,
                new HashSet<>(downscaledHostGroup.getInstanceMetadataViews()));
        assertEquals(2, downscaleCandidates.size());
        assertTrue(downscaleCandidates.stream().anyMatch(instanceMetaData -> "hg0-instanceid-2".equals(instanceMetaData.getInstanceId())),
                "Assert if downscaleCandidates contains hg0-host-2, because FQDN is missing");
        assertTrue(downscaleCandidates.stream().anyMatch(instanceMetaData -> "hg0-host-5".equals(instanceMetaData.getDiscoveryFQDN())),
                "Assert if downscaleCandidates contains hg0-host-5");
    }

    @Test
    public void testDecommissionForLostNodesIfFirstDecommissionSucceeded() throws ApiException {
        mockListClusterHosts();
        mockDecommission(Pair.of(BigDecimal.ONE, new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build()));
        mockAbortCommand(BigDecimal.ONE);
        InstanceMetaData deletedInstanceMetadata = createDeletedInstanceMetadata();

        underTest.decommissionNodes(getStack(), Map.of(deletedInstanceMetadata.getDiscoveryFQDN(), deletedInstanceMetadata), v51Client);

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

        underTest.decommissionNodes(getStack(), Map.of(deletedInstanceMetadata.getDiscoveryFQDN(), deletedInstanceMetadata), v51Client);

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

        underTest.decommissionNodes(getStack(), Map.of(deletedInstanceMetadata.getDiscoveryFQDN(), deletedInstanceMetadata), v51Client);

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
        Map<String, InstanceMetadataView> result = underTest.collectHostsToRemove(stack, groupName, hostNames, v51Client);

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
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(v51Client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
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

        Set<InstanceMetadataView> removableInstances = underTest.collectDownscaleCandidates(v51Client, stack, "compute", 2, instanceMetaDataSet);
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
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(v51Client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
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

        Set<InstanceMetadataView> removableInstances = underTest.collectDownscaleCandidates(v51Client, stack, "compute", 3, instanceMetaDataSet);
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
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(v51Client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
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

        Set<InstanceMetadataView> removableInstances = underTest.collectDownscaleCandidates(v51Client, stack, "compute", 4, instanceMetaDataSet);
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
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(v51Client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
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
        Set<InstanceMetadataView> removableInstances = underTest.collectDownscaleCandidates(v51Client, stack, "compute",
                scalingAdjustment, instanceMetaDataSet);
        assertEquals(expectedHosts.size(), removableInstances.size());

        Set<String> removableHosts = removableInstances.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toSet());

        assertTrue(matchExpectedHosts(removableHosts, expectedHosts), () -> String.format("removableHosts: %s, expectedHosts: %s",
                removableHosts, expectedHosts));
    }

    @Test
    public void testDecommissionComparisonMethodViolatesShouldNotBeThrown() throws ApiException {
        HostTemplatesResourceApi hostTemplatesResourceApi = mock(HostTemplatesResourceApi.class);
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        apiHostTemplateList.setItems(new ArrayList<>());
        when(hostTemplatesResourceApi.readHostTemplates(any())).thenReturn(apiHostTemplateList);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(v51Client)).thenReturn(hostTemplatesResourceApi);

        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
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

        Set<InstanceMetadataView> removableInstances = underTest.collectDownscaleCandidates(v51Client, stack, "compute", nodeCount, instanceMetaDataSet);
        assertEquals(nodeCount, removableInstances.size());
    }

    @Test
    public void testDeleteHostWithoutFqdn() throws ApiException {
        StackDtoDelegate stack = getStack();
        InstanceMetaData instanceMetaData = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, null, "compute");
        ServicesResourceApi servicesResourceApi = mock(ServicesResourceApi.class);
        when(clouderaManagerApiFactory.getServicesResourceApi(eq(v51Client))).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(stack.getName()), any())).thenReturn(new ApiServiceList().items(new ArrayList<>()));
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com"));
        when(hostsResourceApi.readHosts(any(), any(), any())).thenReturn(apiHostList);

        underTest.deleteHost(stack, instanceMetaData, v51Client);

        verify(hostsResourceApi, times(1)).readHosts(any(), any(), any());
        verify(hostsResourceApi, never()).deleteHost(any());
    }

    @Test
    public void testStopRolesOnHosts() throws CloudbreakException, ApiException {
        StackDtoDelegate stack = getStack();
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
        when(clouderaManagerApiFactory.getHostsResourceApi(v53Client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host2.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host3.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host4.example.com"));
        when(hostsResourceApi.readHosts(isNull(), isNull(), any())).thenReturn(apiHostList);
        when(hostsResourceApi.stopAllRolesOnNodeGracefully(any())).thenReturn(new ApiCommand().id(BigDecimal.ONE));
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(eq(v51Client))).thenReturn(clouderaManagerResourceApi);
        ArgumentCaptor<ApiHostNameList> apiHostNameListArgumentCaptor = ArgumentCaptor.forClass(ApiHostNameList.class);
        BigDecimal apiCommandId = BigDecimal.ONE;
        when(clouderaManagerResourceApi.hostsStopRolesCommand(apiHostNameListArgumentCaptor.capture())).thenReturn(getApiCommand(apiCommandId));
        ExtendedPollingResult extendedPollingResult = mock(ExtendedPollingResult.class);
        when(pollingServiceProvider.startPollingStopRolesCommand(any(), eq(v51Client), eq(apiCommandId))).thenReturn(extendedPollingResult);

        underTest.stopRolesOnHosts(stack, v53Client, v51Client, Set.of("host1.example.com", "host2.example.com"), true);
        verify(hostsResourceApi, times(2)).stopAllRolesOnNodeGracefully(any());
        verify(clouderaManagerResourceApi, times(1)).hostsStopRolesCommand(any());
        assertThat(apiHostNameListArgumentCaptor.getValue().getItems()).containsOnly("host1.example.com", "host2.example.com");
    }

    @Test
    public void testStopRolesOnHostsBadNodeFilteredOut() throws CloudbreakException, ApiException {
        StackDtoDelegate stack = getStack();
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
        when(clouderaManagerApiFactory.getHostsResourceApi(v53Client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host2.example.com", ApiHealthSummary.BAD));
        apiHostList.addItemsItem(createApiHostRef("host3.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host4.example.com"));
        when(hostsResourceApi.readHosts(isNull(), isNull(), any())).thenReturn(apiHostList);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(eq(v51Client))).thenReturn(clouderaManagerResourceApi);
        ArgumentCaptor<ApiHostNameList> apiHostNameListArgumentCaptor = ArgumentCaptor.forClass(ApiHostNameList.class);
        BigDecimal apiCommandId = BigDecimal.ONE;
        when(clouderaManagerResourceApi.hostsStopRolesCommand(apiHostNameListArgumentCaptor.capture())).thenReturn(getApiCommand(apiCommandId));
        ExtendedPollingResult extendedPollingResult = mock(ExtendedPollingResult.class);
        when(pollingServiceProvider.startPollingStopRolesCommand(any(), eq(v51Client), eq(apiCommandId))).thenReturn(extendedPollingResult);

        underTest.stopRolesOnHosts(stack, v53Client, v51Client, Set.of("host1.example.com"), true);
        verify(clouderaManagerResourceApi, times(1)).hostsStopRolesCommand(any());
        assertThat(apiHostNameListArgumentCaptor.getValue().getItems()).containsOnly("host1.example.com");
    }

    @Test
    public void testStopRolesOnHostsOneNodeFilteredOut() throws CloudbreakException, ApiException {
        StackDtoDelegate stack = getStack();
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
        when(clouderaManagerApiFactory.getHostsResourceApi(v53Client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host3.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host4.example.com"));
        when(hostsResourceApi.readHosts(isNull(), isNull(), any())).thenReturn(apiHostList);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(eq(v51Client))).thenReturn(clouderaManagerResourceApi);
        ArgumentCaptor<ApiHostNameList> apiHostNameListArgumentCaptor = ArgumentCaptor.forClass(ApiHostNameList.class);
        BigDecimal apiCommandId = BigDecimal.ONE;
        when(clouderaManagerResourceApi.hostsStopRolesCommand(apiHostNameListArgumentCaptor.capture())).thenReturn(getApiCommand(apiCommandId));
        ExtendedPollingResult extendedPollingResult = mock(ExtendedPollingResult.class);
        when(pollingServiceProvider.startPollingStopRolesCommand(any(), eq(v51Client), eq(apiCommandId))).thenReturn(extendedPollingResult);

        underTest.stopRolesOnHosts(stack, v53Client, v51Client, Set.of("host1.example.com", "host2.example.com"), true);
        verify(clouderaManagerResourceApi, times(1)).hostsStopRolesCommand(any());
        assertThat(apiHostNameListArgumentCaptor.getValue().getItems()).containsOnly("host1.example.com");
    }

    @Test
    public void testStopRolesOnHostsAllNodeFilteredOut() throws CloudbreakException, ApiException {
        StackDtoDelegate stack = getStack();
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host3.example.com"));
        apiHostList.addItemsItem(createApiHostRef("host4.example.com"));
        when(hostsResourceApi.readHosts(isNull(), isNull(), any())).thenReturn(apiHostList);

        underTest.stopRolesOnHosts(stack, v53Client, v51Client, Set.of("host1.example.com", "host2.example.com"), true);
        verify(clouderaManagerResourceApi, times(0)).hostsStopRolesCommand(any());
    }

    @Test
    public void testStopRolesOnHostsAllNodeFilteredOutBecauseOfBadState() throws CloudbreakException, ApiException {
        StackDtoDelegate stack = getStack();
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(createApiHostRef("host1.example.com", ApiHealthSummary.BAD));
        apiHostList.addItemsItem(createApiHostRef("host2.example.com", ApiHealthSummary.BAD));
        when(hostsResourceApi.readHosts(isNull(), isNull(), any())).thenReturn(apiHostList);

        underTest.stopRolesOnHosts(stack, v53Client, v51Client, Set.of("host1.example.com", "host2.example.com"), true);
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

        underTest.enterMaintenanceMode(hostList, v51Client);

        verify(hostsResourceApi, times(2)).enterMaintenanceMode(any());
    }

    @Test
    public void testEnterMaintenanceModeWhenNull() throws ApiException {
        Set<String> hostList = new HashSet<>(Arrays.asList("host1", "host2", "host3"));
        ApiHostList hostListResponse = new ApiHostList().items(null);

        when(clouderaManagerApiFactory.getHostsResourceApi(any())).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenReturn(hostListResponse);

        underTest.enterMaintenanceMode(hostList, v51Client);

        verify(hostsResourceApi, times(0)).enterMaintenanceMode(any());
    }

    private static VolumeSetAttributes newRemovableVolume() {
        return new VolumeSetAttributes("az", true, "fstab", List.of(), 50, "vt");
    }

    private static VolumeSetAttributes newNoneRemovableVolume() {
        return new VolumeSetAttributes("az", false, "fstab", List.of(), 50, "vt");
    }

    private boolean matchExpectedHosts(Set<String> removableHosts, List<String> expectedHosts) {
        for (String host : expectedHosts) {
            String[] anyHosts = host.split(":");
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

    private ApiServiceConfig createApiServiceConfigWithReplication(String value, boolean defaultValue) {
        ApiServiceConfig apiServiceConfig = new ApiServiceConfig();
        List<ApiConfig> configItems = new ArrayList<>();
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setName("dfs_replication");
        if (defaultValue) {
            apiConfig.setDefault(value);
        } else {
            apiConfig.setValue(value);
        }
        configItems.add(apiConfig);
        apiServiceConfig.setItems(configItems);
        return apiServiceConfig;
    }

    private List<InstanceGroupDto> createTestInstanceGroups(int groupCount, int hostCount) {
        return createTestInstanceGroups(groupCount, hostCount, InstanceStatus.SERVICES_HEALTHY);
    }

    private List<InstanceGroupDto> createTestInstanceGroups(int groupCount, int hostCount, InstanceStatus instanceStatus) {
        List<InstanceGroupDto> instanceGroupDtos = new ArrayList<>();
        for (long i = 0; i < groupCount; i++) {
            instanceGroupDtos.add(createTestInstanceGroup(i, "hg" + i, hostCount, instanceStatus));
        }
        return instanceGroupDtos;
    }

    private InstanceGroupDto createTestInstanceGroup(Long id, String name, int hostCount, InstanceStatus instanceStatus) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(name);
        instanceGroup.setId(id);
        List<InstanceMetadataView> instanceMetaDatas = new ArrayList<>();
        for (long i = 0; i < hostCount; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(name + "-instanceid-" + i);
            instanceMetaData.setDiscoveryFQDN(name + "-host-" + i);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setInstanceStatus(instanceStatus);
            instanceMetaDatas.add(instanceMetaData);
        }
        return new InstanceGroupDto(instanceGroup, instanceMetaDatas);
    }

    private ApiHostTemplateList createEmptyHostTemplates() {
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        apiHostTemplateList.setItems(List.of());
        return apiHostTemplateList;
    }

    private ApiHostTemplateList createHostTemplatesWithDataNodes(String... hostGroups) {
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        List<ApiHostTemplate> apiHostTemplates = new ArrayList<>();
        for (String hostGroup : hostGroups) {
            ApiHostTemplate apiHostTemplate = new ApiHostTemplate();
            apiHostTemplate.setName(hostGroup);
            ApiRoleConfigGroupRef roleConfigGroupRef = new ApiRoleConfigGroupRef();
            roleConfigGroupRef.setRoleConfigGroupName("_DATANODE_");
            apiHostTemplate.setRoleConfigGroupRefs(List.of(roleConfigGroupRef));
            apiHostTemplates.add(apiHostTemplate);
        }
        apiHostTemplateList.setItems(apiHostTemplates);
        return apiHostTemplateList;
    }

    private List<Resource> resources(VolumeSetAttributes volumeSetAttributes) {
        List<Resource> resources = new ArrayList<>();
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AWS_VOLUMESET);
        resource.setAttributes(new Json(volumeSetAttributes));
        resources.add(resource);
        return resources;
    }

    private InstanceGroup createInstanceGroup() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(createDeletedInstanceMetadata(), createRunningInstanceMetadata()));
        return instanceGroup;
    }

    private void mockDecommission(Pair<BigDecimal, ExtendedPollingResult> resultPair, Pair<BigDecimal, ExtendedPollingResult>... resultPairs)
            throws ApiException {
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(eq(v51Client))).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.hostsDecommissionCommand(any())).thenReturn(getApiCommand(resultPair.getLeft()),
                Arrays.stream(resultPairs).map(resultPairItem -> getApiCommand(resultPairItem.getLeft())).toArray(ApiCommand[]::new));
        when(pollingServiceProvider.startPollingCmHostDecommissioning(any(), eq(v51Client), any(), anyBoolean(), anyInt())).thenReturn(resultPair.getRight(),
                Arrays.stream(resultPairs).map(resultPairItem -> resultPairItem.getRight()).toArray(ExtendedPollingResult[]::new));
    }

    private void mockAbortCommand(BigDecimal commandId, BigDecimal... commandIds) throws ApiException {
        lenient().when(clouderaManagerApiFactory.getCommandsResourceApi(eq(v51Client))).thenReturn(commandsResourceApi);
        lenient().when(commandsResourceApi.abortCommand(any())).thenReturn(getApiCommand(commandId),
                Arrays.stream(commandIds).map(commandIdItem -> getApiCommand(commandIdItem)).toArray(ApiCommand[]::new));
    }

    private void mockListClusterHosts() throws ApiException {
        ApiHostList apiHostRefList = new ApiHostList();
        apiHostRefList.setItems(List.of(createApiHostRef(DELETED_INSTANCE_FQDN), createApiHostRef(RUNNING_INSTANCE_FQDN)));
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenReturn(apiHostRefList);
        when(clouderaManagerApiFactory.getHostsResourceApi(v51Client)).thenReturn(hostsResourceApi);
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
        stack.setStackVersion("7.2.18");
        stack.setMultiAz(multiAz);
        return stack;
    }
}
