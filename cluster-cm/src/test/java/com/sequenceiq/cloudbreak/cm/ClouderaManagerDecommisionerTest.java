package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiHostTemplate;
import com.cloudera.api.swagger.model.ApiHostTemplateList;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupRef;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerDecommisionerTest {
    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private HostTemplatesResourceApi hostTemplatesResourceApi;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @InjectMocks
    private ClouderaManagerDecomissioner underTest;

    @Test
    public void testVerifyNodesAreRemovable() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "fstab", List.of(), 50, "vt");
        Stack stack = createTestStack(volumeSetAttributes);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        Set<HostGroup> hostGroups = createTestHostGroups(1, 6);
        cluster.setHostGroups(hostGroups);
        ApiHostTemplateList hostTemplates = createEmptyHostTemplates();
        Mockito.when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        Mockito.when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        Mockito.when(resourceAttributeUtil.getTypedAttributes(Mockito.any(Resource.class), Mockito.any(Class.class)))
                .thenReturn(Optional.of(volumeSetAttributes));
        // WHEN
        HostGroup firstHostGroup = hostGroups.iterator().next();
        underTest.verifyNodesAreRemovable(stack, firstHostGroup.getInstanceGroup().getInstanceMetaDataSet(), new ApiClient());
        // THEN no exception
    }

    @Test
    public void testVerifyNodesAreRemovableWithoutRepairAndNotEnoughNode() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", true, "fstab", List.of(), 50, "vt");
        Stack stack = createTestStack(volumeSetAttributes);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        Set<HostGroup> hostGroups = createTestHostGroups(2, 2);
        cluster.setHostGroups(hostGroups);
        ApiHostTemplateList hostTemplates = createEmptyHostTemplates();
        Mockito.when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        Mockito.when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        Mockito.when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        // WHEN
        HostGroup firstHostGroup = hostGroups.iterator().next();
        Set<InstanceMetaData> removableInstances = firstHostGroup.getInstanceGroup().getInstanceMetaDataSet();
        InstanceMetaData additionalInstanceMetaData = new InstanceMetaData();
        additionalInstanceMetaData.setInstanceGroup(firstHostGroup.getInstanceGroup());
        removableInstances.add(additionalInstanceMetaData);
        // WHEN
        assertThrows(NotEnoughNodeException.class,
                () -> underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient()));
        // THEN the above exception should have thrown
    }

    @Test
    public void testVerifyNodesAreRemovableWithoutRepairWithReplicationAndTooMuchRemovableNodes() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", true, "fstab", List.of(), 50, "vt");
        Stack stack = createTestStack(volumeSetAttributes);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        Set<HostGroup> hostGroups = createTestHostGroups(2, 5);
        cluster.setHostGroups(hostGroups);
        ApiServiceConfig apiServiceConfig = createApiServiceConfigWithReplication("3", true);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes(hostGroups.stream().findFirst().get().getName());
        Mockito.when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        Mockito.when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        Mockito.when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        Mockito.when(clouderaManagerApiFactory.getServicesResourceApi(Mockito.any(ApiClient.class))).thenReturn(servicesResourceApi);
        Mockito.when(servicesResourceApi.readServiceConfig(stack.getName(), "hdfs", "full")).thenReturn(apiServiceConfig);
        // WHEN
        HostGroup firstHostGroup = hostGroups.iterator().next();
        Set<InstanceMetaData> firstHostGroupInstances = firstHostGroup.getInstanceGroup().getInstanceMetaDataSet();
        Set<InstanceMetaData> removableInstances = firstHostGroupInstances.stream().limit(5).collect(Collectors.toSet());
        assertThrows(NotEnoughNodeException.class,
                () -> underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient()));
        // THEN no exception
    }

    @Test
    public void testVerifyNodesAreRemovableWithoutRepairAndReplication() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", true, "fstab", List.of(), 50, "vt");
        Stack stack = createTestStack(volumeSetAttributes);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        Set<HostGroup> hostGroups = createTestHostGroups(2, 5);
        cluster.setHostGroups(hostGroups);
        ApiServiceConfig apiServiceConfig = createApiServiceConfigWithReplication("3", true);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes(hostGroups.stream().findFirst().get().getName());
        Mockito.when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        Mockito.when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        Mockito.when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        Mockito.when(clouderaManagerApiFactory.getServicesResourceApi(Mockito.any(ApiClient.class))).thenReturn(servicesResourceApi);
        Mockito.when(servicesResourceApi.readServiceConfig(stack.getName(), "hdfs", "full")).thenReturn(apiServiceConfig);
        // WHEN
        HostGroup firstHostGroup = hostGroups.iterator().next();
        Set<InstanceMetaData> firstHostGroupInstances = firstHostGroup.getInstanceGroup().getInstanceMetaDataSet();
        Set<InstanceMetaData> removableInstances = firstHostGroupInstances.stream().limit(2).collect(Collectors.toSet());
        underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient());
        // THEN no exception
    }

    @Test
    public void testVerifyNodesAreRemovableWithRepairAndReplication() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "fstab", List.of(), 50, "vt");
        Stack stack = createTestStack(volumeSetAttributes);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        Set<HostGroup> hostGroups = createTestHostGroups(2, 5);
        cluster.setHostGroups(hostGroups);
