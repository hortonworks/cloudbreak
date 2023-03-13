package com.sequenceiq.cloudbreak.cloud.aws.common.endpoint;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AwsEndpointProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEndpointProvider.class);

    @Value("${aws.use.fips.endpoint:false}")
    private boolean fipsEnabled;

    @Inject
    private AwsRegionEndpointProvider awsRegionEndpointProvider;

    @Inject
    private AwsServiceEndpointProvider awsServiceEndpointProvider;

    public Optional<String> setupFipsEndpointIfNecessary(String service, String region, boolean govCloud) {
        if (fipsEnabled && govCloud) {
            LOGGER.debug("Aws FIPS endpoint will be used for {} service in {} region on govcloud", service, region);
            return Optional.of(endpointConfiguration(service, region, govCloud));
        } else {
            LOGGER.debug("Default aws endpoint will be used for {} service in {} region, govcloud: {}", service, region, govCloud);
            return Optional.empty();
        }
    }

    private String endpointConfiguration(String service, String region, boolean govCloud) {
        String servicePart = awsServiceEndpointProvider.service(service, govCloud);
        String regionPart = awsRegionEndpointProvider.region(service, region, govCloud);
        String endpoint = String.format("https://%s.%s.amazonaws.com", servicePart, regionPart);
        LOGGER.debug("The aws endpoint for the {} service: {}", service, endpoint);
        return endpoint;
    }

}
