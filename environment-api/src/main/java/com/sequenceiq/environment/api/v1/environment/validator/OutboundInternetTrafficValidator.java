package com.sequenceiq.environment.api.v1.environment.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;

public class OutboundInternetTrafficValidator implements ConstraintValidator<ValidOutboundInternetTrafficNetworkRequest, EnvironmentNetworkRequest> {
    @Override
    public boolean isValid(EnvironmentNetworkRequest environmentNetworkRequest, ConstraintValidatorContext constraintValidatorContext) {
        OutboundInternetTraffic outboundInternetTraffic = environmentNetworkRequest.getOutboundInternetTraffic();
        ServiceEndpointCreation serviceEndpointCreation = environmentNetworkRequest.getServiceEndpointCreation();
        if (outboundInternetTraffic == OutboundInternetTraffic.DISABLED
                && (serviceEndpointCreation == null || serviceEndpointCreation == ServiceEndpointCreation.DISABLED)) {
            return false;
        }
        return true;
    }
}
