package com.sequenceiq.cloudbreak.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.StateStatus;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.retry.RetryableFlow;
import com.sequenceiq.cloudbreak.retry.RetryableFlow.RetryableFlowBuilder;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Service
public class OperationRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationRetryService.class);

    private static final int INDEX_BEFORE_FINISHED_STATE = 1;

    private static final int LAST_FIFTY_FLOWLOGS = 50;

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Resource
    private List<String> failHandledEvents;

    @Resource
    private List<FlowConfiguration<?>> flowConfigs;

    public void retry(Long stackId) {
        if (isFlowPending(stackId)) {
            LOGGER.info("Retry cannot be performed, because there is already an active flow. stackId: {}", stackId);
            throw new BadRequestException("Retry cannot be performed, because there is already an active flow.");
        }

        List<FlowLog> flowLogs = flowLogRepository.findAllByStackIdOrderByCreatedDesc(stackId, PageRequest.of(0, LAST_FIFTY_FLOWLOGS));
        List<RetryableFlow> retryableFlows = getRetryableFlows(flowLogs);
        if (CollectionUtils.isEmpty(retryableFlows)) {
            LOGGER.info("Retry cannot be performed. The last flow did not fail or not retryable. stackId: {}", stackId);
            throw new BadRequestException("Retry cannot be performed. The last flow did not fail or not retryable.");
        }

        String name = retryableFlows.get(0).getName();
        String statusDesc = cloudbreakMessagesService.getMessage(Msg.RETRY_FLOW_START.code(), List.of(name));
        LOGGER.info(statusDesc);
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), statusDesc);

        Optional<FlowLog> failedFlowLog = getMostRecentFailedLog(flowLogs);
        failedFlowLog.map(log -> getLastSuccessfulStateLog(log.getCurrentState(), flowLogs))
                .ifPresent(flow2Handler::restartFlow);
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
        List<FlowLog> flowLogs = flowLogRepository.findAllByStackIdOrderByCreatedDesc(stackId, PageRequest.of(0, LAST_FIFTY_FLOWLOGS));
        return getRetryableFlows(flowLogs);
    }

    private List<RetryableFlow> getRetryableFlows(List<FlowLog> flowLogs) {
        return Optional.ofNullable(flowLogs.get(INDEX_BEFORE_FINISHED_STATE))
                .filter(log -> failHandledEvents.contains(log.getNextEvent()))
                .map(toRetryableFlow())
                .map(List::of)
                .orElse(List.of());
    }

    private Function<FlowLog, RetryableFlow> toRetryableFlow() {
        return flowLog -> flowConfigs.stream()
                .filter(fc -> fc.getClass().equals(flowLog.getFlowType())).findFirst()
                .map(FlowConfiguration::getDisplayName)
                .map(displayName -> RetryableFlowBuilder.builder()
                        .setName(displayName)
                        .setFailDate(flowLog.getCreated())
                        .build()).get();
    }

    public enum Msg {
        RETRY_FLOW_START("retry.flow.start");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }
}
