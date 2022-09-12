package com.sequenceiq.cloudbreak.service.flowlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

class FlowLogUtilTest {

    @Test
    void getLastSuccessfulStateLog() {
        List<FlowLog> flowLogs = new ArrayList<>();

        FlowLog successfulFlowLog1 = new FlowLog();
        successfulFlowLog1.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog1.setNextEvent("NEXT_EVENT1");
        successfulFlowLog1.setCreated(1L);
        successfulFlowLog1.setCurrentState("CURRENT_STATE1");
        flowLogs.add(successfulFlowLog1);

        FlowLog successfulFlowLog3 = new FlowLog();
        successfulFlowLog3.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog3.setNextEvent("NEXT_EVENT3");
        successfulFlowLog3.setCreated(3L);
        successfulFlowLog3.setCurrentState("CURRENT_STATE3");
        flowLogs.add(successfulFlowLog3);

        FlowLog successfulFlowLog2 = new FlowLog();
        successfulFlowLog2.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog2.setNextEvent("NEXT_EVENT1");
        successfulFlowLog2.setCreated(2L);
        successfulFlowLog2.setCurrentState("CURRENT_STATE1");
        flowLogs.add(successfulFlowLog2);

        FlowLog failedFlowLog = new FlowLog();
        failedFlowLog.setStateStatus(StateStatus.FAILED);
        failedFlowLog.setCurrentState("FAILED_CURRENT_STATE");
        failedFlowLog.setCreated(4L);
        flowLogs.add(failedFlowLog);

        FlowLog lastSuccessfulFlowLog = FlowLogUtil.getLastSuccessfulStateLog("FAILED_CURRENT_STATE", flowLogs);
        assertEquals(successfulFlowLog3, lastSuccessfulFlowLog);
    }

    @Test
    void getMostRecentFailedLog() {
        List<FlowLog> flowLogs = new ArrayList<>();

        FlowLog successfulFlowLog1 = new FlowLog();
        successfulFlowLog1.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog1.setNextEvent("NEXT_EVENT1");
        successfulFlowLog1.setCreated(1L);
        successfulFlowLog1.setCurrentState("CURRENT_STATE1");
        flowLogs.add(successfulFlowLog1);

        FlowLog failedFlowLog2 = new FlowLog();
        failedFlowLog2.setStateStatus(StateStatus.FAILED);
        failedFlowLog2.setCurrentState("FAILED_CURRENT_STATE");
        failedFlowLog2.setCreated(5L);
        flowLogs.add(failedFlowLog2);

        FlowLog successfulFlowLog4 = new FlowLog();
        successfulFlowLog4.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog4.setNextEvent("NEXT_EVENT3");
        successfulFlowLog4.setCreated(4L);
        successfulFlowLog4.setCurrentState("CURRENT_STATE3");
        flowLogs.add(successfulFlowLog4);

        FlowLog successfulFlowLog3 = new FlowLog();
        successfulFlowLog3.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog3.setNextEvent("NEXT_EVENT1");
        successfulFlowLog3.setCreated(3L);
        successfulFlowLog3.setCurrentState("CURRENT_STATE1");
        flowLogs.add(successfulFlowLog3);

        FlowLog failedFlowLog = new FlowLog();
        failedFlowLog.setStateStatus(StateStatus.FAILED);
        failedFlowLog.setCurrentState("FAILED_CURRENT_STATE");
        failedFlowLog.setCreated(2L);
        flowLogs.add(failedFlowLog);

        Optional<FlowLog> mostRecentFailedLog = FlowLogUtil.getMostRecentFailedLog(flowLogs);
        assertEquals(mostRecentFailedLog.get(), failedFlowLog2);
    }

    @Test
    void isFlowPending() {
        List<FlowLog> flowLogs = new ArrayList<>();

        FlowLog successfulFlowLog1 = new FlowLog();
        successfulFlowLog1.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog1.setNextEvent("NEXT_EVENT1");
        successfulFlowLog1.setCreated(1L);
        successfulFlowLog1.setCurrentState("CURRENT_STATE1");
        flowLogs.add(successfulFlowLog1);

        FlowLog failedFlowLog2 = new FlowLog();
        failedFlowLog2.setStateStatus(StateStatus.FAILED);
        failedFlowLog2.setCurrentState("FAILED_CURRENT_STATE2");
        failedFlowLog2.setCreated(5L);
        flowLogs.add(failedFlowLog2);

        FlowLog successfulFlowLog4 = new FlowLog();
        successfulFlowLog4.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog4.setNextEvent("NEXT_EVENT3");
        successfulFlowLog4.setCreated(4L);
        successfulFlowLog4.setCurrentState("CURRENT_STATE3");
        flowLogs.add(successfulFlowLog4);

        FlowLog successfulFlowLog3 = new FlowLog();
        successfulFlowLog3.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog3.setNextEvent("NEXT_EVENT1");
        successfulFlowLog3.setCreated(3L);
        successfulFlowLog3.setCurrentState("CURRENT_STATE1");
        flowLogs.add(successfulFlowLog3);

        FlowLog failedFlowLog = new FlowLog();
        failedFlowLog.setStateStatus(StateStatus.FAILED);
        failedFlowLog.setCurrentState("FAILED_CURRENT_STATE1");
        failedFlowLog.setCreated(2L);
        flowLogs.add(failedFlowLog);

        FlowLog pendingFlowLog = new FlowLog();
        pendingFlowLog.setStateStatus(StateStatus.PENDING);
        pendingFlowLog.setCurrentState("PENDING_CURRENT_STATE");
        pendingFlowLog.setCreated(6L);
        flowLogs.add(pendingFlowLog);

        assertTrue(FlowLogUtil.getPendingFlowLog(flowLogs).isPresent());
    }

