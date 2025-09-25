package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.CertManagerResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.remoteenvironment.RemoteEnvironmentException;

@ExtendWith(MockitoExtension.class)
class ClassicClusterRootCertificateProviderTest {

    @Mock
    private ClassicClusterClouderaManagerApiClientProvider apiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Spy
    @InjectMocks
    private ClassicClusterRootCertificateProvider underTest;

    private OnPremisesApiProto.Cluster cluster;

    @Mock
    private ApiClient apiClient;

    @Mock
    private CertManagerResourceApi certManagerResourceApi;

    @BeforeEach
    void setUp() {
        cluster = OnPremisesApiProto.Cluster.newBuilder().build();
        lenient().when(apiClientProvider.getClouderaManagerV51Client(cluster)).thenReturn(apiClient);
    }

    @Test
    void getRootCertificateSuccess() throws Exception {
        when(clouderaManagerApiFactory.getCertManagerResourceApi(apiClient)).thenReturn(certManagerResourceApi);
        File pem = mock();
        when(certManagerResourceApi.getTruststore("PEM")).thenReturn(pem);
        doReturn("certs").when(underTest).getRootCertificateFromFile(pem);

        GetRootCertificateResponse result = underTest.getRootCertificate(cluster);

        assertThat(result.getContents()).isEqualTo("certs");
    }

    @Test
    void getRootCertificateApiException() throws Exception {
        when(clouderaManagerApiFactory.getCertManagerResourceApi(apiClient)).thenReturn(certManagerResourceApi);
        ApiException cause = new ApiException("500");
        when(certManagerResourceApi.getTruststore("PEM")).thenThrow(cause);

        assertThatThrownBy(() -> underTest.getRootCertificate(cluster))
                .isInstanceOf(RemoteEnvironmentException.class)
                .hasCause(cause)
                .hasMessage("Failed to get truststore from Cloudera Manager");
    }

    @Test
    void getRootCertificateIOException() throws Exception {
        when(clouderaManagerApiFactory.getCertManagerResourceApi(apiClient)).thenReturn(certManagerResourceApi);
        File pem = mock();
        when(certManagerResourceApi.getTruststore("PEM")).thenReturn(pem);
        doThrow(IOException.class).when(underTest).getRootCertificateFromFile(pem);

        assertThatThrownBy(() -> underTest.getRootCertificate(cluster))
                .isInstanceOf(RemoteEnvironmentException.class)
                .hasNoCause()
                .hasMessage("Failed to read truststore received from Cloudera Manager");
    }

}
