package com.sequenceiq.remoteenvironment.service.connector;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponses;

public interface RemoteEnvironmentConnector {

    RemoteEnvironmentConnectorType type();

    default SimpleRemoteEnvironmentResponses list(String publicCloudAccountId) {
        throw new UnsupportedOperationException("Not implemented for " + type());
    }

    default DescribeEnvironmentResponse describeV1(String publicCloudAccountId, DescribeRemoteEnvironment environment) {
        throw new UnsupportedOperationException("Not implemented for " + type());
    }

    default DescribeEnvironmentV2Response describeV2(String publicCloudAccountId, DescribeRemoteEnvironment environment) {
        throw new UnsupportedOperationException("Not implemented for " + type());
    }

    default GetRootCertificateResponse getRootCertificate(String publicCloudAccountId, String environmentCrn) {
        throw new UnsupportedOperationException("Not implemented for " + type());
    }

    default DescribeDatalakeAsApiRemoteDataContextResponse getRemoteDataContext(String publicCloudAccountId, String environmentCrn) {
        throw new UnsupportedOperationException("Not implemented for " + type());
    }

    default DescribeDatalakeServicesResponse getDatalakeServices(String publicCloudAccountId, String environmentCrn) {
        throw new UnsupportedOperationException("Not implemented for " + type());
    }
}
