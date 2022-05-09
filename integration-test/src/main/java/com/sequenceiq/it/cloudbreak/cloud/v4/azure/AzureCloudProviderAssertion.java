package com.sequenceiq.it.cloudbreak.cloud.v4.azure;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProviderAssertion;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class AzureCloudProviderAssertion extends AbstractCloudProviderAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudProviderAssertion.class);

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public void assertServiceEndpoint(EnvironmentTestDto environmentTestDto) {
        ServiceEndpointCreation serviceEndpointCreationRequest = Optional.ofNullable(environmentTestDto.getRequest().getNetwork().getServiceEndpointCreation())
                .orElse(ServiceEndpointCreation.DISABLED);
        ServiceEndpointCreation serviceEndpointCreationResponse = environmentTestDto.getResponse().getNetwork().getServiceEndpointCreation();
        if (serviceEndpointCreationRequest != serviceEndpointCreationResponse) {
            String message = String.format("Service endpoint creation in request (%s) does not match that in response (%s)", serviceEndpointCreationRequest,
                    serviceEndpointCreationResponse);
            LOGGER.error(message);
            throw new TestFailException(message);
        }

        String databasePrivateDnsZoneIdRequest = environmentTestDto.getRequest().getNetwork().getAzure().getDatabasePrivateDnsZoneId();
        String databasePrivateDnsZoneIdResponse = environmentTestDto.getResponse().getNetwork().getAzure().getDatabasePrivateDnsZoneId();
        if (StringUtils.isNotEmpty(databasePrivateDnsZoneIdRequest) && !databasePrivateDnsZoneIdRequest.equals(databasePrivateDnsZoneIdResponse)) {
            String message = String.format("Private DNS zone id for database was requested to be %s, but is %s", databasePrivateDnsZoneIdRequest,
                    databasePrivateDnsZoneIdResponse);
            LOGGER.error(message);
            throw new TestFailException(message);
        }

    }
}
