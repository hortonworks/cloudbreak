package com.sequenceiq.cloudbreak.service.existingstackfix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.repository.StackFixRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.service.FlowService;

@ExtendWith(MockitoExtension.class)
class UnboundRestartFixServiceTest {

    private static final String POLLABLE_ID = "pollable-id";

    @Mock
    private StackFixRepository stackFixRepository;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private FlowService flowService;

    @InjectMocks
    private UnboundRestartFixService underTest;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(123L);
        stack.setResourceCrn("stack-crn");

        setCmServerReachability(true);
    }

    @Test
    void unaffectedStackVersionNotAffected() {
        stack.setStackVersion("7.2.12");

        boolean result = underTest.isAffected(stack);

        assertThat(result).isFalse();
    }

    @Test
    void affectedStackVersionButUnaffectedImageIdIsNotAffected() throws CloudbreakImageNotFoundException {
        stack.setStackVersion(UnboundRestartFixService.AFFECTED_STACK_VERSION);
        setStackImageId("123456");

        boolean result = underTest.isAffected(stack);

        assertThat(result).isFalse();
    }

    @Test
    void affectedImageIsAffected() throws CloudbreakImageNotFoundException {
        stack.setStackVersion(UnboundRestartFixService.AFFECTED_STACK_VERSION);
        setStackImageId(UnboundRestartFixService.AFFECTED_IMAGE_IDS.stream().findFirst().get());

        boolean result = underTest.isAffected(stack);

        assertThat(result).isTrue();
    }

    @Test
    void unreachableCmServerFailsToApply() {
        setCmServerReachability(false);

        assertThatThrownBy(() -> underTest.doApply(stack))
                .hasMessageStartingWith("CM server is unreachable for stack");
    }

    @Test
    void failedFlowState() {
        when(clusterOperationService.updateSalt(stack)).thenReturn(new FlowIdentifier(FlowType.FLOW, POLLABLE_ID));
        setFlowState(true, true);

        assertThatThrownBy(() -> underTest.doApply(stack))
                .hasMessageStartingWith("Failed to update salt for stack");
    }

    @Test
    void allHostsSucceedToApply() throws CloudbreakOrchestratorFailedException {
        when(clusterOperationService.updateSalt(stack)).thenReturn(new FlowIdentifier(FlowType.FLOW, POLLABLE_ID));
        setFlowState(true, false);

        underTest.doApply(stack);
    }

    private void setCmServerReachability(boolean reachable) {
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setClusterManagerServer(true);
        instanceMetaData.setInstanceStatus(reachable ? InstanceStatus.SERVICES_RUNNING : InstanceStatus.ORCHESTRATION_FAILED);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));
    }

    private void setStackImageId(String id) throws CloudbreakImageNotFoundException {
        Image image = mock(Image.class);
        when(image.getImageId()).thenReturn(id);
        when(stackImageService.getCurrentImage(stack)).thenReturn(image);
    }

    private void setFlowState(boolean finished, boolean failed) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(!finished);
        flowCheckResponse.setLatestFlowFinalizedAndFailed(failed);
        when(flowService.getFlowState(POLLABLE_ID)).thenReturn(flowCheckResponse);
    }

}
