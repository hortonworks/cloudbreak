package com.sequenceiq.cloudbreak.service.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

@ExtendWith(MockitoExtension.class)
class ComponentLocatorServiceTest {

    private static final long CLUSTER_ID = 1L;

    private static final String MASTER_1 = "master1";

    private static final String MASTER_2 = "master2";

    private static final String MASTER_3 = "master3";

    private static final String EXECUTOR = "executor";

    private static final String COORDINATOR = "coordinator";

    private static final String MASTER1_FQDN = "master1.fqdn";

    private static final String MASTER2_FQDN = "master2.fqdn";

    private static final String MASTER3_FQDN = "master3.fqdn";

    private static final String NAMENODE = "NAMENODE";

    private static final String KUDU_MASTER = "KUDU_MASTER";

    private static final String SPARK_YARN_HISTORY_SERVER = "SPARK_YARN_HISTORY_SERVER";

    private static final List<String> EXECUTOR_FQDNS = List.of("executor.fqdn1", "executor.fqdn2", "executor.fqdn3");

    private static final List<String> COORDINATOR_FQDNS = List.of("coordinator.fqdn1", "coordinator.fqdn2", "coordinator.fqdn3");

    private static final List<String> COMPONENT_NAMES = List.of("CM-API", "KUDU_MASTER", "CM-UI", "IMPALA_DEBUG_UI",
            "SPARK_YARN_HISTORY_SERVER", "JOBHISTORY", "IMPALAD", "HUE_LOAD_BALANCER", "NAMENODE", "RESOURCEMANAGER");

    private static final Set<String> MASTER1_COMPONENTS
            = new LinkedHashSet<>(List.of("KUDU_MASTER", "SECONDARYNAMENODE", NAMENODE, "BALANCER"));

    private static final Set<String> MASTER2_COMPONENTS
            = new LinkedHashSet<>(List.of(KUDU_MASTER, "STATESTORE", "HUE_LOAD_BALANCER", "HUE_SERVER", "CATALOGSERVER"));

    private static final Set<String> MASTER3_COMPONENTS
            = new LinkedHashSet<>(List.of("KUDU_MASTER", SPARK_YARN_HISTORY_SERVER, "JOBHISTORY", "RESOURCEMANAGER"));

    private static final Set<String> EXECUTOR_COMPONENTS
            = new LinkedHashSet<>(List.of("NODEMANAGER", "GATEWAY", "KUDU_TSERVER", "DATANODE"));

    private static final Set<String> COORDINATOR_COMPONENTS
            = new LinkedHashSet<>(List.of("NODEMANAGER", "GATEWAY", "KUDU_TSERVER", "DATANODE"));

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private BlueprintTextProcessor blueprintTextProcessor;

    @InjectMocks
    private ComponentLocatorService underTest;

    @Test
    void testGetComponentLocation() {

        HostGroup master1 = new HostGroup();
        master1.setName(MASTER_1);
        InstanceGroup ig1 = new InstanceGroup();
        InstanceMetaData imd1 = new InstanceMetaData();
        imd1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd1.setDiscoveryFQDN(MASTER1_FQDN);
        ig1.setInstanceMetaData(Set.of(imd1));
        master1.setInstanceGroup(ig1);

        HostGroup master2 = new HostGroup();
        master2.setName(MASTER_2);
        InstanceGroup ig2 = new InstanceGroup();
        InstanceMetaData imd2 = new InstanceMetaData();
        imd2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd2.setDiscoveryFQDN(MASTER2_FQDN);
        ig2.setInstanceMetaData(Set.of(imd2));
        master2.setInstanceGroup(ig2);

        HostGroup master3 = new HostGroup();
        master3.setName(MASTER_3);
        InstanceGroup ig3 = new InstanceGroup();
        InstanceMetaData imd3 = new InstanceMetaData();
        imd3.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd3.setDiscoveryFQDN(MASTER3_FQDN);
        ig3.setInstanceMetaData(Set.of(imd3));
        master3.setInstanceGroup(ig3);

        HostGroup executor = new HostGroup();
        executor.setName(EXECUTOR);
        InstanceGroup ig4 = new InstanceGroup();
        Set<InstanceMetaData> imd4 = EXECUTOR_FQDNS.stream().map(fqdn -> {
            InstanceMetaData imd = new InstanceMetaData();
            imd.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            imd.setDiscoveryFQDN(fqdn);
            return imd;
        }).collect(Collectors.toSet());
        ig4.setInstanceMetaData(imd4);
        executor.setInstanceGroup(ig4);

        HostGroup coordinator = new HostGroup();
        coordinator.setName(COORDINATOR);
        InstanceGroup ig5 = new InstanceGroup();
        Set<InstanceMetaData> imd5 = COORDINATOR_FQDNS.stream().map(fqdn -> {
            InstanceMetaData imd = new InstanceMetaData();
            imd.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            imd.setDiscoveryFQDN(fqdn);
            return imd;
        }).collect(Collectors.toSet());
        ig5.setInstanceMetaData(imd5);
        coordinator.setInstanceGroup(ig5);

        Set<HostGroup> hostGroups = new LinkedHashSet<>(List.of(master1, master2, master3, executor, coordinator));

        when(blueprintTextProcessor.getComponentsInHostGroup(MASTER_1)).thenReturn(MASTER1_COMPONENTS);
        when(blueprintTextProcessor.getComponentsInHostGroup(MASTER_2)).thenReturn(MASTER2_COMPONENTS);
        when(blueprintTextProcessor.getComponentsInHostGroup(MASTER_3)).thenReturn(MASTER3_COMPONENTS);
        when(blueprintTextProcessor.getComponentsInHostGroup(EXECUTOR)).thenReturn(EXECUTOR_COMPONENTS);
        when(blueprintTextProcessor.getComponentsInHostGroup(COORDINATOR)).thenReturn(COORDINATOR_COMPONENTS);

        when(hostGroupService.getByCluster(CLUSTER_ID)).thenReturn(hostGroups);

        Map<String, List<String>> result = underTest.getComponentLocation(CLUSTER_ID, blueprintTextProcessor, COMPONENT_NAMES);

        assertEquals(1, result.get(NAMENODE).size());
        assertEquals(MASTER1_FQDN, result.get(NAMENODE).get(0));
        assertEquals(3, result.get(KUDU_MASTER).size());
        assertEquals(1, result.get(SPARK_YARN_HISTORY_SERVER).size());
        assertEquals(6, result.size());
    }

}