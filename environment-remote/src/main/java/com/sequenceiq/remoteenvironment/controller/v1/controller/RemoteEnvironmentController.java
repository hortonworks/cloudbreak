package com.sequenceiq.remoteenvironment.controller.v1.controller;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
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
    public SimpleRemoteEnvironmentResponses list(List<String> types) {
        return remoteEnvironmentService.list(ThreadBasedUserCrnProvider.getUserCrn(), types);
    }

    @Override
    public DescribeEnvironmentResponse getByCrn(@RequestObject DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext(request);
        return remoteEnvironmentService.describeV1(ThreadBasedUserCrnProvider.getUserCrn(), request);
    }

    @Override
    public DescribeDatalakeAsApiRemoteDataContextResponse getRdcByCrn(@RequestObject DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext(request);
        return remoteEnvironmentService.getRdcByCrn(ThreadBasedUserCrnProvider.getUserCrn(), request);
    }

    @Override
    public DescribeDatalakeServicesResponse getDatalakeServicesByCrn(@RequestObject DescribeDatalakeServicesRequest request) {
        MDCBuilder.addResourceCrn(request.getClusterid());
        return remoteEnvironmentService.getDatalakeServicesByCrn(ThreadBasedUserCrnProvider.getUserCrn(), request);
    }

    @Override
    public GetRootCertificateResponse getRootCertificateByCrn(@RequestObject String environmentCrn) {
        MDCBuilder.addResourceCrn(environmentCrn);
        return remoteEnvironmentService.getRootCertificateByCrn(ThreadBasedUserCrnProvider.getUserCrn(), environmentCrn);
    }
}
