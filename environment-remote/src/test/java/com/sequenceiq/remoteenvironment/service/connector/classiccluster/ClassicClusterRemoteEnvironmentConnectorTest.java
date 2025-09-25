package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;

@ExtendWith(MockitoExtension.class)
class ClassicClusterRemoteEnvironmentConnectorTest {

    private static final String CLUSTER_CRN = "crn";

    @Mock
    private RemoteClusterServiceClient remoteClusterServiceClient;

    @Mock
    private ClassicClusterRemoteDataContextProvider remoteDataContextProvider;

    @Mock
    private ClassicClusterRootCertificateProvider rootCertificateProvider;

    @Mock
    private ClassicClusterDatalakeServicesProvider datalakeServicesProvider;

    @InjectMocks
    private ClassicClusterRemoteEnvironmentConnector underTest;

    @Mock
    private OnPremisesApiProto.Cluster cluster;

    @BeforeEach
    void setUp() {
        lenient().when(remoteClusterServiceClient.describeClassicCluster(CLUSTER_CRN)).thenReturn(cluster);
    }

    @Test
    void getRemoteDataContext() throws Exception {
        DescribeDatalakeAsApiRemoteDataContextResponse rdc = mock();
        when(remoteDataContextProvider.getRemoteDataContext(cluster)).thenReturn(rdc);

        DescribeDatalakeAsApiRemoteDataContextResponse result = underTest.getRemoteDataContext("", CLUSTER_CRN);

        assertThat(result).isEqualTo(rdc);
    }

    @Test
    void getDatalakeServices() throws Exception {
        DescribeDatalakeServicesResponse datalakeServices = mock();
        when(datalakeServicesProvider.getDatalakeServices(cluster)).thenReturn(datalakeServices);

        DescribeDatalakeServicesResponse result = underTest.getDatalakeServices("", CLUSTER_CRN);

        assertThat(result).isEqualTo(datalakeServices);
    }

    @Test
    void getRootCertificate() throws Exception {
        GetRootCertificateResponse rootCertificate = mock();
        when(rootCertificateProvider.getRootCertificate(cluster)).thenReturn(rootCertificate);

        GetRootCertificateResponse result = underTest.getRootCertificate("", CLUSTER_CRN);

        assertThat(result).isEqualTo(rootCertificate);
    }

}
