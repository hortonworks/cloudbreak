package com.sequenceiq.cloudbreak.telemetry.databus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.telemetry.context.DatabusContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

public class DatabusConfigServiceTest {

    private DatabusConfigService underTest;

    @BeforeEach
    public void setUp() {
        underTest = new DatabusConfigService();
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
    public void testIsEnabledWithoutCredential() {
        // GIVEN
        TelemetryContext context = telemetryContext();
        context.setDatabusContext(DatabusContext.builder()
                .enabled()
                .withValidation()
                .withEndpoint("myendpoint")
                .build());
        // WHEN
        boolean result = underTest.isEnabled(context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsEnabledWhenDatabusDisabled() {
        // GIVEN
        TelemetryContext context = telemetryContext();
        DataBusCredential credential = new DataBusCredential();
        credential.setPrivateKey("privateKey");
        credential.setAccessKey("accessKey");
        context.setDatabusContext(DatabusContext.builder()
                .withValidation()
                .withEndpoint("myendpoint")
                .withCredential(credential)
                .build());
        // WHEN
        boolean result = underTest.isEnabled(context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsEnabledWithoutCredentialProperties() {
        // GIVEN
        DataBusCredential credential = new DataBusCredential();
        // WHEN
        boolean result = underTest.isEnabled(telemetryContext(credential));
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCreateConfig() {
        // GIVEN
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext()).toMap();
        // THEN
        assertEquals("accessKey", result.get("accessKeyId"));
        assertEquals("privateKey", result.get("accessKeySecret"));
        assertEquals("ECDSA", result.get("accessKeySecretAlgorithm"));
        assertEquals("myendpoint", result.get("endpoint"));
    }

    @Test
    public void testCreateConfigWithDefaultAccessType() {
        // GIVEN
        DataBusCredential credential = new DataBusCredential();
        credential.setPrivateKey("privateKey");
        credential.setAccessKey("accessKey");
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext(credential)).toMap();
        // THEN
        assertEquals("accessKey", result.get("accessKeyId"));
        assertEquals("privateKey", result.get("accessKeySecret"));
        assertEquals("Ed25519", result.get("accessKeySecretAlgorithm"));
    }

    private TelemetryContext telemetryContext() {
        DataBusCredential credential = new DataBusCredential();
        credential.setPrivateKey("privateKey");
        credential.setAccessKey("accessKey");
        credential.setAccessKeyType("ECDSA");
        return telemetryContext(credential);
    }

    private TelemetryContext telemetryContext(DataBusCredential credential) {
        TelemetryContext context = new TelemetryContext();
        DatabusContext databusContext = DatabusContext.builder()
                .enabled()
                .withCredential(credential)
                .withEndpoint("myendpoint")
                .withValidation()
                .build();
        context.setDatabusContext(databusContext);
        return context;
    }
}
