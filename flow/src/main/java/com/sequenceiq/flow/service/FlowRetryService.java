package com.sequenceiq.flow.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.RetryResponse;
import com.sequenceiq.flow.domain.RetryableFlow;
import com.sequenceiq.flow.domain.RetryableStateResponse;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.repository.FlowLogRepository;

@Service
public class FlowRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowRetryService.class);

    private static final int INDEX_BEFORE_FINISHED_STATE = 1;

    private static final int LAST_FIFTY_FLOWLOGS = 50;

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Resource
    private List<String> retryableEvents;

    @Resource
    private List<FlowConfiguration<?>> flowConfigs;

    public Optional<FlowLog> getLastRetryableFailedFlow(Long stackId) {
        return Optional.ofNullable(getRetryableStateResponse(stackId).getLastSuccessfulStateFlowLog());
    }

    private RetryableStateResponse getRetryableStateResponse(Long stackId) {
        if (isFlowPending(stackId)) {
            return RetryableStateResponse.flowPending();
        }
        List<FlowLog> flowLogs = flowLogRepository.findAllByResourceIdOrderByCreatedDesc(stackId, PageRequest.of(0, LAST_FIFTY_FLOWLOGS));
        List<RetryableFlow> retryableFlows = getRetryableFlows(flowLogs);
        if (CollectionUtils.isEmpty(retryableFlows)) {
            String lastKnownState = getLastKnownStateMessage(flowLogs);
            return RetryableStateResponse.lastFlowNotFailedOrNotRetryable(lastKnownState);
        }
        Optional<FlowLog> failedFlowLog = getMostRecentFailedLog(flowLogs);
        Optional<FlowLog> lastSuccessfulStateFlowLog = failedFlowLog.map(log -> getLastSuccessfulStateLog(log.getCurrentState(), flowLogs));
        if (lastSuccessfulStateFlowLog.isPresent()) {
            String name = retryableFlows.get(0).getName();
            return RetryableStateResponse.retryable(name, lastSuccessfulStateFlowLog.get());
        } else {
            return RetryableStateResponse.noSuccessfulState();
        }
    }

    public RetryResponse retry(Long stackId) {
        RetryableStateResponse retryableStateResponse = getRetryableStateResponse(stackId);
        switch (retryableStateResponse.getState()) {
            case FLOW_PENDING:
                String flowPendingMessage = "Retry cannot be performed, because there is already an active flow.";
                LOGGER.info(flowPendingMessage + " stackId: {}", stackId);
                throw new BadRequestException(flowPendingMessage);
            case LAST_NOT_FAILED_OR_NOT_RETRYABLE:
                String notFailedOrNotRetryableMessage = "Retry cannot be performed. The last flow did not fail or not retryable."
                        + retryableStateResponse.getLastKnownStateMessage();
                LOGGER.info(notFailedOrNotRetryableMessage + " stackId: {}", stackId);
                throw new BadRequestException(notFailedOrNotRetryableMessage);
            case NO_SUCCESSFUL_STATE:
                String noSuccessfulStateMessage = "Cannot restart previous flow because there is no successful state in the flow.";
                LOGGER.info(noSuccessfulStateMessage + " stackId: {}", stackId);
                throw new BadRequestException(noSuccessfulStateMessage);
            case RETRYABLE:
                FlowLog lastSuccessfulStateFlowLog = retryableStateResponse.getLastSuccessfulStateFlowLog();
                flow2Handler.restartFlow(lastSuccessfulStateFlowLog);
                if (lastSuccessfulStateFlowLog.getFlowChainId() != null) {
                    return new RetryResponse(retryableStateResponse.getName(),
                            new FlowIdentifier(FlowType.FLOW_CHAIN, lastSuccessfulStateFlowLog.getFlowChainId()));
                } else {
                    return new RetryResponse(retryableStateResponse.getName(),
                            new FlowIdentifier(FlowType.FLOW, lastSuccessfulStateFlowLog.getFlowId()));
                }
            default:
                throw new NotImplementedException("Retry state handling is not implemented: " + retryableStateResponse.getState());
        }
    }

    private String getLastKnownStateMessage(List<FlowLog> flowLogs) {
        String state = "unknown";
        if (!flowLogs.isEmpty()) {
            String currentState = flowLogs.get(0).getCurrentState();
            if (currentState != null) {
                state = currentState;
            }
        }
        return " Last known state: " + state;
    }

    private FlowLog getLastSuccessfulStateLog(String failedState, List<FlowLog> flowLogs) {
        Optional<FlowLog> firstFailedLogOfState = flowLogs.stream()
                .filter(log -> failedState.equals(log.getCurrentState()))
                .findFirst();

        Integer lastSuccessfulStateIndex = firstFailedLogOfState.map(flowLogs::indexOf).map(i -> ++i).orElse(0);
        return flowLogs.get(lastSuccessfulStateIndex);
    }

    private Optional<FlowLog> getMostRecentFailedLog(List<FlowLog> flowLogs) {
        return flowLogs.stream()
                .filter(log -> StateStatus.FAILED.equals(log.getStateStatus()))
                .findFirst();
    }

    private Boolean isFlowPending(Long stackId) {
        return flowLogRepository.findAnyByStackIdAndStateStatus(stackId, StateStatus.PENDING);
    }

    public List<RetryableFlow> getRetryableFlows(Long stackId) {
        if (isFlowPending(stackId)) {
            LOGGER.info("Retry cannot be performed, because there is already an active flow. stackId: {}", stackId);
            return List.of();
        }
        List<FlowLog> flowLogs = flowLogRepository.findAllByResourceIdOrderByCreatedDesc(stackId, PageRequest.of(0, LAST_FIFTY_FLOWLOGS));
        return getRetryableFlows(flowLogs);
    }

    private List<RetryableFlow> getRetryableFlows(List<FlowLog> flowLogs) {
        if (flowLogs.size() > 2) {
            return Optional.ofNullable(flowLogs.get(INDEX_BEFORE_FINISHED_STATE))
                    .filter(log -> retryableEvents.contains(log.getNextEvent()))
                    .map(toRetryableFlow())
                    .map(List::of)
                    .orElse(List.of());
        } else {
            return Collections.emptyList();
        }
    }

    private Function<FlowLog, RetryableFlow> toRetryableFlow() {
        return flowLog -> flowConfigs.stream()
                .filter(fc -> flowLog.isFlowType(fc.getClass())).findFirst()
                .map(FlowConfiguration::getDisplayName)
                .map(displayName -> RetryableFlow.RetryableFlowBuilder.builder()
                        .setName(displayName)
                        .setFailDate(flowLog.getCreated())
                        .build()).get();
    }
}
