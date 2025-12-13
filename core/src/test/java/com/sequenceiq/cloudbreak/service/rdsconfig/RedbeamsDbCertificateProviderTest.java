package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.DatabaseSslDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;

@ExtendWith(MockitoExtension.class)
class RedbeamsDbCertificateProviderTest {

    private static final String DB_SERVER_CRN = "crn:cdp:redbeams:us-west-1:default:databaseServer:e63520c8-aaf0-4bf3-b872-5613ce496ac3";

    private static final String DB_SERVER_CRN_2 = "crn:cdp:redbeams:us-west-1:default:databaseServer:e63520c9-aaf0-4bf3-b872-5613ce496ac3";

    @Mock
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @InjectMocks
    private RedbeamsDbCertificateProvider underTest;

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndNoRdsConfiguredShouldReturnWithEmptyResult() {
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn("");
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        assertThat(result.getSslCerts()).isEmpty();
        assertThat(result.isSslEnabledForStack()).isFalse();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndRdsConfiguredButItsSslConfigIsNullShouldReturnWithEmptyResult() {
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN)).thenReturn(new DatabaseServerV4Response());

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        assertThat(result.getSslCerts()).isEmpty();
        assertThat(result.isSslEnabledForStack()).isFalse();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndRdsConfiguredWithoutSSL() {
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(new SslConfigV4Response());
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN)).thenReturn(databaseServerV4Response);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        assertThat(result.getSslCerts()).isEmpty();
        assertThat(result.isSslEnabledForStack()).isFalse();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndRdsConfiguredWithSSLButNoCerts() {
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(stackView.getResourceCrn()).thenReturn("stackCrn");
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        SslConfigV4Response sslConfig = getSslConfigV4ResponseWithCertificate(Set.of());
        databaseServerV4Response.setSslConfig(sslConfig);
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN)).thenReturn(databaseServerV4Response);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.getRelatedSslCerts(stack));

        assertThat(illegalStateException)
                .hasMessage("External DB SSL enforcement is enabled for cluster(crn:'stackCrn') and remote database('" + DB_SERVER_CRN + "')," +
                "but no certificates have been returned!");
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndRdsConfiguredWithSSL() {
        String certificateA = "certificate-A";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        SslConfigV4Response sslConfig = getSslConfigV4ResponseWithCertificate(Set.of(certificateA));
        databaseServerV4Response.setSslConfig(sslConfig);
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN)).thenReturn(databaseServerV4Response);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
        assertThat(result.isSslEnabledForStack()).isTrue();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndRdsConfiguredAndThereIsNoRelatedSdx() {
        String certificateA = "certificate-A";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN)).thenReturn(databaseServerV4Response);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
        assertThat(result.isSslEnabledForStack()).isTrue();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndRdsConfiguredAndRelatedSdxDoesNotHaveRdsConfigured() {
        String certificateA = "certificate-A";

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);

        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(Optional.of(SdxBasicView.builder().build()));
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN)).thenReturn(databaseServerV4Response);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
        assertThat(result.isSslEnabledForStack()).isTrue();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndNoRdsConfiguredButRelatedSdxHasRdsConfigured() {
        String certificateB = "certificate-B";

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(null);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);

        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(
                Optional.of(SdxBasicView.builder().withDbServerCrn(DB_SERVER_CRN).build()));
        DatabaseServerV4Response databaseServerV4ResponseB = new DatabaseServerV4Response();
        databaseServerV4ResponseB.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateB)));
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN)).thenReturn(databaseServerV4ResponseB);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateB);
        assertThat(result.isSslEnabledForStack()).isFalse();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndRdsConfiguredButRelatedSdxRdsHasNoSSLConfigured() {
        String certificateA = "certificate-A";

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);

        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(
                Optional.of(SdxBasicView.builder().withDbServerCrn(DB_SERVER_CRN_2).build()));
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN)).thenReturn(databaseServerV4Response);
        DatabaseServerV4Response databaseServerV4ResponseB = new DatabaseServerV4Response();
        SslConfigV4Response sslConfig = new SslConfigV4Response();
        sslConfig.setSslMode(SslMode.DISABLED);
        databaseServerV4ResponseB.setSslConfig(sslConfig);
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN_2)).thenReturn(databaseServerV4ResponseB);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
        assertThat(result.isSslEnabledForStack()).isTrue();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndRdsConfiguredAndRelatedSdxHasRdsConfigured() {
        String certificateA = "certificate-A";
        String certificateB = "certificate-B";

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);

        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(
                Optional.of(SdxBasicView.builder().withDbServerCrn(DB_SERVER_CRN_2).build()));
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN)).thenReturn(databaseServerV4Response);
        DatabaseServerV4Response databaseServerV4ResponseB = new DatabaseServerV4Response();
        databaseServerV4ResponseB.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateB)));
        when(dbServerConfigurer.getDatabaseServer(DB_SERVER_CRN_2)).thenReturn(databaseServerV4ResponseB);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA, certificateB);
        assertThat(result.isSslEnabledForStack()).isTrue();
    }

    private SslConfigV4Response getSslConfigV4ResponseWithCertificate(Set<String> certs) {
        SslConfigV4Response sslConfig = new SslConfigV4Response();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        sslConfig.setSslMode(SslMode.ENABLED);
        sslConfig.setSslCertificates(certs);
        return sslConfig;
    }

}