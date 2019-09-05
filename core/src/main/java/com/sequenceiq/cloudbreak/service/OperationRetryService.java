package com.sequenceiq.cloudbreak.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.StateStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;

@Service
public class OperationRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationRetryService.class);

    private static final List<Status> RETRYABLE_FAIL_STATUSES = List.of(Status.CREATE_FAILED, Status.UPDATE_FAILED);

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private FlowLogRepository flowLogRepository;

    public void retry(Stack stack) {
        List<FlowLog> flowLogs = flowLogRepository.findAllByStackIdOrderByCreatedDesc(stack.getId());
        if (isFlowPending(flowLogs)) {
            LOGGER.info("Retry cannot be performed, because there is already an active flow. stackId: {}", stack.getId());
            throw new BadRequestException("Retry cannot be performed, because there is already an active flow.");
        }

        Optional<FlowLog> failedFlowLog = getMostRecentFailedLog(flowLogs);
        failedFlowLog.map(log -> getLastSuccessfulStateLog(log.getCurrentState(), flowLogs))
                .ifPresent(flow2Handler::restartFlow);
    }

    private FlowLog getLastSuccessfulStateLog(String failedState, List<FlowLog> flowLogs) {
        Optional<FlowLog> firstFailedLogOfState = flowLogs.stream()
                .sorted(Comparator.comparing(FlowLog::getCreated))
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

    private boolean isFlowPending(List<FlowLog> flowLogs) {
        return flowLogs.stream()
                .anyMatch(fl -> StateStatus.PENDING.equals(fl.getStateStatus()));
    }
}
