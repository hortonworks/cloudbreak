package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class ConfigUpdateUtilServiceTest {

    private static final String YARN_LOCAL_DIR = "yarn_nodemanager_local_dirs";

    private static final String YARN_LOG_DIR = "yarn_nodemanager_log_dirs";

    private static final String IMPALA_SCRATCH_DIR = "scratch_dirs";

    private static final String IMPALA_DATACACHE_DIR = "datacache_dirs";

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

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
        Map<String, String> serviceStatuses = new HashMap<>();
        serviceStatuses.put("yarn", "STOPPED");
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
        config.put(YARN_LOCAL_DIR, "/hadoopfs/ephfs1/nodemanager");
        config.put(YARN_LOG_DIR, "/hadoopfs/ephfs1/nodemanager/log");

        List<String> roleGroupNames = List.of("YARN");

        Resource resource = mock(Resource.class);
        doReturn("test").when(resource).getInstanceGroup();
        doReturn(ResourceType.AWS_VOLUMESET).when(resource).getResourceType();
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        doReturn("/hadoopfs/ephfs1 ").when(volumeSetAttributes).getFstab();
        doReturn(Optional.of(volumeSetAttributes)).when(resourceAttributeUtil).getTypedAttributes(any(), eq(VolumeSetAttributes.class));
        doReturn(Set.of(resource)).when(stack).getResources();

        underTest.updateCMConfigsForComputeAndStartServices(stack, hostTemplateServiceComponents, roleGroupNames, "test");

        verify(clusterModificationService, times(1)).updateServiceConfig(eq("yarn"), eq(config), eq(roleGroupNames));
        verify(clusterModificationService, times(1)).startClouderaManagerService("yarn", false);
    }

    @Test
    void testUpdateCMConfigsForComputeAndStartImpalaServicesSuccess() throws Exception {
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("impala", "impala");
        hostTemplateServiceComponents.add(serviceComponent);

        Map<String, String> config = new HashMap<>();
        config.put(IMPALA_SCRATCH_DIR, "/hadoopfs/ephfs1/impala/scratch,/hadoopfs/ephfs2/impala/scratch");
        config.put(IMPALA_DATACACHE_DIR, "/hadoopfs/ephfs1/impala/datacache,/hadoopfs/ephfs2/impala/datacache");

        List<String> roleGroupNames = List.of("IMPALAD");

        Resource resource = mock(Resource.class);
        doReturn("test").when(resource).getInstanceGroup();
        doReturn(ResourceType.AWS_VOLUMESET).when(resource).getResourceType();
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        doReturn("/hadoopfs/ephfs1 /hadoopfs/ephfs2 ").when(volumeSetAttributes).getFstab();
        doReturn(Optional.of(volumeSetAttributes)).when(resourceAttributeUtil).getTypedAttributes(any(), eq(VolumeSetAttributes.class));
        doReturn(Set.of(resource)).when(stack).getResources();

        underTest.updateCMConfigsForComputeAndStartServices(stack, hostTemplateServiceComponents, roleGroupNames, "test");

        verify(clusterModificationService, times(1)).updateServiceConfig(eq("impala"), eq(config), eq(roleGroupNames));
        verify(clusterModificationService, times(1)).startClouderaManagerService("impala", false);
    }

    @Test
    void testUpdateCMConfigsForComputeAndStartServicesException() throws Exception {
        doReturn(1L).when(stack).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);

        Map<String, String> config = new HashMap<>();
        config.put(YARN_LOCAL_DIR, "/hadoopfs/ephfs1/nodemanager");
        config.put(YARN_LOG_DIR, "/hadoopfs/ephfs1/nodemanager/log");

        Resource resource = mock(Resource.class);
        doReturn("test").when(resource).getInstanceGroup();
        doReturn(ResourceType.AWS_VOLUMESET).when(resource).getResourceType();
        doReturn(Set.of(resource)).when(stack).getResources();

        doThrow(new CloudbreakServiceException("Test")).when(resourceAttributeUtil).getTypedAttributes(any(), eq(VolumeSetAttributes.class));

        List<String> roleGroupNames = List.of("YARN");

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.updateCMConfigsForComputeAndStartServices(stack,
                hostTemplateServiceComponents, roleGroupNames, "test"));

        verify(clusterModificationService, times(0)).updateServiceConfig(eq("yarn"), eq(config), eq(roleGroupNames));
        assertEquals("Unable to update and start CM services for service yarn, in stack 1: Test", exception.getMessage());
    }
}
