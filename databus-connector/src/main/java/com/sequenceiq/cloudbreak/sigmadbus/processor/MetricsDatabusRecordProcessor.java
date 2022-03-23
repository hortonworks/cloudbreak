package com.sequenceiq.cloudbreak.sigmadbus.processor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.config.SigmaDatabusConfig;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;

import io.opentracing.Tracer;

@Component
public class MetricsDatabusRecordProcessor extends AbstractDatabusRecordProcessor<MonitoringConfiguration> {

    public static final String DBUS_METRICS_HEADER_ENV_CRN = "@metrics-environment-crn";

    public static final String DBUS_METRICS_HEADER_RESOURCE_CRN = "@metrics-resource-crn";

    public static final String DBUS_METRICS_HEADER_RESOURCE_NAME = "@metrics-resource-name";

    public static final String DBUS_METRICS_HEADER_RESOURCE_VERSION = "@metrics-resource-version";

    public static final String DBUS_METRICS_HEADER_METRICS_TYPE = "@metrics-type";

    public MetricsDatabusRecordProcessor(
            SigmaDatabusConfig sigmaDatabusConfig,
            MonitoringConfiguration monitoringConfiguration,
            @Value("${cluster.monitoring.databus.processing.workers:1}") int numberOfWorkers,
            @Value("${cluster.monitoring.databus.processing.queueSizeLimit:2000}") int queueSizeLimit,
            Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        super(sigmaDatabusConfig, monitoringConfiguration, numberOfWorkers, queueSizeLimit, tracer, regionAwareInternalCrnGeneratorFactory);
    }
}
