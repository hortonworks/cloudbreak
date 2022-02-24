package com.sequenceiq.flow.service;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;

@RunWith(MockitoJUnitRunner.class)
public class FlowNameFormatServiceTest {

    @InjectMocks
    private FlowNameFormatService undertest;

    @Test
    public void testFormatFlowName() {
        String result = undertest.formatFlowName("SdxCreateFlowConfig");
        assertEquals("datalake create", result);
    }

    @Test
    public void testFormatFlowNameRedbeams() {
        String result = undertest.formatFlowName("RedbeamsProvisionFlowConfig");
        assertEquals("external database provision", result);
    }

    @Test
    public void testFormatFlowNameEmpty() {
        String result = undertest.formatFlowName("");
        assertEquals("", result);
    }

    @Test
    public void testFormatFlowNameNull() {
        String result = undertest.formatFlowName(null);
        assertEquals("", result);
    }

    @Test
    public void testFormatFlowNameAllLowerCase() {
        String result = undertest.formatFlowName("alllowercase");
        assertEquals("alllowercase", result);
    }

    @Test
    public void testFormatFlow() {
        Set<FlowLogIdWithTypeAndTimestamp> input = Set.of(
                new TestFlowView(TestClusterCreateFlowConfig.class),
                new TestFlowView(TestClusterSdxUpgradeFlowConfig.class));
        String result = undertest.formatFlows(input);
        assertTrue(result.contains("test cluster create"));
        assertTrue(result.contains("test cluster datalake upgrade"));
    }

    @Test
    public void testFormatFlowEmpty() {
        Set<FlowLogIdWithTypeAndTimestamp> input = Collections.emptySet();
        String result = undertest.formatFlows(input);
        assertEquals("", result);
    }

    @Test
    public void testFormatFlowNull() {
        String result = undertest.formatFlows(null);
        assertEquals("", result);
    }

    private static class TestClusterCreateFlowConfig {

    }

    private static class TestClusterSdxUpgradeFlowConfig {

    }

}