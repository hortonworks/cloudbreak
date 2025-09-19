package com.sequenceiq.remoteenvironment.controller.v1.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponses;
import com.sequenceiq.remoteenvironment.service.RemoteEnvironmentConnectorProvider;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@DisableCheckPermissions
@Controller
public class RemoteEnvironmentController implements RemoteEnvironmentEndpoint {

    @Inject
    private RemoteEnvironmentConnectorProvider remoteEnvironmentConnectorProvider;

    @Override
    public SimpleRemoteEnvironmentResponses list() {
        MDCBuilder.buildMdcContext();
        // TODO CB-30614 extend for all remote environment types
        return remoteEnvironmentConnectorProvider.getForType(RemoteEnvironmentConnectorType.PRIVATE_CONTROL_PLANE)
                .list(ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public DescribeEnvironmentResponse getByCrn(DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext();
        return remoteEnvironmentConnectorProvider.getForCrn(request.getCrn())
                .describeV1(ThreadBasedUserCrnProvider.getAccountId(), request);
    }

    @Override
    public DescribeDatalakeAsApiRemoteDataContextResponse getRdcByCrn(DescribeRemoteEnvironment request) {
        return remoteEnvironmentConnectorProvider.getForCrn(request.getCrn())
                .getRemoteDataContext(ThreadBasedUserCrnProvider.getAccountId(), request.getCrn());
    }

    @Override
    public DescribeDatalakeServicesResponse getDatalakeServicesByCrn(DescribeDatalakeServicesRequest request) {
        return remoteEnvironmentConnectorProvider.getForCrn(request.getClusterid())
                .getDatalakeServices(ThreadBasedUserCrnProvider.getAccountId(), request.getClusterid());
    }

    @Override
    public GetRootCertificateResponse getRootCertificateByCrn(String environmentCrn) {
        return remoteEnvironmentConnectorProvider.getForCrn(environmentCrn)
                .getRootCertificate(ThreadBasedUserCrnProvider.getAccountId(), environmentCrn);
    }
}
