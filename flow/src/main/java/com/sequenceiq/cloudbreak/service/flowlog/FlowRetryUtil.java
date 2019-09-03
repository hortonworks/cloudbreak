package com.sequenceiq.cloudbreak.service.flowlog;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

public class FlowRetryUtil {

    private FlowRetryUtil() {
    }

    public static FlowLog getLastSuccessfulStateLog(String failedState, List<FlowLog> flowLogs) {
        List<FlowLog> reversedOrderedFlowLog = flowLogs.stream()
                .sorted(Comparator.comparing(FlowLog::getCreated).reversed())
                .collect(Collectors.toList());
        for (int i = 0; i < reversedOrderedFlowLog.size(); i++) {
            if (failedState.equals(reversedOrderedFlowLog.get(i).getCurrentState())) {
                if (reversedOrderedFlowLog.size() < (i + 1)) {
                    return reversedOrderedFlowLog.get(i);
                } else {
                    return reversedOrderedFlowLog.get(i + 1);
                }
            }
        }
        return reversedOrderedFlowLog.get(0);
    }

    public static Optional<FlowLog> getMostRecentFailedLog(List<FlowLog> flowLogs) {
        return flowLogs.stream()
                .filter(log -> StateStatus.FAILED.equals(log.getStateStatus())).max(Comparator.comparing(FlowLog::getCreated));
    }

    public static boolean isFlowPending(List<FlowLog> flowLogs) {
        return flowLogs.stream()
                .anyMatch(fl -> StateStatus.PENDING.equals(fl.getStateStatus()));
    }
}
