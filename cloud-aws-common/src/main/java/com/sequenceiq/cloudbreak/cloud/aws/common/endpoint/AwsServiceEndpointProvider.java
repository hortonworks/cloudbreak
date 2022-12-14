package com.sequenceiq.cloudbreak.cloud.aws.common.endpoint;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AwsServiceEndpointProvider {

    @Value("${aws.fips.suffixed.services:kms,elasticfilesystem,s3}")
    private Set<String> fipsSuffixedServices;

    public String service(String service, boolean govCloud) {
        return fipsSuffixedServices.contains(service) ? centralService(service, govCloud) : service;
    }

    private String centralService(String service, boolean govCloud) {
        return govCloud ? (service + "-fips") : service;
    }
}
