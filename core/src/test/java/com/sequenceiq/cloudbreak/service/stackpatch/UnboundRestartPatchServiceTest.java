package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalCrnModifier;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.StackPatchRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.service.FlowService;

@ExtendWith(MockitoExtension.class)
class UnboundRestartPatchServiceTest {

    private static final String POLLABLE_ID = "pollable-id";

    private static final String INTERNAL_CRN_WITH_ACCOUNT_ID = "internal-crn-with-account-id";

    @Mock
    private StackPatchRepository stackPatchRepository;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private FlowService flowService;

    @Mock
    private InternalCrnModifier internalCrnModifier;

    @InjectMocks
    private UnboundRestartPatchService underTest;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(123L);
        stack.setResourceCrn("crn:cdp:datalake:us-west-1:tenant:datalake:935ad382-fe9c-400b-bf38-2156c1f09b6d");

        lenient().when(internalCrnModifier.getInternalCrnWithAccountId(any())).thenReturn(INTERNAL_CRN_WITH_ACCOUNT_ID);

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
        stack.setStackVersion("7.2.11");
        setStackImageId("123456");

        boolean result = underTest.isAffected(stack);

        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"26cd5a65-cd5c-457d-8d48-9caf1a486516", "19cf97b8-56d8-4be9-b317-998eea99d884", "c24acec3-9110-4474-9082-3620deac0910"})
    void affectedImageIsAffected(String imageId) throws CloudbreakImageNotFoundException {
        stack.setStackVersion("7.2.11");
        setStackImageId(imageId);

        boolean result = underTest.isAffected(stack);

        assertThat(result).isTrue();
    }

    @Test
    void unreachableCmServerFailsToApply() {
        setCmServerReachability(false);

        boolean result = underTest.isAffected(stack);

        assertThat(result).isFalse();
    }

    @Test
    void failedFlowState() {
        when(clusterOperationService.updateSalt(stack)).thenReturn(new FlowIdentifier(FlowType.FLOW, POLLABLE_ID));
        setFlowState(true, true);

        assertThatThrownBy(() -> underTest.doApply(stack))
                .isInstanceOf(ExistingStackPatchApplyException.class)
                .hasMessageStartingWith("Failed to update salt for stack");
    }

    @Test
    void allHostsSucceedToApplyAndInternalCrnWithAccountIdIsTheActor() throws ExistingStackPatchApplyException {
        AtomicReference<String> actor = new AtomicReference<>();
        when(clusterOperationService.updateSalt(stack)).then(invocation -> {
            actor.set(ThreadBasedUserCrnProvider.getUserCrn());
            return new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        });
        setFlowState(true, false);

        underTest.doApply(stack);

        assertThat(actor.get()).isEqualTo(INTERNAL_CRN_WITH_ACCOUNT_ID);
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
