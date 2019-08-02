package com.sequenceiq.cloudbreak.telemetry;

import java.util.Map;

public interface TelemetryConfigView {
    Map<String, Object> toMap();
}
