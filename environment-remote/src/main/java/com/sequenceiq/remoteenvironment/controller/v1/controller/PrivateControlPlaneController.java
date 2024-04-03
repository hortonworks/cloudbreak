package com.sequenceiq.remoteenvironment.controller.v1.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.endpoint.PrivateControlPlaneEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationRequests;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationResponses;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequests;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationResponses;
import com.sequenceiq.remoteenvironment.service.PrivateControlPlaneService;

@Controller
public class PrivateControlPlaneController implements PrivateControlPlaneEndpoint {

    @Inject
    private PrivateControlPlaneService remoteControlPlaneService;

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public PrivateControlPlaneRegistrationResponses register(PrivateControlPlaneRegistrationRequests request) {
        MDCBuilder.buildMdcContext(request);
        return remoteControlPlaneService.register(request);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public PrivateControlPlaneDeRegistrationResponses deregister(PrivateControlPlaneDeRegistrationRequests request) {
        MDCBuilder.buildMdcContext(request);
        return remoteControlPlaneService.deregister(request);
    }
}
