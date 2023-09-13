package com.sequenceiq.cloudbreak.service.blueprint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

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

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @InjectMocks
    private ComponentLocatorService underTest;

    @Test
    void testGetComponentLocation() {

        StackDto stackDto = mock(StackDto.class);

        InstanceGroup ig1 = new InstanceGroup();
        ig1.setGroupName(MASTER_1);
        InstanceMetaData imd1 = new InstanceMetaData();
        imd1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd1.setDiscoveryFQDN(MASTER1_FQDN);
        InstanceGroupDto master1 = new InstanceGroupDto(ig1, List.of(imd1));

        InstanceGroup ig2 = new InstanceGroup();
        ig2.setGroupName(MASTER_2);
        InstanceMetaData imd2 = new InstanceMetaData();
        imd2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd2.setDiscoveryFQDN(MASTER2_FQDN);
        InstanceGroupDto master2 = new InstanceGroupDto(ig2, List.of(imd2));

        InstanceGroup ig3 = new InstanceGroup();
        ig3.setGroupName(MASTER_3);
        InstanceMetaData imd3 = new InstanceMetaData();
        imd3.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd3.setDiscoveryFQDN(MASTER3_FQDN);
        InstanceGroupDto master3 = new InstanceGroupDto(ig3, List.of(imd3));

        InstanceGroup ig4 = new InstanceGroup();
        ig4.setGroupName(EXECUTOR);
        List<InstanceMetadataView> imd4 = EXECUTOR_FQDNS.stream().map(fqdn -> {
            InstanceMetaData imd = new InstanceMetaData();
            imd.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            imd.setDiscoveryFQDN(fqdn);
            return imd;
        }).collect(Collectors.toList());
        InstanceGroupDto executor = new InstanceGroupDto(ig4, imd4);

        InstanceGroup ig5 = new InstanceGroup();
        ig5.setGroupName(COORDINATOR);
        List<InstanceMetadataView> imd5 = COORDINATOR_FQDNS.stream().map(fqdn -> {
            InstanceMetaData imd = new InstanceMetaData();
            imd.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            imd.setDiscoveryFQDN(fqdn);
            return imd;
        }).collect(Collectors.toList());
        InstanceGroupDto coordinator = new InstanceGroupDto(ig5, imd5);

        List<InstanceGroupDto> instanceGroupDtos = new ArrayList<>(List.of(master1, master2, master3, executor, coordinator));

        when(blueprintTextProcessor.getComponentsInHostGroup(MASTER_1)).thenReturn(MASTER1_COMPONENTS);
        when(blueprintTextProcessor.getComponentsInHostGroup(MASTER_2)).thenReturn(MASTER2_COMPONENTS);
        when(blueprintTextProcessor.getComponentsInHostGroup(MASTER_3)).thenReturn(MASTER3_COMPONENTS);
        when(blueprintTextProcessor.getComponentsInHostGroup(EXECUTOR)).thenReturn(EXECUTOR_COMPONENTS);
        when(blueprintTextProcessor.getComponentsInHostGroup(COORDINATOR)).thenReturn(COORDINATOR_COMPONENTS);

        when(stackDto.getInstanceGroupDtos()).thenReturn(instanceGroupDtos);

        Map<String, List<String>> result = underTest.getComponentLocation(stackDto, blueprintTextProcessor, COMPONENT_NAMES);

        assertEquals(1, result.get(NAMENODE).size());
        assertEquals(MASTER1_FQDN, result.get(NAMENODE).get(0));
        assertEquals(3, result.get(KUDU_MASTER).size());
        assertEquals(1, result.get(SPARK_YARN_HISTORY_SERVER).size());
        assertEquals(6, result.size());
    }

    @Test
    void testGetImpalaCoordinatorLocationsWhenTheBlueprintOnTheStackDtoDelegateIsNull() {
        StackDto stackDto = mock(StackDto.class);

        Map<String, List<String>> result = assertDoesNotThrow(() -> underTest.getImpalaCoordinatorLocations(stackDto));

        verifyNoInteractions(cmTemplateProcessorFactory);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetImpalaCoordinatorLocationsWhenTheBlueprintOnTheStackDtoDelegateIsNotNullButStackDoesNotHaveGroups() {
        String blueprintText = "BLUEPRINTTEXT";
        StackDto stackDto = mock(StackDto.class);
        Blueprint blueprint = mock(Blueprint.class);
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getBlueprintText()).thenReturn(blueprintText);

        Map<String, List<String>> result = assertDoesNotThrow(() -> underTest.getImpalaCoordinatorLocations(stackDto));

        verify(cmTemplateProcessorFactory, times(1)).get(blueprintText);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetImpalaCoordinatorLocationsWhenTheBlueprintOnTheStackDtoDelegateIsNotNullButStackHaveGroups() {
        Stack stackDto = TestUtil.cluster().getStack();
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        String impalaServiceName = "impala";
        when(cmTemplateProcessor.getImpalaCoordinatorsInHostGroup(anyString())).thenReturn(Set.of(impalaServiceName)).thenReturn(Set.of());

        Map<String, List<String>> result = assertDoesNotThrow(() -> underTest.getImpalaCoordinatorLocations(stackDto));

        verify(cmTemplateProcessorFactory, times(1)).get(anyString());
        verify(cmTemplateProcessor, times(3)).getImpalaCoordinatorsInHostGroup(anyString());
        assertFalse(result.isEmpty());
        assertFalse(result.get(impalaServiceName).isEmpty());
    }
}