    @Test
    void isFlowPendingButNoPending() {
        List<FlowLog> flowLogs = new ArrayList<>();

        FlowLog successfulFlowLog1 = new FlowLog();
        successfulFlowLog1.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog1.setNextEvent("NEXT_EVENT1");
        successfulFlowLog1.setCreated(1L);
        successfulFlowLog1.setCurrentState("CURRENT_STATE1");
        flowLogs.add(successfulFlowLog1);

        FlowLog failedFlowLog2 = new FlowLog();
        failedFlowLog2.setStateStatus(StateStatus.FAILED);
        failedFlowLog2.setCurrentState("FAILED_CURRENT_STATE2");
        failedFlowLog2.setCreated(5L);
        flowLogs.add(failedFlowLog2);

        FlowLog successfulFlowLog4 = new FlowLog();
        successfulFlowLog4.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog4.setNextEvent("NEXT_EVENT3");
        successfulFlowLog4.setCreated(4L);
        successfulFlowLog4.setCurrentState("CURRENT_STATE3");
        flowLogs.add(successfulFlowLog4);

        FlowLog successfulFlowLog3 = new FlowLog();
        successfulFlowLog3.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog3.setNextEvent("NEXT_EVENT1");
        successfulFlowLog3.setCreated(3L);
        successfulFlowLog3.setCurrentState("CURRENT_STATE1");
        flowLogs.add(successfulFlowLog3);

        FlowLog failedFlowLog = new FlowLog();
        failedFlowLog.setStateStatus(StateStatus.FAILED);
        failedFlowLog.setCurrentState("FAILED_CURRENT_STATE1");
        failedFlowLog.setCreated(2L);
        flowLogs.add(failedFlowLog);

        assertFalse(FlowLogUtil.getPendingFlowLog(flowLogs).isPresent());
    }

    @Test
    void testTryDeserializeTriggerEvent() {
        FlowChainLog flowChainLog = getFlowChainLog();
        Payload payload = FlowLogUtil.tryDeserializeTriggerEvent(flowChainLog);
        assertEquals("triggerEventJacksonValue", ((SimplePayload) payload).getKey());
    }

    @Test
    void testTryDeserializeTriggerEventAllNull() {
        FlowChainLog flowChainLog = new FlowChainLog("type", "id", "parentId", null, "userCrn", null);
        Payload payload = FlowLogUtil.tryDeserializeTriggerEvent(flowChainLog);
        assertNull(payload);
    }

    @Test
    void testTryDeserializeTriggerEventContainsGarbage() {
        FlowChainLog flowChainLog = new FlowChainLog("type", "id", "parentId", "garbage", "userCrn", "garbage");
        Payload payload = FlowLogUtil.tryDeserializeTriggerEvent(flowChainLog);
        assertNull(payload);
    }

    private static FlowChainLog getFlowChainLog() {
        return new FlowChainLog("type", "id", "parentId", "any", "userCrn",
                "{\"@type\":\"" + SimplePayload.class.getName() + "\",\"key\":\"triggerEventJacksonValue\"}");
    }

    @Test
    void testTryDeserializePayload() {
        FlowLog flowLog = getFlowLog();
        Payload payload = FlowLogUtil.tryDeserializePayload(flowLog);
        assertEquals("payloadJacksonValue", ((SimplePayload) payload).getKey());
    }

    @Test
    void testTryDeserializePayloadAllNull() {
        FlowLog flowLog = new FlowLog(123L, "flowId", "chainId", "userCrn", "nextEvent", null,
                com.sequenceiq.flow.domain.ClassValue.of(SimplePayload.class), null,
                com.sequenceiq.flow.domain.ClassValue.ofUnknown("unknown"), "currentState");
        Payload payload = FlowLogUtil.tryDeserializePayload(flowLog);
        assertNull(payload);
    }

    @Test
    void testTryDeserializePayloadContainsGarbage() {
        FlowLog flowLog = new FlowLog(123L, "flowId", "chainId", "userCrn", "nextEvent",
                "garbage",
                com.sequenceiq.flow.domain.ClassValue.of(SimplePayload.class),
                "garbage",
                com.sequenceiq.flow.domain.ClassValue.ofUnknown("unknown"), "currentState");
        Payload payload = FlowLogUtil.tryDeserializePayload(flowLog);
        assertNull(payload);
    }

    private static FlowLog getFlowLog() {
        return new FlowLog(123L, "flowId", "chainId", "userCrn", "nextEvent",
                "{\"@type\":\"" + SimplePayload.class.getName() + "\",\"key\":\"payloadJacksonValue\"}",
                com.sequenceiq.flow.domain.ClassValue.of(SimplePayload.class),
                "any",
                com.sequenceiq.flow.domain.ClassValue.ofUnknown("unknown"), "currentState");
    }

    static class SimplePayload implements Payload {

        private final String key;

        @JsonCreator
        SimplePayload(@JsonProperty("key") String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        @Override
        public Long getResourceId() {
            return 123L;
        }

        @Override
        public String toString() {
            return "Simple{" +
                    "key='" + key + '\'' +
                    '}';
        }
    }
}
