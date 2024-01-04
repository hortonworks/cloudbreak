package com.sequenceiq.cloudbreak.cloud.gcp.client;

import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.ExponentialBackOff;
import com.sequenceiq.cloudbreak.cloud.gcp.client.metric.CompositeHttpExecuteInterceptor;
import com.sequenceiq.cloudbreak.cloud.gcp.client.metric.MetricLoggerInterceptor;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpRequestTimeout;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;

public abstract class GcpServiceFactory {

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    protected HttpRequestInitializer requestInitializer(GoogleCredential credential) {
        ExponentialBackOff backOff = new ExponentialBackOff();
        return request -> {
            credential.initialize(request);
            MetricLoggerInterceptor metricLoggerInterceptor =
                    new MetricLoggerInterceptor(metricService, getClass().getSimpleName().toLowerCase(Locale.ROOT));
            request.setResponseInterceptor(metricLoggerInterceptor);
            CompositeHttpExecuteInterceptor compositeHttpExecuteInterceptor = new CompositeHttpExecuteInterceptor(List.of(credential, metricLoggerInterceptor));
            request.setInterceptor(compositeHttpExecuteInterceptor);
            request.setConnectTimeout(GcpRequestTimeout.getTimeout());
            request.setReadTimeout(GcpRequestTimeout.getTimeout());
            request.setIOExceptionHandler(new GcpLoggingHttpBackOffIOExceptionHandler(backOff));
            request.setUnsuccessfulResponseHandler(new GcpCustomHttpUnsuccessfulResponseHandler(credential, backOff));
        };
    }
}
