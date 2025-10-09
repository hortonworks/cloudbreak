package com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane;

import static com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane.PrivateControlPlaneClient.DL_SERVICES_PATH;
import static com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane.PrivateControlPlaneClient.RDC_PATH;
import static com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane.PrivateControlPlaneClient.ROOT_CERT_PATH;
import static com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane.PrivateControlPlaneClient.SERVICE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyProxyClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyProxyClientFactory;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PrivateControlPlaneClientTest {

    private static final String CONTROL_PLANE = "controlPlane";

    private static final String ENVIRONMENT = "environment";

    private static final String DATALAKE = "datalake";

    private static final String ERROR_RESPONSE = "error";

    private static final String CLUSTER_ID = "clusterId";

    private static final String USER_CRN = "user";

    private static final String CERT = "certecske";

    @Mock
    private ClusterProxyProxyClientFactory fastClusterProxyProxyClientFactory;

    @Mock
    private ClusterProxyProxyClientFactory slowClusterProxyProxyClientFactory;

    @Mock
    private ClusterProxyProxyClient clusterProxyProxyClient;

    @InjectMocks
    private PrivateControlPlaneClient underTest;

    @Mock
    private DescribeDatalakeAsApiRemoteDataContextResponse describeDatalakeAsApiRemoteDataContextResponse;

    @Mock
    private DescribeDatalakeServicesResponse describeDatalakeServicesResponse;

    @Mock
    private GetRootCertificateResponse getRootCertificateResponse;

    @BeforeEach
    public void setup() {
        when(describeDatalakeAsApiRemoteDataContextResponse.getDatalake()).thenReturn(DATALAKE);
        when(describeDatalakeServicesResponse.getClusterid()).thenReturn(DATALAKE);
        when(getRootCertificateResponse.getContents()).thenReturn(CERT);
        when(fastClusterProxyProxyClientFactory.create(CONTROL_PLANE, SERVICE_NAME, USER_CRN)).thenReturn(clusterProxyProxyClient);
        when(slowClusterProxyProxyClientFactory.create(CONTROL_PLANE, SERVICE_NAME, USER_CRN)).thenReturn(clusterProxyProxyClient);
    }

    @Test
    public void testGetRemoteDataContext() {
        ArgumentCaptor<DescribeDatalakeAsApiRemoteDataContextRequest> requestCaptor =
                ArgumentCaptor.forClass(DescribeDatalakeAsApiRemoteDataContextRequest.class);
        when(clusterProxyProxyClient.post(eq(RDC_PATH), requestCaptor.capture(), eq(DescribeDatalakeAsApiRemoteDataContextResponse.class)))
                .thenReturn(describeDatalakeAsApiRemoteDataContextResponse);

        DescribeDatalakeAsApiRemoteDataContextResponse result = underTest.getRemoteDataContext(CONTROL_PLANE, USER_CRN, ENVIRONMENT);

        assertEquals(requestCaptor.getValue().getDatalake(), ENVIRONMENT);
        assertEquals(DATALAKE, describeDatalakeAsApiRemoteDataContextResponse.getDatalake());
    }

    @Test
    public void testGetDatalakeServices() {
        ArgumentCaptor<DescribeDatalakeServicesRequest> requestCaptor =
                ArgumentCaptor.forClass(DescribeDatalakeServicesRequest.class);
        when(clusterProxyProxyClient.post(eq(DL_SERVICES_PATH), requestCaptor.capture(), eq(DescribeDatalakeServicesResponse.class)))
                .thenReturn(describeDatalakeServicesResponse);

        DescribeDatalakeServicesResponse result = underTest.getDatalakeServices(CONTROL_PLANE, USER_CRN, ENVIRONMENT);

        assertEquals(requestCaptor.getValue().getClusterid(), ENVIRONMENT);
        assertEquals(DATALAKE, result.getClusterid());
    }

    @Test
    public void testGetRootCertificate() {
        ArgumentCaptor<GetRootCertificateRequest> requestCaptor =
                ArgumentCaptor.forClass(GetRootCertificateRequest.class);
        when(clusterProxyProxyClient.post(eq(ROOT_CERT_PATH), requestCaptor.capture(), eq(GetRootCertificateResponse.class)))
                .thenReturn(getRootCertificateResponse);

        GetRootCertificateResponse result = underTest.getRootCertificate(CONTROL_PLANE, USER_CRN, ENVIRONMENT);

        assertEquals(requestCaptor.getValue().getEnvironmentName(), ENVIRONMENT);
        assertEquals(CERT, result.getContents());
    }
}
