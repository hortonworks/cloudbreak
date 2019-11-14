package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterCommonServiceTest {

    @InjectMocks
    private ClusterCommonService underTest;

    @Before
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

    @Test(expected = NotFoundException.class)
    public void testIniFileGenerationWithoutAgents() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        cluster.setClusterManagerIp(null);
        stack.setCluster(cluster);

        // WHEN
        underTest.getHostNamesAsIniString(stack, "cloudbreak");
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

}
