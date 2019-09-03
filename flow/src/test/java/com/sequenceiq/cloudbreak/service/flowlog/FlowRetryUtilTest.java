package com.sequenceiq.cloudbreak.service.flowlog;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

public class FlowRetryUtilTest {

    @Test
    public void getLastSuccessfulStateLog() {
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

        FlowLog lastSuccessfulFlowLog = FlowRetryUtil.getLastSuccessfulStateLog("FAILED_CURRENT_STATE", flowLogs);
        assertEquals(successfulFlowLog3, lastSuccessfulFlowLog);
    }

    @Test
    public void getMostRecentFailedLog() {
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

        Optional<FlowLog> mostRecentFailedLog = FlowRetryUtil.getMostRecentFailedLog(flowLogs);
        assertEquals(mostRecentFailedLog.get(), failedFlowLog2);
    }

    @Test
    public void isFlowPending() {
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

        assertTrue(FlowRetryUtil.isFlowPending(flowLogs));
    }

    @Test
    public void isFlowPendingButNoPending() {
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

        assertFalse(FlowRetryUtil.isFlowPending(flowLogs));
    }
}