package com.sequenceiq.flow.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationResource;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.api.model.operation.OperationProgressStatus;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.FlowStateTransitionResponse;
import com.sequenceiq.flow.api.model.StateStatus;

@Component
public class OperationDetailsPopulator {

    private static final int DEFAULT_PROGRESS = -1;

    private static final int MAX_PROGRESS = 100;

    public OperationView createOperationView(OperationFlowsView operationFlowsView,
            OperationResource resource) {
        return createOperationView(operationFlowsView, resource, new ArrayList<>());
    }

    public OperationView createOperationView(OperationFlowsView operationFlowsView,
            OperationResource resource, List<Class<?>> expectedTypeOrder) {
        OperationView response = new OperationView();
        response.setOperationType(operationFlowsView.getOperationType());
        response.setOperationResource(resource);
        response.setOperationId(operationFlowsView.getOperationId());
        List<String> typeOrderList = CollectionUtils.isNotEmpty(expectedTypeOrder)
                ? expectedTypeOrder.stream().map(Class::getCanonicalName).collect(Collectors.toList())
                : operationFlowsView.getTypeOrderList();
        int expectedNumberOfFlows = typeOrderList.size();
        Map<String, FlowProgressResponse> flowTypeProgressMap = operationFlowsView.getFlowTypeProgressMap();
        List<Optional<FlowProgressResponse>> responseListToProcess = new ArrayList<>();
        for (String typeName : typeOrderList) {
            if (flowTypeProgressMap != null && flowTypeProgressMap.containsKey(typeName)) {
                responseListToProcess.add(Optional.of(flowTypeProgressMap.get(typeName)));
            } else {
                responseListToProcess.add(Optional.empty());
            }
        }
        populateOperationDetails(response, responseListToProcess, expectedNumberOfFlows, operationFlowsView.getProgressFromHistory());
        return response;
    }

    private void populateOperationDetails(OperationView source,
            List<Optional<FlowProgressResponse>> operations, int expectedNumberOfFlows, Integer progressFromHistory) {
        int preProgress = 0;
        int overallProgress = 0;
        boolean foundFlow = false;
        boolean expectFlowChainId = operations.size() > 1;
        for (Optional<FlowProgressResponse> operationOpt : operations) {
            if (operationOpt.isPresent()) {
                if (!foundFlow) {
                    overallProgress += preProgress;
                    source.setOperationId(operationOpt.get().getFlowId());
                }
                foundFlow = true;
                FlowProgressResponse flowProgress = operationOpt.get();
                if (flowProgress.getProgress() != DEFAULT_PROGRESS) {
                    overallProgress += flowProgress.getProgress();
                }
                source.getOperations().add(flowProgress);
            } else if (expectFlowChainId && !foundFlow) {
                preProgress += MAX_PROGRESS;
                operationOpt.ifPresent(flowProgressResponse -> source.setOperationId(flowProgressResponse.getFlowChainId()));
            }
        }
        setProgressAndStatus(source, expectedNumberOfFlows, progressFromHistory, overallProgress);
    }

    private void setProgressAndStatus(OperationView source, int expectedNumberOfFlows, Integer progressFromHistory, int overallProgress) {
        if (CollectionUtils.isNotEmpty(source.getOperations())) {
            int fullProgress = overallProgress / expectedNumberOfFlows;
            source.setProgress(fullProgress);
            if (fullProgress == MAX_PROGRESS) {
                source.setProgressStatus(calculateProgress(source.getOperations()));
            } else {
                source.setProgressStatus(OperationProgressStatus.RUNNING);
            }
            if (progressFromHistory != null && progressFromHistory != DEFAULT_PROGRESS) {
                source.setProgress(progressFromHistory);
            }
        } else {
            source.setProgress(DEFAULT_PROGRESS);
            source.setProgressStatus(OperationProgressStatus.UNKNOWN);
        }
    }

    private OperationProgressStatus calculateProgress(List<FlowProgressResponse> provisionFlows) {
        FlowProgressResponse lastFlowProgress = provisionFlows.get(provisionFlows.size() - 1);
        if (lastFlowProgress.getTransitions() == null) {
            return OperationProgressStatus.UNKNOWN;
        }
        OperationProgressStatus progressStatus = OperationProgressStatus.FINISHED;
        boolean hasCancelState = false;
        boolean hasFailedStatus = false;
        boolean hasPendingStatus = false;
        for (FlowStateTransitionResponse fsTransitionResp : lastFlowProgress.getTransitions()) {
            if (OperationProgressStatus.CANCELLED.name().equals(fsTransitionResp.getStatus())) {
                hasCancelState = true;
            } else if (StateStatus.FAILED.name().equals(fsTransitionResp.getStatus())) {
                hasFailedStatus = true;
            } else if (StateStatus.PENDING.name().equals(fsTransitionResp.getStatus())) {
                hasPendingStatus = true;
            }
        }
        if (hasFailedStatus) {
            progressStatus = OperationProgressStatus.FAILED;
        } else if (hasCancelState) {
            progressStatus = OperationProgressStatus.CANCELLED;
        } else if (hasPendingStatus) {
            progressStatus = OperationProgressStatus.RUNNING;
        }
        return progressStatus;
    }

}
