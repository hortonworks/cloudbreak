package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

@Component
public class AwsApacheClient {

    @Value("${cb.aws.maxconnections:500}")
    private Integer maxConenections;

    private SdkHttpClient sdkHttpClient;

    @PostConstruct
    private void init() {
        sdkHttpClient = ApacheHttpClient.builder().maxConnections(maxConenections).build();
    }

    public SdkHttpClient getApacheHttpClient() {
        return sdkHttpClient;
    }
}
