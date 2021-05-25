package com.sequenceiq.cdp.databus.processor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cdp.databus.service.AccountDatabusConfigService;
import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;

@Component
public class MetricsDatabusRecordProcessor extends AbstractDatabusRecordProcessor<MonitoringConfiguration> {

    public static final String DBUS_METRICS_HEADER_ENV_CRN = "@metrics-environment-crn";

    public static final String DBUS_METRICS_HEADER_RESOURCE_CRN = "@metrics-resource-crn";

    public static final String DBUS_METRICS_HEADER_RESOURCE_NAME = "@metrics-resource-name";

    public static final String DBUS_METRICS_HEADER_RESOURCE_VERSION = "@metrics-resource-version";

    public static final String DBUS_METRICS_HEADER_METRICS_TYPE = "@metrics-type";

    public MetricsDatabusRecordProcessor(AccountDatabusConfigService accountDatabusConfigService, AltusDatabusConfiguration altusDatabusConfiguration,
            MonitoringConfiguration monitoringConfiguration, @Value("${cluster.monitoring.databus.processing.workers:1}") int numberOfWorkers,
            @Value("${cluster.monitoring.databus.processing.queueSizeLimit:2000}") int queueSizeLimit) {
        super(accountDatabusConfigService, altusDatabusConfiguration, monitoringConfiguration, numberOfWorkers, queueSizeLimit);
    }

    @Override
    public String getAccountMachineUserNamePrefix() {
        return "cb-account-metrics-publisher";
    }
}
