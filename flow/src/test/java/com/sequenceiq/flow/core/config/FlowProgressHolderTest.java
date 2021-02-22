package com.sequenceiq.flow.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.FlowConstants;

public class FlowProgressHolderTest {

    private static final int EXPECTED_TEST_STATE_PROGRESS = 50;

    private static final int EXPECTED_INIT_STATE_PROGRESS = 0;

    private static final int EXPECTED_COMPLETED_STATE_PROGRESS = 100;

    private static final int EXPECTED_UNKNOWN_STATE_PROGRESS = -1;

    private FlowProgressHolder underTest;

    @BeforeEach
    public void setUp() {
        underTest = new FlowProgressHolder(createFlowConfigs());
        underTest.init();
    }

    @Test
    public void testGetProgressPercentageForState() {
        // GIVEN
        // WHEN
        int testStateResult = underTest.getProgressPercentageForState(TestFlowConfig.class, TestFlowConfig.TestFlowState.TEST_STATE.name());
        // THEN
        assertEquals(EXPECTED_TEST_STATE_PROGRESS, testStateResult);
    }

    @Test
    public void testGetProgressPercentageForInitState() {
        // GIVEN
        // WHEN
        int initResult = underTest.getProgressPercentageForState(TestFlowConfig.class, TestFlowConfig.TestFlowState.INIT_STATE.name());
        // THEN
        assertEquals(EXPECTED_INIT_STATE_PROGRESS, initResult);
    }

    @Test
    public void testGetProgressPercentageForFinalState() {
        // GIVEN
        // WHEN
        int finalResult = underTest.getProgressPercentageForState(TestFlowConfig.class, TestFlowConfig.TestFlowState.FINAL_STATE.name());
        // THEN
        assertEquals(EXPECTED_COMPLETED_STATE_PROGRESS, finalResult);
    }

    @Test
    public void testGetProgressPercentageForCancelledState() {
        // GIVEN
        // WHEN
        int cancelledResult = underTest.getProgressPercentageForState(TestFlowConfig.class, FlowConstants.CANCELLED_STATE);
        // THEN
        assertEquals(EXPECTED_COMPLETED_STATE_PROGRESS, cancelledResult);
    }

    @Test
    public void testGetProgressPercentageForUnknownState() {
        // GIVEN
        // WHEN
        int unknownResult = underTest.getProgressPercentageForState(TestFlowConfig.class, "UnknownState");
        // THEN
        assertEquals(EXPECTED_UNKNOWN_STATE_PROGRESS, unknownResult);
    }

    @Test
    public void testGetProgressPercentageForStateWithUnknownClass() {
        // GIVEN
        // WHEN
        int unknownClassResult = underTest.getProgressPercentageForState("UnknownFlowConfigClass", TestFlowConfig.TestFlowState.TEST_STATE.name());
        // THEN
        assertEquals(EXPECTED_UNKNOWN_STATE_PROGRESS, unknownClassResult);
    }

    private List<? extends AbstractFlowConfiguration> createFlowConfigs() {
        List<AbstractFlowConfiguration> flowConfigs = new ArrayList<>();
        flowConfigs.add(new TestFlowConfig());
        return flowConfigs;
    }

}
