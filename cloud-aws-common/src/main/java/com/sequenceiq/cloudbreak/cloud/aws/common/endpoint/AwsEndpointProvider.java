package com.sequenceiq.cloudbreak.cloud.aws.common.endpoint;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.client.builder.AwsClientBuilder;

@Component
public class AwsEndpointProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEndpointProvider.class);

    @Value("${aws.fips.use.endpoint:false}")
    private boolean fipsEnabled;

    @Inject
    private AwsRegionEndpointProvider awsRegionEndpointProvider;

    @Inject
    private AwsServiceEndpointProvider awsServiceEndpointProvider;

    public <T extends AwsClientBuilder> void setupEndpoint(T clientBuilder, String service, String region, boolean govCloud) {
        if (fipsEnabled && govCloud) {
            LOGGER.debug("Aws FIPS endpoint will be used for {} service in {} region on govcloud", service, region);
            clientBuilder.withEndpointConfiguration(endpointConfiguration(service, region, govCloud));
        } else {
            LOGGER.debug("Default aws endpoint will be used for {} service in {} region, govcloud: {}", service, region, govCloud);
            clientBuilder.withRegion(region);
        }
    }

    public String getEndpointString(String service, String region, boolean govCloud) {
        String servicePart = awsServiceEndpointProvider.service(service, govCloud);
        String regionPart = awsRegionEndpointProvider.region(service, region, govCloud);
        return String.format("https://%s.%s.amazonaws.com", servicePart, regionPart);
    }

    AwsClientBuilder.EndpointConfiguration endpointConfiguration(String service, String region, boolean govCloud) {
        String endpoint = getEndpointString(service, region, govCloud);
        LOGGER.debug("The aws endpoint for the {} service: {}", service, endpoint);
        return new AwsClientBuilder.EndpointConfiguration(endpoint, region);
    }

}
