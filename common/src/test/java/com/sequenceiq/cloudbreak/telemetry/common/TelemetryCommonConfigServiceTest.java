package com.sequenceiq.cloudbreak.telemetry.common;

import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService.AGENT_LOG_FOLDER_PREFIX;
import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService.SERVER_LOG_FOLDER_PREFIX;
import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService.SERVICE_LOG_FOLDER_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConnectionConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryRepoConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;
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
    private AltusDatabusConnectionConfiguration altusDatabusConnectionConfiguration;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new TelemetryCommonConfigService(anonymizationRuleResolver, telemetryUpgradeConfiguration,
                telemetryRepoConfiguration, altusDatabusConnectionConfiguration);
    }

    @Test
    public void testCreateTelemetryCommonConfigs() {
        // GIVEN
        given(telemetryUpgradeConfiguration.isEnabled()).willReturn(true);
        TelemetryComponentUpgradeConfiguration cdpTelemetryConfig = new TelemetryComponentUpgradeConfiguration();
        cdpTelemetryConfig.setDesiredVersion("0.0.1");
        given(telemetryUpgradeConfiguration.getCdpTelemetry()).willReturn(cdpTelemetryConfig);
        given(altusDatabusConnectionConfiguration.getMaxTimeSeconds()).willReturn(1);
        Telemetry telemetry = new Telemetry();
        Map<String, Object> fluentAttributes = new HashMap<>();
        fluentAttributes.put(SERVICE_LOG_FOLDER_PREFIX, "/var/log");
        fluentAttributes.put(SERVER_LOG_FOLDER_PREFIX, "/custom/log");
        fluentAttributes.put(AGENT_LOG_FOLDER_PREFIX, "/grid/0/log");
        telemetry.setFluentAttributes(fluentAttributes);
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
        TelemetryClusterDetails telemetryClusterDetails = TelemetryClusterDetails.Builder.builder()
                .build();
        // WHEN
        TelemetryCommonConfigView result = underTest.createTelemetryCommonConfigs(telemetry, vmLogs, telemetryClusterDetails);
        // THEN
        assertEquals("/var/log/mylog.log", vmLogs.get(0).getPath());
        assertEquals("/grid/0/log/*", vmLogs.get(1).getPath());
        assertEquals("/my/path/custom/log/*", vmLogs.get(2).getPath());
        assertEquals("0.0.1", result.toMap().get("desiredCdpTelemetryVersion").toString());
        assertEquals(1, result.toMap().get("databusConnectMaxTime"));
    }
}
