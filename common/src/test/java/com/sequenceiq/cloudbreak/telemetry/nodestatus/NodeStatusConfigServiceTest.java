package com.sequenceiq.cloudbreak.telemetry.nodestatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.telemetry.context.NodeStatusContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;

public class NodeStatusConfigServiceTest {

    private NodeStatusConfigService underTest;

    @BeforeEach
    public void setUp() {
        underTest = new NodeStatusConfigService();
    }

    @Test
    public void testIsEnabled() {
        // GIVEN
        // WHEN
        boolean result = underTest.isEnabled(telemetryContext());
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsEnabledWithoutContext() {
        // GIVEN
        // WHEN
        boolean result = underTest.isEnabled(null);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsEnabledWithoutNodeStatusContext() {
        // GIVEN
        // WHEN
        boolean result = underTest.isEnabled(new TelemetryContext());
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCreateConfigs() {
        // GIVEN
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext()).toMap();
        // THEN
        assertEquals("myuser", result.get("serverUsername"));
        assertEquals("mypassword", result.get("serverPassword"));
        assertEquals(true, result.get("saltPingEnabled"));
    }

    private TelemetryContext telemetryContext() {
        TelemetryContext context = new TelemetryContext();
        context.setNodeStatusContext(NodeStatusContext.builder()
                .withUsername("myuser")
                .withPassword("mypassword".toCharArray())
                .saltPingEnabled()
                .build());
        return context;
    }
}
