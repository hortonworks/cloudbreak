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
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;

@ExtendWith(MockitoExtension.class)
class ClassicClusterRemoteEnvironmentConnectorTest {

    private static final String CLUSTER_CRN = "crn";

    @Mock
    private RemoteClusterServiceClient remoteClusterServiceClient;

    @Mock
    private ClassicClusterRemoteDataContextProvider remoteDataContextProvider;

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

}
