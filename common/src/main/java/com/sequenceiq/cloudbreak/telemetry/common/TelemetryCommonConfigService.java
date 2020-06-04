package com.sequenceiq.cloudbreak.telemetry.common;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.model.VmLogs;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class TelemetryCommonConfigService {

    private final String version;

    public TelemetryCommonConfigService(@Value("${info.app.version:}") String version) {
        this.version = version;
    }

    public TelemetryCommonConfigView createTelemetryCommonConfigs(Telemetry telemetry, List<VmLogs> logs,
            String clusterType, String clusterCrn, String clusterName, String clusterOwner, String platform) {
        final TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder()
                .withOwner(clusterOwner)
                .withName(clusterName)
                .withType(clusterType)
                .withCrn(clusterCrn)
                .withPlatform(platform)
                .withVersion(version)
                .build();
        return new TelemetryCommonConfigView.Builder()
                .withClusterDetails(clusterDetails)
                .withRules(telemetry.getRules())
                .withVmLogs(logs)
                .build();
    }
}
