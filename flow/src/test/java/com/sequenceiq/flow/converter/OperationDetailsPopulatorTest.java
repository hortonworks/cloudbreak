package com.sequenceiq.flow.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.FlowStateTransitionResponse;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationProgressStatus;
import com.sequenceiq.flow.api.model.operation.OperationResource;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.api.model.operation.OperationView;

@ExtendWith(MockitoExtension.class)
public class OperationDetailsPopulatorTest {

    private static final int DEFAULT_PROGRESS = -1;

    private static final int MAX_PROGRESS = 100;

    private static final int IN_PROGRESS = 66;

    private static final String DUMMY_CLASS = "Sample";

    private OperationDetailsPopulator underTest;

    @BeforeEach
    public void setUp() {
        underTest = new OperationDetailsPopulator();
    }

    @Test
    public void testCreateOperationView() {
        // GIVEN
        Map<String, FlowProgressResponse> flowProgressResponseMap = new HashMap<>();
        flowProgressResponseMap.put(DUMMY_CLASS, createFlowProgressResponse("SUCCESSFUL", true).get());
        OperationFlowsView operationFlowsView = createOperationFlowsView(flowProgressResponseMap);
        // WHEN
        OperationView operationView = underTest.createOperationView(operationFlowsView, OperationResource.ENVIRONMENT);
        // THEN
        assertEquals(MAX_PROGRESS, operationView.getProgress());
        assertEquals(OperationProgressStatus.FINISHED, operationView.getProgressStatus());
    }

    @Test
    public void testCreateOperationViewWithFailedState() {
        // GIVEN
        Map<String, FlowProgressResponse> flowProgressResponseMap = new HashMap<>();
        flowProgressResponseMap.put(DUMMY_CLASS, createFlowProgressResponse("FAILED", true).get());
        OperationFlowsView operationFlowsView = createOperationFlowsView(flowProgressResponseMap);
        // WHEN
        OperationView operationView = underTest.createOperationView(operationFlowsView, OperationResource.ENVIRONMENT);
        // THEN
        assertEquals(MAX_PROGRESS, operationView.getProgress());
        assertEquals(OperationProgressStatus.FAILED, operationView.getProgressStatus());
    }

    @Test
    public void testCreateOperationViewWithPendingState() {
        // GIVEN
        Map<String, FlowProgressResponse> flowProgressResponseMap = new HashMap<>();
        flowProgressResponseMap.put(DUMMY_CLASS, createFlowProgressResponse("SUCCESSFUL", false).get());
        OperationFlowsView operationFlowsView = createOperationFlowsView(flowProgressResponseMap);
        // WHEN
        OperationView operationView = underTest.createOperationView(operationFlowsView, OperationResource.ENVIRONMENT);
        // THEN
        assertEquals(IN_PROGRESS, operationView.getProgress());
        assertEquals(OperationProgressStatus.RUNNING, operationView.getProgressStatus());
    }

    @Test
    public void testCreateOperationViewWithoutFlows() {
        // GIVEN
        OperationFlowsView operationFlowsView = createOperationFlowsView(new HashMap<>());
        // WHEN
        OperationView operationView = underTest.createOperationView(operationFlowsView, OperationResource.ENVIRONMENT);
        // THEN
        assertEquals(DEFAULT_PROGRESS, operationView.getProgress());
        assertEquals(OperationProgressStatus.UNKNOWN, operationView.getProgressStatus());
    }

    private OperationFlowsView createOperationFlowsView(Map<String, FlowProgressResponse> flowProgressResponseMap) {
        return createOperationFlowsView(flowProgressResponseMap, OperationType.PROVISION);
    }

    private OperationFlowsView createOperationFlowsView(Map<String, FlowProgressResponse> flowProgressResponseMap, OperationType operationType) {
        return OperationFlowsView.Builder.newBuilder()
                .withOperationType(operationType)
                .withFlowTypeProgressMap(flowProgressResponseMap)
                .withTypeOrderList(List.of(DUMMY_CLASS))
                .build();
    }

    private Optional<FlowProgressResponse> createFlowProgressResponse(String secondTransitionState, boolean finished) {
        FlowProgressResponse response = new FlowProgressResponse();
        List<FlowStateTransitionResponse> transitionResponseList = new ArrayList<>();
        FlowStateTransitionResponse transitionResponse1 = new FlowStateTransitionResponse();
        transitionResponse1.setState("state1");
        transitionResponse1.setNextEvent("nextEvent1");
        transitionResponse1.setStatus("SUCCESSFUL");
        FlowStateTransitionResponse transitionResponse2 = new FlowStateTransitionResponse();
        transitionResponse2.setStatus("state2");
        transitionResponse2.setNextEvent("nextEvent2");
        transitionResponse2.setStatus(secondTransitionState);
        transitionResponseList.add(transitionResponse1);
        transitionResponseList.add(transitionResponse2);
        response.setTransitions(transitionResponseList);
        if (finished) {
            response.setProgress(MAX_PROGRESS);
            response.setFinalized(false);
            response.setMaxNumberOfTransitions(transitionResponseList.size());
        } else {
            response.setProgress(IN_PROGRESS);
            response.setFinalized(true);
            response.setMaxNumberOfTransitions(transitionResponseList.size() + 1);
        }
        return Optional.of(response);
    }
}
