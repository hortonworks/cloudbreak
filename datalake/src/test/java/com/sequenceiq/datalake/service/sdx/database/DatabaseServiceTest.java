package com.sequenceiq.datalake.service.sdx.database;

import static com.sequenceiq.datalake.service.sdx.SdxService.DATABASE_SSL_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
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
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.converter.DatabaseServerConverter;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxDatabaseRepository;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslConfigV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabasePropertiesV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
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
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Mock
    private SdxDatabaseRepository sdxDatabaseRepository;

    @Mock
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    @Mock
    private SdxNotificationService sdxNotificationService;

    @Mock
    private EventSenderService eventSenderService;

    @InjectMocks
    private DatabaseService underTest;

    private SdxCluster defaultSdxCluster;

    @BeforeEach
    public void init() {
        defaultSdxCluster = getSdxCluster();
    }

    static Object[][] sslEnforcementDataProvider() {
        return new Object[][] {
                // testCaseName supportedPlatform runtime sslEnforcementAppliedExpected
                {"supportedPlatform=false", false, null, false},
                {"supportedPlatform=true and runtime=null", true, null, true},
                {"supportedPlatform=true and runtime=7.0.0", true, "7.0.0", false},
                {"supportedPlatform=true and runtime=7.1.0", true, "7.1.0", false},
                {"supportedPlatform=true and runtime=7.2.0", true, "7.2.0", false},
                {"supportedPlatform=true and runtime=7.2.1", true, "7.2.1", false},
                {"supportedPlatform=true and runtime=7.2.2", true, "7.2.2", true},
                {"supportedPlatform=true and runtime=7.2.3", true, "7.2.3", true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslEnforcementDataProvider")
    public void shouldSetDbConfigBasedOnClusterShape(String testCaseName, boolean supportedPlatform, String runtime,
            boolean sslEnforcementAppliedExpected) {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setCrn(CLUSTER_CRN);
        cluster.setRuntime(runtime);
        SdxDatabase database = new SdxDatabase();
        database.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        cluster.setSdxDatabase(database);
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("test");
        env.setLocation(locationResponse);
        env.setCrn(ENV_CRN);
        DatabaseConfig databaseConfig = getDatabaseConfig();

        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));
        when(databaseServerV4Endpoint.createInternal(any(), any())).thenThrow(BadRequestException.class);
        DatabaseConfigKey dbConfigKey = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKey)).thenReturn(databaseConfig);
        when(databaseParameterSetterMap.get(CloudPlatform.AWS)).thenReturn(getDatabaseParameterSetter());
        when(platformConfig.isExternalDatabaseSslEnforcementSupportedFor(CloudPlatform.AWS)).thenReturn(supportedPlatform);
        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(DatalakeStatusEnum.REQUESTED);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(status);

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
        cluster.setSdxDatabase(new SdxDatabase());
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");

        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(DatalakeStatusEnum.DELETE_REQUESTED);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(status);

        assertThrows(CloudbreakServiceException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(cluster, env));
        });
    }

    @Test
    public void shouldCallStartAndWaitForAvailableStatus() {
        SdxCluster cluster = getSdxCluster();

        DatabaseServerV4Response databaseServerV4Response = mock(DatabaseServerV4Response.class);

        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(databaseServerV4Response);
        when(databaseServerV4Response.getStatus()).thenReturn(Status.AVAILABLE);
        underTest.start(cluster);

        verify(databaseServerV4Endpoint).start(DATABASE_CRN);
    }

    @Test
    public void shouldCallStopAndWaitForStoppedStatus() {
        SdxCluster cluster = getSdxCluster();

        DatabaseServerV4Response databaseServerV4Response = mock(DatabaseServerV4Response.class);
        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(databaseServerV4Response);
        when(databaseServerV4Response.getStatus()).thenReturn(Status.STOPPED);

        underTest.stop(cluster);

        verify(databaseServerV4Endpoint).stop(DATABASE_CRN);
    }

    @Test
    public void testGetDatabaseServerShouldReturnDatabaseServer() {
        SdxCluster cluster = getSdxCluster();
        when(sdxService.getByCrn(USER_CRN, CLUSTER_CRN)).thenReturn(cluster);
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
    public void testShouldWaitAndGetDatabase() {
        defaultSdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        SdxDatabase database = new SdxDatabase();
        database.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        defaultSdxCluster.setSdxDatabase(database);
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("asdf");
        env.setLocation(locationResponse);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setCrn(DATABASE_CRN);
        databaseServerV4Response.setClusterCrn(CLUSTER_CRN);
        databaseServerV4Response.setStatus(Status.AVAILABLE);
        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(databaseServerV4Response);
        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(DatalakeStatusEnum.ENVIRONMENT_CREATED);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(status);
        DatabaseServerStatusV4Response databaseServerStatusV4Response = new DatabaseServerStatusV4Response();
        databaseServerStatusV4Response.setResourceCrn(DATABASE_CRN);
        databaseServerStatusV4Response.setStatus(Status.AVAILABLE);
        when(databaseServerV4Endpoint.createInternal(any(), any())).thenReturn(databaseServerStatusV4Response);
        DatabaseServerParameterSetter databaseServerParameterSetter = new AwsDatabaseServerParameterSetter();
        when(databaseParameterSetterMap.get(CloudPlatform.AWS)).thenReturn(databaseServerParameterSetter);
        DatabaseConfig databaseConfig = getDatabaseConfig();
        DatabaseConfigKey dbConfigKey = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKey)).thenReturn(databaseConfig);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));

        DatabaseServerStatusV4Response response =  ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(defaultSdxCluster, env));

        verify(sdxDatabaseRepository, times(1)).save(any());
        verify(sdxClusterRepository, times(1)).save(any());
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.EXTERNAL_DATABASE_CREATION_IN_PROGRESS),
                eq("External database creation in progress"), eq(defaultSdxCluster));
    }

    @Test
    public void testGetDatabaseServerWhenNoDatabaseCrnShouldThrowNotFoundException() {
        SdxCluster cluster = new SdxCluster();
        cluster.setSdxDatabase(new SdxDatabase());
        when(sdxService.getByCrn(USER_CRN, CLUSTER_CRN)).thenReturn(cluster);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> underTest.getDatabaseServer(USER_CRN, CLUSTER_CRN));

        assertThat(exception.getMessage()).isEqualTo("Database for Data Lake with Data Lake crn: 'cluster crn' not found.");
        verify(databaseServerV4Endpoint, never()).getByCrn(anyString());
    }

    @Test
    public void testGetDatabaseServerRequestWithCustomProperties() {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setCrn(CLUSTER_CRN);
        SdxDatabase sdxDatabase = new SdxDatabase();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("instancetype", "customInstancetype");
        attributes.put("storage", 128L);
        sdxDatabase.setAttributes(new Json(attributes));
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        cluster.setSdxDatabase(sdxDatabase);
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("test");
        env.setLocation(locationResponse);
        env.setCrn(ENV_CRN);
        DatabaseConfig databaseConfig = getDatabaseConfig();

        DatabaseConfigKey dbConfigKey = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKey)).thenReturn(databaseConfig);
        when(databaseParameterSetterMap.get(CloudPlatform.AWS)).thenReturn(getDatabaseParameterSetter());
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));

        DatabaseServerV4StackRequest databaseServerV4StackRequest = underTest.getDatabaseServerRequest(CloudPlatform.AWS, cluster, env,
                "initiatorUserCrn");

        assertThat(databaseServerV4StackRequest).isNotNull();
        assertThat(databaseServerV4StackRequest.getInstanceType()).isEqualTo("customInstancetype");
        assertThat(databaseServerV4StackRequest.getDatabaseVendor()).isEqualTo("vendor");
        assertThat(databaseServerV4StackRequest.getStorageSize()).isEqualTo(128L);
    }

    @Test
    public void testGetDatabaseServerRequestWithArm64ArchitectureFallbackToX86() {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setArchitecture(Architecture.ARM64);
        cluster.setCrn(CLUSTER_CRN);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(new Json(new HashMap<>()));
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        cluster.setSdxDatabase(sdxDatabase);
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("test");
        env.setLocation(locationResponse);
        env.setCrn(ENV_CRN);
        DatabaseConfig databaseConfig = getDatabaseConfig();

        DatabaseConfigKey dbConfigKey = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKey)).thenReturn(databaseConfig);
        when(databaseParameterSetterMap.get(CloudPlatform.AWS)).thenReturn(getDatabaseParameterSetter());
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), anyString(), anyString(), any(), any(), eq("arm64")))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), new HashMap<>()));
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), anyString(), anyString(), any(), any(), eq("x86_64")))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));

        DatabaseServerV4StackRequest databaseServerV4StackRequest = underTest.getDatabaseServerRequest(CloudPlatform.AWS, cluster, env,
                "initiatorUserCrn");

        assertThat(databaseServerV4StackRequest).isNotNull();
        assertThat(databaseServerV4StackRequest.getInstanceType()).isEqualTo("instanceType");
        assertThat(databaseServerV4StackRequest.getDatabaseVendor()).isEqualTo("vendor");
    }

    @Test
    public void testGetDatabaseServerRequestWithPreviousDatabaseModified() {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setCrn(CLUSTER_CRN);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("previousDatabaseCrn", DATABASE_CRN);
        attributes.put("previousClusterShape", SdxClusterShape.LIGHT_DUTY.toString());
        sdxDatabase.setAttributes(new Json(attributes));
        cluster.setSdxDatabase(sdxDatabase);
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        env.setCrn(ENV_CRN);
        DatabaseConfig databaseConfig = getDatabaseConfig();
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setStorageSize(1024L);
        databaseServerV4Response.setInstanceType("m5.8xlarge");
        DatabaseConfigKey dbConfigKeyLight = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKeyLight)).thenReturn(databaseConfig);
        DatabaseServerParameterSetter parameterSetter = new AwsDatabaseServerParameterSetter();
        when(databaseParameterSetterMap.get(any(CloudPlatform.class))).thenReturn(parameterSetter);
        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(databaseServerV4Response);

        DatabaseServerV4StackRequest databaseServerV4StackRequest = underTest.getDatabaseServerRequest(CloudPlatform.AWS, cluster, env,
                "initiatorUserCrn");

        assertThat(databaseServerV4StackRequest).isNotNull();
        assertThat(databaseServerV4StackRequest.getInstanceType()).isEqualTo("m5.8xlarge");
        assertThat(databaseServerV4StackRequest.getStorageSize()).isEqualTo(1024L);
    }

    @Test
    public void testGetDatabaseServerRequestWithPreviousDatabaseModifiedForAzureFlexibleDb() {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("CLUSTER_NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setCrn(CLUSTER_CRN);
        SdxDatabase sdxDatabase = new SdxDatabase();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("previousDatabaseCrn", DATABASE_CRN);
        attributes.put("previousClusterShape", SdxClusterShape.LIGHT_DUTY.toString());
        sdxDatabase.setAttributes(new Json(attributes));
        cluster.setSdxDatabase(sdxDatabase);
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("azure");
        env.setCrn(ENV_CRN);
        DatabaseConfig databaseConfig = getFlexibleDatabaseConfig();
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setStorageSize(1024L);
        databaseServerV4Response.setInstanceType("m5.8xlarge");

        DatabaseConfigKey dbConfigKeyLight = new DatabaseConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKeyLight)).thenReturn(databaseConfig);
        when(databaseParameterSetterMap.get(CloudPlatform.AZURE)).thenReturn(getAzureDatabaseParameterSetter());
        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(databaseServerV4Response);

        DatabaseServerV4StackRequest databaseServerV4StackRequest = underTest.getDatabaseServerRequest(CloudPlatform.AZURE, cluster, env,
                "initiatorUserCrn");

        assertThat(databaseServerV4StackRequest).isNotNull();
        assertThat(databaseServerV4StackRequest.getInstanceType()).isEqualTo("m5.8xlarge");
        assertThat(databaseServerV4StackRequest.getStorageSize()).isEqualTo(1024L);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslEnforcementDataProvider")
    void shouldSetSSLAccordingToAttributeWhenTrue(String testCaseName, boolean supportedPlatform, String runtime,
            boolean sslEnforcementAppliedExpected) {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setCrn(CLUSTER_CRN);
        cluster.setRuntime(runtime);
        SdxDatabase sdxDatabase = new SdxDatabase();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(DATABASE_SSL_ENABLED, true);
        sdxDatabase.setAttributes(new Json(attributes));
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        cluster.setSdxDatabase(sdxDatabase);

        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("test");
        env.setLocation(locationResponse);
        env.setCrn(ENV_CRN);
        DatabaseConfig databaseConfig = getDatabaseConfig();

        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));
        when(databaseServerV4Endpoint.createInternal(any(), any())).thenThrow(BadRequestException.class);
        DatabaseConfigKey dbConfigKey = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKey)).thenReturn(databaseConfig);
        when(databaseParameterSetterMap.get(CloudPlatform.AWS)).thenReturn(getDatabaseParameterSetter());
        when(platformConfig.isExternalDatabaseSslEnforcementSupportedFor(CloudPlatform.AWS)).thenReturn(supportedPlatform);
        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(DatalakeStatusEnum.REQUESTED);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(status);

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslEnforcementDataProvider")
    void shouldSetSSLAccordingToAttributeWhenFalse(String testCaseName, boolean supportedPlatform, String runtime,
            boolean sslEnforcementAppliedExpected) {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setCrn(CLUSTER_CRN);
        cluster.setRuntime(runtime);
        SdxDatabase sdxDatabase = new SdxDatabase();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(DATABASE_SSL_ENABLED, false);
        sdxDatabase.setAttributes(new Json(attributes));
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        cluster.setSdxDatabase(sdxDatabase);

        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("test");
        env.setLocation(locationResponse);
        env.setCrn(ENV_CRN);
        DatabaseConfig databaseConfig = getDatabaseConfig();

        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));
        when(databaseServerV4Endpoint.createInternal(any(), any())).thenThrow(BadRequestException.class);
        DatabaseConfigKey dbConfigKey = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKey)).thenReturn(databaseConfig);
        when(databaseParameterSetterMap.get(CloudPlatform.AWS)).thenReturn(getDatabaseParameterSetter());
        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(DatalakeStatusEnum.REQUESTED);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(status);

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
        assertThat(sslConfig).isNull();
        verifyNoInteractions(sdxClusterRepository);
        verifyNoInteractions(notificationService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslEnforcementDataProvider")
    void shouldFunctionNormallyIfSSLAttributeMissing(String testCaseName, boolean supportedPlatform, String runtime,
            boolean sslEnforcementAppliedExpected) {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setCrn(CLUSTER_CRN);
        cluster.setRuntime(runtime);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setAttributes(null);
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        cluster.setSdxDatabase(sdxDatabase);

        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("test");
        env.setLocation(locationResponse);
        env.setCrn(ENV_CRN);
        DatabaseConfig databaseConfig = getDatabaseConfig();

        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));
        when(databaseServerV4Endpoint.createInternal(any(), any())).thenThrow(BadRequestException.class);
        DatabaseConfigKey dbConfigKey = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKey)).thenReturn(databaseConfig);
        when(databaseParameterSetterMap.get(CloudPlatform.AWS)).thenReturn(getDatabaseParameterSetter());
        when(platformConfig.isExternalDatabaseSslEnforcementSupportedFor(CloudPlatform.AWS)).thenReturn(supportedPlatform);
        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(DatalakeStatusEnum.REQUESTED);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(status);

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

    private DatabaseConfig getDatabaseConfig() {
        return new DatabaseConfig("instanceType", "vendor", 100);
    }

    private DatabaseConfig getFlexibleDatabaseConfig() {
        return new DatabaseConfig(null, "vendor", 100);
    }

    private DatabaseServerParameterSetter getAzureDatabaseParameterSetter() {
        return new DatabaseServerParameterSetter() {
            @Override
            public void setParameters(DatabaseServerV4StackRequest request, SdxCluster sdxCluster,
                    DetailedEnvironmentResponse environmentResponse, String userCrn) {
                request.setAzure(new AzureDatabaseServerV4Parameters());
            }

            @Override
            public CloudPlatform getCloudPlatform() {
                return CloudPlatform.AZURE;
            }
        };
    }

    private DatabaseServerParameterSetter getDatabaseParameterSetter() {
        return new AwsDatabaseServerParameterSetter();
    }

    private SdxCluster getSdxCluster() {
        SdxCluster cluster = new SdxCluster();
        cluster.setId(999L);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn(DATABASE_CRN);
        cluster.setSdxDatabase(sdxDatabase);
        return cluster;
    }

    @Test
    void testUpdateDatabaseTypeWhenTypeChangeRequired() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn("test-crn");

        DatabasePropertiesV4Response databasePropertiesV4Response = new DatabasePropertiesV4Response();
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setDatabasePropertiesV4Response(databasePropertiesV4Response);
        databasePropertiesV4Response.setDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER.name());
        when(azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase)).thenReturn(AzureDatabaseType.SINGLE_SERVER);
        when(sdxDatabaseRepository.save(any(SdxDatabase.class))).thenReturn(sdxDatabase);
        when(databaseServerV4Endpoint.getByCrn(anyString())).thenReturn(databaseServerV4Response);

        SdxDatabase result = underTest.updateDatabaseTypeFromRedbeams(sdxDatabase);

        verify(azureDatabaseAttributesService).updateDatabaseType(sdxDatabase, AzureDatabaseType.FLEXIBLE_SERVER);
        verify(sdxDatabaseRepository).save(sdxDatabase);
        assertNotNull(result);
    }

    @Test
    void testUpdateDatabaseTypeWhenNoUpdateRequiredSameTypes() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn("test-crn");

        DatabasePropertiesV4Response databasePropertiesV4Response = new DatabasePropertiesV4Response();
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setDatabasePropertiesV4Response(databasePropertiesV4Response);
        databasePropertiesV4Response.setDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER.name());
        when(azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase)).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);
        when(databaseServerV4Endpoint.getByCrn(anyString())).thenReturn(databaseServerV4Response);

        SdxDatabase result = underTest.updateDatabaseTypeFromRedbeams(sdxDatabase);

        verify(azureDatabaseAttributesService, never()).updateDatabaseType(any(), any());
        verify(sdxDatabaseRepository, never()).save(any());
        assertEquals(sdxDatabase, result);
    }

    @Test
    void testUpdateDatabaseTypeWhenNoDatabaseCrn() {
        // Given
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn(null);

        // When
        SdxDatabase result = underTest.updateDatabaseTypeFromRedbeams(sdxDatabase);

        // Then
        verify(azureDatabaseAttributesService, never()).updateDatabaseType(any(), any());
        verify(sdxDatabaseRepository, never()).save(any());
        assertEquals(sdxDatabase, result);
    }

    @Test
    void testUpdateDatabaseTypeWhenNoDatabaseType() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn("test-crn");

        DatabasePropertiesV4Response databasePropertiesV4Response = new DatabasePropertiesV4Response();
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setDatabasePropertiesV4Response(databasePropertiesV4Response);
        databasePropertiesV4Response.setDatabaseType(null);
        when(databaseServerV4Endpoint.getByCrn(anyString())).thenReturn(databaseServerV4Response);

        SdxDatabase result = underTest.updateDatabaseTypeFromRedbeams(sdxDatabase);

        verify(azureDatabaseAttributesService, never()).updateDatabaseType(any(), any());
        verify(sdxDatabaseRepository, never()).save(any());
        assertEquals(sdxDatabase, result);
    }

    @Test
    void testUpdateDatabaseTypeWhenNoPropertiesResponse() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn("test-crn");
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setDatabasePropertiesV4Response(null);
        when(databaseServerV4Endpoint.getByCrn(anyString())).thenReturn(databaseServerV4Response);

        SdxDatabase result = underTest.updateDatabaseTypeFromRedbeams(sdxDatabase);

        verify(azureDatabaseAttributesService, never()).updateDatabaseType(any(), any());
        verify(sdxDatabaseRepository, never()).save(any());
        assertEquals(sdxDatabase, result);
    }
}
