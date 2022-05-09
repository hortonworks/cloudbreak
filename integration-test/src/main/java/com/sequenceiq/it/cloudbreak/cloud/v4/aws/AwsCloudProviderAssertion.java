package com.sequenceiq.it.cloudbreak.cloud.v4.aws;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProviderAssertion;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class AwsCloudProviderAssertion extends AbstractCloudProviderAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudProviderAssertion.class);

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public void assertServiceEndpoint(EnvironmentTestDto environmentTestDto) {
        ServiceEndpointCreation serviceEndpointCreationRequest = Optional.ofNullable(environmentTestDto.getRequest().getNetwork().getServiceEndpointCreation())
                .orElse(ServiceEndpointCreation.DISABLED);
        ServiceEndpointCreation serviceEndpointCreationResponse = environmentTestDto.getResponse().getNetwork().getServiceEndpointCreation();
        if (serviceEndpointCreationResponse != serviceEndpointCreationRequest) {
            String message = String.format("Service endpoint creation is expected to be %s, but is %s", serviceEndpointCreationRequest,
                    serviceEndpointCreationResponse);
            LOGGER.error(message);
            throw new TestFailException(message);
        }

    }
}
