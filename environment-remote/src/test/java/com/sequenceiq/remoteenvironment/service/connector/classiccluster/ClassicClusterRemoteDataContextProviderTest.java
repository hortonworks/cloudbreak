package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.CdpResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.remoteenvironment.RemoteEnvironmentException;

@ExtendWith(MockitoExtension.class)
class ClassicClusterRemoteDataContextProviderTest {

    private static final String CLUSTER_NAME = "clusterName";

    private static final String CLUSTER_CRN = "clusterCrn";

    @Mock
    private ClassicClusterClouderaManagerApiClientProvider apiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Spy
    @InjectMocks
    private ClassicClusterRemoteDataContextProvider underTest;

    private OnPremisesApiProto.Cluster cluster;

    @Mock
    private ApiClient apiClient;

    @Mock
    private CdpResourceApi cdpResourceApi;

    @BeforeEach
    void setUp() {
        cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setName(CLUSTER_NAME)
                .setClusterCrn(CLUSTER_CRN)
                .build();
        lenient().when(apiClientProvider.getClouderaManagerRootClient(cluster)).thenReturn(apiClient);
    }

    @Test
    void getRemoteDataContextSuccess() throws Exception {
        when(clouderaManagerApiFactory.getCdpResourceApi(apiClient)).thenReturn(cdpResourceApi);
        ApiRemoteDataContext apiRemoteDataContext = mock();
        when(cdpResourceApi.getRemoteContextByCluster(CLUSTER_NAME)).thenReturn(apiRemoteDataContext);
        com.cloudera.cdp.servicediscovery.model.ApiRemoteDataContext convertedApiRemoteDataContext = mock();
        when(underTest.convert(apiRemoteDataContext)).thenReturn(convertedApiRemoteDataContext);

        DescribeDatalakeAsApiRemoteDataContextResponse result = underTest.getRemoteDataContext(cluster);

        assertThat(result.getDatalake()).isEqualTo(CLUSTER_CRN);
        assertThat(result.getContext()).isEqualTo(convertedApiRemoteDataContext);
    }

    @Test
    void getRemoteDataContextApiException() throws Exception {
        when(clouderaManagerApiFactory.getCdpResourceApi(apiClient)).thenReturn(cdpResourceApi);
        ApiRemoteDataContext apiRemoteDataContext = mock();
        ApiException cause = new ApiException("500");
        when(cdpResourceApi.getRemoteContextByCluster(CLUSTER_NAME)).thenThrow(cause);

        assertThatThrownBy(() -> underTest.getRemoteDataContext(cluster))
                .isInstanceOf(RemoteEnvironmentException.class)
                .hasCause(cause)
                .hasMessage("Failed to get remote data context from Cloudera Manager");
    }

    @Test
    void getRemoteDataContextJsonProcessingException() throws Exception {
        when(clouderaManagerApiFactory.getCdpResourceApi(apiClient)).thenReturn(cdpResourceApi);
        ApiRemoteDataContext apiRemoteDataContext = mock();
        when(cdpResourceApi.getRemoteContextByCluster(CLUSTER_NAME)).thenReturn(apiRemoteDataContext);
        doThrow(JsonProcessingException.class).when(underTest).convert(apiRemoteDataContext);

        assertThatThrownBy(() -> underTest.getRemoteDataContext(cluster))
                .isInstanceOf(RemoteEnvironmentException.class)
                .hasNoCause()
                .hasMessage("Failed to process remote data context. Please contact Cloudera support to get this resolved.");
    }

}
