package com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListEnvironmentsResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyProxyClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyProxyClientFactory;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;

@Component
public class PrivateControlPlaneClient {

    static final String SERVICE_NAME = "PvcControlPlane";

    static final String LIST_PATH = "api/v1/environments2/listEnvironments";

    static final String DESCRIBE_PATH = "api/v1/environments2/describeEnvironment";

    static final String RDC_PATH = "api/v1/servicediscovery/describeDatalakeAsApiRemoteDataContext";

    static final String DL_SERVICES_PATH = "api/v1/servicediscovery/describeDatalakeServices";

    static final String ROOT_CERT_PATH = "api/v1/environments2/getRootCertificate";

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateControlPlaneClient.class);

    @Inject
    private ClusterProxyProxyClientFactory fastClusterProxyProxyClientFactory;

    @Inject
    private ClusterProxyProxyClientFactory slowClusterProxyProxyClientFactory;

    public ListEnvironmentsResponse listEnvironments(String clusterIdentifier, String userCrn) {
        LOGGER.info("Reading remote cluster list with cluster proxy configuration for cluster identifer: {}", clusterIdentifier);
        ClusterProxyProxyClient clusterProxyProxyClient = fastClusterProxyProxyClientFactory.create(clusterIdentifier, SERVICE_NAME, userCrn);
        return clusterProxyProxyClient.post(LIST_PATH, new Object(), ListEnvironmentsResponse.class);
    }

    public DescribeEnvironmentV2Response getEnvironment(String clusterIdentifier, String userCrn, String environmentCrn) {
        LOGGER.info("Reading remote cluster with cluster proxy configuration for cluster identifer: {}", clusterIdentifier);
        DescribeEnvironmentRequest postRequest = new DescribeEnvironmentRequest();
        postRequest.setEnvironmentName(environmentCrn);
        postRequest.setOutputView(DescribeEnvironmentRequest.OutputViewEnum.FULL);
        ClusterProxyProxyClient clusterProxyProxyClient = slowClusterProxyProxyClientFactory.create(clusterIdentifier, SERVICE_NAME, userCrn);
        return clusterProxyProxyClient.post(DESCRIBE_PATH, postRequest, DescribeEnvironmentV2Response.class);
    }

    public DescribeDatalakeAsApiRemoteDataContextResponse getRemoteDataContext(String clusterIdentifier, String userCrn, String environmentCrn) {
        LOGGER.info("Reading Remote Data Context with cluster proxy configuration for cluster Identifier: {}", clusterIdentifier);
        DescribeDatalakeAsApiRemoteDataContextRequest postRequest = new DescribeDatalakeAsApiRemoteDataContextRequest();
        postRequest.setDatalake(environmentCrn);
        ClusterProxyProxyClient clusterProxyProxyClient = slowClusterProxyProxyClientFactory.create(clusterIdentifier, SERVICE_NAME, userCrn);
        return clusterProxyProxyClient.post(RDC_PATH, postRequest, DescribeDatalakeAsApiRemoteDataContextResponse.class);
    }

    public DescribeDatalakeServicesResponse getDatalakeServices(String clusterIdentifier, String userCrn, String environmentCrn) {
        LOGGER.info("Reading Datalake services with cluster proxy configuration for cluster Identifier: {}", clusterIdentifier);
        DescribeDatalakeServicesRequest postRequest = new DescribeDatalakeServicesRequest();
        postRequest.setClusterid(environmentCrn);
        ClusterProxyProxyClient clusterProxyProxyClient = slowClusterProxyProxyClientFactory.create(clusterIdentifier, SERVICE_NAME, userCrn);
        return clusterProxyProxyClient.post(DL_SERVICES_PATH, postRequest, DescribeDatalakeServicesResponse.class);
    }

    public GetRootCertificateResponse getRootCertificate(String clusterIdentifier, String userCrn, String environmentCrn) {
        LOGGER.info("Reading root certificate with cluster proxy configuration for cluster Identifier: {}", clusterIdentifier);
        GetRootCertificateRequest postRequest = new GetRootCertificateRequest().environmentName(environmentCrn);
        ClusterProxyProxyClient clusterProxyProxyClient = slowClusterProxyProxyClientFactory.create(clusterIdentifier, SERVICE_NAME, userCrn);
        return clusterProxyProxyClient.post(ROOT_CERT_PATH, postRequest, GetRootCertificateResponse.class);
    }
}
