package com.sequenceiq.remoteenvironment.controller.v1.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponses;
import com.sequenceiq.remoteenvironment.service.RemoteEnvironmentService;

@Controller
public class RemoteEnvironmentController implements RemoteEnvironmentEndpoint {

    @Inject
    private RemoteEnvironmentService remoteEnvironmentService;

    @Override
    @DisableCheckPermissions
    public SimpleRemoteEnvironmentResponses list() {
        MDCBuilder.buildMdcContext();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return new SimpleRemoteEnvironmentResponses(remoteEnvironmentService.listRemoteEnvironment(accountId));
    }
}
