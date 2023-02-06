package com.sequenceiq.cloudbreak.service.flowlog;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.domain.StateStatus;

public class FlowLogUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowLogUtil.class);

    private FlowLogUtil() {
    }

    public static Optional<FlowLog> getFirstStateLog(List<FlowLog> flowLogs) {
        return flowLogs.stream().min(Comparator.comparing(FlowLog::getCreated));
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

    public static boolean isFlowInFailedState(List<FlowLogWithoutPayload> flowLogs, Set<String> failHandledEvents) {
        if (flowLogs.size() > 2) {
            FlowLogWithoutPayload lastFlowLog = flowLogs.get(0);
            FlowLogWithoutPayload secondLastFlowLog = flowLogs.get(1);
            LOGGER.debug("Last two log items: {}, {}", lastFlowLog.minimizedString(), secondLastFlowLog.minimizedString());
            return lastFlowLog.getFinalized()
                    && (failHandledEvents.contains(secondLastFlowLog.getNextEvent())
                    || StateStatus.FAILED.equals(secondLastFlowLog.getStateStatus()));
        }
        return false;
    }

    public static Payload tryDeserializeTriggerEvent(FlowChainLog flowChainLog) {
        return tryDeserialize(flowChainLog.getTriggerEventJackson(), Payload.class, false);
    }

    public static Payload tryDeserializePayload(FlowLog flowLog) {
        return tryDeserialize(flowLog.getPayloadJackson(), Payload.class, false);
    }

    public static Payload deserializePayload(FlowLog flowLog) {
        return deserialize(flowLog.getPayloadJackson(), Payload.class, false);
    }

    public static Map<Object, Object> deserializeVariables(FlowLog flowLog) {
        return (Map<Object, Object>) deserialize(flowLog.getVariablesJackson(), Map.class, true);
    }

    public static Map<Object, Object> tryDeserializeVariables(FlowLog flowLog) {
        return (Map<Object, Object>) tryDeserialize(flowLog.getVariablesJackson(), Map.class, true);
    }

    private static <T> T tryDeserialize(String json, Class<T> type, boolean useTypedJsonReader) {
        if (null == json) {
            return null;
        }
        try {
            return deserialize(json, type, useTypedJsonReader);
        } catch (Exception e) {
            LOGGER.warn("Deserialization failed {}", e.getMessage(), e);
            return null;
        }
    }

    private static <T> T deserialize(String json, Class<T> type, boolean useTypedJsonReader) {
        if (useTypedJsonReader) {
            return TypedJsonUtil.readValueUnchecked(json, type);
        } else {
            return JsonUtil.readValueUnchecked(json, type);
        }
    }
}
