package com.sequenceiq.remoteenvironment.controller.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneDeRegistrationResponse;

@Component
public class PrivateControlPlaneDeRegistrationRequestToPrivateControlPlaneDeRegistrationResponseConverter {

    public PrivateControlPlaneDeRegistrationResponse convert(String crn) {
        PrivateControlPlaneDeRegistrationResponse response = new PrivateControlPlaneDeRegistrationResponse();
        response.setCrn(crn);
        return response;
    }

}
