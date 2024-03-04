package com.sequenceiq.remoteenvironment.controller.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.RemoteControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.RemoteControlPlaneRegistrationResponse;

@Component
public class RemoteControlPlaneRegistrationRequestToRemoteControlPlaneRegistrationResponseConverter {

    public RemoteControlPlaneRegistrationResponse convert(RemoteControlPlaneRegistrationRequest source) {
        RemoteControlPlaneRegistrationResponse response = new RemoteControlPlaneRegistrationResponse();
        response.setCrn(source.getCrn());
        return response;
    }

}
