package com.sequenceiq.cloudbreak.telemetry;

import java.util.Map;

import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;

public interface TelemetryPillarConfigGenerator<O extends TelemetryConfigView> {

    O createConfigs(TelemetryContext context);

    boolean isEnabled(TelemetryContext context);

    String saltStateName();

    default Map<String, Map<String, Map<String, Object>>> getSaltPillars(TelemetryConfigView configView, TelemetryContext context) {
        return Map.of(saltStateName(), Map.of(String.format("/%s/init.sls", saltStateName()), Map.of(saltStateName(), configView.toMap())));
    }
}
