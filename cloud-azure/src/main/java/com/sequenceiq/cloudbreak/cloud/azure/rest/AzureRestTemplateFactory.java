package com.sequenceiq.cloudbreak.cloud.azure.rest;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AzureRestTemplateFactory {

    public RestTemplate create() {
        return new RestTemplate();
    }
}
