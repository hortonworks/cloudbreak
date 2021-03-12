package com.sequenceiq.cloudbreak.cloud.gcp.client;

import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.http.apache.ApacheHttpTransport;
import com.sequenceiq.cloudbreak.cloud.gcp.tracing.GcpTracingInterceptor;

@Configuration
public class GcpHttpClientConfig {

    @Bean
    public ApacheHttpTransport gcpApacheHttpTransport(GcpTracingInterceptor gcpTracingInterceptor) {
        DefaultHttpClient defaultHttpClient = ApacheHttpTransport.newDefaultHttpClient();
        defaultHttpClient.addRequestInterceptor(gcpTracingInterceptor);
        defaultHttpClient.addResponseInterceptor(gcpTracingInterceptor);
        return new ApacheHttpTransport(defaultHttpClient);
    }
}
