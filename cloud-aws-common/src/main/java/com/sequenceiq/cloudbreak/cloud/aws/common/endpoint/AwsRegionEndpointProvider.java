package com.sequenceiq.cloudbreak.cloud.aws.common.endpoint;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AwsRegionEndpointProvider {

    @Value("${aws.fips.region.independent.services:iam}")
    private Set<String> regionIndependentServices;

    public String region(String service, String region, boolean govCloud) {
        return regionIndependentServices.contains(service) ? centralRegion(region, govCloud) : region;
    }

    private String centralRegion(String region, boolean govCloud) {
        return govCloud ? "us-gov" : region;
    }
}
