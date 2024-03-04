package com.sequenceiq.remoteenvironment.controller.v1.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponses;
import com.sequenceiq.remoteenvironment.service.RemoteEnvironmentService;

@Controller
public class RemoteEnvironmentController implements RemoteEnvironmentEndpoint {

    public static final int TEN = 10;

    @Inject
    private RemoteEnvironmentService remoteEnvironmentService;

    @Override
    public SimpleRemoteEnvironmentResponses list() {
        return new SimpleRemoteEnvironmentResponses(remoteEnvironmentService.listRemoteEnvironment(TEN));
    }
}
