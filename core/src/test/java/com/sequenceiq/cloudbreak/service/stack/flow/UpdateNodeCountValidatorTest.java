package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.NODE_FAILURE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.stack.DependentRolesHealthCheckService.UNDEFINED_DEPENDENCY;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterHealthService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.DependentRolesHealthCheckService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.ScalabilityOption;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateNodeCountValidatorTest {

    private static final String TEST_COMPUTE_GROUP = "compute";

    private static final String TEST_BLUEPRINT_TEXT = "blueprintText";

    private static final Optional<String> FORBIDDEN_DOWN = Optional.of("Requested scaling down is forbidden");

    private static final Optional<String> FORBIDDEN_UP = Optional.of("Requested scaling up is forbidden");

    private static final Optional<String> NOT_ENOUGH_NODE = Optional.of("You can not go under the minimal node count.");

    private static final Optional<String> NO_ERROR = Optional.empty();

    private static final long STACK_ID = 1L;

    private static final Long CLUSTER_ID = 2L;

    private static final String ACCOUNT_ID = "accountId";

    private static final String STACK_CRN = String.format("crn:cdp:datahub:us-west-1:%s:cluster:cluster", ACCOUNT_ID);

    private static final int SCALING_ADJUSTMENT = 5;

    @InjectMocks
    private UpdateNodeCountValidator underTest;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi connector;

    @Mock
    private ClusterHealthService clusterHealthService;

    @Mock
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private DependentRolesHealthCheckService dependentRolesHealthCheckService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private CmTemplateValidator cmTemplateValidator;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    private Blueprint blueprint;

    private Optional<ClouderaManagerProduct> cdhProduct;

    private Set<InstanceGroup> instanceGroups;

    @BeforeEach
    void setUp() {
        blueprint = new Blueprint();
        cdhProduct = Optional.empty();
        instanceGroups = Set.of();
    }

    @ParameterizedTest(name = "The master node count is {0} this will be scaled with {2} " +
            "node and the minimum is {1} the ScalabilityOption is {3}.")
    @MethodSource("testValidateScalabilityOfInstanceGroupData")
    public void testValidateScalabilityOfInstanceGroup(
            int instanceGroupNodeCount,
            int minimumNodeCount,
            int scalingNodeCount,
            ScalabilityOption scalabilityOption,
            Optional<String> errorMessageSegment) {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentV4Request = mock(InstanceGroupAdjustmentV4Request.class);
        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);

        when(stack.getStack()).thenReturn(stackView);
        when(instanceGroupAdjustmentV4Request.getInstanceGroup()).thenReturn("master");
        when(instanceGroupAdjustmentV4Request.getScalingAdjustment()).thenReturn(scalingNodeCount);
        when(stack.getInstanceGroupByInstanceGroupName("master")).thenReturn(instanceGroupDto);
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroup);
        when(stackView.getName()).thenReturn("master-stack");
        when(instanceGroup.getGroupName()).thenReturn("master");
        when(instanceGroup.getMinimumNodeCount()).thenReturn(minimumNodeCount);
        when(instanceGroupDto.getNodeCount()).thenReturn(instanceGroupNodeCount);
        when(instanceGroup.getScalabilityOption()).thenReturn(scalabilityOption);

        if (errorMessageSegment.isPresent()) {
            BadRequestException badRequestException = assertThrows(BadRequestException.class,
                    () -> underTest.validateScalabilityOfInstanceGroup(stack, instanceGroupAdjustmentV4Request));
            assertTrue(badRequestException.getMessage().contains(errorMessageSegment.get()));
        } else {
            assertDoesNotThrow(() -> underTest.validateScalabilityOfInstanceGroup(stack, instanceGroupAdjustmentV4Request));
        }
    }

    private static Stream<Arguments> testValidateScalabilityOfInstanceGroupData() {
        return Stream.of(
                Arguments.of(3, 3, -1, ScalabilityOption.ALLOWED, NOT_ENOUGH_NODE),
                Arguments.of(3, 3, -1, ScalabilityOption.ONLY_DOWNSCALE, NOT_ENOUGH_NODE),
                Arguments.of(3, 3, -1, ScalabilityOption.ONLY_UPSCALE, NOT_ENOUGH_NODE),
                Arguments.of(3, 3, -1, ScalabilityOption.FORBIDDEN, NOT_ENOUGH_NODE),
                Arguments.of(3, 2, -1, ScalabilityOption.ALLOWED, NO_ERROR),
                Arguments.of(3, 2, -1, ScalabilityOption.ONLY_DOWNSCALE, NO_ERROR),
                Arguments.of(3, 2, -1, ScalabilityOption.ONLY_UPSCALE, FORBIDDEN_DOWN),
                Arguments.of(3, 2, -1, ScalabilityOption.FORBIDDEN, FORBIDDEN_DOWN),
                Arguments.of(3, 2, 1, ScalabilityOption.ALLOWED, NO_ERROR),
                Arguments.of(3, 2, 1, ScalabilityOption.ONLY_DOWNSCALE, FORBIDDEN_UP),
                Arguments.of(3, 2, 1, ScalabilityOption.ONLY_UPSCALE, NO_ERROR),
                Arguments.of(3, 2, 1, ScalabilityOption.FORBIDDEN, FORBIDDEN_UP),
                Arguments.of(3, 2, 2, ScalabilityOption.ALLOWED, NO_ERROR),
                Arguments.of(3, 2, 2, ScalabilityOption.ONLY_DOWNSCALE, FORBIDDEN_UP),
                Arguments.of(3, 2, 2, ScalabilityOption.ONLY_UPSCALE, NO_ERROR),
                Arguments.of(3, 2, 2, ScalabilityOption.FORBIDDEN, FORBIDDEN_UP),
                Arguments.of(3, 0, 1, ScalabilityOption.ALLOWED, NO_ERROR),
                Arguments.of(3, 0, 1, ScalabilityOption.ONLY_DOWNSCALE, FORBIDDEN_UP),
                Arguments.of(3, 0, 1, ScalabilityOption.ONLY_UPSCALE, NO_ERROR),
                Arguments.of(3, 0, 1, ScalabilityOption.FORBIDDEN, FORBIDDEN_UP)
        );
    }

    @ParameterizedTest(name = "The stack status is {0}.")
    @MethodSource("testValidateStatusForStopStartHostGroupData")
    public void testValidateStatusForStopStartHostGroup(
            Status status,
            Optional<String> errorMessageSegment,
            String instancegroup,
            boolean unDefinedDependencyPresent,
            boolean unhealthyHostgroup,
            Integer scalingAdjustment) {
        StackDto stackdto = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        setupMocksForStopStartInstanceGroupValidation(stackdto);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentV4Request = mock(InstanceGroupAdjustmentV4Request.class);
        Set<String> dependComponents = new HashSet<>();
        List<String> unHealthyHosts = new ArrayList<>();
        if (unhealthyHostgroup) {
            unHealthyHosts.add("master");
        }
        dependComponents.add("RESOURCEMANAGER");

        when(instanceGroupAdjustmentV4Request.getInstanceGroup()).thenReturn(instancegroup);
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        when(stackdto.getStack()).thenReturn(stackView);
        when(stackdto.getStack().getName()).thenReturn("master-stack");
        when(stackdto.getStack().getStatus()).thenReturn(status);
        when(clusterApiConnectors.getConnector(any(StackDtoDelegate.class))).thenReturn(connector);
        when(connector.clusterHealthService()).thenReturn(clusterHealthService);
        Map<String, String> componentsHealth = Map.of("YARN_RESOURCEMANAGER_HEALTH", "BAD");
        when(clusterHealthService.readServicesHealth("master-stack")).thenReturn(componentsHealth);

        when(dependentRolesHealthCheckService.getUnhealthyDependentHostGroups(any(), any(), any())).thenReturn(unHealthyHosts);
        when(dependentRolesHealthCheckService.getDependentComponentsHeathChecksForHostGroup(any(), any())).thenReturn(Set.of("YARN_RESOURCEMANAGER_HEALTH"));

        if (unDefinedDependencyPresent) {
            dependComponents.add(UNDEFINED_DEPENDENCY);
        }
        when(dependentRolesHealthCheckService.getDependentComponentsForHostGroup(any(), any())).thenReturn(dependComponents);

        if (status == UPDATE_IN_PROGRESS) {
            when(stackdto.getStack().isModificationInProgress()).thenReturn(true);
        } else {
            when(stackdto.getStack().isModificationInProgress()).thenReturn(false);
        }

        if (status == AVAILABLE) {
            when(stackdto.getStack().isAvailable()).thenReturn(true);
            when(stackdto.getStack().isAvailableWithStoppedInstances()).thenReturn(false);
        } else {
            when(stackdto.getStack().isAvailable()).thenReturn(false);
            when(stackdto.getStack().isAvailableWithStoppedInstances()).thenReturn(false);
        }

        if (errorMessageSegment.isPresent()) {
            checkExecutableThrowsException(errorMessageSegment.get(),
                    () -> underTest.validateStackStatusForStopStartHostGroup(stackdto, instanceGroupAdjustmentV4Request.getInstanceGroup(), scalingAdjustment));
        } else {
            assertDoesNotThrow(() -> underTest.validateStackStatusForStopStartHostGroup(stackdto,
                    instanceGroupAdjustmentV4Request.getInstanceGroup(), scalingAdjustment));
        }
    }

    private void checkExecutableThrowsException(String expectedErrorMessageSegment, Executable executable) {
        BadRequestException badRequestException = assertThrows(BadRequestException.class, executable);
        assertEquals(expectedErrorMessageSegment, badRequestException.getMessage());
        assertTrue(badRequestException.getMessage().contains(expectedErrorMessageSegment));
    }

    private static Stream<Arguments> testValidateStatusForStopStartHostGroupData() {
        return Stream.of(
                Arguments.of(AVAILABLE, NO_ERROR, "compute", false, false, 1),
                Arguments.of(AVAILABLE, NO_ERROR, "compute", false, false, -1),
                Arguments.of(AVAILABLE,
                        Optional.of("Upscaling is not allowed for HostGroup: 'compute' as Data hub 'master-stack' has " +
                                "health checks which are not healthy: [[YARN_RESOURCEMANAGER_HEALTH]] for instances in hostGroup(s): [[master]]"),
                        "compute", false, true, 1),
                Arguments.of(AVAILABLE,
                        Optional.of("Downscaling is not allowed for HostGroup: 'compute' as Data hub 'master-stack' has " +
                                "health checks which are not healthy: [[YARN_RESOURCEMANAGER_HEALTH]] for instances in hostGroup(s): [[master]]"),
                        "compute", false, true, -1),
                Arguments.of(AVAILABLE, NO_ERROR, "compute", true, false, 1),
                Arguments.of(AVAILABLE, NO_ERROR, "compute", true, false, -1),
                Arguments.of(UPDATE_IN_PROGRESS,
                        Optional.of("Data Hub 'master-stack' has 'UPDATE_IN_PROGRESS' state. Upscaling is not allowed."), "compute", false, false, 1),
                Arguments.of(UPDATE_IN_PROGRESS,
                        Optional.of("Data Hub 'master-stack' has 'UPDATE_IN_PROGRESS' state. Downscaling is not allowed."), "compute", false, false, -1),
                Arguments.of(UPDATE_IN_PROGRESS,
                        Optional.of("Data Hub 'master-stack' has 'UPDATE_IN_PROGRESS' state. Upscaling is not allowed."), "compute", false, true, 1),
                Arguments.of(UPDATE_IN_PROGRESS,
                        Optional.of("Data Hub 'master-stack' has 'UPDATE_IN_PROGRESS' state. Downscaling is not allowed."), "compute", false, true, -1),
                Arguments.of(NODE_FAILURE, NO_ERROR, "compute", false, false, 1),
                Arguments.of(NODE_FAILURE, NO_ERROR, "compute", false, false, -1),
                Arguments.of(NODE_FAILURE,
                        Optional.of("Upscaling is not allowed for HostGroup: 'compute' as Data hub 'master-stack' has " +
                                "health checks which are not healthy: [[YARN_RESOURCEMANAGER_HEALTH]] for instances in hostGroup(s): [[master]]"), "compute",
                        false, true, 1),
                Arguments.of(NODE_FAILURE,
                        Optional.of("Downscaling is not allowed for HostGroup: 'compute' as Data hub 'master-stack' has " +
                                "health checks which are not healthy: [[YARN_RESOURCEMANAGER_HEALTH]] for instances in hostGroup(s): [[master]]"), "compute",
                        false, true, -1),
                Arguments.of(NODE_FAILURE,
                        Optional.of("Data Hub 'master-stack' has 'NODE_FAILURE' state. Node group start operation is not allowed for this state."),
                        "compute", true, false, 1),
                Arguments.of(NODE_FAILURE,
                        Optional.of("Data Hub 'master-stack' has 'NODE_FAILURE' state. Node group start operation is not allowed for this state."),
                        "compute", true, false, -1),
                Arguments.of(NODE_FAILURE,
                        Optional.of("Data Hub 'master-stack' has 'NODE_FAILURE' state. Node group start operation is not allowed for this state."),
                        "compute", true, true, 1),
                Arguments.of(NODE_FAILURE,
                        Optional.of("Data Hub 'master-stack' has 'NODE_FAILURE' state. Node group start operation is not allowed for this state."),
                        "compute", true, true, -1)
        );
    }

    private InstanceMetadataView generateInstanceMetadata(InstanceStatus instanceStatus, InstanceMetadataType instanceMetadataType) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceMetadataType(instanceMetadataType);
        instanceMetaData.setInstanceStatus(instanceStatus);
        instanceMetaData.setDiscoveryFQDN("hg0-host-2");
        return instanceMetaData;
    }

    @Test
    public void testValidateCMStatusNonPrimaryGatewayUnhealthy() {
        StackDto stack = mock(StackDto.class);
        List<InstanceMetadataView> instanceMetaDataAsList = new ArrayList<>();
        instanceMetaDataAsList.add(generateInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, InstanceMetadataType.GATEWAY_PRIMARY));
        instanceMetaDataAsList.add(generateInstanceMetadata(InstanceStatus.SERVICES_UNHEALTHY, InstanceMetadataType.GATEWAY));
        when(stack.getAllAvailableInstances()).thenReturn(instanceMetaDataAsList);
        assertDoesNotThrow(() -> underTest.validateCMStatus(stack));
    }

    @Test
    public void testValidateCMStatusAllGatewayHealthy() {
        StackDto stack = mock(StackDto.class);
        List<InstanceMetadataView> instanceMetaDataAsList = new ArrayList<>();
        instanceMetaDataAsList.add(generateInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, InstanceMetadataType.GATEWAY_PRIMARY));
        instanceMetaDataAsList.add(generateInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, InstanceMetadataType.GATEWAY));
        when(stack.getAllAvailableInstances()).thenReturn(instanceMetaDataAsList);
        assertDoesNotThrow(() -> underTest.validateCMStatus(stack));
    }

    @Test
    public void testValidateCMStatusAllGatewayUnhealthy() {
        StackDto stack = mock(StackDto.class);
        List<InstanceMetadataView> instanceMetaDataAsList = new ArrayList<>();
        instanceMetaDataAsList.add(generateInstanceMetadata(InstanceStatus.SERVICES_UNHEALTHY, InstanceMetadataType.GATEWAY_PRIMARY));
        instanceMetaDataAsList.add(generateInstanceMetadata(InstanceStatus.SERVICES_UNHEALTHY, InstanceMetadataType.GATEWAY));
        when(stack.getAllAvailableInstances()).thenReturn(instanceMetaDataAsList);
        checkExecutableThrowsException("Upscale is not allowed because the CM host is not healthy: hg0-host-2: SERVICES_UNHEALTHY.",
                () -> underTest.validateCMStatus(stack));
    }

    @Test
    public void testValidateCMStatusPrimaryGatewayUnhealthy() {
        StackDto stack = mock(StackDto.class);
        List<InstanceMetadataView> instanceMetaDataAsList = new ArrayList<>();
        instanceMetaDataAsList.add(generateInstanceMetadata(InstanceStatus.SERVICES_UNHEALTHY, InstanceMetadataType.GATEWAY_PRIMARY));
        instanceMetaDataAsList.add(generateInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, InstanceMetadataType.GATEWAY));
        when(stack.getAllAvailableInstances()).thenReturn(instanceMetaDataAsList);
        checkExecutableThrowsException("Upscale is not allowed because the CM host is not healthy: hg0-host-2: SERVICES_UNHEALTHY.",
                () -> underTest.validateCMStatus(stack));
    }

    @Test
    public void testValidateInstanceGroupForStopStartIsSuccessful() {
        StackDto stack = mock(StackDto.class);
        setupMocksForStopStartInstanceGroupValidation(stack);
        assertDoesNotThrow(() -> underTest.validateInstanceGroupForStopStart(stack, "compute", 5));
    }

    @Test
    public void testValidateInstanceGroupForStopStartThrowsExceptionForUpscale() {
        StackDto stack = mock(StackDto.class);
        setupMocksForStopStartInstanceGroupValidation(stack);
        assertThatThrownBy(() -> underTest.validateInstanceGroupForStopStart(stack, "worker", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Start instances operation is not allowed for worker host group.");
    }

    @Test
    public void testValidateInstanceGroupForStopStartThrowsExceptionForDownscale() {
        StackDto stack = mock(StackDto.class);
        setupMocksForStopStartInstanceGroupValidation(stack);
        assertThatThrownBy(() -> underTest.validateInstanceGroupForStopStart(stack, "worker", -1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Stop instances operation is not allowed for worker host group.");
    }

    @Test
    public void testValidateInstanceGroupForStopStartThrowsExceptionForZeroScalingAdjustment() {
        StackDto stack = mock(StackDto.class);
        setupMocksForStopStartInstanceGroupValidation(stack);
        assertThatThrownBy(() -> underTest.validateInstanceGroupForStopStart(stack, "worker", 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Zero Scaling adjustment detected for worker host group.");
    }

    private void setupMocksForStopStartInstanceGroupValidation(StackDto stack) {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        Cluster cluster = mock(Cluster.class);
        Blueprint blueprint = mock(Blueprint.class);

        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getBlueprintJsonText()).thenReturn(TEST_BLUEPRINT_TEXT);
        when(blueprint.getBlueprintJsonText()).thenReturn(TEST_BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getComputeHostGroups(any())).thenReturn(Set.of(TEST_COMPUTE_GROUP));
    }

    @Test
    void validateStackStatusUpscaleAvailable() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        underTest.validateStackStatus(stack, true);
    }

    @Test
    void validateStackStatusDownscaleAvailable() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        underTest.validateStackStatus(stack, false);
    }

    @Test
    void validateStackStatusUpscaleNodeFailureTargetedUpscaleSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.TRUE);
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));
        underTest.validateStackStatus(stack, true);
    }

    @Test
    void validateStackStatusDownscaleNodeFailureTargetedUpscaleSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.TRUE);
        Stack stack = new Stack();
        stack.setName("stack");
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.validateStackStatus(stack, false));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateStackStatusUpscaleNodeFailureTargetedUpscaleNotSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.FALSE);
        Stack stack = new Stack();
        stack.setName("stack");
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.validateStackStatus(stack, true));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateStackStatusDownscaleNodeFailureTargetedUpscaleNotSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.FALSE);
        Stack stack = new Stack();
        stack.setName("stack");
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.validateStackStatus(stack, false));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateClusterStatusUpscaleAvailable() {
        Stack stack = new Stack();
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        underTest.validateClusterStatus(stack, true);
    }

    @Test
    void validateClusterStatusDownscaleAvailable() {
        Stack stack = new Stack();
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        underTest.validateClusterStatus(stack, false);
    }

    @Test
    void validateClusterStatusUpscaleNodeFailureTargetedUpscaleSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.TRUE);
        Stack stack = new Stack();
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));
        underTest.validateClusterStatus(stack, true);
    }

    @Test
    void validateClusterStatusDownscaleNodeFailureTargetedUpscaleSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.TRUE);
        Stack stack = new Stack();
        stack.setName("stack");
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.validateClusterStatus(stack, false));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateClusterStatusUpscaleNodeFailureTargetedUpscaleNotSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.FALSE);
        Stack stack = new Stack();
        stack.setName("stack");
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.validateClusterStatus(stack, false));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateClusterStatusDownscaleNodeFailureTargetedUpscaleNotSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.FALSE);
        Stack stack = new Stack();
        stack.setName("stack");
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.validateClusterStatus(stack, false));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateServiceRolesTestWhenJsonAndGroupNotFound() {
        setupMocksForValidateServiceRolesSingleGroupNotFound();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = createInstanceGroupAdjustmentV4Request();

        underTest.validateServiceRoles(stackDto, instanceGroupAdjustmentJson);

        verifyValidateHostGroupScalingRequestSingleGroupNotFound();
    }

    private void setupMocksForValidateServiceRolesSingleGroupNotFound() {
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(hostGroupService.hasHostGroupInCluster(CLUSTER_ID, TEST_COMPUTE_GROUP)).thenReturn(false);
    }

    private void verifyValidateHostGroupScalingRequestSingleGroupNotFound() {
        verifyNoInteractions(cmTemplateValidator);
        verifyNoInteractions(clusterComponentConfigProvider);
        verifyNoInteractions(instanceGroupService);
    }

    @Test
    void validateServiceRolesTestWhenJsonAndGroupPresent() {
        setupMocksForValidateServiceRolesSingleGroupPresent();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = createInstanceGroupAdjustmentV4Request();

        underTest.validateServiceRoles(stackDto, instanceGroupAdjustmentJson);

        verifyValidateHostGroupScalingRequestSingleGroupPresent(false);
    }

    private InstanceGroupAdjustmentV4Request createInstanceGroupAdjustmentV4Request() {
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentJson.setInstanceGroup(TEST_COMPUTE_GROUP);
        instanceGroupAdjustmentJson.setScalingAdjustment(SCALING_ADJUSTMENT);
        return instanceGroupAdjustmentJson;
    }

    private void setupMocksForValidateServiceRolesSingleGroupPresent() {
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(hostGroupService.hasHostGroupInCluster(CLUSTER_ID, TEST_COMPUTE_GROUP)).thenReturn(true);
        when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        when(clusterComponentConfigProvider.getNormalizedCdhProductWithNormalizedVersion(CLUSTER_ID)).thenReturn(cdhProduct);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(instanceGroupService.findNotTerminatedByStackId(STACK_ID)).thenReturn(instanceGroups);
    }

    private void verifyValidateHostGroupScalingRequestSingleGroupPresent(boolean forced) {
        verify(cmTemplateValidator).validateHostGroupScalingRequest(
                ACCOUNT_ID,
                blueprint,
                cdhProduct,
                TEST_COMPUTE_GROUP,
                SCALING_ADJUSTMENT,
                instanceGroups,
                forced);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void validateServiceRolesTestWhenSingleGroupAndGroupNotFound(boolean forced) {
        setupMocksForValidateServiceRolesSingleGroupNotFound();

        underTest.validateServiceRoles(stackDto, TEST_COMPUTE_GROUP, SCALING_ADJUSTMENT, forced);

        verifyValidateHostGroupScalingRequestSingleGroupNotFound();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void validateServiceRolesTestWhenSingleGroupAndGroupPresent(boolean forced) {
        setupMocksForValidateServiceRolesSingleGroupPresent();

        underTest.validateServiceRoles(stackDto, TEST_COMPUTE_GROUP, SCALING_ADJUSTMENT, forced);

        verifyValidateHostGroupScalingRequestSingleGroupPresent(forced);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void validateServiceRolesTestWhenGroupMap(boolean forced) {
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        when(clusterComponentConfigProvider.getNormalizedCdhProductWithNormalizedVersion(CLUSTER_ID)).thenReturn(cdhProduct);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(instanceGroupService.findNotTerminatedByStackId(STACK_ID)).thenReturn(instanceGroups);

        Map<String, Integer> instanceGroupAdjustments = Map.of();

        underTest.validateServiceRoles(stackDto, instanceGroupAdjustments, forced);

        verifyNoInteractions(hostGroupService);
        verify(cmTemplateValidator).validateHostGroupScalingRequest(
                ACCOUNT_ID,
                blueprint,
                instanceGroupAdjustments,
                cdhProduct,
                instanceGroups,
                forced);
    }

}