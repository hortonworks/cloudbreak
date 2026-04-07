package com.sequenceiq.environment.environment.service.stack;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.exception.StackOperationFailedException;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@ExtendWith(MockitoExtension.class)
class StackServiceTest {

    private static final String USERCRN = CrnTestUtil.getUserCrnBuilder()
            .setAccountId("acc")
            .setResource("user")
            .build().toString();

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String FLOW_ID = "flowId";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private FlowCancelService flowCancelService;

    @Mock
    private FlowLogDBService flowLogDBService;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private FlowEndpoint flowEndpoint;

    @InjectMocks
    private StackService underTest;

    @Test
    void testCheckFlowWhenFlowType() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        assertDoesNotThrow(() -> underTest.checkFlow(flowIdentifier));

        verify(flowEndpoint, times(1)).hasFlowRunningByFlowId(flowIdentifier.getPollableId());
        verify(flowEndpoint, never()).hasFlowRunningByChainId(flowIdentifier.getPollableId());
    }

    @Test
    void testCheckFlowWhenFlowChain() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_ID);

        assertDoesNotThrow(() -> underTest.checkFlow(flowIdentifier));

        verify(flowEndpoint, never()).hasFlowRunningByFlowId(flowIdentifier.getPollableId());
        verify(flowEndpoint, times(1)).hasFlowRunningByChainId(flowIdentifier.getPollableId());
    }

    @Test
    void testCheckFlowWhenNotTriggered() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.NOT_TRIGGERED, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> underTest.checkFlow(flowIdentifier));

        verify(flowEndpoint, never()).hasFlowRunningByFlowId(flowIdentifier.getPollableId());
        verify(flowEndpoint, never()).hasFlowRunningByChainId(flowIdentifier.getPollableId());
        assertEquals("Stack flow is not triggered", ex.getMessage());
    }

    @Test
    void modifyUserDefinedTags() {
        String resourceCrn = "resourceCrn";
        Map<String, String> userDefinedTags = Map.of("owner", "john doe");

        ThreadBasedUserCrnProvider.doAs(USERCRN, () -> underTest.triggerUserDefinedTagsUpdate(resourceCrn, userDefinedTags));

        verify(stackV4Endpoint).triggerUserDefinedTagsUpdateInternal(0L, resourceCrn, userDefinedTags);
    }

    @Test
    void modifyUserDefinedTagsFailureTest() {
        String resourceCrn = "resourceCrn";
        Map<String, String> userDefinedTags = Map.of("owner", "john doe");
        when(stackV4Endpoint.triggerUserDefinedTagsUpdateInternal(0L, resourceCrn, userDefinedTags)).thenThrow(new WebApplicationException("Error"));
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("custom error");
        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USERCRN, () -> underTest.triggerUserDefinedTagsUpdate(resourceCrn, userDefinedTags)))
                .hasMessage("custom error")
                .isExactlyInstanceOf(StackOperationFailedException.class);
    }

    @Test
    void testGetAllNotDeletedClustersByEnvironmentCrn() {
        StackViewV4Response stack1 = new StackViewV4Response();
        stack1.setName("stack1");
        ClusterViewV4Response cluster1 = new ClusterViewV4Response();
        stack1.setCluster(cluster1);
        cluster1.setStatus(Status.DELETE_FAILED);
        StackViewV4Response stack2 = new StackViewV4Response();
        stack2.setName("stack2");
        ClusterViewV4Response cluster2 = new ClusterViewV4Response();
        stack2.setCluster(cluster2);
        cluster2.setStatus(Status.AVAILABLE);
        StackViewV4Response stack3 = new StackViewV4Response();
        stack3.setName("stack3");
        ClusterViewV4Response cluster3 = new ClusterViewV4Response();
        stack3.setCluster(cluster3);
        cluster3.setStatus(Status.DELETE_COMPLETED);
        StackViewV4Response stack4 = new StackViewV4Response();
        stack4.setName("stack4");
        ClusterViewV4Response cluster4 = new ClusterViewV4Response();
        stack4.setCluster(cluster4);
        cluster4.setStatus(Status.DELETED_ON_PROVIDER_SIDE);
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses();
        stackViewV4Responses.setResponses(List.of(stack1, stack2, stack3, stack4));

        when(stackV4Endpoint.list(0L, ENVIRONMENT_CRN, false)).thenReturn(stackViewV4Responses);

        List<StackViewV4Response> response = underTest.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN);

        assertEquals(1, response.size());
        assertEquals("stack2", response.getFirst().getName());
    }

}