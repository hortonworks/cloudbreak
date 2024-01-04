package com.sequenceiq.cloudbreak.cloud.aws.common.metrics;

import static com.sequenceiq.cloudbreak.cloud.aws.common.metrics.AwsMetricTag.OPERATION_NAME;
import static com.sequenceiq.cloudbreak.cloud.aws.common.metrics.AwsMetricTag.SERVICE_ID;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.base.CaseFormat;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;

import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.SdkMetric;

@Component
public class AwsMetricPublisher implements MetricPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsMetricPublisher.class);

    private static final Set<SdkMetric<?>> ALLOWED_METRICS = Set.of(CoreMetric.API_CALL_DURATION, CoreMetric.API_CALL_SUCCESSFUL,
            CoreMetric.SERVICE_CALL_DURATION, CoreMetric.RETRY_COUNT, HttpMetric.MAX_CONCURRENCY, HttpMetric.LEASED_CONCURRENCY,
            HttpMetric.PENDING_CONCURRENCY_ACQUIRES);

    private static final String AWS_METRIC_PREFIX = "aws_";

    private static final String API_CALL_FAILED_METRIC_NAME = "aws_api_call_failed_total";

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Override
    public void publish(MetricCollection metricCollection) {
        try {
            Optional<String> serviceId = metricCollection.metricValues(CoreMetric.SERVICE_ID).stream().filter(Objects::nonNull).findFirst();
            Optional<String> operationName = metricCollection.metricValues(CoreMetric.OPERATION_NAME).stream().filter(Objects::nonNull).findFirst();
            if (serviceId.isEmpty() || operationName.isEmpty()) {
                LOGGER.warn("ServiceId or OperationName is empty for AWS MetricCollection: {}", metricCollection);
                return;
            }

            publishMetrics(metricCollection, serviceId.get(), operationName.get());
        } catch (Exception e) {
            LOGGER.warn("Publishing AWS metrics failed", e);
        }
    }

    private void publishMetrics(MetricCollection metricCollection, String serviceId, String operationName) {
        metricCollection.stream()
                .filter(metricRecord -> metricRecord.metric() != null && metricRecord.value() != null)
                .filter(metricRecord -> ALLOWED_METRICS.contains(metricRecord.metric()))
                .forEach(metricRecord -> {
            String metricName = convertMetricName(metricRecord.metric().name());
            Class<?> metricValueClass = metricRecord.metric().valueClass();
            String[] tags = tags(serviceId, operationName);

            if (CoreMetric.API_CALL_SUCCESSFUL.equals(metricRecord.metric())) {
                Boolean success = (Boolean) metricRecord.value();
                if (Boolean.FALSE.equals(success)) {
                    metricService.incrementMetricCounter(API_CALL_FAILED_METRIC_NAME, tags);
                }
            } else if (Duration.class.isAssignableFrom(metricValueClass)) {
                Duration duration = (Duration) metricRecord.value();
                metricService.recordTimerMetric(metricName, duration, tags);
            } else if (Number.class.isAssignableFrom(metricValueClass)) {
                Double amount = Double.valueOf(metricRecord.value().toString());
                metricService.incrementMetricCounter(metricName, amount, tags);
            }
        });

        metricCollection.children().forEach(child -> publishMetrics(child, serviceId, operationName));
    }

    @Override
    public void close() {
    }

    private String convertMetricName(String metricName) {
        return AWS_METRIC_PREFIX + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, metricName);
    }

    private String[] tags(String serviceId, String operationName) {
        return new String[] { SERVICE_ID.name(), serviceId, OPERATION_NAME.name(), operationName };
    }
}
