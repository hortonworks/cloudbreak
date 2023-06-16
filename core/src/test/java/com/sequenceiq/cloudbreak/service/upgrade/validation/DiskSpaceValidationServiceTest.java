package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorRunParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class DiskSpaceValidationServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private DiskSpaceValidationService underTest;

    private Stack stack;

    private List<GatewayConfig> gatewayConfigs;

    private Set<Node> nodes;

    private Map<String, String> freeSpaceByNodes;

    @BeforeEach
    void before() {
        stack = createStack();
        gatewayConfigs = Collections.emptyList();
        nodes = Collections.emptySet();
        freeSpaceByNodes = createFreeSpaceByNodesMap();

        when(resourceService.getAllByStackId(STACK_ID)).thenReturn(emptySet());
        when(stackUtil.collectNodesWithDiskData(stack)).thenReturn(nodes);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        when(hostOrchestrator.runShellCommandOnNodes(any(OrchestratorRunParams.class))).thenReturn(freeSpaceByNodes);
    }

    @Test
    void testValidateFreeSpaceForUpgradeShouldNotThrowExceptionWhenThereAreEnoughFreeSpaceForUpgrade() {
        underTest.validateFreeSpaceForUpgrade(stack, 9000L);
        verifyMocks();
    }

    @Test
    void testValidateFreeSpaceForUpgradeShouldThrowExceptionWhenThereAreNoEnoughFreeSpaceAndTheRequiredSpaceIsReturnedInMb() {
        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateFreeSpaceForUpgrade(stack, 92000L));
        assertEquals("There is not enough free space on the nodes to perform upgrade operation. The required and the available free space by nodes: host1: "
                        + "required free space is: 225 MB and the available free space is: 89 MB, "
                        + "host2: required free space is: 314 MB and the available free space is: 166 MB",
                exception.getMessage());
        verifyMocks();
    }

    @Test
    void testValidateFreeSpaceForUpgradeShouldThrowExceptionWhenThereAreNoEnoughFreeSpaceAndTheRequiredSpaceIsReturnedInGb() {
        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateFreeSpaceForUpgrade(stack, 1750000L));
        assertEquals("There is not enough free space on the nodes to perform upgrade operation. The required and the available free space by nodes: host1: "
                        + "required free space is: 4.2 GB and the available free space is: 89 MB, "
                        + "host2: required free space is: 5.8 GB and the available free space is: 166 MB",
                exception.getMessage());
        verifyMocks();
    }

    private void verifyMocks() {
        verify(resourceService).getAllByStackId(STACK_ID);
        verify(stackUtil).collectNodesWithDiskData(stack);
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        ArgumentCaptor<OrchestratorRunParams> paramCaptor = ArgumentCaptor.forClass(OrchestratorRunParams.class);
        verify(hostOrchestrator).runShellCommandOnNodes(paramCaptor.capture());
        OrchestratorRunParams params = paramCaptor.getValue();
        assertThat(params.nodes()).isEqualTo(nodes);
        assertThat(params.gatewayConfigs()).isEqualTo(gatewayConfigs);
        assertThat(params.command()).contains("df -k");
    }

    private Map<String, String> createFreeSpaceByNodesMap() {
        return Map.of(
                "host1", "91000",
                "host2", "170000");
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setInstanceGroups(createInstanceGroups());
        return stack;
    }

    private Set<InstanceGroup> createInstanceGroups() {
        return Set.of(
                createInstanceGroup(InstanceGroupType.CORE, "host1"),
                createInstanceGroup(InstanceGroupType.GATEWAY, "host2")
        );
    }

    private InstanceGroup createInstanceGroup(InstanceGroupType instanceGroupType, String hostname) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(instanceGroupType);
        instanceGroup.setInstanceMetaData(Collections.singleton(createInstanceMetaData(hostname)));
        return instanceGroup;
    }

    private InstanceMetaData createInstanceMetaData(String hostname) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(hostname);
        return instanceMetaData;
    }
}
