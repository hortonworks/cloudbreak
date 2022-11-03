package com.sequenceiq.cloudbreak.telemetry.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.telemetry.context.MonitoringContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;

@ExtendWith(MockitoExtension.class)
public class MonitoringConfigServiceTest {

    private static final Integer DEFAULT_CM_SMON_PORT = 61010;

    private static final String ACCESS_KEY_ID = "ACCESS_KEY_ID";

    private static final char[] PRIVATE_KEY = "PRIVATE_KEY".toCharArray();

    private static final String ACCESS_KEY_TYPE = "ECDSA";

    @InjectMocks
    private MonitoringConfigService underTest;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @Mock
    private ExporterConfiguration cmMonitoringConfiguration;

    @Mock
    private BlackboxExporterConfiguration blackboxExporterConfiguration;

    @Mock
    private MonitoringGlobalAuthConfig monitoringGlobalAuthConfig;

    @Mock
    private MonitoringAgentConfiguration monitoringAgentConfiguration;

    @Mock
    private RequestSignerConfiguration requestSignerConfiguration;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new MonitoringConfigService(monitoringConfiguration, monitoringGlobalAuthConfig);
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
    public void testIsEnabledWithoutMonitoringContext() {
        // GIVEN
        TelemetryContext context = telemetryContext();
        context.setMonitoringContext(null);
        // WHEN
        boolean result = underTest.isEnabled(context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsEnabledWithoutContext() {
        // GIVEN
        TelemetryContext context = telemetryContext();
        context.setMonitoringContext(null);
        // WHEN
        boolean result = underTest.isEnabled(null);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCreateConfigs() {
        // GIVEN
        given(monitoringConfiguration.isEnabled()).willReturn(true);
        given(monitoringConfiguration.isDevStack()).willReturn(false);
        given(monitoringConfiguration.getRequestSigner()).willReturn(requestSignerConfiguration);
        given(requestSignerConfiguration.isEnabled()).willReturn(true);
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(monitoringConfiguration.getAgent()).willReturn(monitoringAgentConfiguration);
        given(cmMonitoringConfiguration.getPort()).willReturn(DEFAULT_CM_SMON_PORT);
        given(monitoringConfiguration.getBlackboxExporter()).willReturn(blackboxExporterConfiguration);
        given(blackboxExporterConfiguration.getClouderaIntervalSeconds()).willReturn(1000);
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext()).toMap();
        // THEN
        assertEquals("myendpoint", result.get("remoteWriteUrl"));
        assertEquals(true, result.get("cmAutoTls"));
        assertEquals(ACCESS_KEY_TYPE, result.get("monitoringAccessKeyType"));
        assertEquals(true, result.get("enabled"));
        assertEquals("user", result.get("cmUsername"));
        assertEquals(1000, result.get("blackboxExporterClouderaIntervalSeconds"));
    }

    private TelemetryContext telemetryContext() {
        TelemetryContext telemetryContext = new TelemetryContext();
        MonitoringCredential cred = new MonitoringCredential();
        cred.setAccessKey(ACCESS_KEY_ID);
        cred.setPrivateKey(new String(PRIVATE_KEY));
        cred.setAccessKeyType(ACCESS_KEY_TYPE);
        MonitoringContext monitoringContext = MonitoringContext
                .builder()
                .enabled()
                .withCmAuth(new MonitoringAuthConfig("user", "pass".toCharArray()))
                .withCmAutoTls(true)
                .withClusterType(MonitoringClusterType.CLOUDERA_MANAGER)
                .withRemoteWriteUrl("myendpoint")
                .withSharedPassword("mypass".toCharArray())
                .withCredential(cred)
                .build();
        telemetryContext.setMonitoringContext(monitoringContext);
        return telemetryContext;
    }
}
