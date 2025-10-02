package com.sequenceiq.remoteenvironment.controller.v2.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentV2Endpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.service.RemoteEnvironmentService;

@DisableCheckPermissions
@Controller
public class RemoteEnvironmentV2Controller implements RemoteEnvironmentV2Endpoint {

    @Inject
    private RemoteEnvironmentService remoteEnvironmentService;

    @Override
    public DescribeEnvironmentV2Response getByCrn(DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext();
        return remoteEnvironmentService.describeV2(request);
    }
}
