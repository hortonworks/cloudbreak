package com.sequenceiq.cloudbreak.cloud.gcp.client;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;

@Configuration
public class GcpHttpClientConfig {

    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }
}
