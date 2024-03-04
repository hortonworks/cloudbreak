package com.sequenceiq.remoteenvironment.controller.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.RemoteControlPlaneDeRegistrationResponse;

@Component
public class RemoteControlPlaneDeRegistrationRequestToRemoteControlPlaneDeRegistrationResponseConverter {

    public RemoteControlPlaneDeRegistrationResponse convert(String crn) {
        RemoteControlPlaneDeRegistrationResponse response = new RemoteControlPlaneDeRegistrationResponse();
        response.setCrn(crn);
        return response;
    }

}
