package com.sequenceiq.cloudbreak.telemetry.common;

import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService.AGENT_LOG_FOLDER_PREFIX;
import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService.SERVER_LOG_FOLDER_PREFIX;
import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService.SERVICE_LOG_FOLDER_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConnectionConfiguration;
import com.sequenceiq.cloudbreak.telemetry.DevTelemetryRepoConfigurationHolder;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryRepoConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryRepoConfigurationHolder;
import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.context.LogShipperContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.VmLog;

@ExtendWith(MockitoExtension.class)
public class TelemetryCommonConfigServiceTest {

    private TelemetryCommonConfigService underTest;

    @Mock
    private AnonymizationRuleResolver anonymizationRuleResolver;

    @Mock
    private TelemetryUpgradeConfiguration telemetryUpgradeConfiguration;

    @Mock
    private TelemetryRepoConfiguration telemetryRepoConfiguration;

    @Mock
    private TelemetryRepoConfigurationHolder telemetryRepoConfigurationHolder;

    @Mock
    private DevTelemetryRepoConfigurationHolder devTelemetryRepoConfigurationHolder;

    @Mock
    private AltusDatabusConnectionConfiguration altusDatabusConnectionConfiguration;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new TelemetryCommonConfigService(anonymizationRuleResolver, telemetryUpgradeConfiguration,
                telemetryRepoConfigurationHolder, devTelemetryRepoConfigurationHolder, altusDatabusConnectionConfiguration);
    }

    @Test
    public void testIsEnabled() {
        // GIVEN
        TelemetryContext context = new TelemetryContext();
        context.setLogShipperContext(LogShipperContext.builder().build());
        context.setClusterDetails(TelemetryClusterDetails.Builder.builder().build());
        context.setTelemetry(new Telemetry());
        // WHEN
        boolean result = underTest.isEnabled(context);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsEnabledWithoutTelemetry() {
        // GIVEN
        TelemetryContext context = new TelemetryContext();
        context.setLogShipperContext(LogShipperContext.builder().build());
        context.setClusterDetails(TelemetryClusterDetails.Builder.builder().build());
        // WHEN
        boolean result = underTest.isEnabled(context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsEnabledWithoutLoggingContext() {
        // GIVEN
        TelemetryContext context = new TelemetryContext();
        context.setTelemetry(new Telemetry());
        context.setClusterDetails(TelemetryClusterDetails.Builder.builder().build());
        // WHEN
        boolean result = underTest.isEnabled(context);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsEnabledWithoutClusterDetails() {
        // GIVEN
        TelemetryContext context = new TelemetryContext();
        context.setLogShipperContext(LogShipperContext.builder().build());
        context.setTelemetry(new Telemetry());
        // WHEN
        boolean result = underTest.isEnabled(context);
        // THEN
        assertFalse(result);
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
    public void testCreateTelemetryCommonConfigs() {
        // GIVEN
        TelemetryContext context = new TelemetryContext();
        List<VmLog> vmLogs = generateSimpleVmLogs();
        mockTelemetryComponentUpgradeConfig();
        createTelemetryModel(context, vmLogs);

        given(telemetryUpgradeConfiguration.isEnabled()).willReturn(true);
        given(telemetryRepoConfiguration.name()).willReturn("cdp-infra-tools-rhel7");
        given(altusDatabusConnectionConfiguration.getMaxTimeSeconds()).willReturn(1);
        given(telemetryRepoConfigurationHolder.selectCorrectRepoConfig(context)).willReturn(telemetryRepoConfiguration);
        // WHEN
        Map<String, Object> result = underTest.createConfigs(context).toMap();
        // THEN
        assertEquals("/var/log/mylog.log", vmLogs.get(0).getPath());
        assertEquals("/grid/0/log/*", vmLogs.get(1).getPath());
        assertEquals("/my/path/custom/log/*", vmLogs.get(2).getPath());
        assertEquals("0.0.1", result.get("desiredCdpTelemetryVersion").toString());
        assertEquals(1, result.get("databusConnectMaxTime"));
        assertEquals("cdp-infra-tools-rhel7", result.get("repoName"));
    }

    private static void createTelemetryModel(TelemetryContext context, List<VmLog> vmLogs) {
        Telemetry telemetry = new Telemetry();
        Map<String, Object> fluentAttributes = new HashMap<>();
        fluentAttributes.put(SERVICE_LOG_FOLDER_PREFIX, "/var/log");
        fluentAttributes.put(SERVER_LOG_FOLDER_PREFIX, "/custom/log");
        fluentAttributes.put(AGENT_LOG_FOLDER_PREFIX, "/grid/0/log");
        telemetry.setFluentAttributes(fluentAttributes);
        TelemetryClusterDetails telemetryClusterDetails = TelemetryClusterDetails.Builder.builder()
                .build();
        LogShipperContext logShipperContext = LogShipperContext.builder()
                .withVmLogs(vmLogs)
                .build();
        context.setTelemetry(telemetry);
        context.setClusterDetails(telemetryClusterDetails);
        context.setLogShipperContext(logShipperContext);
    }

    private void mockTelemetryComponentUpgradeConfig() {
        TelemetryComponentUpgradeConfiguration cdpTelemetryConfig = new TelemetryComponentUpgradeConfiguration();
        cdpTelemetryConfig.setDesiredVersion("0.0.1");
        given(telemetryUpgradeConfiguration.getCdpTelemetry()).willReturn(cdpTelemetryConfig);
    }

    private static List<VmLog> generateSimpleVmLogs() {
        List<VmLog> vmLogs = new ArrayList<>();
        VmLog log1 = new VmLog();
        log1.setPath("${serviceLogFolderPrefix}/mylog.log");
        VmLog log2 = new VmLog();
        log2.setPath("${agentLogFolderPrefix}/*");
        VmLog log3 = new VmLog();
        log3.setPath("/my/path${serverLogFolderPrefix}/*");
        vmLogs.add(log1);
        vmLogs.add(log2);
        vmLogs.add(log3);
        return vmLogs;
    }
}
