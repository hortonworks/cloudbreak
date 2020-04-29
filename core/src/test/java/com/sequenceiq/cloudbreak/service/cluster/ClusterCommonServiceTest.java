package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;

@ExtendWith(MockitoExtension.class)
public class ClusterCommonServiceTest {

    @InjectMocks
    private ClusterCommonService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private EnvironmentService environmentService;

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testIniFileGeneration() {
        // GIVEN

        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        cluster.setClusterManagerIp("gatewayIP");
        stack.setCluster(cluster);
        stack.setInstanceGroups(generateInstanceMetadata());

        // WHEN
        String result = underTest.getHostNamesAsIniString(stack, "cloudbreak");
        // THEN
        assertTrue(result.contains("[server]\ngatewayIP\n\n"));
        assertTrue(result.contains("[cluster]\nname=cl1\n\n"));
        assertTrue(result.contains("[master]\nh1\n"));
        assertTrue(result.contains("[agent]\n"));
        assertTrue(result.contains("[all:vars]\nansible_ssh_user=cloudbreak\n"));
    }

    @Test
    public void testIniFileGenerationWithoutAgents() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        cluster.setClusterManagerIp(null);
        stack.setCluster(cluster);

        // WHEN
        assertThrows(NotFoundException.class, () -> underTest.getHostNamesAsIniString(stack, "cloudbreak"));
    }

    private Set<InstanceGroup> generateInstanceMetadata() {
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        InstanceGroup master = new InstanceGroup();
        master.setGroupName("master");
        InstanceGroup worker = new InstanceGroup();
        worker.setGroupName("worker");

        InstanceMetaData master1 = new InstanceMetaData();
        master1.setDiscoveryFQDN("h1");
        master1.setInstanceGroup(master);
        master1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);

        master.setInstanceMetaData(new HashSet<>(Arrays.asList(master1)));

        InstanceMetaData worker1 = new InstanceMetaData();
        worker1.setDiscoveryFQDN("worker-1");
        worker1.setInstanceGroup(worker);
        worker1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);

        InstanceMetaData worker2 = new InstanceMetaData();
        worker2.setDiscoveryFQDN("worker-2");
        worker2.setInstanceGroup(worker);
        worker2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);

        worker.setInstanceMetaData(new HashSet<>(Arrays.asList(worker1, worker2)));

        instanceGroups.add(master);
        instanceGroups.add(worker);
        return instanceGroups;
    }

    @Test
    public void testUpdateNodeCountWhenCheckCallEnvironmentCheck() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));

        when(stackService.getByCrn("crn")).thenReturn(stack);

        doThrow(RuntimeException.class).when(environmentService).checkEnvironmentStatus(stack, EnvironmentStatus.upscalable());

        UpdateClusterV4Request update = new UpdateClusterV4Request();
        update.setHostGroupAdjustment(new HostGroupAdjustmentV4Request());
        assertThrows(RuntimeException.class, () -> underTest.put("crn", update));
        verify(environmentService).checkEnvironmentStatus(stack, EnvironmentStatus.upscalable());
    }
}
