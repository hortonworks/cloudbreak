package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.InstanceStorageInfo;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@ExtendWith(MockitoExtension.class)
public class CoreVerticalScaleServiceTest {

    private static final String YARN_LOCAL_DIR = "yarn_nodemanager_local_dirs";

    private static final String YARN_LOG_DIR = "yarn_nodemanager_log_dirs";

    private static final String IMPALA_SCRATCH_DIR = "scratch_dirs";

    private static final String IMPALA_DATACACHE_DIR = "datacache_dirs";

    @Mock
    private ClusterService clusterService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private TemplateService templateService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @InjectMocks
    private CoreVerticalScaleService underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterModificationService clusterModificationService;

    @BeforeEach
    void setUp() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stackDto);
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
    }

    @Test
    public void testStopClouderaManagerServicesAndUpdateClusterConfigsSuccess() throws Exception {
        Map<String, String> serviceStatuses = new HashMap<>();
        serviceStatuses.put("yarn", "STOPPED");
        doReturn(serviceStatuses).when(clusterModificationService).fetchServiceStatuses();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        doReturn(1L).when(stackDto).getId();

        underTest.stopClouderaManagerServicesAndUpdateClusterConfigs(stackDto, hostTemplateServiceComponents,
                new ArrayList<>());

        verify(clusterModificationService, times(1)).stopClouderaManagerService(eq("yarn"));
        verify(clusterModificationService, times(1)).fetchServiceStatuses();
        verify(clusterHostServiceRunner, times(1)).updateClusterConfigs(eq(stackDto), eq(true));
    }

    @Test
    public void testStopClouderaManagerServicesAndUpdateClusterConfigsException() throws Exception {
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        doThrow(new Exception("Test")).when(clusterModificationService).stopClouderaManagerService(eq("yarn"));
        doReturn(1L).when(stackDto).getId();

        Exception exception = assertThrows(Exception.class, () -> underTest.stopClouderaManagerServicesAndUpdateClusterConfigs(stackDto,
                hostTemplateServiceComponents, new ArrayList<>()));

        assertEquals("Unable to stop CM services for service yarn, in stack 1: Test", exception.getMessage());
    }

    @Test
    public void testUpdateClouderaManagerConfigsForComputeGroupAndStartServices() throws Exception {
        Map<String, String> serviceStatuses = new HashMap<>();
        serviceStatuses.put("yarn", "STARTED");
        doReturn(serviceStatuses).when(clusterModificationService).fetchServiceStatuses();

        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);

        Map<String, String> config = new HashMap<>();
        config.put(YARN_LOCAL_DIR, "/hadoopfs/ephfs1/nodemanager");
        config.put(YARN_LOG_DIR, "/hadoopfs/ephfs1/nodemanager/log");

        List<InstanceStorageInfo> instanceStorageInfo = List.of(new InstanceStorageInfo(true, 1, 100));
        List<String> roleGroupNames = List.of("YARN");

        underTest.updateClouderaManagerConfigsForComputeGroupAndStartServices(stackDto, hostTemplateServiceComponents, instanceStorageInfo,
                roleGroupNames);

        verify(clusterModificationService, times(1)).updateServiceConfig(eq("yarn"), eq(config), eq(roleGroupNames));
        verify(clusterModificationService, times(1)).startClouderaManagerService("yarn");
        verify(clusterModificationService, times(1)).fetchServiceStatuses();
    }

    @Test
    public void testUpdateClouderaManagerConfigsForComputeException() throws Exception {
        doReturn(1L).when(stackDto).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);

        Map<String, String> config = new HashMap<>();
        config.put(YARN_LOCAL_DIR, "/hadoopfs/ephfs1/nodemanager");
        config.put(YARN_LOG_DIR, "/hadoopfs/ephfs1/nodemanager/log");

        doThrow(new Exception("Test")).when(clusterModificationService).startClouderaManagerService("yarn");

        List<InstanceStorageInfo> instanceStorageInfo = List.of(new InstanceStorageInfo(true, 1, 100));
        List<String> roleGroupNames = List.of("YARN");

        Exception exception = assertThrows(Exception.class, () -> underTest.updateClouderaManagerConfigsForComputeGroupAndStartServices(stackDto,
                hostTemplateServiceComponents, instanceStorageInfo, roleGroupNames));

        verify(clusterModificationService, times(1)).updateServiceConfig(eq("yarn"), eq(config), eq(roleGroupNames));
        assertEquals("Unable to start CM services for service yarn, in stack 1: Test", exception.getMessage());
    }
}
