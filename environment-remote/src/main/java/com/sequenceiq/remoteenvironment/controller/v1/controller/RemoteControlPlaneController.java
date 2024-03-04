package com.sequenceiq.remoteenvironment.controller.v1.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.endpoint.RemoteControlPlaneEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.RemoteControlPlaneDeRegistrationResponse;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.RemoteControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.RemoteControlPlaneRegistrationResponse;
import com.sequenceiq.remoteenvironment.service.RemoteControlPlaneService;

@Controller
public class RemoteControlPlaneController implements RemoteControlPlaneEndpoint {

    @Inject
    private RemoteControlPlaneService remoteControlPlaneService;

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public RemoteControlPlaneRegistrationResponse register(RemoteControlPlaneRegistrationRequest request) {
        return remoteControlPlaneService.register(request);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public RemoteControlPlaneDeRegistrationResponse deregister(String crn) {
        return remoteControlPlaneService.deregister(crn);
    }
}
