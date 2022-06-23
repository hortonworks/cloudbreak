package com.sequenceiq.cloudbreak.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.metrics.processor.MetricsProcessorConfiguration;
import com.sequenceiq.cloudbreak.metrics.processor.MetricsRecordProcessor;
import com.sequenceiq.cloudbreak.metrics.processor.MetricsRecordRequest;

import prometheus.Remote;
import prometheus.Types;

@Component
public class MetricsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsClient.class);

    private static final String METRIC_LABEL_NAME = "__name__";

    private static final String RESOURCE_CRN_LABEL_NAME = "resource_crn";

    private static final String PLATFORM_LABEL_NAME = "platform";

    private static final String CLUSTER_STATUS_LABEL_NAME = "cluster_status";

    private static final String CLUSTER_TYPE_LABEL_NAME = "cluster_type";

    private static final String METRIC_NAME_VALUE = "cb_cluster_state";

    private final MetricsRecordProcessor metricsRecordProcessor;

    private final EntitlementService entitlementService;

    private final MetricsProcessorConfiguration configuration;

    public MetricsClient(MetricsRecordProcessor metricsRecordProcessor, EntitlementService entitlementService) {
        this.metricsRecordProcessor = metricsRecordProcessor;
        this.entitlementService = entitlementService;
        this.configuration = metricsRecordProcessor.getConfiguration();
    }

    public void processStackStatus(String resourceCrn, String platform, String status, Integer statusOrdinal) {
        if (!configuration.isEnabled()) {
            LOGGER.debug("Processing stack status (compute monitoring) is disabled.");
            return;
        }
        if (!configuration.isComputeMonitoringSupported()) {
            LOGGER.debug("Compute metrics processing is skipped (disabled).");
            return;
        }
        Crn crn = Crn.safeFromString(resourceCrn);
        if (configuration.isPaasSupported() || entitlementService.isCdpSaasEnabled(crn.getAccountId())) {
            processRequest(resourceCrn, platform, status, statusOrdinal, crn);
        } else {
            LOGGER.debug("Compute metrics processing is skipped (no paas or entitlement support )");
        }
    }

    private void processRequest(String resourceCrn, String platform, String status, Integer statusOrdinal, Crn crn) {
        Remote.WriteRequest writeRequest = Remote.WriteRequest.newBuilder()
                .addMetadata(Types.MetricMetadata.newBuilder()
                        .setType(Types.MetricMetadata.MetricType.INFO)
                        .build())
                .addTimeseries(Types.TimeSeries.newBuilder()
                        .addLabels(Types.Label.newBuilder()
                                .setName(METRIC_LABEL_NAME)
                                .setValue(METRIC_NAME_VALUE)
                                .build())
                        .addLabels(Types.Label.newBuilder()
                                .setName(RESOURCE_CRN_LABEL_NAME)
                                .setValue(resourceCrn)
                                .build())
                        .addLabels(Types.Label.newBuilder()
                                .setName(PLATFORM_LABEL_NAME)
                                .setValue(platform)
                                .build())
                        .addLabels(Types.Label.newBuilder()
                                .setName(CLUSTER_STATUS_LABEL_NAME)
                                .setValue(status)
                                .build())
                        .addLabels(Types.Label.newBuilder()
                                .setName(CLUSTER_TYPE_LABEL_NAME)
                                .setValue(crn.getService().getName())
                                .build())
                        .addSamples(Types.Sample.newBuilder()
                                .setValue(statusOrdinal.doubleValue())
                                .setTimestamp(System.currentTimeMillis())
                                .build())
                ).build();
        MetricsRecordRequest request = new MetricsRecordRequest(writeRequest, Crn.safeFromString(resourceCrn).getAccountId());
        metricsRecordProcessor.processRecord(request);
    }
}