//        Multimap<Long, InstanceMetaData> hostGroupWithInstances = createTestHostGroupWithInstances();
        ApiServiceConfig apiServiceConfig = createApiServiceConfigWithReplication("3", true);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes(hostGroups.stream().findFirst().get().getName());
        Mockito.when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        Mockito.when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        Mockito.when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        Mockito.when(clouderaManagerApiFactory.getServicesResourceApi(Mockito.any(ApiClient.class))).thenReturn(servicesResourceApi);
        Mockito.when(servicesResourceApi.readServiceConfig(stack.getName(), "hdfs", "full")).thenReturn(apiServiceConfig);
        // WHEN
        HostGroup firstHostGroup = hostGroups.iterator().next();
        Set<InstanceMetaData> firstHostGroupInstances = firstHostGroup.getInstanceGroup().getInstanceMetaDataSet();
        Set<InstanceMetaData> removableInstances = firstHostGroupInstances.stream().limit(5).collect(Collectors.toSet());
        underTest.verifyNodesAreRemovable(stack, removableInstances, new ApiClient());
        // THEN no exception
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

    private Set<HostGroup> createTestHostGroups(int groupCount, int hostCount) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (long i = 0; i < groupCount; i++) {
            hostGroups.add(createTestHostGroup(i, "hg" + i, hostCount));
        }
        return hostGroups;
    }

    private HostGroup createTestHostGroup(Long id, String name, int hostCount) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(name);
        hostGroup.setId(id);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(name);
        Set<InstanceMetaData> instanceMetaDatas = new HashSet<>();
        for (long i = 0; i < hostCount; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setDiscoveryFQDN(name + "-host-" + i);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            instanceMetaDatas.add(instanceMetaData);
        }
        instanceGroup.setInstanceMetaData(instanceMetaDatas);
        hostGroup.setInstanceGroup(instanceGroup);
        return hostGroup;
    }

    private Stack createTestStack(VolumeSetAttributes volumeSetAttributes) {
        Stack stack = new Stack();
        stack.setName("stack");
        stack.setPlatformVariant(CloudConstants.AWS);
        stack.setResources(resources(volumeSetAttributes));
        return stack;
    }

    private ApiHostTemplateList createEmptyHostTemplates() {
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        apiHostTemplateList.setItems(List.of());
        return apiHostTemplateList;
    }

    private ApiHostTemplateList createHostTemplatesWithDataNodes(String name) {
        ApiHostTemplateList apiHostTemplateList = new ApiHostTemplateList();
        ApiHostTemplate apiHostTemplate = new ApiHostTemplate();
        apiHostTemplate.setName(name);
        ApiRoleConfigGroupRef roleConfigGroupRef = new ApiRoleConfigGroupRef();
        roleConfigGroupRef.setRoleConfigGroupName("_DATANODE_");
        apiHostTemplate.setRoleConfigGroupRefs(List.of(roleConfigGroupRef));
        apiHostTemplateList.setItems(List.of(apiHostTemplate));
        return apiHostTemplateList;
    }

    private Set<Resource> resources(VolumeSetAttributes volumeSetAttributes) {
        Set<Resource> resources = new HashSet<>();
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AWS_VOLUMESET);
        resource.setAttributes(new Json(volumeSetAttributes));
        resources.add(resource);
        return resources;
    }
}
