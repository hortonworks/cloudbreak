package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane.PrivateControlPlaneRemoteEnvironmentConnector;

@ExtendWith(MockitoExtension.class)
class ClassicClusterRemoteEnvironmentConnectorTest {

    private static final String CLUSTER_CRN = "cluster";

    private static final String ACCOUNT_ID = "acc";

    private static final String USER_CRN = "user";

    private static final String PVC_CP_ENV_CRN = "env";

    @Mock
    private RemoteClusterServiceClient remoteClusterServiceClient;

    @Mock
    private PrivateControlPlaneRemoteEnvironmentConnector privateControlPlaneRemoteEnvironmentConnector;

    @Mock
    private ClassicClusterListService listService;

    @Mock
    private ClassicClusterDescribeService describeService;

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
        lenient().when(remoteClusterServiceClient.describeClassicCluster(USER_CRN, CLUSTER_CRN, false)).thenReturn(cluster);
        underTest = spy(underTest);
        lenient().doReturn(true).when(underTest).isBaseCluster(cluster);
    }

    @Test
    void list() {
        OnPremisesApiProto.Cluster computeCluster = mock();
        doReturn(false).when(underTest).isBaseCluster(computeCluster);
        when(remoteClusterServiceClient.listClassicClusters(USER_CRN)).thenReturn(List.of(cluster, computeCluster));

        Collection<SimpleRemoteEnvironmentResponse> result = underTest.list(USER_CRN);

        verify(listService).list(List.of(cluster));
    }

    @Test
    void describeV1() {
        DescribeEnvironmentV2Response mockV2 = mock();
        DescribeEnvironmentResponse mockV1 = mock();
        when(remoteClusterServiceClient.describeClassicCluster(USER_CRN, CLUSTER_CRN, true)).thenReturn(cluster);
        when(mockV2.toV1Response()).thenReturn(mockV1);
        when(describeService.describe(cluster)).thenReturn(mockV2);

        DescribeEnvironmentResponse result = underTest.describeV1(USER_CRN, CLUSTER_CRN);

        assertThat(result).isEqualTo(mockV1);
        verify(describeService).describe(cluster);
        verifyNoInteractions(privateControlPlaneRemoteEnvironmentConnector);
    }

    @Test
    void describeV1ComputeCluster() {
        doReturn(false).when(underTest).isBaseCluster(cluster);

        assertThatThrownBy(() -> underTest.describeV1(USER_CRN, CLUSTER_CRN))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only Classic Clusters with BASE_CLUSTER cluster type can be used as environment.");
    }

    @Test
    void describeV1WithPvcCpSuccess() {
        when(cluster.getEnvironmentCrn()).thenReturn(PVC_CP_ENV_CRN);
        DescribeEnvironmentResponse describeEnvironmentResponse = mock();
        when(privateControlPlaneRemoteEnvironmentConnector.describeV1(USER_CRN, PVC_CP_ENV_CRN)).thenReturn(describeEnvironmentResponse);

        DescribeEnvironmentResponse result = underTest.describeV1(USER_CRN, CLUSTER_CRN);

        assertThat(result).isEqualTo(describeEnvironmentResponse);
    }

    @Test
    void describeV1WithPvcCpFailure() {
        when(cluster.getEnvironmentCrn()).thenReturn(PVC_CP_ENV_CRN);
        RuntimeException ex = new RuntimeException();
        when(privateControlPlaneRemoteEnvironmentConnector.describeV1(USER_CRN, PVC_CP_ENV_CRN)).thenThrow(ex);

        assertThatThrownBy(() -> underTest.describeV1(USER_CRN, CLUSTER_CRN))
                .isEqualTo(ex);
    }

    @Test
    void describeV2() {
        DescribeEnvironmentV2Response describeEnvironmentV2Response = mock();
        when(remoteClusterServiceClient.describeClassicCluster(USER_CRN, CLUSTER_CRN, true)).thenReturn(cluster);
        when(describeService.describe(cluster)).thenReturn(describeEnvironmentV2Response);

        DescribeEnvironmentResponse result = underTest.describeV2(USER_CRN, CLUSTER_CRN);

        assertThat(result).isEqualTo(describeEnvironmentV2Response);
    }

    @Test
    void describeV2WithPvcCpSuccess() {
        when(cluster.getEnvironmentCrn()).thenReturn(PVC_CP_ENV_CRN);
        DescribeEnvironmentV2Response describeEnvironmentV2Response = mock();
        when(privateControlPlaneRemoteEnvironmentConnector.describeV2(USER_CRN, PVC_CP_ENV_CRN)).thenReturn(describeEnvironmentV2Response);

        DescribeEnvironmentResponse result = underTest.describeV2(USER_CRN, CLUSTER_CRN);

        assertThat(result).isEqualTo(describeEnvironmentV2Response);
    }

    @Test
    void describeV2WithPvcCpFailure() {
        when(cluster.getEnvironmentCrn()).thenReturn(PVC_CP_ENV_CRN);
        RuntimeException ex = new RuntimeException();
        when(privateControlPlaneRemoteEnvironmentConnector.describeV2(USER_CRN, PVC_CP_ENV_CRN)).thenThrow(ex);

        assertThatThrownBy(() -> underTest.describeV2(USER_CRN, CLUSTER_CRN))
                .isEqualTo(ex);
    }

    @Test
    void getRemoteDataContext() throws Exception {
        DescribeDatalakeAsApiRemoteDataContextResponse rdc = mock();
        when(remoteDataContextProvider.getRemoteDataContext(cluster)).thenReturn(rdc);

        DescribeDatalakeAsApiRemoteDataContextResponse result = underTest.getRemoteDataContext(USER_CRN, CLUSTER_CRN);

        assertThat(result).isEqualTo(rdc);
    }

    @Test
    void getDatalakeServices() throws Exception {
        DescribeDatalakeServicesResponse datalakeServices = mock();
        when(datalakeServicesProvider.getDatalakeServices(cluster)).thenReturn(datalakeServices);

        DescribeDatalakeServicesResponse result = underTest.getDatalakeServices(USER_CRN, CLUSTER_CRN);

        assertThat(result).isEqualTo(datalakeServices);
    }

    @Test
    void getRootCertificate() {
        GetRootCertificateResponse rootCertificate = mock();
        when(rootCertificateProvider.getRootCertificate(cluster)).thenReturn(rootCertificate);

        GetRootCertificateResponse result = underTest.getRootCertificate(USER_CRN, CLUSTER_CRN);

        assertThat(result).isEqualTo(rootCertificate);
        verifyNoInteractions(privateControlPlaneRemoteEnvironmentConnector);
    }

    @Test
    void getRootCertificateWithPvcCpSuccess() {
        when(cluster.getEnvironmentCrn()).thenReturn(PVC_CP_ENV_CRN);
        GetRootCertificateResponse rootCertificate = mock();
        when(privateControlPlaneRemoteEnvironmentConnector.getRootCertificate(USER_CRN, PVC_CP_ENV_CRN)).thenReturn(rootCertificate);

        GetRootCertificateResponse result = underTest.getRootCertificate(USER_CRN, CLUSTER_CRN);

        assertThat(result).isEqualTo(rootCertificate);
        verifyNoInteractions(rootCertificateProvider);
    }

    @Test
    void getRootCertificateWithPvcCpFailure() {
        when(cluster.getEnvironmentCrn()).thenReturn(PVC_CP_ENV_CRN);
        RuntimeException ex = new RuntimeException();
        when(privateControlPlaneRemoteEnvironmentConnector.getRootCertificate(USER_CRN, PVC_CP_ENV_CRN)).thenThrow(ex);

        assertThatThrownBy(() -> underTest.getRootCertificate(USER_CRN, CLUSTER_CRN))
                .isEqualTo(ex);
    }

}
