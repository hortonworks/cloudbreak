package com.sequenceiq.remoteenvironment.controller.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneRegistrationResponse;

@Component
public class PrivateControlPlaneRegistrationRequestToPrivateControlPlaneRegistrationResponseConverter {

    public PrivateControlPlaneRegistrationResponse convert(PrivateControlPlaneRegistrationRequest source) {
        PrivateControlPlaneRegistrationResponse response = new PrivateControlPlaneRegistrationResponse();
        response.setCrn(source.getCrn());
        return response;
    }

}
