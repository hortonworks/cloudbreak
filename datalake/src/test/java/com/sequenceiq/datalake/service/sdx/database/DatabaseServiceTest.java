package com.sequenceiq.datalake.service.sdx.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.converter.DatabaseServerConverter;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslConfigV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.client.RedbeamsServiceCrnClient;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
public class DatabaseServiceTest {

    private static final String DATABASE_CRN = "database crn";

    private static final String CLUSTER_CRN = "cluster crn";

    private static final String ACCOUNT_ID = "1234";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:2";

    @Captor
    private ArgumentCaptor<AllocateDatabaseServerV4Request> allocateDatabaseServerV4RequestCaptor =
            ArgumentCaptor.forClass(AllocateDatabaseServerV4Request.class);

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private RedbeamsServiceCrnClient redbeamsClient;

    @Mock
    private SdxNotificationService notificationService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxService sdxService;

    @Spy
    private DatabaseServerConverter databaseServerConverter;

    @Mock
    private Map<DatabaseConfigKey, DatabaseConfig> dbConfigs;

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Mock
    private Map<CloudPlatform, DatabaseServerParameterSetter> databaseParameterSetterMap;

    @Mock
    private PlatformConfig platformConfig;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private DatabaseService underTest;

    static Object[][] sslEnforcementDataProvider() {
        return new Object[][]{
                // testCaseName supportedPlatform runtime entitled sslEnforcementAppliedExpected
                {"supportedPlatform=false", false, null, null, false},
                {"supportedPlatform=true and runtime=null and entitled=false", true, null, false, false},
                {"supportedPlatform=true and runtime=null and entitled=true", true, null, true, true},
                {"supportedPlatform=true and runtime=7.0.0", true, "7.0.0", null, false},
                {"supportedPlatform=true and runtime=7.1.0", true, "7.1.0", null, false},
                {"supportedPlatform=true and runtime=7.2.0", true, "7.2.0", null, false},
                {"supportedPlatform=true and runtime=7.2.1", true, "7.2.1", null, false},
                {"supportedPlatform=true and runtime=7.2.2 and entitled=false", true, "7.2.2", false, false},
                {"supportedPlatform=true and runtime=7.2.2 and entitled=true", true, "7.2.2", true, true},
                {"supportedPlatform=true and runtime=7.2.3 and entitled=false", true, "7.2.3", false, false},
                {"supportedPlatform=true and runtime=7.2.3 and entitled=true", true, "7.2.3", true, true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslEnforcementDataProvider")
    public void shouldSetDbConfigBasedOnClusterShape(String testCaseName, boolean supportedPlatform, String runtime, Boolean entitled,
            boolean sslEnforcementAppliedExpected) {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setCrn(CLUSTER_CRN);
        cluster.setRuntime(runtime);
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        env.setCrn(ENV_CRN);
        DatabaseConfig databaseConfig = getDatabaseConfig();

        when(databaseServerV4Endpoint.createInternal(any(), any())).thenThrow(BadRequestException.class);
        DatabaseConfigKey dbConfigKey = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKey)).thenReturn(databaseConfig);
        when(databaseParameterSetterMap.get(CloudPlatform.AWS)).thenReturn(getDatabaseParameterSetter());
        when(platformConfig.isExternalDatabaseSslEnforcementSupportedFor(CloudPlatform.AWS)).thenReturn(supportedPlatform);
        if (entitled != null) {
            when(entitlementService.databaseWireEncryptionEnabled(ACCOUNT_ID)).thenReturn(entitled);
        }
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(DatalakeStatusEnum.REQUESTED);
        when(sdxStatusService.getActualStatusForSdx(any())).thenReturn(status);

        assertThatCode(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(cluster, env))).isInstanceOf(BadRequestException.class);

        verify(databaseServerV4Endpoint).createInternal(allocateDatabaseServerV4RequestCaptor.capture(), anyString());
        AllocateDatabaseServerV4Request dbRequest = allocateDatabaseServerV4RequestCaptor.getValue();
        assertThat(dbRequest).isNotNull();
        DatabaseServerV4StackRequest databaseServer = dbRequest.getDatabaseServer();
        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.getInstanceType()).isEqualTo("instanceType");
        assertThat(databaseServer.getDatabaseVendor()).isEqualTo("vendor");
        assertThat(databaseServer.getStorageSize()).isEqualTo(100L);
        assertThat(dbRequest.getClusterCrn()).isEqualTo(CLUSTER_CRN);
        assertThat(databaseServer.getAws()).isNotNull();
        SslConfigV4Request sslConfig = dbRequest.getSslConfig();
        if (sslEnforcementAppliedExpected) {
            assertThat(sslConfig).isNotNull();
            assertThat(sslConfig.getSslMode()).isEqualTo(SslMode.ENABLED);
        } else {
            assertThat(sslConfig).isNull();
        }
        verifyNoInteractions(sdxClusterRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    public void shouldThrowExceptionBecauseSDXIsTerminated() {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setCrn(CLUSTER_CRN);
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");

        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(DatalakeStatusEnum.DELETE_REQUESTED);
        when(sdxStatusService.getActualStatusForSdx(any())).thenReturn(status);

        assertThrows(CloudbreakServiceException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(cluster, env));
        });
    }

    @Test
    public void shouldCallStartAndWaitForAvailableStatus() {
        SdxCluster cluster = new SdxCluster();
        cluster.setDatabaseCrn(DATABASE_CRN);

        DatabaseServerV4Response databaseServerV4Response = mock(DatabaseServerV4Response.class);

        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(databaseServerV4Response);
        when(databaseServerV4Response.getStatus()).thenReturn(Status.AVAILABLE);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.start(cluster);

        verify(databaseServerV4Endpoint).start(DATABASE_CRN);
    }

    @Test
    public void shouldCallStopAndWaitForStoppedStatus() {
        SdxCluster cluster = new SdxCluster();
        cluster.setDatabaseCrn(DATABASE_CRN);

        DatabaseServerV4Response databaseServerV4Response = mock(DatabaseServerV4Response.class);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(databaseServerV4Response);
        when(databaseServerV4Response.getStatus()).thenReturn(Status.STOPPED);

        underTest.stop(cluster);

        verify(databaseServerV4Endpoint).stop(DATABASE_CRN);
    }

    @Test
    public void testGetDatabaseServerShouldReturnDatabaseServer() {
        SdxCluster cluster = new SdxCluster();
        cluster.setDatabaseCrn(DATABASE_CRN);
        when(sdxService.getByCrn(USER_CRN, CLUSTER_CRN)).thenReturn(cluster);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setCrn(DATABASE_CRN);
        databaseServerV4Response.setClusterCrn(CLUSTER_CRN);
        databaseServerV4Response.setStatus(Status.AVAILABLE);
        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(databaseServerV4Response);

        StackDatabaseServerResponse response = underTest.getDatabaseServer(USER_CRN, CLUSTER_CRN);

        assertThat(response.getClusterCrn()).isEqualTo(CLUSTER_CRN);
        assertThat(response.getCrn()).isEqualTo(DATABASE_CRN);
        assertThat(response.getStatus()).isEqualTo(DatabaseServerStatus.AVAILABLE);
    }

    @Test
    public void testGetDatabaseServerWhenNoDatabaseCrnShouldThrowNotFoundException() {
        when(sdxService.getByCrn(USER_CRN, CLUSTER_CRN)).thenReturn(new SdxCluster());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> underTest.getDatabaseServer(USER_CRN, CLUSTER_CRN));

        assertThat(exception.getMessage()).isEqualTo("Database for Data Lake with Data Lake crn: 'cluster crn' not found.");
        verify(databaseServerV4Endpoint, never()).getByCrn(anyString());
    }

    private DatabaseConfig getDatabaseConfig() {
        return new DatabaseConfig("instanceType", "vendor", 100);
    }

    private DatabaseServerParameterSetter getDatabaseParameterSetter() {
        return new DatabaseServerParameterSetter() {
            @Override
            public void setParameters(DatabaseServerV4StackRequest request, SdxDatabaseAvailabilityType availabilityType, String databaseEngineVersion) {
                request.setAws(new AwsDatabaseServerV4Parameters());
            }

            @Override
            public CloudPlatform getCloudPlatform() {
                return CloudPlatform.AWS;
            }
        };
    }

}
