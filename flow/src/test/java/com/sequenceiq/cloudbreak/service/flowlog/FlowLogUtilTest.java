package com.sequenceiq.cloudbreak.service.flowlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
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
    void testIsFlowInFailedState() {
        List<FlowLogWithoutPayload> flowLogs = new ArrayList<>();

        FlowLogWithoutPayload failedFlowLog = mock(FlowLogWithoutPayload.class);
        when(failedFlowLog.getStateStatus()).thenReturn(StateStatus.FAILED);
        when(failedFlowLog.getCurrentState()).thenReturn("FAILED_CURRENT_STATE1");
        when(failedFlowLog.getCreated()).thenReturn(10L);
        when(failedFlowLog.getFinalized()).thenReturn(true);
        flowLogs.add(failedFlowLog);

        FlowLogWithoutPayload successfulFlowLog1 = mock(FlowLogWithoutPayload.class);
        when(successfulFlowLog1.getStateStatus()).thenReturn(StateStatus.SUCCESSFUL);
        when(successfulFlowLog1.getNextEvent()).thenReturn("NEXT_EVENT1");
        when(successfulFlowLog1.getCreated()).thenReturn(3L);
        when(successfulFlowLog1.getCurrentState()).thenReturn("CURRENT_STATE1");
        flowLogs.add(successfulFlowLog1);

        FlowLogWithoutPayload successfulFlowLog2 = mock(FlowLogWithoutPayload.class);
        when(successfulFlowLog2.getStateStatus()).thenReturn(StateStatus.SUCCESSFUL);
        when(successfulFlowLog2.getNextEvent()).thenReturn("NEXT_EVENT2");
        when(successfulFlowLog2.getCreated()).thenReturn(9L);
        when(successfulFlowLog2.getCurrentState()).thenReturn("CURRENT_STATE2");
        flowLogs.add(successfulFlowLog2);

        FlowLogWithoutPayload successfulFlowLog3 = mock(FlowLogWithoutPayload.class);
        when(successfulFlowLog3.getStateStatus()).thenReturn(StateStatus.SUCCESSFUL);
        when(successfulFlowLog3.getNextEvent()).thenReturn("NEXT_EVENT3");
        when(successfulFlowLog3.getCreated()).thenReturn(4L);
        when(successfulFlowLog3.getCurrentState()).thenReturn("CURRENT_STATE3");
        flowLogs.add(successfulFlowLog3);

        assertTrue(FlowLogUtil.isFlowInFailedState(flowLogs, Set.of("FAILED_HANDLED_EVENT")));
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
