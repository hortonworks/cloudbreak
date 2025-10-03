package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class ConfigUpdateUtilServiceTest {

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private List<CmHostGroupRoleConfigProvider> cmHostGroupRoleConfigProviders;

    @Mock
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @InjectMocks
    private ConfigUpdateUtilService underTest;

    @Mock
    private StackDto stack;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterModificationService clusterModificationService;

    @BeforeEach
    void setUp() {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stack);
    }

    @Test
    void testStopClouderaManagerServicesSuccess() throws Exception {
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        doReturn(1L).when(stack).getId();

        underTest.stopClouderaManagerServices(stack, hostTemplateServiceComponents);

        verify(clusterModificationService, times(1)).stopClouderaManagerService("yarn", true);
    }

    @Test
    void testStopClouderaManagerServicesAndUpdateClusterConfigsException() throws Exception {
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        doThrow(new Exception("Test")).when(clusterModificationService).stopClouderaManagerService("yarn", true);
        doReturn(1L).when(stack).getId();

        Exception exception = assertThrows(Exception.class, () -> underTest.stopClouderaManagerServices(stack, hostTemplateServiceComponents));

        assertEquals("Unable to stop CM services for service yarn, in stack 1: Test", exception.getMessage());
    }

    @Test
    void testUpdateCMConfigsForComputeAndStartServicesSuccess() throws Exception {
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);

        Map<String, String> config = new HashMap<>();
        config.put("yarn_nodemanager_local_dirs", "/hadoopfs/fs1/nodemanager");
        config.put("yarn_nodemanager_log_dirs", "/hadoopfs/fs1/nodemanager/log");

        List<String> roleGroupNames = List.of("YARN");

        // Setup mocks
        CmHostGroupRoleConfigProvider yarnProvider = mock(CmHostGroupRoleConfigProvider.class);
        when(yarnProvider.getServiceType()).thenReturn("YARN");
        when(yarnProvider.getConfigAfterAddingVolumes(any(HostgroupView.class), any(), any())).thenReturn(config);

        List<CmHostGroupRoleConfigProvider> providers = new ArrayList<>();
        providers.add(yarnProvider);
        when(cmHostGroupRoleConfigProviders.stream()).thenReturn(providers.stream());

        Resource resource = mock(Resource.class);
        when(resource.getInstanceGroup()).thenReturn("test");
        when(resource.getResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        when(volumeSetAttributes.getVolumes()).thenReturn(List.of());
        when(resourceAttributeUtil.getTypedAttributes(any(), eq(VolumeSetAttributes.class))).thenReturn(Optional.of(volumeSetAttributes));
        when(stack.getResources()).thenReturn(Set.of(resource));

        // Mock InstanceGroupDto and its dependencies
        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        Template template = mock(Template.class);
        InstanceMetadataView instanceMetadata = mock(InstanceMetadataView.class);

        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroup);
        when(instanceGroup.getGroupName()).thenReturn("test");
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.CORE);

        when(template.getVolumeTemplates()).thenReturn(Set.of());
        when(template.getTemporaryStorage()).thenReturn(null);
        when(template.getInstanceStorageCount()).thenReturn(0);
        when(template.getInstanceStorageSize()).thenReturn(null);

        when(instanceMetadata.getDiscoveryFQDN()).thenReturn("host1.example.com");
        when(instanceGroupDto.getNotDeletedAndNotZombieInstanceMetaData()).thenReturn(List.of(instanceMetadata));

        when(stack.getInstanceGroupDtos()).thenReturn(List.of(instanceGroupDto));

        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        when(stackToTemplatePreparationObjectConverter.convert(stack)).thenReturn(templatePreparationObject);

        underTest.updateCMConfigsForComputeAndStartServices(stack, hostTemplateServiceComponents, roleGroupNames, "test");

        verify(clusterModificationService, times(1)).updateServiceConfig(eq("yarn"), eq(config), anyList());
        verify(clusterModificationService, times(1)).startClouderaManagerService("yarn", true);
    }
}
