package com.sequenceiq.remoteenvironment.controller.v1.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.endpoint.PrivateControlPlaneEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneDeRegistrationResponse;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneRegistrationResponse;
import com.sequenceiq.remoteenvironment.service.PrivateControlPlaneService;

@Controller
public class PrivateControlPlaneController implements PrivateControlPlaneEndpoint {

    @Inject
    private PrivateControlPlaneService remoteControlPlaneService;

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public PrivateControlPlaneRegistrationResponse register(PrivateControlPlaneRegistrationRequest request) {
        return remoteControlPlaneService.register(request);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public PrivateControlPlaneDeRegistrationResponse deregister(String crn) {
        return remoteControlPlaneService.deregister(crn);
    }
}
