package com.sequenceiq.cloudbreak.telemetry.common;

import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService.AGENT_LOG_FOLDER_PREFIX;
import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService.SERVER_LOG_FOLDER_PREFIX;
import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService.SERVICE_LOG_FOLDER_PREFIX;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.VmLog;

public class TelemetryCommonConfigServiceTest {

    private TelemetryCommonConfigService underTest;

    @Before
    public void setUp() {
        underTest = new TelemetryCommonConfigService(null);
    }

    @Test
    public void testResolveLogPathReferences() {
        // GIVEN
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
        // WHEN
        underTest.resolveLogPathReferences(telemetry, vmLogs);
        // THEN
        assertEquals("/var/log/mylog.log", vmLogs.get(0).getPath());
        assertEquals("/grid/0/log/*", vmLogs.get(1).getPath());
        assertEquals("/my/path/custom/log/*", vmLogs.get(2).getPath());

    }
}
