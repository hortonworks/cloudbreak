package com.sequenceiq.remoteenvironment.controller.v1.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponses;
import com.sequenceiq.remoteenvironment.service.RemoteEnvironmentService;

@DisableCheckPermissions
@Controller
public class RemoteEnvironmentController implements RemoteEnvironmentEndpoint {

    @Inject
    private RemoteEnvironmentService remoteEnvironmentService;

    @Override
    public SimpleRemoteEnvironmentResponses list() {
        MDCBuilder.buildMdcContext();
        return new SimpleRemoteEnvironmentResponses(
                remoteEnvironmentService.listRemoteEnvironments(ThreadBasedUserCrnProvider.getAccountId()));
    }

    @Override
    public DescribeEnvironmentResponse getByCrn(DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext();
        return remoteEnvironmentService
                .getRemoteEnvironment(ThreadBasedUserCrnProvider.getAccountId(), request.getCrn());
    }

    @Override
    public DescribeDatalakeAsApiRemoteDataContextResponse getRdcByCrn(DescribeRemoteEnvironment request) {
        return remoteEnvironmentService.getRdcForEnvironment(ThreadBasedUserCrnProvider.getAccountId(), request.getCrn());
    }

    @Override
    public DescribeDatalakeServicesResponse getDatalakeServicesByCrn(DescribeDatalakeServicesRequest request) {
        return remoteEnvironmentService.getDatalakeServicesForEnvironment(ThreadBasedUserCrnProvider.getAccountId(), request.getClusterid());
    }
}
