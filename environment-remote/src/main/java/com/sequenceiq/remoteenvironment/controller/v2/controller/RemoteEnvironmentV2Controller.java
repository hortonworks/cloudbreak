package com.sequenceiq.remoteenvironment.controller.v2.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentV2Endpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.service.RemoteEnvironmentConnectorProvider;

@DisableCheckPermissions
@Controller
public class RemoteEnvironmentV2Controller implements RemoteEnvironmentV2Endpoint {

    @Inject
    private RemoteEnvironmentConnectorProvider remoteEnvironmentConnectorProvider;

    @Override
    public DescribeEnvironmentV2Response getByCrn(DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext();
        return remoteEnvironmentConnectorProvider.getForCrn(request.getCrn())
                .describeV2(ThreadBasedUserCrnProvider.getAccountId(), request);
    }
}
