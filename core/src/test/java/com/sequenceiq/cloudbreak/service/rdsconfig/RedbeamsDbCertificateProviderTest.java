package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;

@ExtendWith(MockitoExtension.class)
class RedbeamsDbCertificateProviderTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    @Mock
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Mock
    private DatalakeResourcesService datalakeResourcesService;

    @Mock
    private StackService stackService;

    @Mock
    private DatalakeService datalakeService;

    @InjectMocks
    private RedbeamsDbCertificateProvider underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "certsPath", SSL_CERTS_FILE_PATH);
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndNoRdsConfiguredShouldReturnWithEmptyResult() {
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn("");
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setType(StackType.DATALAKE);

        Set<String> actual = underTest.getRelatedSslCerts(stack, cluster);

        assertThat(actual).isEmpty();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndRdsConfiguredButItsSslConfigIsNullShouldReturnWithEmptyResult() {
        String dbServerCrn = "adbservercrn";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setType(StackType.DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseNeeded(cluster)).thenReturn(Boolean.TRUE);
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(new DatabaseServerV4Response());


        Set<String> actual = underTest.getRelatedSslCerts(stack, cluster);

        assertThat(actual).isEmpty();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndRdsConfiguredWithoutSSL() {
        String dbServerCrn = "adbservercrn";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setType(StackType.DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseNeeded(cluster)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(new SslConfigV4Response());
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);

        Set<String> actual = underTest.getRelatedSslCerts(stack, cluster);

        assertThat(actual).isEmpty();
    }

    @Test
    void getRelatedSslCertsWhenTheClusterSdxAndRdsConfiguredWithSSL() {
        String dbServerCrn = "adbservercrn";
        String certificateA = "certificate-A";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setType(StackType.DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseNeeded(cluster)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        SslConfigV4Response sslConfig = getSslConfigV4ResponseWithCertificate(Set.of(certificateA));
        databaseServerV4Response.setSslConfig(sslConfig);
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);

        Set<String> actual = underTest.getRelatedSslCerts(stack, cluster);

        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndNoRdsConfiguredAndThereIsNoRelatedSdx() {
        String dbServerCrn = "adbservercrn";
        String certificateA = "certificate-A";
        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setType(StackType.WORKLOAD);
        when(dbServerConfigurer.isRemoteDatabaseNeeded(cluster)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);

        Set<String> actual = underTest.getRelatedSslCerts(stack, cluster);

        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndNoRdsConfiguredAndRelatedSdxDoesNotHaveRdsConfigured() {
        String dbServerCrn = "adbservercrn";
        String certificateA = "certificate-A";

        Cluster sdxCluster = TestUtil.cluster();
        sdxCluster.setId(2L);
        sdxCluster.setDatabaseServerCrn(null);
        Stack sdxStack = sdxCluster.getStack();
        sdxStack.setCluster(sdxCluster);
        sdxStack.setType(StackType.DATALAKE);
        sdxStack.setId(2L);

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setType(StackType.WORKLOAD);
        stack.setDatalakeResourceId(1L);

        when(datalakeService.getDatalakeStackByDatahubStack(any())).thenReturn(Optional.of(sdxStack));
        when(dbServerConfigurer.isRemoteDatabaseNeeded(sdxCluster)).thenReturn(Boolean.FALSE);
        when(dbServerConfigurer.isRemoteDatabaseNeeded(cluster)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);

        Set<String> actual = underTest.getRelatedSslCerts(stack, cluster);

        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndNoRdsConfiguredButRelatedSdxHasRdsConfigured() {
        String dbServerCrnB = "dbservercrn-B";
        String certificateB = "certificate-B";

        Cluster sdxCluster = TestUtil.cluster();
        sdxCluster.setId(2L);
        sdxCluster.setDatabaseServerCrn(dbServerCrnB);
        Stack sdxStack = sdxCluster.getStack();
        sdxStack.setCluster(sdxCluster);
        sdxStack.setType(StackType.DATALAKE);
        sdxStack.setId(2L);

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(null);
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setType(StackType.WORKLOAD);
        stack.setDatalakeResourceId(1L);

        when(datalakeService.getDatalakeStackByDatahubStack(any())).thenReturn(Optional.of(sdxStack));
        when(dbServerConfigurer.isRemoteDatabaseNeeded(cluster)).thenReturn(Boolean.FALSE);
        when(dbServerConfigurer.isRemoteDatabaseNeeded(sdxCluster)).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4ResponseB = new DatabaseServerV4Response();
        databaseServerV4ResponseB.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateB)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrnB)).thenReturn(databaseServerV4ResponseB);

        Set<String> actual = underTest.getRelatedSslCerts(stack, cluster);

        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateB);
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndRdsConfiguredButRelatedSdxRdsHasNoSSLConfigured() {
        String dbServerCrn = "dbservercrn-A";
        String dbServerCrnB = "dbservercrn-B";
        String certificateA = "certificate-A";

        Cluster sdxCluster = TestUtil.cluster();
        sdxCluster.setId(2L);
        sdxCluster.setDatabaseServerCrn(dbServerCrnB);
        Stack sdxStack = sdxCluster.getStack();
        sdxStack.setCluster(sdxCluster);
        sdxStack.setType(StackType.DATALAKE);
        sdxStack.setId(2L);

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setType(StackType.WORKLOAD);
        stack.setDatalakeResourceId(1L);

        when(datalakeService.getDatalakeStackByDatahubStack(any())).thenReturn(Optional.of(sdxStack));
        when(dbServerConfigurer.isRemoteDatabaseNeeded(any())).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);
        DatabaseServerV4Response databaseServerV4ResponseB = new DatabaseServerV4Response();
        SslConfigV4Response sslConfig = new SslConfigV4Response();
        sslConfig.setSslMode(SslMode.DISABLED);
        databaseServerV4ResponseB.setSslConfig(sslConfig);
        when(dbServerConfigurer.getDatabaseServer(dbServerCrnB)).thenReturn(databaseServerV4ResponseB);

        Set<String> actual = underTest.getRelatedSslCerts(stack, cluster);

        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA);
    }

    @Test
    void getRelatedSslCertsWhenTheClusterDistroXAndRdsConfiguredAndRelatedSdxHasRdsConfigured() {
        String dbServerCrn = "dbservercrn-A";
        String dbServerCrnB = "dbservercrn-B";
        String certificateA = "certificate-A";
        String certificateB = "certificate-B";

        Cluster sdxCluster = TestUtil.cluster();
        sdxCluster.setId(2L);
        sdxCluster.setDatabaseServerCrn(dbServerCrnB);
        Stack sdxStack = sdxCluster.getStack();
        sdxStack.setCluster(sdxCluster);
        sdxStack.setType(StackType.DATALAKE);
        sdxStack.setId(2L);

        Cluster cluster = TestUtil.cluster();
        cluster.setDatabaseServerCrn(dbServerCrn);
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setType(StackType.WORKLOAD);
        stack.setDatalakeResourceId(1L);

        when(datalakeService.getDatalakeStackByDatahubStack(any())).thenReturn(Optional.of(sdxStack));
        when(dbServerConfigurer.isRemoteDatabaseNeeded(any())).thenReturn(Boolean.TRUE);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateA)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrn)).thenReturn(databaseServerV4Response);
        DatabaseServerV4Response databaseServerV4ResponseB = new DatabaseServerV4Response();
        databaseServerV4ResponseB.setSslConfig(getSslConfigV4ResponseWithCertificate(Set.of(certificateB)));
        when(dbServerConfigurer.getDatabaseServer(dbServerCrnB)).thenReturn(databaseServerV4ResponseB);

        Set<String> actual = underTest.getRelatedSslCerts(stack, cluster);

        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(certificateA, certificateB);
    }

    @Test
    void getSslCertsFilePathTest() {
        assertThat(underTest.getSslCertsFilePath()).isEqualTo(SSL_CERTS_FILE_PATH);
    }

    private SslConfigV4Response getSslConfigV4ResponseWithCertificate(Set<String> certs) {
        SslConfigV4Response sslConfig = new SslConfigV4Response();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        sslConfig.setSslMode(SslMode.ENABLED);
        sslConfig.setSslCertificates(certs);
        return sslConfig;
    }

}