package com.sequenceiq.cloudbreak.service.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.DatabaseSslDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class DatabaseSslServiceTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    private static final String CERT_EXTERNAL_1 = "certExternal1";

    private static final String CERT_EXTERNAL_2 = "certExternal2";

    private static final String CERT_EMBEDDED = "certEmbedded";

    private static final String DATABASE_SERVER_CRN = "databaseServerCrn";

    private static final String DATABASE_SERVER_CRN_DATALAKE = "databaseServerCrnDatalake";

    private static final Long CLUSTER_ID = 12L;

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Mock
    private RedbeamsDbCertificateProvider dbCertificateProvider;

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @Mock
    private FreeipaClientService freeipaClientService;

    @Mock
    private DatalakeService datalakeService;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private DatabaseSslService underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private StackView stackView;

    @Mock
    private ClusterView clusterView;

    @Mock
    private Stack datalakeStack;

    @Mock
    private Cluster datalakeCluster;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "certsPath", SSL_CERTS_FILE_PATH);

        lenient().when(stackDto.getStack()).thenReturn(stackView);
        lenient().when(stackDto.getCluster()).thenReturn(clusterView);
        lenient().when(clusterView.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN);
    }

    @Test
    void getSslCertsFilePathTest() {
        assertThat(underTest.getSslCertsFilePath()).isEqualTo(SSL_CERTS_FILE_PATH);
    }

    static Object[][] isDbSslEnabledByClusterViewDataProvider() {
        return new Object[][]{
                // dbSslEnabled, resultExpected
                {null, false},
                {false, false},
                {true, true},
        };
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("isDbSslEnabledByClusterViewDataProvider")
    void isDbSslEnabledByClusterViewTest(Boolean dbSslEnabled, boolean resultExpected) {
        when(clusterView.getDbSslEnabled()).thenReturn(dbSslEnabled);

        assertThat(underTest.isDbSslEnabledByClusterView(stackView, clusterView)).isEqualTo(resultExpected);
    }

    static Object[][] externalDbSslDetailsDataProvider() {
        return new Object[][]{
                // sslCerts, sslEnabledForStack
                {Set.of(), false},
                {Set.of(CERT_EXTERNAL_1), true},
                {Set.of(CERT_EXTERNAL_1, CERT_EXTERNAL_2), true},
        };
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("externalDbSslDetailsDataProvider")
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenDatalakeWithExternalDb(Set<String> sslCerts, boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, sslEnabledForStack);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(true);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(sslCerts);
        assertThat(result.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(datalakeService, never()).getDatalakeStackByDatahubStack(any(StackView.class));
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("externalDbSslDetailsDataProvider")
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenDatahubWithExternalDbButNoDatalake(Set<String> sslCerts, boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, sslEnabledForStack);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(true);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.empty());
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(sslCerts);
        assertThat(result.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("externalDbSslDetailsDataProvider")
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenDatahubAndDatalakeAndExternalDbOnly(Set<String> sslCerts, boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, sslEnabledForStack);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(true);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN_DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN_DATALAKE)).thenReturn(true);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(sslCerts);
        assertThat(result.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @Test
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenDatalakeWithEmbeddedDbNoSsl() {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(Set.of(), false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(stackView, clusterView)).thenReturn(false);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEmpty();
        assertThat(result.isSslEnabledForStack()).isFalse();

        verify(datalakeService, never()).getDatalakeStackByDatahubStack(any(StackView.class));
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @Test
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenDatahubWithEmbeddedDbNoSslButNoDatalake() {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(Set.of(), false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(stackView, clusterView)).thenReturn(false);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.empty());
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEmpty();
        assertThat(result.isSslEnabledForStack()).isFalse();

        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("externalDbSslDetailsDataProvider")
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenDatahubWithExternalDbAndDatalakeWithEmbeddedDbNoSsl(Set<String> sslCerts,
            boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, sslEnabledForStack);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(true);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN_DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN_DATALAKE)).thenReturn(false);
        when(embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(datalakeStack, datalakeCluster)).thenReturn(false);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(sslCerts);
        assertThat(result.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);

        verifyNoMoreInteractions(embeddedDatabaseService);
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    static Object[][] externalDbSslCertsDataProvider() {
        return new Object[][]{
                // sslCerts
                {Set.of()},
                {Set.of(CERT_EXTERNAL_1)},
                {Set.of(CERT_EXTERNAL_1, CERT_EXTERNAL_2)},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("externalDbSslCertsDataProvider")
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenDatahubWithEmbeddedDbNoSslAndDatalakeWithExternalDb(Set<String> sslCerts) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(stackView, clusterView)).thenReturn(false);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN_DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN_DATALAKE)).thenReturn(true);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(sslCerts);
        assertThat(result.isSslEnabledForStack()).isFalse();

        verifyNoMoreInteractions(embeddedDatabaseService);
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("externalDbSslCertsDataProvider")
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenStackWithEmbeddedDbSsl(Set<String> sslCerts) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(stackView, clusterView)).thenReturn(true);
        when(stackView.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(CERT_EMBEDDED);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isNotNull();
        assertThat(result.getSslCerts()).hasSize(sslCerts.size() + 1);
        assertThat(result.getSslCerts()).containsAll(sslCerts);
        assertThat(result.getSslCerts()).contains(CERT_EMBEDDED);
        assertThat(result.isSslEnabledForStack()).isTrue();

        verify(stackView, never()).getType();
        verify(datalakeService, never()).getDatalakeStackByDatahubStack(any(StackView.class));
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "rootCertificate={0}")
    @ValueSource(strings = {"", " "})
    @NullSource
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenStackWithEmbeddedDbSslAndBlankFreeIpaRootCert(String rootCertificate) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(Set.of(), false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(stackView, clusterView)).thenReturn(true);
        when(stackView.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(rootCertificate);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                () -> underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto));

        assertThat(illegalStateException).hasMessage("Got a blank FreeIPA root certificate.");

        verify(stackView, never()).getType();
        verify(datalakeService, never()).getDatalakeStackByDatahubStack(any(StackView.class));
        verify(clusterService, never()).updateDbSslCert(anyLong(), any(DatabaseSslDetails.class));
    }

    @Test
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenStackWithEmbeddedDbAndMismatchingSslDetailsFlag() {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(Set.of(), true);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                () -> underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto));

        assertThat(illegalStateException).hasMessage("Mismatching sslDetails.sslEnabledForStack in RedbeamsDbCertificateProvider response. " +
                "Expecting false because the stack uses an embedded DB, but it was true.");

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(stackView, never()).getType();
        verify(datalakeService, never()).getDatalakeStackByDatahubStack(any(StackView.class));
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService, never()).updateDbSslCert(anyLong(), any(DatabaseSslDetails.class));
    }

    @Test
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenDatahubWithEmbeddedDbNoSslAndDatalakeWithEmbeddedDbSsl() {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(Set.of(), false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(stackView, clusterView)).thenReturn(false);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN_DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN_DATALAKE)).thenReturn(false);
        when(embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(datalakeStack, datalakeCluster)).thenReturn(true);
        when(stackView.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(CERT_EMBEDDED);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(Set.of(CERT_EMBEDDED));
        assertThat(result.isSslEnabledForStack()).isFalse();

        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("externalDbSslDetailsDataProvider")
    void getDbSslDetailsForCreationAndUpdateInClusterTestWhenDatahubWithExternalDbAndDatalakeWithEmbeddedDbSsl(Set<String> sslCerts,
            boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, sslEnabledForStack);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(true);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN_DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN_DATALAKE)).thenReturn(false);
        when(embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(datalakeStack, datalakeCluster)).thenReturn(true);
        when(stackView.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(CERT_EMBEDDED);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isNotNull();
        assertThat(result.getSslCerts()).hasSize(sslCerts.size() + 1);
        assertThat(result.getSslCerts()).containsAll(sslCerts);
        assertThat(result.getSslCerts()).contains(CERT_EMBEDDED);
        assertThat(result.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(stackView, clusterView);
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("externalDbSslDetailsDataProvider")
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenDatalakeWithExternalDb(Set<String> sslCerts, boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, sslEnabledForStack);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(true);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(sslCerts);
        assertThat(result.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(clusterView, never()).getDbSslEnabled();
        verify(datalakeService, never()).getDatalakeStackByDatahubStack(any(StackView.class));
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("externalDbSslDetailsDataProvider")
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenDatahubWithExternalDbButNoDatalake(Set<String> sslCerts, boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, sslEnabledForStack);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(true);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.empty());
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(sslCerts);
        assertThat(result.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(clusterView, never()).getDbSslEnabled();
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("externalDbSslDetailsDataProvider")
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenDatahubAndDatalakeAndExternalDbOnly(Set<String> sslCerts, boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, sslEnabledForStack);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(true);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN_DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN_DATALAKE)).thenReturn(true);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(sslCerts);
        assertThat(result.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(clusterView, never()).getDbSslEnabled();
        verify(datalakeCluster, never()).getDbSslEnabled();
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false})
    @NullSource
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenDatalakeWithEmbeddedDbNoSsl(Boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(Set.of(), false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(clusterView.getDbSslEnabled()).thenReturn(sslEnabledForStack);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEmpty();
        assertThat(result.isSslEnabledForStack()).isFalse();

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(datalakeService, never()).getDatalakeStackByDatahubStack(any(StackView.class));
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false})
    @NullSource
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenDatahubWithEmbeddedDbNoSslButNoDatalake(Boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(Set.of(), false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.empty());
        when(clusterView.getDbSslEnabled()).thenReturn(sslEnabledForStack);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEmpty();
        assertThat(result.isSslEnabledForStack()).isFalse();

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    static Object[][] datahubWithExternalDbAndDatalakeWithEmbeddedDbNoSslDataProvider() {
        return new Object[][]{
                // sslCerts, sslEnabledForStack, sslEnabledForDatalake
                {Set.of(), false, false},
                {Set.of(), false, null},
                {Set.of(CERT_EXTERNAL_1), true, false},
                {Set.of(CERT_EXTERNAL_1), true, null},
                {Set.of(CERT_EXTERNAL_1, CERT_EXTERNAL_2), true, false},
                {Set.of(CERT_EXTERNAL_1, CERT_EXTERNAL_2), true, null},
        };
    }

    @ParameterizedTest(name = "{1},{0},{2}")
    @MethodSource("datahubWithExternalDbAndDatalakeWithEmbeddedDbNoSslDataProvider")
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenDatahubWithExternalDbAndDatalakeWithEmbeddedDbNoSsl(Set<String> sslCerts,
            boolean sslEnabledForStack, Boolean sslEnabledForDatalake) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, sslEnabledForStack);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(true);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN_DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN_DATALAKE)).thenReturn(false);
        when(datalakeCluster.getDbSslEnabled()).thenReturn(sslEnabledForDatalake);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(sslCerts);
        assertThat(result.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(clusterView, never()).getDbSslEnabled();
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    static Object[][] datahubWithEmbeddedDbNoSslAndDatalakeWithExternalDbDataProvider() {
        return new Object[][]{
                // sslCerts, sslEnabledForStack
                {Set.of(), false},
                {Set.of(), null},
                {Set.of(CERT_EXTERNAL_1), false},
                {Set.of(CERT_EXTERNAL_1), null},
                {Set.of(CERT_EXTERNAL_1, CERT_EXTERNAL_2), false},
                {Set.of(CERT_EXTERNAL_1, CERT_EXTERNAL_2), null},
        };
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("datahubWithEmbeddedDbNoSslAndDatalakeWithExternalDbDataProvider")
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenDatahubWithEmbeddedDbNoSslAndDatalakeWithExternalDb(Set<String> sslCerts,
            Boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(clusterView.getDbSslEnabled()).thenReturn(sslEnabledForStack);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN_DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN_DATALAKE)).thenReturn(true);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(sslCerts);
        assertThat(result.isSslEnabledForStack()).isFalse();

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(datalakeCluster, never()).getDbSslEnabled();
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("externalDbSslCertsDataProvider")
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenStackWithEmbeddedDbSsl(Set<String> sslCerts) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(clusterView.getDbSslEnabled()).thenReturn(true);
        when(stackView.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(CERT_EMBEDDED);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isNotNull();
        assertThat(result.getSslCerts()).hasSize(sslCerts.size() + 1);
        assertThat(result.getSslCerts()).containsAll(sslCerts);
        assertThat(result.getSslCerts()).contains(CERT_EMBEDDED);
        assertThat(result.isSslEnabledForStack()).isTrue();

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(stackView, never()).getType();
        verify(datalakeService, never()).getDatalakeStackByDatahubStack(any(StackView.class));
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "rootCertificate={0}")
    @ValueSource(strings = {"", " "})
    @NullSource
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenStackWithEmbeddedDbSslAndBlankFreeIpaRootCert(String rootCertificate) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(Set.of(), false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(clusterView.getDbSslEnabled()).thenReturn(true);
        when(stackView.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(rootCertificate);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                () -> underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto));

        assertThat(illegalStateException).hasMessage("Got a blank FreeIPA root certificate.");

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(stackView, never()).getType();
        verify(datalakeService, never()).getDatalakeStackByDatahubStack(any(StackView.class));
        verify(clusterService, never()).updateDbSslCert(anyLong(), any(DatabaseSslDetails.class));
    }

    @Test
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenStackWithEmbeddedDbAndMismatchingSslDetailsFlag() {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(Set.of(), true);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                () -> underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto));

        assertThat(illegalStateException).hasMessage("Mismatching sslDetails.sslEnabledForStack in RedbeamsDbCertificateProvider response. " +
                "Expecting false because the stack uses an embedded DB, but it was true.");

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(clusterView, never()).getDbSslEnabled();
        verify(stackView, never()).getType();
        verify(datalakeService, never()).getDatalakeStackByDatahubStack(any(StackView.class));
        verify(freeipaClientService, never()).getRootCertificateByEnvironmentCrn(anyString());
        verify(clusterService, never()).updateDbSslCert(anyLong(), any(DatabaseSslDetails.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false})
    @NullSource
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenDatahubWithEmbeddedDbNoSslAndDatalakeWithEmbeddedDbSsl(Boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(Set.of(), false);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(false);
        when(clusterView.getDbSslEnabled()).thenReturn(sslEnabledForStack);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN_DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN_DATALAKE)).thenReturn(false);
        when(datalakeCluster.getDbSslEnabled()).thenReturn(true);
        when(stackView.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(CERT_EMBEDDED);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isEqualTo(Set.of(CERT_EMBEDDED));
        assertThat(result.isSslEnabledForStack()).isFalse();

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

    @ParameterizedTest(name = "{1},{0}")
    @MethodSource("externalDbSslDetailsDataProvider")
    void getDbSslDetailsForRotationAndUpdateInClusterTestWhenDatahubWithExternalDbAndDatalakeWithEmbeddedDbSsl(Set<String> sslCerts,
            boolean sslEnabledForStack) {
        DatabaseSslDetails sslDetails = new DatabaseSslDetails(sslCerts, sslEnabledForStack);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(sslDetails);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN)).thenReturn(true);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(datalakeService.getDatalakeStackByDatahubStack(stackView)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DATABASE_SERVER_CRN_DATALAKE);
        when(dbServerConfigurer.isRemoteDatabaseRequested(DATABASE_SERVER_CRN_DATALAKE)).thenReturn(false);
        when(datalakeCluster.getDbSslEnabled()).thenReturn(true);
        when(stackView.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(CERT_EMBEDDED);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        DatabaseSslDetails result = underTest.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);

        assertThat(result).isSameAs(sslDetails);
        assertThat(result.getSslCerts()).isNotNull();
        assertThat(result.getSslCerts()).hasSize(sslCerts.size() + 1);
        assertThat(result.getSslCerts()).containsAll(sslCerts);
        assertThat(result.getSslCerts()).contains(CERT_EMBEDDED);
        assertThat(result.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);

        verify(embeddedDatabaseService, never()).isSslEnforcementForEmbeddedDatabaseEnabled(any(StackView.class), any(ClusterView.class));
        verify(clusterView, never()).getDbSslEnabled();
        verify(clusterService).updateDbSslCert(CLUSTER_ID, result);
    }

}