package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
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
        String dbServerCrn = "adbservercrn";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(dbServerCrn)).thenReturn(Boolean.TRUE);
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(new DatabaseServerV4Response());

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        assertThat(result.getSslCerts()).isEmpty();
        assertThat(result.isSslEnabledForStack()).isFalse();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndRdsConfiguredWithoutSSL() {
        String dbServerCrn = "adbservercrn";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(dbServerCrn)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(new SslConfigV4Response());
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        assertThat(result.getSslCerts()).isEmpty();
        assertThat(result.isSslEnabledForStack()).isFalse();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndRdsConfiguredWithSSLButNoCerts() {
        String dbServerCrn = "adbservercrn";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(stackView.getResourceCrn()).thenReturn("stackCrn");
        when(dbServerConfigurer.isRemoteDatabaseRequested(dbServerCrn)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        SslConfigV4Response sslConfig = getSslConfigV4ResponseWithCertificate(Set.of());
        databaseServerV4Response.setSslConfig(sslConfig);
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);

        IllegalStateException illegalStateException = Assertions.assertThrows(IllegalStateException.class, () -> underTest.getRelatedSslCerts(stack));

        assertThat(illegalStateException)
                .hasMessage("External DB SSL enforcement is enabled for cluster(crn:'stackCrn') and remote database('adbservercrn')," +
                "but no certificates have been returned!");
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndRdsConfiguredWithSSL() {
        String dbServerCrn = "adbservercrn";
        String certificateA = "certificate-A";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(dbServerCrn)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        SslConfigV4Response sslConfig = getSslConfigV4ResponseWithCertificate(Set.of(certificateA));
        databaseServerV4Response.setSslConfig(sslConfig);
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
        assertThat(result.isSslEnabledForStack()).isTrue();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndRdsConfiguredAndThereIsNoRelatedSdx() {
        String dbServerCrn = "adbservercrn";
        String certificateA = "certificate-A";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(dbServerConfigurer.isRemoteDatabaseRequested(dbServerCrn)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
        assertThat(result.isSslEnabledForStack()).isTrue();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndRdsConfiguredAndRelatedSdxDoesNotHaveRdsConfigured() {
        String dbServerCrn = "adbservercrn";
        String certificateA = "certificate-A";

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);

        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(
                Optional.of(new SdxBasicView(null, null, null, null, false, 1L, null)));
        when(dbServerConfigurer.isRemoteDatabaseRequested(null)).thenReturn(Boolean.FALSE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(dbServerCrn)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
        assertThat(result.isSslEnabledForStack()).isTrue();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndNoRdsConfiguredButRelatedSdxHasRdsConfigured() {
        String dbServerCrnB = "dbservercrn-B";
        String certificateB = "certificate-B";

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(null);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);

        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(
                Optional.of(new SdxBasicView(null, null, null, null, false, 1L, dbServerCrnB)));
        when(dbServerConfigurer.isRemoteDatabaseRequested(null)).thenReturn(Boolean.FALSE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(dbServerCrnB)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4ResponseB = new DatabaseServerV4Response();
        databaseServerV4ResponseB.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateB)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrnB)).thenReturn(databaseServerV4ResponseB);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateB);
        assertThat(result.isSslEnabledForStack()).isFalse();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndRdsConfiguredButRelatedSdxRdsHasNoSSLConfigured() {
        String dbServerCrn = "dbservercrn-A";
        String dbServerCrnB = "dbservercrn-B";
        String certificateA = "certificate-A";

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);

        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(
                Optional.of(new SdxBasicView(null, null, null, null, false, 1L, dbServerCrnB)));
        when(dbServerConfigurer.isRemoteDatabaseRequested(any())).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);
        DatabaseServerV4Response databaseServerV4ResponseB = new DatabaseServerV4Response();
        SslConfigV4Response sslConfig = new SslConfigV4Response();
        sslConfig.setSslMode(SslMode.DISABLED);
        databaseServerV4ResponseB.setSslConfig(sslConfig);
        when(dbServerConfigurer.getDatabaseServer(dbServerCrnB)).thenReturn(databaseServerV4ResponseB);

        DatabaseSslDetails result = underTest.getRelatedSslCerts(stack);

        Set<String> actual = result.getSslCerts();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
        assertThat(result.isSslEnabledForStack()).isTrue();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndRdsConfiguredAndRelatedSdxHasRdsConfigured() {
        String dbServerCrn = "dbservercrn-A";
        String dbServerCrnB = "dbservercrn-B";
        String certificateA = "certificate-A";
        String certificateB = "certificate-B";

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(cluster);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);

        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(
                Optional.of(new SdxBasicView(null, null, null, null, false, 1L, dbServerCrnB)));
        when(dbServerConfigurer.isRemoteDatabaseRequested(any())).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);
        DatabaseServerV4Response databaseServerV4ResponseB = new DatabaseServerV4Response();
        databaseServerV4ResponseB.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateB)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrnB)).thenReturn(databaseServerV4ResponseB);

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