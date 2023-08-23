package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@ExtendWith(MockitoExtension.class)
public class ConfigUpdateUtilServiceTest {

    private static final String YARN_LOCAL_DIR = "yarn_nodemanager_local_dirs";

    private static final String YARN_LOG_DIR = "yarn_nodemanager_log_dirs";

    private static final String IMPALA_SCRATCH_DIR = "scratch_dirs";

    private static final String IMPALA_DATACACHE_DIR = "datacache_dirs";

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

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
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
    }

    @Test
    public void testStopClouderaManagerServicesSuccess() throws Exception {
        Map<String, String> serviceStatuses = new HashMap<>();
        serviceStatuses.put("yarn", "STOPPED");
        doReturn(serviceStatuses).when(clusterModificationService).fetchServiceStatuses();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        doReturn(1L).when(stack).getId();

        underTest.stopClouderaManagerServices(stack, hostTemplateServiceComponents);

        verify(clusterModificationService, times(1)).stopClouderaManagerService(eq("yarn"));
        verify(clusterModificationService, times(1)).fetchServiceStatuses();
    }

    @Test
    public void testStopClouderaManagerServicesAndUpdateClusterConfigsException() throws Exception {
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        doThrow(new Exception("Test")).when(clusterModificationService).stopClouderaManagerService(eq("yarn"));
        doReturn(1L).when(stack).getId();

        Exception exception = assertThrows(Exception.class, () -> underTest.stopClouderaManagerServices(stack, hostTemplateServiceComponents));

        assertEquals("Unable to stop CM services for service yarn, in stack 1: Test", exception.getMessage());
    }

    @Test
    public void testUpdateCMConfigsForComputeAndStartServicesSuccess() throws Exception {
        Map<String, String> serviceStatuses = new HashMap<>();
        serviceStatuses.put("yarn", "STARTED");
        doReturn(serviceStatuses).when(clusterModificationService).fetchServiceStatuses();

        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);

        Map<String, String> config = new HashMap<>();
        config.put(YARN_LOCAL_DIR, "/hadoopfs/ephfs1/nodemanager");
        config.put(YARN_LOG_DIR, "/hadoopfs/ephfs1/nodemanager/log");

        List<String> roleGroupNames = List.of("YARN");

        underTest.updateCMConfigsForComputeAndStartServices(stack, hostTemplateServiceComponents, 1, 2, roleGroupNames,
                TemporaryStorage.EPHEMERAL_VOLUMES);

        verify(clusterModificationService, times(1)).updateServiceConfig(eq("yarn"), eq(config), eq(roleGroupNames));
        verify(clusterModificationService, times(1)).startClouderaManagerService("yarn");
        verify(clusterModificationService, times(1)).fetchServiceStatuses();
    }

    @Test
    public void testUpdateCMConfigsForComputeAndStartImpalaServicesSuccess() throws Exception {
        Map<String, String> serviceStatuses = new HashMap<>();
        serviceStatuses.put("impala", "STARTED");
        doReturn(serviceStatuses).when(clusterModificationService).fetchServiceStatuses();

        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("impala", "impala");
        hostTemplateServiceComponents.add(serviceComponent);

        Map<String, String> config = new HashMap<>();
        config.put(IMPALA_SCRATCH_DIR, "/hadoopfs/ephfs1/impala/scratch,/hadoopfs/ephfs2/impala/scratch");
        config.put(IMPALA_DATACACHE_DIR, "/hadoopfs/ephfs1/impala/datacache,/hadoopfs/ephfs2/impala/datacache");

        List<String> roleGroupNames = List.of("IMPALAD");

        underTest.updateCMConfigsForComputeAndStartServices(stack, hostTemplateServiceComponents, 2, 2, roleGroupNames,
                TemporaryStorage.EPHEMERAL_VOLUMES);

        verify(clusterModificationService, times(1)).updateServiceConfig(eq("impala"), eq(config), eq(roleGroupNames));
        verify(clusterModificationService, times(1)).startClouderaManagerService("impala");
        verify(clusterModificationService, times(1)).fetchServiceStatuses();
    }

    @Test
    public void testUpdateCMConfigsForComputeAndStartServicesException() throws Exception {
        doReturn(1L).when(stack).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);

        Map<String, String> config = new HashMap<>();
        config.put(YARN_LOCAL_DIR, "/hadoopfs/ephfs1/nodemanager");
        config.put(YARN_LOG_DIR, "/hadoopfs/ephfs1/nodemanager/log");

        doThrow(new Exception("Test")).when(clusterModificationService).startClouderaManagerService("yarn");

        List<String> roleGroupNames = List.of("YARN");

        Exception exception = assertThrows(Exception.class, () -> underTest.updateCMConfigsForComputeAndStartServices(stack,
                hostTemplateServiceComponents, 1, 2, roleGroupNames, TemporaryStorage.EPHEMERAL_VOLUMES));

        verify(clusterModificationService, times(1)).updateServiceConfig(eq("yarn"), eq(config), eq(roleGroupNames));
        assertEquals("Unable to start CM services for service yarn, in stack 1: Test", exception.getMessage());
    }
}
