package com.sequenceiq.flow.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.core.config.FlowProgressHolder;
import com.sequenceiq.flow.core.config.TestFlowConfig;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

@ExtendWith(MockitoExtension.class)
public class FlowProgressResponseConverterTest {

    private static final String DUMMY_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private FlowProgressResponseConverter underTest;

    @Mock
    private FlowProgressHolder flowProgressHolder;

    @BeforeEach
    public void setUp() {
        underTest = new FlowProgressResponseConverter(flowProgressHolder);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(flowProgressHolder.getProgressPercentageForState(TestFlowConfig.class.getCanonicalName(), "FINISHED")).willReturn(100);
        // WHEN
        FlowProgressResponse response = underTest.convert(createFlowLogs(false), DUMMY_CRN);
        // THEN
        assertEquals("flow1", response.getFlowId());
        assertEquals(2, response.getTransitions().size());
        assertEquals(0.001, response.getElapsedTimeInSeconds());
        assertEquals(100, response.getProgress());
    }

    @Test
    public void testConvertList() {
        // GIVEN
        given(flowProgressHolder.getProgressPercentageForState(TestFlowConfig.class.getCanonicalName(), "FINISHED")).willReturn(100);
        given(flowProgressHolder.getProgressPercentageForState(TestFlowConfig.class.getCanonicalName(), "CANCELLED")).willReturn(100);
        // WHEN
        List<FlowProgressResponse> responses = underTest.convertList(createFlowLogs(true), DUMMY_CRN);
        FlowProgressResponse latestResponse = responses.get(0);
        FlowProgressResponse firstResponse = responses.get(1);
        // THEN
        assertEquals(2, responses.size());
        assertEquals("flow2", latestResponse.getFlowId());
        assertEquals(2, latestResponse.getTransitions().size());
        assertEquals(0.002, latestResponse.getElapsedTimeInSeconds());
        assertEquals(100, latestResponse.getProgress());
        assertEquals("flow1", firstResponse.getFlowId());
        assertEquals(2, firstResponse.getTransitions().size());
        assertEquals(0.001, firstResponse.getElapsedTimeInSeconds());
        assertEquals(100, firstResponse.getProgress());
    }

    @Test
    public void testConvertWithEmptyList() {
        // GIVEN
        // WHEN
        FlowProgressResponse response = underTest.convert(new ArrayList<>(), DUMMY_CRN);
        // THEN
        assertNull(response.getFlowId());
    }

    @Test
    public void testConvertWithNull() {
        // GIVEN
        // WHEN
        FlowProgressResponse response = underTest.convert(null, DUMMY_CRN);
        // THEN
        assertNull(response.getFlowId());
    }

    @Test
    public void testConvertListWithEmptyList() {
        // GIVEN
        // WHEN
        List<FlowProgressResponse> responses = underTest.convertList(new ArrayList<>(), DUMMY_CRN);
        // THEN
        assertTrue(responses.isEmpty());

    }

    @Test
    public void testConvertListWithNull() {
        // GIVEN
        // WHEN
        List<FlowProgressResponse> responses = underTest.convertList(null, DUMMY_CRN);
        // THEN
        assertTrue(responses.isEmpty());
    }

    private List<FlowLog> createFlowLogs(boolean multipleFlowIds) {
        List<FlowLog> flowLogs = new ArrayList<>();
        FlowLog flowLog1 = new FlowLog();
        flowLog1.setCreated(2L);
        flowLog1.setCurrentState("FINISHED");
        FlowLog flowLog2 = new FlowLog();
        flowLog2.setCreated(1L);
        flowLog2.setCurrentState("INIT_STATE");
        setDefaults("flow1", flowLog1, flowLog2);
        flowLogs.add(flowLog1);
        flowLogs.add(flowLog2);
        if (multipleFlowIds) {
            FlowLog flowLog3 = new FlowLog();
            flowLog3.setCreated(5L);
            flowLog3.setCurrentState("CANCELLED");
            FlowLog flowLog4 = new FlowLog();
            flowLog4.setCreated(3L);
            flowLog4.setCurrentState("INIT_STATE");
            flowLogs.add(flowLog3);
            flowLogs.add(flowLog4);
            setDefaults("flow2", flowLog3, flowLog4);
        }
        return flowLogs;
    }

    private void setDefaults(String flowId, FlowLog... flowLogs) {
        for (FlowLog fl : flowLogs) {
            fl.setFlowId(flowId);
            fl.setFinalized(true);
            fl.setFlowType(ClassValue.of(TestFlowConfig.class));
            fl.setStateStatus(StateStatus.SUCCESSFUL);
        }
    }
}
