package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.PrivateEndpointType;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;

@Component
public class ServiceEndpointCreationToEndpointTypeConverter {

    //CHECKSTYLE:OFF:FallThroughCheck
    public PrivateEndpointType convert(ServiceEndpointCreation serviceEndpointCreation, String cloudPlatform) {
        if (serviceEndpointCreation == null) {
            return PrivateEndpointType.NONE;
        }
        switch (serviceEndpointCreation) {
            case ENABLED_PRIVATE_ENDPOINT:
                return PrivateEndpointType.USE_PRIVATE_ENDPOINT;
            case ENABLED:
                if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
                    return PrivateEndpointType.NONE;
                } else if (CloudPlatform.AWS.equalsIgnoreCase(cloudPlatform)) {
                    return PrivateEndpointType.USE_VPC_ENDPOINT;
                }
            case DISABLED:
            default:
                return PrivateEndpointType.NONE;
        }
    }
    //CHECKSTYLE:ON
}
