package com.sequenceiq.cloudbreak.cloud.gcp.client;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.ExponentialBackOff;
import com.sequenceiq.cloudbreak.cloud.gcp.client.metric.CompositeHttpExecuteInterceptor;
import com.sequenceiq.cloudbreak.cloud.gcp.client.metric.MetricLoggerInterceptor;
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
            request.setIOExceptionHandler(new HttpBackOffIOExceptionHandler(backOff));
            request.setUnsuccessfulResponseHandler(unsuccessfulResponseHandler(credential, backOff));
        };
    }

    private HttpUnsuccessfulResponseHandler unsuccessfulResponseHandler(GoogleCredential credential, ExponentialBackOff backOff) {
        HttpBackOffUnsuccessfulResponseHandler httpBackOffUnsuccessfulRespHandler = new HttpBackOffUnsuccessfulResponseHandler(backOff);
        return (request, response, supportsRetry) ->
                credential.handleResponse(request, response, supportsRetry)
                        || httpBackOffUnsuccessfulRespHandler.handleResponse(request, response, supportsRetry);
    }
}
