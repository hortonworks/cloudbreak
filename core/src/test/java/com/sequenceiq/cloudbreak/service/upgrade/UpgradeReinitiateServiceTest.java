package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeReinitiableV4Response;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.chain.UpgradeDistroxFlowEventChainFactory;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradeFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.util.CodUtil;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.UpgradeReinitiateStatus;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@ExtendWith(MockitoExtension.class)
class UpgradeReinitiateServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String FLOW_CHAIN_ID = "flowChainId";

    private static final String IMAGE_ID = "imageId";

    private static final String RUNTIME_VERSION = "7.3.2";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private UpgradeDistroxFlowEventChainFactory upgradeDistroxFlowEventChainFactory;

    @Mock
    private FlowService flowService;

    @Mock
    private StackStatusService stackStatusService;

    @InjectMocks
    private UpgradeReinitiateService underTest;

    static Stream<Arguments> testCheckClusterUpgradeReinitiableWhenFlowLogsAvailableArguments() {
        return Stream.of(
                Arguments.of(
                        getFlowCheckResponse(true, false),
                        UpgradeReinitiateStatus.REINITIABLE,
                        "The last upgrade for this cluster finished with a failure, therefore the cluster is eligible for upgrade reinitiation."
                ),
                Arguments.of(
                        getFlowCheckResponse(false, true),
                        UpgradeReinitiateStatus.NON_REINITIABLE,
                        "The last upgrade for this cluster is still in progress, therefore there is no reason to reinitiate the upgrade."
                ),
                Arguments.of(
                        getFlowCheckResponse(false, false),
                        UpgradeReinitiateStatus.NON_REINITIABLE,
                        "The last upgrade for this cluster finished successfully, therefore there is no reason to reinitiate the upgrade."
                )
        );
    }

    @MethodSource("testCheckClusterUpgradeReinitiableWhenFlowLogsAvailableArguments")
    @ParameterizedTest
    void testCheckClusterUpgradeReinitiableWhenFlowLogsAvailable(FlowCheckResponse flowCheckResponse,
            UpgradeReinitiateStatus expectedStatus, String expectedReason) {
        StackDto stackDto = mock();
        when(stackDto.getStack()).thenReturn(mock());
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        FlowChainLog flowChainLog = mock();
        when(flowChainLog.getFlowChainId()).thenReturn(FLOW_CHAIN_ID);
        when(upgradeDistroxFlowEventChainFactory.getName()).thenCallRealMethod();
        when(flowChainLogService.findLastByResourceIdAndFlowChainTypeOrderByCreatedDesc(STACK_ID, "UpgradeDistroxFlowEventChainFactory"))
                .thenReturn(Optional.of(flowChainLog));
        when(flowService.getFlowChainState(FLOW_CHAIN_ID)).thenReturn(flowCheckResponse);

        UpgradeReinitiableV4Response result = underTest.checkClusterUpgradeReinitiable(STACK_ID);

        assertEquals(expectedStatus, result.status());
        assertEquals(expectedReason, result.reason());
    }

    static Stream<Arguments> testCheckClusterUpgradeReinitiableWhenNoFlowLogsArguments() {
        Stream.Builder<Arguments> arguments = Stream.builder();
        arguments.add(Arguments.of(List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE)
                ),
                UpgradeReinitiateStatus.NON_REINITIABLE,
                "There were no upgrades for this cluster based on the past statuses, therefore upgrade reinitiation is not needed."
        ));
        for (DetailedStackStatus successfulUpgradeStatus : DetailedStackStatus.getUpgradeSuccessStatuses()) {
            arguments.add(Arguments.of(List.of(
                            getStackStatus(DetailedStackStatus.AVAILABLE),
                            getStackStatus(successfulUpgradeStatus),
                            getStackStatus(DetailedStackStatus.AVAILABLE)
                    ),
                    UpgradeReinitiateStatus.NON_REINITIABLE,
                    "The last upgrade for this cluster finished successfully based on the past statuses, therefore upgrade reinitiation is not needed."
            ));
        }
        for (DetailedStackStatus failedUpgradeStatus : DetailedStackStatus.getUpgradeFailureStatuses()) {
            arguments.add(Arguments.of(List.of(
                            getStackStatus(DetailedStackStatus.AVAILABLE),
                            getStackStatus(failedUpgradeStatus),
                            getStackStatus(DetailedStackStatus.AVAILABLE)
                    ),
                    UpgradeReinitiateStatus.REINITIABLE,
                    "The last upgrade for this cluster finished with a failure based on the past statuses," +
                            " therefore the cluster is eligible for upgrade reinitiation."
            ));
        }
        return arguments.build();
    }

    @MethodSource("testCheckClusterUpgradeReinitiableWhenNoFlowLogsArguments")
    @ParameterizedTest
    void testCheckClusterUpgradeReinitiableWhenNoFlowLogs(List<StackStatus> stackStatuses, UpgradeReinitiateStatus expectedStatus, String expectedReason) {
        StackDto stackDto = mock();
        when(stackDto.getStack()).thenReturn(mock());
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackStatusService.findAllStackStatusesById(STACK_ID)).thenReturn(stackStatuses);

        UpgradeReinitiableV4Response result = underTest.checkClusterUpgradeReinitiable(STACK_ID);

        assertEquals(expectedStatus, result.status());
        assertEquals(expectedReason, result.reason());
    }

    @Test
    void testCheckClusterUpgradeReinitiableWhenCodCluster() {
        StackDto stackDto = mock();
        StackView stackView = mock();
        when(stackDto.getStack()).thenReturn(stackView);
        StackTags stackTags = new StackTags(Map.of(), Map.of(ClusterTemplateApplicationTag.SERVICE_TYPE.key(), CodUtil.OPERATIONAL_DB), Map.of());
        when(stackView.getTags()).thenReturn(new Json(stackTags));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        UpgradeReinitiableV4Response result = underTest.checkClusterUpgradeReinitiable(STACK_ID);

        assertEquals(UpgradeReinitiateStatus.NON_REINITIABLE, result.status());
        assertEquals("The cluster is not eligible for upgrade reinitiation: " +
                "Please note that COD cluster upgrades are supported only through the Operational Database UI or CLI!", result.reason());
    }

    @Test
    void testTryRetrieveLastDistroxUpgradeV1Request() {
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, IMAGE_ID);
        DistroXUpgradeFlowChainTriggerEvent expectedTriggerEvent = new DistroXUpgradeFlowChainTriggerEvent(
                FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, STACK_ID, null, imageChangeDto, true, true, null, true, RUNTIME_VERSION);
        FlowChainLog flowChainLog = mock();
        when(flowChainLog.getTriggerEventJackson()).thenReturn(new Json(expectedTriggerEvent).getValue());
        when(upgradeDistroxFlowEventChainFactory.getName()).thenCallRealMethod();
        when(flowChainLogService.findLastByResourceIdAndFlowChainTypeOrderByCreatedDesc(STACK_ID, "UpgradeDistroxFlowEventChainFactory"))
                .thenReturn(Optional.of(flowChainLog));

        DistroXUpgradeV1Request distroXUpgradeV1Request = underTest.tryRetrieveLastDistroxUpgradeV1Request(STACK_ID).get();

        assertEquals(IMAGE_ID, distroXUpgradeV1Request.getImageId());
        assertEquals(RUNTIME_VERSION, distroXUpgradeV1Request.getRuntime());
        assertEquals(true, distroXUpgradeV1Request.getLockComponents());
        assertEquals(true, distroXUpgradeV1Request.getRollingUpgradeEnabled());
        assertEquals(DistroXUpgradeReplaceVms.ENABLED, distroXUpgradeV1Request.getReplaceVms());
    }

    @Test
    void testTryRetrieveLastDistroxUpgradeV1RequestWhenNoFlowChainLog() {
        when(upgradeDistroxFlowEventChainFactory.getName()).thenCallRealMethod();
        when(flowChainLogService.findLastByResourceIdAndFlowChainTypeOrderByCreatedDesc(STACK_ID, "UpgradeDistroxFlowEventChainFactory"))
                .thenReturn(Optional.empty());

        assertEquals(Optional.empty(), underTest.tryRetrieveLastDistroxUpgradeV1Request(STACK_ID));
    }

    private static FlowCheckResponse getFlowCheckResponse(boolean latestFlowFinalizedAndFailed, boolean hasActiveFlow) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setLatestFlowFinalizedAndFailed(latestFlowFinalizedAndFailed);
        flowCheckResponse.setHasActiveFlow(hasActiveFlow);
        return flowCheckResponse;
    }

    private static StackStatus getStackStatus(DetailedStackStatus detailedStackStatus) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setDetailedStackStatus(detailedStackStatus);
        return stackStatus;
    }
}
