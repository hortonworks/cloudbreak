package com.sequenceiq.cloudbreak.cloud.gcp.client.metric;

import java.time.Duration;

import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseInterceptor;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;

public class MetricLoggerInterceptor implements HttpExecuteInterceptor, HttpResponseInterceptor {

    private final MetricService metricService;

    private final String gcpService;

    private long start;

    public MetricLoggerInterceptor(MetricService metricService, String gcpService) {
        this.metricService = metricService;
        this.gcpService = gcpService;
    }

    @Override
    public void intercept(HttpRequest request) {
        start = System.nanoTime();
    }

    @Override
    public void interceptResponse(HttpResponse response) {
        long finished = System.nanoTime() - start;
        Duration duration = Duration.ofNanos(finished);
        metricService.recordTimerMetric(GcpMetricType.REQUEST_TIME, duration, GcpMetricTag.GCP_SERVICE.name(), gcpService);
    }
}
