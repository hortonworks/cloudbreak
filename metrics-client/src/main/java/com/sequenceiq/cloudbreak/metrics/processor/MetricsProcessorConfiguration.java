package com.sequenceiq.cloudbreak.metrics.processor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.streaming.config.AbstractStreamingConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;

@Component
public class MetricsProcessorConfiguration extends AbstractStreamingConfiguration {

    private final MonitoringConfiguration monitoringConfiguration;

    private final Integer httpTimeout;

    public MetricsProcessorConfiguration(MonitoringConfiguration monitoringConfiguration,
            @Value("${telemetry.monitoring.status-processor.enabled}") boolean enabled,
            @Value("${telemetry.monitoring.status-processor.workers}") int numberOfWorkers,
            @Value("${telemetry.monitoring.status-processor.queue-size-limit}") int queueSizeLimit,
            @Value("${telemetry.monitoring.status-processor.http-timeout-seconds}") int httpTimeout) {
        super(enabled, numberOfWorkers, queueSizeLimit);
        this.monitoringConfiguration = monitoringConfiguration;
        this.httpTimeout = httpTimeout;
    }

    public String getRemoteWriteUrl() {
        return monitoringConfiguration.getRemoteWriteUrl();
    }

    public boolean isComputeMonitoringSupported() {
        return monitoringConfiguration.isEnabled() && StringUtils.isNotBlank(monitoringConfiguration.getRemoteWriteUrl());
    }

    public boolean isPaasSupported() {
        return monitoringConfiguration.isPaasSupport();
    }

    public Integer getHttpTimeout() {
        return httpTimeout;
    }
}
