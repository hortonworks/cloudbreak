package com.sequenceiq.cloudbreak.converter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.Logging;
import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.cloudbreak.cloud.model.WorkloadAnalytics;
import com.sequenceiq.environment.api.v1.environment.model.response.LoggingResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TelemetryResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.WorkloadAnalyticsResponse;

public class TelemetryMergerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryMergerUtil.class);

    private TelemetryMergerUtil() {
    }

    /**
     * Merge environment and stack telemetry settings.
     * Stack level telemetry settings should override the environment level one.
     * @param envTelemetryResponse Environment level (global) telemetry settings
     * @param stackTelemetry Stack (cluster) level telemetry settings
     * @return final telemetry settings
     */
    public static Telemetry mergeGlobalAndStackLevelTelemetry(TelemetryResponse envTelemetryResponse, Telemetry stackTelemetry) {
        final Telemetry envTelemetry = createEnvTelemetry(envTelemetryResponse);
        return createStackTelemetry(stackTelemetry, envTelemetry.getLogging(), envTelemetry.getWorkloadAnalytics());
    }

    private static Telemetry createEnvTelemetry(TelemetryResponse envTelemetry) {
        Logging logging = null;
        WorkloadAnalytics workloadAnalytics = null;
        if (envTelemetry != null) {
            if (envTelemetry.getLogging() != null) {
                LOGGER.debug("Found global (environment) telemetry (logging) settings.");
                LoggingResponse loggingResponse = envTelemetry.getLogging();
                logging = new Logging(loggingResponse.isEnabled(), loggingResponse.getOutput(), loggingResponse.getAttributes());
            }
            if (envTelemetry.getWorkloadAnalytics() != null) {
                LOGGER.debug("Found global (environment) telemetry (workload analytics) settings.");
                WorkloadAnalyticsResponse waResponse = envTelemetry.getWorkloadAnalytics();
                workloadAnalytics = new WorkloadAnalytics(waResponse.isEnabled(), waResponse.getDatabusEndpoint(), null, null, waResponse.getAttributes());
            }
        }
        return new Telemetry(logging, workloadAnalytics);
    }

    private static Telemetry createStackTelemetry(Telemetry stackTelemetry, Logging logging, WorkloadAnalytics workloadAnalytics) {
        if (stackTelemetry != null) {
            if (stackTelemetry.getLogging() != null) {
                LOGGER.debug("Found stack level telemetry (logging) settings. It will override enviroment level telemetry configs.");
                logging = stackTelemetry.getLogging();
            }
            if (stackTelemetry.getWorkloadAnalytics() != null) {
                LOGGER.debug("Found stack level telemetry (workload analytics) settings. It will override enviroment level telemetry configs.");
                workloadAnalytics = stackTelemetry.getWorkloadAnalytics();
            }
        }
        return new Telemetry(logging, workloadAnalytics);
    }
}
