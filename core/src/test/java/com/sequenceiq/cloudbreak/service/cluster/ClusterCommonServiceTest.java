package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
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

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testInventoryFileGeneration() {
        // GIVEN

        Stack stack = new Stack();
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        cluster.setClusterManagerIp("gatewayIP");
        stack.setCluster(cluster);

        when(instanceMetaDataService.getAllInstanceMetadataByStackId(anyLong())).thenReturn(generateInstanceMetadata());

        // WHEN
        String result = underTest.getHostNamesAsIniString(stack, "cloudbreak");
        // THEN

        assertEquals("[cluster]\n" +
                "name=cl1\n" +
                "\n" +
                "[server]\n" +
                "gatewayIP\n" +
                "\n" +
                "[worker]\n" +
                "worker-1\n" +
                "pub-worker-2\n" +
                "\n" +
                "[master]\n" +
                "m1\n" +
                "\n" +
                "[agent]\n" +
                "m1\n" +
                "worker-1\n" +
                "pub-worker-2\n" +
                "\n" +
                "[all:vars]\n" +
                "ansible_ssh_user=cloudbreak\n" +
                "ansible_ssh_common_args='-o StrictHostKeyChecking=no'\n" +
                "ansible_become=yes\n", result);
    }

    @Test
    public void testInventoryFileGenerationWithoutAgents() {
        Stack stack = new Stack();
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        cluster.setClusterManagerIp(null);
        stack.setCluster(cluster);

        // WHEN
        when(instanceMetaDataService.getAllInstanceMetadataByStackId(anyLong())).thenReturn(Collections.emptySet());
        assertThrows(NotFoundException.class, () -> underTest.getHostNamesAsIniString(stack, "cloudbreak"));
    }

    private Set<InstanceMetaData> generateInstanceMetadata() {
        //LinkedHashSet required to keep the order, and have a reliable test run
        Set<InstanceMetaData> instanceMetaData = new LinkedHashSet<>();
        InstanceGroup master = new InstanceGroup();
        master.setGroupName("master");
        InstanceGroup worker = new InstanceGroup();
        worker.setGroupName("worker");

        InstanceMetaData master1 = new InstanceMetaData();
        master1.setPublicIp("m1");
        master1.setInstanceGroup(master);
        master1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetaData.add(master1);

        master.setInstanceMetaData(new HashSet<>(Arrays.asList(master1)));

        InstanceMetaData worker1 = new InstanceMetaData();
        worker1.setPrivateIp("worker-1");
        worker1.setInstanceGroup(worker);
        worker1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetaData.add(worker1);

        InstanceMetaData worker2 = new InstanceMetaData();
        // If we have both then we need public ip
        worker2.setPublicIp("pub-worker-2");
        worker2.setPrivateIp("worker-2");
        worker2.setInstanceGroup(worker);
        worker2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);

        worker.setInstanceMetaData(new HashSet<>(Arrays.asList(worker1, worker2)));
        instanceMetaData.add(worker2);

        return instanceMetaData;
    }

    @Test
    public void testUpdateNodeCountWhenCheckCallEnvironmentCheck() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus<>(stack, AVAILABLE));

        when(stackService.getByCrn("crn")).thenReturn(stack);
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        doThrow(RuntimeException.class).when(environmentService).checkEnvironmentStatus(stack, EnvironmentStatus.upscalable());

        UpdateClusterV4Request update = new UpdateClusterV4Request();
        update.setHostGroupAdjustment(new HostGroupAdjustmentV4Request());
        assertThrows(RuntimeException.class, () -> underTest.put("crn", update));
        verify(environmentService).checkEnvironmentStatus(stack, EnvironmentStatus.upscalable());
    }
}
