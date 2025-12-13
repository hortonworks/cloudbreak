package com.sequenceiq.flow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;

@ExtendWith(MockitoExtension.class)
class FlowNameFormatServiceTest {

    @InjectMocks
    private FlowNameFormatService undertest;

    @Test
    void testFormatFlowName() {
        String result = undertest.formatFlowName("SdxCreateFlowConfig");
        assertEquals("datalake create", result);
    }

    @Test
    void testFormatFlowNameRedbeams() {
        String result = undertest.formatFlowName("RedbeamsProvisionFlowConfig");
        assertEquals("external database provision", result);
    }

    @Test
    void testFormatFlowNameEmpty() {
        String result = undertest.formatFlowName("");
        assertEquals("", result);
    }

    @Test
    void testFormatFlowNameNull() {
        String result = undertest.formatFlowName(null);
        assertEquals("", result);
    }

    @Test
    void testFormatFlowNameAllLowerCase() {
        String result = undertest.formatFlowName("alllowercase");
        assertEquals("alllowercase", result);
    }

    @Test
    void testFormatFlow() {
        Set<FlowLogIdWithTypeAndTimestamp> input = Set.of(
                new TestFlowView(TestClusterCreateFlowConfig.class),
                new TestFlowView(TestClusterSdxUpgradeFlowConfig.class));
        String result = undertest.formatFlows(input);
        assertTrue(result.contains("test cluster create"));
        assertTrue(result.contains("test cluster datalake upgrade"));
    }

    @Test
    void testFormatFlowEmpty() {
        Set<FlowLogIdWithTypeAndTimestamp> input = Collections.emptySet();
        String result = undertest.formatFlows(input);
        assertEquals("", result);
    }

    @Test
    void testFormatFlowNull() {
        String result = undertest.formatFlows(null);
        assertEquals("", result);
    }

    private static class TestClusterCreateFlowConfig {

    }

    private static class TestClusterSdxUpgradeFlowConfig {

    }

}