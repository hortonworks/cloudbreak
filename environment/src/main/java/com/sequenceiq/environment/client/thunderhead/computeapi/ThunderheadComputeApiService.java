package com.sequenceiq.environment.client.thunderhead.computeapi;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderheadcompute.api.DefaultApi;
import com.cloudera.thunderheadcompute.model.DescribeCustomConfigRequest;
import com.cloudera.thunderheadcompute.model.DescribeCustomConfigResponse;
import com.google.common.base.Preconditions;
import com.sequenceiq.environment.environment.dto.dataservices.CustomDockerRegistryParameters;

@Service
public class ThunderheadComputeApiService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThunderheadComputeApiService.class);

    @Inject
    private ThunderheadComputeApiClientFactory computeApiClientFactory;

    public boolean customConfigDescribable(CustomDockerRegistryParameters customDockerRegistryParameters) {
        boolean result = false;
        Preconditions.checkNotNull(customDockerRegistryParameters, "customDockerRegistryParameters must not be null.");
        try {
            DefaultApi computeApiClient = computeApiClientFactory.create();
            DescribeCustomConfigRequest describeCustomConfigRequest = new DescribeCustomConfigRequest().crn(customDockerRegistryParameters.crn());
            DescribeCustomConfigResponse describeCustomConfigResponse = computeApiClient.describeCustomConfig(describeCustomConfigRequest);
            result = customDockerRegistryParameters.crn().equals(describeCustomConfigResponse.getCrn());
            LOGGER.info("Custom docker registry config could be found on Thunderhead Compute API with name: '{}' and CRN: '{}'",
                    describeCustomConfigResponse.getName(), describeCustomConfigResponse.getCrn());
        } catch (Exception exception) {
            LOGGER.warn("Custom docker registry config could NOT be described using Thunderhead Compute API with CRN: '{}'",
                    customDockerRegistryParameters.crn(), exception);
        }
        return result;
    }
}
