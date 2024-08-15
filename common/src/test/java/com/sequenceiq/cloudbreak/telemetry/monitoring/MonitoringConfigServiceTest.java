package com.sequenceiq.cloudbreak.telemetry.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.telemetry.context.DatabusContext;
import com.sequenceiq.cloudbreak.telemetry.context.MonitoringContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

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

    @Spy
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new MonitoringConfigService(monitoringConfiguration, monitoringGlobalAuthConfig, adlsGen2ConfigGenerator);
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
        assertEquals(2, ((List) result.get("blackboxExporterCloudLinks")).size());
        assertEquals(4, ((List) result.get("blackboxExporterClouderaLinks")).size());
        assertTrue(((List) result.get("blackboxExporterCloudLinks")).contains("https://s3.us-west-1.amazonaws.com"));
    }

    @Test
    public void testCreateConfigsAwsGov() {
        // GIVEN
        given(monitoringConfiguration.getRequestSigner()).willReturn(requestSignerConfiguration);
        given(requestSignerConfiguration.isEnabled()).willReturn(true);
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(monitoringConfiguration.getAgent()).willReturn(monitoringAgentConfiguration);
        given(cmMonitoringConfiguration.getPort()).willReturn(DEFAULT_CM_SMON_PORT);
        given(monitoringConfiguration.getBlackboxExporter()).willReturn(blackboxExporterConfiguration);
        given(blackboxExporterConfiguration.getClouderaIntervalSeconds()).willReturn(1000);
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext("AWS", "us-gov-west-1")).toMap();
        // THEN
        assertEquals("myendpoint", result.get("remoteWriteUrl"));
        assertEquals(true, result.get("cmAutoTls"));
        assertEquals(ACCESS_KEY_TYPE, result.get("monitoringAccessKeyType"));
        assertEquals(true, result.get("enabled"));
        assertEquals("user", result.get("cmUsername"));
        assertEquals(1000, result.get("blackboxExporterClouderaIntervalSeconds"));
        assertEquals(2, ((List) result.get("blackboxExporterCloudLinks")).size());
        assertEquals(4, ((List) result.get("blackboxExporterClouderaLinks")).size());
        assertTrue(((List) result.get("blackboxExporterCloudLinks")).contains("https://s3-fips.us-gov-west-1.amazonaws.com"));
    }

    @Test
    public void testCreateConfigsAzure() {
        // GIVEN
        given(monitoringConfiguration.getRequestSigner()).willReturn(requestSignerConfiguration);
        given(requestSignerConfiguration.isEnabled()).willReturn(true);
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(monitoringConfiguration.getAgent()).willReturn(monitoringAgentConfiguration);
        given(cmMonitoringConfiguration.getPort()).willReturn(DEFAULT_CM_SMON_PORT);
        given(monitoringConfiguration.getBlackboxExporter()).willReturn(blackboxExporterConfiguration);
        given(blackboxExporterConfiguration.getClouderaIntervalSeconds()).willReturn(1000);
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext("AZURE", "")).toMap();
        // THEN
        assertEquals("myendpoint", result.get("remoteWriteUrl"));
        assertEquals(true, result.get("cmAutoTls"));
        assertEquals(ACCESS_KEY_TYPE, result.get("monitoringAccessKeyType"));
        assertEquals(true, result.get("enabled"));
        assertEquals("user", result.get("cmUsername"));
        assertEquals(1000, result.get("blackboxExporterClouderaIntervalSeconds"));
        assertEquals(2, ((List) result.get("blackboxExporterCloudLinks")).size());
        assertEquals(4, ((List) result.get("blackboxExporterClouderaLinks")).size());
        assertTrue(((List) result.get("blackboxExporterCloudLinks")).contains("https://management.azure.com"));
        assertTrue(((List) result.get("blackboxExporterCloudLinks")).contains("https://storageaccount.dfs.core.windows.net"));
    }

    @Test
    public void testCreateConfigsGcp() {
        // GIVEN
        given(monitoringConfiguration.getRequestSigner()).willReturn(requestSignerConfiguration);
        given(requestSignerConfiguration.isEnabled()).willReturn(true);
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(monitoringConfiguration.getAgent()).willReturn(monitoringAgentConfiguration);
        given(cmMonitoringConfiguration.getPort()).willReturn(DEFAULT_CM_SMON_PORT);
        given(monitoringConfiguration.getBlackboxExporter()).willReturn(blackboxExporterConfiguration);
        given(blackboxExporterConfiguration.getClouderaIntervalSeconds()).willReturn(1000);
        // WHEN
        Map<String, Object> result = underTest.createConfigs(telemetryContext("GCP", "")).toMap();
        // THEN
        assertEquals("myendpoint", result.get("remoteWriteUrl"));
        assertEquals(true, result.get("cmAutoTls"));
        assertEquals(ACCESS_KEY_TYPE, result.get("monitoringAccessKeyType"));
        assertEquals(true, result.get("enabled"));
        assertEquals("user", result.get("cmUsername"));
        assertEquals(1000, result.get("blackboxExporterClouderaIntervalSeconds"));
        assertEquals(1, ((List) result.get("blackboxExporterCloudLinks")).size());
        assertEquals(4, ((List) result.get("blackboxExporterClouderaLinks")).size());
        assertTrue(((List) result.get("blackboxExporterCloudLinks")).contains("https://storage.googleapis.com"));
    }

    private TelemetryContext telemetryContext() {
        return telemetryContext("AWS", "us-west-1");
    }

    private TelemetryContext telemetryContext(String cloudPlatform, String region) {
        Telemetry telemetry = new Telemetry();
        Logging logging = new Logging();
        if ("AZURE".equals(cloudPlatform)) {
            AdlsGen2CloudStorageV1Parameters parameters = new AdlsGen2CloudStorageV1Parameters();
            logging.setAdlsGen2(parameters);
            logging.setStorageLocation("abfs://location@storageaccount.dfs.core.windows.net");
        }
        telemetry.setLogging(logging);
        TelemetryContext telemetryContext = new TelemetryContext();
        MonitoringCredential cred = new MonitoringCredential();
        cred.setAccessKey(ACCESS_KEY_ID);
        cred.setPrivateKey(new String(PRIVATE_KEY));
        cred.setAccessKeyType(ACCESS_KEY_TYPE);
        MonitoringContext monitoringContext = MonitoringContext
                .builder()
                .enabled()
                .withCmAuth(new MonitoringAuthConfig("user", "pass"))
                .withCmAutoTls(true)
                .withClusterType(MonitoringClusterType.CLOUDERA_MANAGER)
                .withRemoteWriteUrl("myendpoint")
                .withSharedPassword("mypass")
                .withCredential(cred)
                .build();
        telemetryContext.setMonitoringContext(monitoringContext);
        DatabusContext databusContext = DatabusContext
                .builder()
                .enabled()
                .withEndpoint("https://dbus.cloudera.com")
                .withS3Endpoint("https://s3.dbus.cloudera.com")
                .build();
        telemetryContext.setDatabusContext(databusContext);
        telemetryContext.setTelemetry(telemetry);
        telemetryContext.setRegion(region);
        telemetryContext.setCloudPlatform(cloudPlatform);
        return telemetryContext;
    }
}
