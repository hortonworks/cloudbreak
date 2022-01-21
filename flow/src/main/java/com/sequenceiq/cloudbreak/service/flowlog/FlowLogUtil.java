package com.sequenceiq.cloudbreak.service.flowlog;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cedarsoftware.util.io.JsonReader;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

public class FlowLogUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowLogUtil.class);

    private FlowLogUtil() {
    }

    public static FlowLog getFirstStateLog(List<FlowLog> flowLogs) {
        List<FlowLog> reversedOrderedFlowLog = flowLogs.stream()
                .sorted(Comparator.comparing(FlowLog::getCreated))
                .collect(Collectors.toList());
        return reversedOrderedFlowLog.get(0);
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

    public static Optional<FlowLog> getPendingFlowLog(List<FlowLog> flowLogs) {
        return flowLogs.stream()
                .filter(fl -> StateStatus.PENDING.equals(fl.getStateStatus()))
                .findFirst();
    }

    public static boolean isFlowFailHandled(List<FlowLog> flowLogs, Set<String> failHandledEvents) {
        if (flowLogs.size() > 2) {
            FlowLog lastFlowLog = flowLogs.get(0);
            FlowLog secondLastFlowLog = flowLogs.get(1);
            LOGGER.debug("Last two log items: {}, {}", lastFlowLog, secondLastFlowLog);
            return lastFlowLog.getFinalized()
                    && failHandledEvents.contains(secondLastFlowLog.getNextEvent());
        }
        return false;
    }

    public static Payload tryDeserializeTriggerEvent(FlowChainLog flowChainLog) {
        if (null == flowChainLog.getTriggerEvent()) {
            return null;
        } else {
            try {
                return (Payload) JsonReader.jsonToJava(flowChainLog.getTriggerEvent());
            } catch (Exception exception) {
                LOGGER.warn("Couldn't deserialize trigger event from flow chain log {}", flowChainLog);
                return null;
            }
        }
    }

    public static Payload tryDeserializePayload(FlowLog flowLog) {
        if (null == flowLog.getPayload()) {
            return null;
        } else {
            try {
                return (Payload) JsonReader.jsonToJava(flowLog.getPayload());
            } catch (Exception exception) {
                LOGGER.warn("Couldn't deserialize payload from flow log {}", flowLog);
                return null;
            }
        }
    }
}
