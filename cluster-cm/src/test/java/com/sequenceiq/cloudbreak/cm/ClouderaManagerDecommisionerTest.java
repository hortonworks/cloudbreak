package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
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
        Set<HostGroup> hostGroups = createTestHostGroups(1, 3);
        Multimap<Long, HostMetadata> hostGroupWithInstances = createTestHostGroupWithInstances();
        ApiHostTemplateList hostTemplates = createEmptyHostTemplates();
        Mockito.when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        Mockito.when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        Mockito.when(resourceAttributeUtil.getTypedAttributes(Mockito.any(Resource.class), Mockito.any(Class.class)))
                .thenReturn(Optional.of(volumeSetAttributes));
        // WHEN
        underTest.verifyNodesAreRemovable(stack, hostGroupWithInstances, hostGroups, new ApiClient());
        // THEN no exception
    }

    @Test
    public void testVerifyNodesAreRemovableWithRepairAndNotEnoughNode() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "fstab", List.of(), 50, "vt");
        Stack stack = createTestStack(volumeSetAttributes);
        Set<HostGroup> hostGroups = createTestHostGroups(1, 1);
        Multimap<Long, HostMetadata> hostGroupWithInstances = createTestHostGroupWithInstances();
        ApiHostTemplateList hostTemplates = createEmptyHostTemplates();
        Mockito.when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        Mockito.when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        Mockito.when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        // WHEN
        underTest.verifyNodesAreRemovable(stack, hostGroupWithInstances, hostGroups, new ApiClient());
        // THEN no exception
    }

    @Test
    public void testVerifyNodesAreRemovableWithoutRepairAndNotEnoughNode() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", true, "fstab", List.of(), 50, "vt");
        Stack stack = createTestStack(volumeSetAttributes);
        Set<HostGroup> hostGroups = createTestHostGroups(1, 1);
        Multimap<Long, HostMetadata> hostGroupWithInstances = createTestHostGroupWithInstances();
        ApiHostTemplateList hostTemplates = createEmptyHostTemplates();
        Mockito.when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        Mockito.when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        Mockito.when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        // WHEN
        assertThrows(NotEnoughNodeException.class,
                () -> underTest.verifyNodesAreRemovable(stack, hostGroupWithInstances, hostGroups, new ApiClient()));
        // THEN the above exception should have thrown
    }

    @Test
    public void testVerifyNodesAreRemovableWithoutRepairAndReplication() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", true, "fstab", List.of(), 50, "vt");
        Stack stack = createTestStack(volumeSetAttributes);
        Set<HostGroup> hostGroups = createTestHostGroups(1, 6);
        Multimap<Long, HostMetadata> hostGroupWithInstances = createTestHostGroupWithInstances();
        ApiServiceConfig apiServiceConfig = createApiServiceConfigWithReplication("3", true);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes(hostGroups.stream().findFirst().get().getName());
        Mockito.when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        Mockito.when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        Mockito.when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        Mockito.when(clouderaManagerApiFactory.getServicesResourceApi(Mockito.any(ApiClient.class))).thenReturn(servicesResourceApi);
        Mockito.when(servicesResourceApi.readServiceConfig(stack.getName(), "hdfs", "full")).thenReturn(apiServiceConfig);
        // WHEN
        underTest.verifyNodesAreRemovable(stack, hostGroupWithInstances, hostGroups, new ApiClient());
        // THEN no exception
    }

    @Test
    public void testVerifyNodesAreRemovableWithRepairAndReplication() throws ApiException {
        // GIVEN
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", false, "fstab", List.of(), 50, "vt");
        Stack stack = createTestStack(volumeSetAttributes);
        Set<HostGroup> hostGroups = createTestHostGroups(1, 3);
        Multimap<Long, HostMetadata> hostGroupWithInstances = createTestHostGroupWithInstances();
        ApiServiceConfig apiServiceConfig = createApiServiceConfigWithReplication("3", true);
        ApiHostTemplateList hostTemplates = createHostTemplatesWithDataNodes(hostGroups.stream().findFirst().get().getName());
        Mockito.when(clouderaManagerApiFactory.getHostTemplatesResourceApi(Mockito.any(ApiClient.class))).thenReturn(hostTemplatesResourceApi);
        Mockito.when(hostTemplatesResourceApi.readHostTemplates(stack.getName())).thenReturn(hostTemplates);
        Mockito.when(resourceAttributeUtil.getTypedAttributes(stack.getDiskResources().get(0), VolumeSetAttributes.class))
                .thenReturn(Optional.of(volumeSetAttributes));
        Mockito.when(clouderaManagerApiFactory.getServicesResourceApi(Mockito.any(ApiClient.class))).thenReturn(servicesResourceApi);
        Mockito.when(servicesResourceApi.readServiceConfig(stack.getName(), "hdfs", "full")).thenReturn(apiServiceConfig);
        // WHEN
        underTest.verifyNodesAreRemovable(stack, hostGroupWithInstances, hostGroups, new ApiClient());
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

    private  HostGroup createTestHostGroup(Long id, String name, int hostCount) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(name);
        hostGroup.setId(id);
        Set<HostMetadata> hostMetadataSet = new HashSet<>();
        for (long i = 0; i < hostCount; i++) {
            hostMetadataSet.add(createHostMetadata(i));
        }
        hostGroup.setHostMetadata(hostMetadataSet);
        return hostGroup;
    }

    private HostMetadata createHostMetadata(Long id) {
        HostMetadata hm = new HostMetadata();
        hm.setId(id);
        return hm;
    }

    private Stack createTestStack(VolumeSetAttributes volumeSetAttributes) {
        Stack stack = new Stack();
        stack.setName("stack");
        stack.setPlatformVariant(CloudConstants.AWS);
        stack.setResources(resources(volumeSetAttributes));
        return stack;
    }

    private Multimap<Long, HostMetadata> createTestHostGroupWithInstances() {
        Multimap<Long, HostMetadata> hostGroupWithInstances = ArrayListMultimap.create();
        for (long i = 0; i < 3; i++) {
            for (long j = 0; j < 3; j++) {
                HostMetadata hm = new HostMetadata();
                hm.setHostName("host" + i + j);
                hostGroupWithInstances.put(i, hm);
            }
        }
        return hostGroupWithInstances;
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
