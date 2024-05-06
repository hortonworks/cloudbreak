package com.sequenceiq.remoteenvironment.controller.v1.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
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
        return new SimpleRemoteEnvironmentResponses(
                remoteEnvironmentService.listRemoteEnvironments(ThreadBasedUserCrnProvider.getAccountId()));
    }

    @Override
    @DisableCheckPermissions
    public DescribeEnvironmentResponse getByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @ResourceCrn String crn) {
        MDCBuilder.buildMdcContext();
        return remoteEnvironmentService
                .getRemoteEnvironment(ThreadBasedUserCrnProvider.getAccountId(), crn);
    }
}
