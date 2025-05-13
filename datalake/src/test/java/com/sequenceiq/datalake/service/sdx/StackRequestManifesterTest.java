package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.SDX_VALIDATION_FAILED_AND_SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.idbmms.GrpcIdbmmsClient;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationException;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.telemetry.response.WorkloadAnalyticsResponse;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.datalake.converter.DatabaseRequestConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageValidator;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.service.azure.HostEncryptionCalculator;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TagResponse;

@ExtendWith(MockitoExtension.class)
public class StackRequestManifesterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String ENVIRONMENT_CRN = "crn:myEnvironment";

    private static final String BAD_ENVIRONMENT_CRN = "crn:myBadEnvironment";

    private static final String STACK_NAME = "myStack";

    private static final String CLOUD_PLATFORM_AWS = CloudPlatform.AWS.name();

    private static final String CLOUD_PLATFORM_AZURE = CloudPlatform.AZURE.name();

    private static final String USER_1 = "user1";

    private static final String GROUP_1 = "group1";

    private static final String USER_2 = "user2";

    private static final String GROUP_2 = "group2";

    private static final String USER_ROLE_1 = "user-role1";

    private static final String GROUP_ROLE_1 = "group-role1";

    private static final String USER_ROLE_2 = "user-role2";

    private static final String GROUP_ROLE_2 = "group-role2";

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENCRYPTION_KEY = "mykey";

    private static final String AWS_ENCRYPTION_KEY = "dummyAwsKey";

    private static final String DISK_ENCRYPTION_SET_ID = "dummyDiskEncryptionSetId";

    @Mock
    private GrpcIdbmmsClient idbmmsClient;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackV4Request stackV4Request;

    private ClusterV4Request clusterV4Request;

    private CloudStorageRequest cloudStorage;

    @Mock
    private MappingsConfig mappingsConfig;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private GatewayManifester gatewayManifester;

    @Mock
    private CloudStorageValidator cloudStorageValidator;

    @Mock
    private DatabaseRequestConverter databaseRequestConverter;

    @Mock
    private MultiAzDecorator multiAzDecorator;

    @Mock
    private HostEncryptionCalculator hostEncryptionCalculator;

    @Mock
    private SdxNotificationService sdxNotificationService;

    @Mock
    private EventSenderService eventSenderService;

    @InjectMocks
    private StackRequestManifester underTest;

    static Object[][] encryptionTypeDataProvider() {
        return new Object[][]{
                // testCaseName, encryptionType, encryptionKey, encryptionTypeExpected, encryptionKeyExpected
                {"encryptionType == null", null, null, EncryptionType.DEFAULT, null},
                {"EncryptionType.NONE", EncryptionType.NONE, null, EncryptionType.NONE, null},
                {"EncryptionType.DEFAULT", EncryptionType.DEFAULT, null, EncryptionType.DEFAULT, null},
                {"EncryptionType.CUSTOM", EncryptionType.CUSTOM, ENCRYPTION_KEY, EncryptionType.CUSTOM, ENCRYPTION_KEY},
        };
    }

    @BeforeEach
    public void setUp() {
        clusterV4Request = new ClusterV4Request();
        cloudStorage = new CloudStorageRequest();
    }

    @Test
    public void testSetupStackRequestForCloudbreakWhenMultiAzAndAWSNativeAndNotGovCloudThenVariantMustBeAwsNative() throws IOException {
        SdxCluster sdxCluster = getSdxCluster(true);
        StackV4Request stackV4Request = getStackV4Request(sdxCluster, true);
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(false);

        when(gatewayManifester.configureGatewayForSdxCluster(any())).thenReturn(stackV4Request);
        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        String variant = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class).getVariant();
        assertThat(variant).isEqualTo("AWS_NATIVE");
        verify(multiAzDecorator).decorateStackRequestWithAwsNative(any());
        verify(multiAzDecorator).decorateStackRequestWithMultiAz(any(), any(), any());
    }

    @Test
    public void testSetupStackRequestForCloudbreakWhenMultiAzAndAWSNativeAndGovCloudThenVariantMustBeAwsGovNative() throws IOException {
        SdxCluster sdxCluster = getSdxCluster(true);
        StackV4Request stackV4Request = getStackV4Request(sdxCluster, true);
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(true);

        when(gatewayManifester.configureGatewayForSdxCluster(any())).thenReturn(stackV4Request);
        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        String variant = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class).getVariant();
        assertThat(variant).isEqualTo("AWS_NATIVE_GOV");
        verify(multiAzDecorator).decorateStackRequestWithAwsNative(any());
        verify(multiAzDecorator).decorateStackRequestWithMultiAz(any(), any(), any());
    }

    @Test
    public void testAwsNativeEnforcementDuringSetupStackRequestWhenEntitled() throws IOException {
        SdxCluster sdxCluster = getSdxCluster(false);
        StackV4Request stackV4Request = getStackV4Request(sdxCluster, false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(false);

        when(gatewayManifester.configureGatewayForSdxCluster(any())).thenReturn(stackV4Request);
        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());

        when(entitlementService.enforceAwsNativeForSingleAzDatalakeEnabled(any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        String variant = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class).getVariant();
        assertThat(variant).isEqualTo("AWS_NATIVE");
        verify(multiAzDecorator, never()).decorateStackRequestWithAwsNative(any());
        verify(multiAzDecorator, never()).decorateStackRequestWithMultiAz(any(), any(), any());
    }

    @Test
    public void testAwsNativeEnforcementDuringSetupStackRequestWithNoEntitlementAndRuntimeVersionThatMakesAwsNativeVariantDefault() throws IOException {
        SdxCluster sdxCluster = getSdxCluster(false);
        sdxCluster.setRuntime("7.3.1");
        StackV4Request stackV4Request = getStackV4Request(sdxCluster, false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(false);

        when(gatewayManifester.configureGatewayForSdxCluster(any())).thenReturn(stackV4Request);
        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());

        when(entitlementService.enforceAwsNativeForSingleAzDatalakeEnabled(any())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        String variant = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class).getVariant();
        assertThat(variant).isEqualTo("AWS_NATIVE");
        verify(multiAzDecorator, never()).decorateStackRequestWithAwsNative(any());
        verify(multiAzDecorator, never()).decorateStackRequestWithMultiAz(any(), any(), any());
    }

    @Test
    public void testAwsNativeEnforcementDuringSetupStackRequestWithNoEntitlementAndRuntimeVersionLowerThanRequiredForMakingAwsNativeVariantDefault()
            throws IOException {
        SdxCluster sdxCluster = getSdxCluster(false);
        sdxCluster.setRuntime("7.3.0");
        StackV4Request stackV4Request = getStackV4Request(sdxCluster, false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(false);

        when(gatewayManifester.configureGatewayForSdxCluster(any())).thenReturn(stackV4Request);
        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());

        when(entitlementService.enforceAwsNativeForSingleAzDatalakeEnabled(any())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        String variant = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class).getVariant();
        assertThat(variant).isNull();
        verify(multiAzDecorator, never()).decorateStackRequestWithAwsNative(any());
        verify(multiAzDecorator, never()).decorateStackRequestWithMultiAz(any(), any(), any());
    }

    @Test
    public void testAwsNativeEnforcementDuringSetupStackRequestWhenNotEntitled() throws IOException {
        SdxCluster sdxCluster = getSdxCluster(false);
        StackV4Request stackV4Request = getStackV4Request(sdxCluster, false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(false);

        when(gatewayManifester.configureGatewayForSdxCluster(any())).thenReturn(stackV4Request);
        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());
        when(entitlementService.enforceAwsNativeForSingleAzDatalakeEnabled(any())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        String variant = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class).getVariant();
        assertThat(variant).isNotEqualTo("AWS_NATIVE");
        verify(multiAzDecorator, never()).decorateStackRequestWithAwsNative(any());
        verify(multiAzDecorator, never()).decorateStackRequestWithMultiAz(any(), any(), any());
    }

    @Test
    public void testAwsNativeEnforcementDuringSetupStackRequestWhenAzure() throws IOException {
        SdxCluster sdxCluster = getSdxCluster(false);
        StackV4Request stackV4Request = getStackV4Request(sdxCluster, false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(false);
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AZURE.name());

        when(gatewayManifester.configureGatewayForSdxCluster(any())).thenReturn(stackV4Request);
        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());
        lenient().when(entitlementService.enforceAwsNativeForSingleAzDatalakeEnabled(any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        String variant = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class).getVariant();
        assertThat(variant).isNotEqualTo("AWS_NATIVE");
        verify(multiAzDecorator, never()).decorateStackRequestWithAwsNative(any());
        verify(multiAzDecorator, never()).decorateStackRequestWithMultiAz(any(), any(), any());
    }

    @Test
    public void testWorkloadAnalyticsEnabledWhenEnabledForEnvironment() throws IOException {
        SdxCluster sdxCluster = getSdxCluster(false);
        getStackV4Request(sdxCluster, false);

        ReflectionTestUtils.setField(underTest, "telemetryPublisherEnabled", true);

        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(false);
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        telemetryResponse.setLogging(new LoggingResponse());
        FeaturesResponse featuresResponse = new FeaturesResponse();
        FeatureSetting workloadAnalyticsFeature = new FeatureSetting();
        workloadAnalyticsFeature.setEnabled(true);
        featuresResponse.setWorkloadAnalytics(workloadAnalyticsFeature);
        telemetryResponse.setFeatures(featuresResponse);
        WorkloadAnalyticsResponse workloadAnalyticsResponse = new WorkloadAnalyticsResponse();
        workloadAnalyticsResponse.setAttributes(Map.of("test", "result"));
        telemetryResponse.setWorkloadAnalytics(workloadAnalyticsResponse);
        detailedEnvironmentResponse.setTelemetry(telemetryResponse);

        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        TelemetryRequest telemetry = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class).getTelemetry();
        assertNotNull(telemetry.getWorkloadAnalytics());
        assertEquals("result", telemetry.getWorkloadAnalytics().getAttributes().get("test"));
    }

    @Test
    public void testWorkloadAnalyticsDisabledWhenDisabledForEnvironment() throws IOException {
        SdxCluster sdxCluster = getSdxCluster(false);
        getStackV4Request(sdxCluster, false);

        ReflectionTestUtils.setField(underTest, "telemetryPublisherEnabled", true);

        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(false);
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        telemetryResponse.setLogging(new LoggingResponse());
        FeaturesResponse featuresResponse = new FeaturesResponse();
        FeatureSetting workloadAnalyticsFeature = new FeatureSetting();
        workloadAnalyticsFeature.setEnabled(false);
        featuresResponse.setWorkloadAnalytics(workloadAnalyticsFeature);
        telemetryResponse.setFeatures(featuresResponse);
        WorkloadAnalyticsResponse workloadAnalyticsResponse = new WorkloadAnalyticsResponse();
        workloadAnalyticsResponse.setAttributes(Map.of("test", "result"));
        telemetryResponse.setWorkloadAnalytics(workloadAnalyticsResponse);
        detailedEnvironmentResponse.setTelemetry(telemetryResponse);

        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        TelemetryRequest telemetry = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class).getTelemetry();
        assertNull(telemetry.getWorkloadAnalytics());
    }

    @Test
    public void testWorkloadAnalyticsDisabledWhenMissingForEnvironment() throws IOException {
        SdxCluster sdxCluster = getSdxCluster(false);
        getStackV4Request(sdxCluster, false);

        ReflectionTestUtils.setField(underTest, "telemetryPublisherEnabled", true);

        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(false);
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        telemetryResponse.setLogging(new LoggingResponse());
        FeaturesResponse featuresResponse = new FeaturesResponse();
        telemetryResponse.setFeatures(featuresResponse);
        detailedEnvironmentResponse.setTelemetry(telemetryResponse);

        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        TelemetryRequest telemetry = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class).getTelemetry();
        assertNull(telemetry.getWorkloadAnalytics());
    }

    @Test
    public void configureStackForSdxClusterTestWhenMultiAzAndAzure() {
        SdxCluster sdxCluster = getSdxCluster(true);
        StackV4Request stackV4Request = getStackV4Request(sdxCluster, false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(false);
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AZURE.name());

        when(gatewayManifester.configureGatewayForSdxCluster(any())).thenReturn(stackV4Request);
        doNothing().when(cloudStorageValidator).validate(any(), any(), any());
        when(databaseRequestConverter.createExternalDbRequest(sdxCluster)).thenReturn(new DatabaseRequest());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.configureStackForSdxCluster(sdxCluster, detailedEnvironmentResponse));

        verify(multiAzDecorator, never()).decorateStackRequestWithAwsNative(any());
        verify(multiAzDecorator).decorateStackRequestWithMultiAz(any(), any(), any());
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenNoCloudStorage() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isNull();
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithExistingAccountMappingAndEmptyMaps() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);

        clusterV4Request.setCloudStorage(cloudStorage);
        AccountMappingBase accountMapping = new AccountMappingBase();
        cloudStorage.setAccountMapping(accountMapping);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping()).isSameAs(accountMapping);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping().getGroupMappings()).isEmpty();
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping().getUserMappings()).isEmpty();
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithExistingAccountMappingAndNonemptyMaps() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        clusterV4Request.setCloudStorage(cloudStorage);
        AccountMappingBase accountMapping = new AccountMappingBase();
        accountMapping.setGroupMappings(Map.ofEntries(Map.entry(GROUP_1, GROUP_ROLE_1)));
        accountMapping.setUserMappings(Map.ofEntries(Map.entry(USER_1, USER_ROLE_1)));
        cloudStorage.setAccountMapping(accountMapping);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping()).isSameAs(accountMapping);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping().getGroupMappings()).containsOnly(Map.entry(GROUP_1, GROUP_ROLE_1));
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping().getUserMappings()).containsOnly(Map.entry(USER_1, USER_ROLE_1));
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithNoAccountMappingAndIdbmmsSourceAndSuccess() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(idbmmsClient.getMappingsConfig("crn", ENVIRONMENT_CRN)).thenReturn(mappingsConfig);
        when(mappingsConfig.getGroupMappings()).thenReturn(Map.ofEntries(Map.entry(GROUP_2, GROUP_ROLE_2)));
        when(mappingsConfig.getActorMappings()).thenReturn(Map.ofEntries(Map.entry(USER_2, USER_ROLE_2)));

        clusterV4Request.setCloudStorage(cloudStorage);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        AccountMappingBase accountMapping = clusterV4Request.getCloudStorage().getAccountMapping();
        assertThat(accountMapping).isNotNull();
        assertThat(accountMapping.getGroupMappings()).containsOnly(Map.entry(GROUP_2, GROUP_ROLE_2));
        assertThat(accountMapping.getUserMappings()).containsOnly(Map.entry(USER_2, USER_ROLE_2));
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithNoAccountMappingAndIdbmmsSourceAndFailure() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(idbmmsClient.getMappingsConfig("crn", BAD_ENVIRONMENT_CRN)).thenThrow(new IdbmmsOperationException("Houston, we have a problem."));

        clusterV4Request.setCloudStorage(cloudStorage);

        assertThrows(
                BadRequestException.class,
                () -> underTest.setupCloudStorageAccountMapping(stackV4Request, BAD_ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS));
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithNoAccountMappingAndMockSourceAndAws() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        clusterV4Request.setCloudStorage(cloudStorage);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.MOCK, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping()).isNull();
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithNoAccountMappingAndMockSourceAndAzure() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        clusterV4Request.setCloudStorage(cloudStorage);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.MOCK, CLOUD_PLATFORM_AZURE);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping()).isNull();
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithNoAccountMappingAndNoneSource() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        clusterV4Request.setCloudStorage(cloudStorage);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.NONE, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping()).isNull();
    }

    @Test
    void testRAZSetupCloudStorageAccountMappingsWithRAZMapping() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        clusterV4Request.setCloudStorage(cloudStorage);
        when(idbmmsClient.getMappingsConfig("crn", ENVIRONMENT_CRN)).thenReturn(mappingsConfig);

        // Enable RAZ for this test and make sure role is checked for.
        clusterV4Request.setRangerRazEnabled(true);
        when(mappingsConfig.getActorMappings()).thenReturn(Map.of("rangerraz", ""));

        // Should not throw an error.
        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS);
    }

    @Test
    void testRAZSetupCloudStorageAccountMappingsWithoutRAZMapping() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        clusterV4Request.setCloudStorage(cloudStorage);
        when(idbmmsClient.getMappingsConfig("crn", ENVIRONMENT_CRN)).thenReturn(mappingsConfig);

        // Enable RAZ without mapping which should throw an error.
        clusterV4Request.setRangerRazEnabled(true);
        when(mappingsConfig.getActorMappings()).thenReturn(Map.of());
        assertThrows(
                BadRequestException.class,
                () -> underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS));
    }

    @Test
    public void testAddAzureIdbrokerMsiToTelemetry() {
        Map<String, Object> attributes = new HashMap<>();
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        List<StorageIdentityBase> identities = new ArrayList<>();
        StorageIdentityBase identity = new StorageIdentityBase();
        identity.setType(CloudIdentityType.ID_BROKER);
        AdlsGen2CloudStorageV1Parameters adlsGen2 = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2.setManagedIdentity("msi");
        identity.setAdlsGen2(adlsGen2);
        identities.add(identity);
        cloudStorageRequest.setIdentities(identities);
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        clusterV4Request.setCloudStorage(cloudStorageRequest);

        underTest.addAzureIdbrokerMsiToTelemetry(attributes, stackV4Request);

        assertThat(clusterV4Request.getCloudStorage()).isNotNull();
        assertThat(attributes.get(FluentConfigView.AZURE_IDBROKER_INSTANCE_MSI)).isEqualTo("msi");
    }

    @Test
    public void testAddAzureIdbrokerMsiToTelemetryWithoutCloudStorage() {
        Map<String, Object> attributes = new HashMap<>();
        underTest.addAzureIdbrokerMsiToTelemetry(attributes, stackV4Request);

        assertThat(clusterV4Request.getCloudStorage()).isNull();
        assertThat(attributes.size()).isEqualTo(0);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAzureAndDiskEncryptionSetIdAndNoInstanceGroups() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.AZURE.name());
        envResponse.setAzure(AzureEnvironmentParameters.builder()
                .withResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                        .withDiskEncryptionSetId(DISK_ENCRYPTION_SET_ID)
                        .withEncryptionKeyUrl(ENCRYPTION_KEY)
                        .build())
                .build());

        List<InstanceGroupV4Request> instanceGroups = new ArrayList<>();
        when(stackV4Request.getInstanceGroups()).thenReturn(instanceGroups);

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        assertThat(instanceGroups).isEmpty();
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAzureAndDiskEncryptionSetIdAndNoInstanceTemplateParameters() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.AZURE.name());
        envResponse.setAzure(AzureEnvironmentParameters.builder()
                .withResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                        .withDiskEncryptionSetId(DISK_ENCRYPTION_SET_ID)
                        .withEncryptionKeyUrl(ENCRYPTION_KEY)
                        .build())
                .build());
        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verifyAzureEncryption(instanceGroupV4Request.getTemplate(), EncryptionType.CUSTOM, DISK_ENCRYPTION_SET_ID, ENCRYPTION_KEY);
    }

    @Test
    void setupYarnDetailsShouldFailWhenEnvIsMissingData() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        StackV4Request stackV4Request = new StackV4Request();
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.setupYarnDetails(envResponse, stackV4Request));
        assertEquals("There is no queue defined in your environment, please create a new yarn environment with queue", exception.getMessage());
    }

    @Test
    void setupYarnDetailsShouldPassWithCorrectData() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        EnvironmentNetworkYarnParams environmentNetworkYarnParams = new EnvironmentNetworkYarnParams();
        environmentNetworkYarnParams.setQueue("myqueue");
        environmentNetworkYarnParams.setLifetime(1);
        environmentNetworkResponse.setYarn(environmentNetworkYarnParams);
        envResponse.setNetwork(environmentNetworkResponse);
        StackV4Request stackV4Request = new StackV4Request();
        underTest.setupYarnDetails(envResponse, stackV4Request);
        assertEquals("myqueue", stackV4Request.getYarn().getYarnQueue());
        assertEquals(1, stackV4Request.getYarn().getLifetime());
    }

    @Test
    void whenValidateCloudStorageAndHandleTimeoutShouldNotThrowException() {
        SdxCluster sdxCluster = new SdxCluster();
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        StackV4Request stackV4Request = new StackV4Request();
        doThrow(new RuntimeException("")).when(cloudStorageValidator).validate(any(), any(), any());
        assertDoesNotThrow(() -> underTest.validateCloudStorageAndHandleTimeout(sdxCluster, envResponse, stackV4Request));
        verify(sdxNotificationService, times(1)).send(eq(SDX_VALIDATION_FAILED_AND_SKIPPED), any(), any(SdxCluster.class));
    }

    @Test
    void whenValidateCloudStorageShouldThrowExceptionOnValidationError() {
        SdxCluster sdxCluster = new SdxCluster();
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request cluster = new ClusterV4Request();
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        cluster.setCloudStorage(cloudStorageRequest);
        stackV4Request.setCluster(cluster);
        doAnswer(invocation -> {
            ValidationResult.ValidationResultBuilder builder = invocation.getArgument(2);
            builder.error("Invalid storage configuration");
            return null;
        }).when(cloudStorageValidator).validate(any(CloudStorageRequest.class), any(), any(ValidationResult.ValidationResultBuilder.class));
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateCloudStorageAndHandleTimeout(sdxCluster, envResponse, stackV4Request));
        assertEquals("Invalid storage configuration", exception.getMessage());
    }

    @Test
    void whenValidateCloudStorageShouldWarnValidationWarning() {
        SdxCluster sdxCluster = new SdxCluster();
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request cluster = new ClusterV4Request();
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        cluster.setCloudStorage(cloudStorageRequest);
        stackV4Request.setCluster(cluster);
        doAnswer(invocation -> {
            ValidationResult.ValidationResultBuilder builder = invocation.getArgument(2);
            builder.warning("Storage might not be configured properly");
            return null;
        }).when(cloudStorageValidator).validate(any(CloudStorageRequest.class), any(), any(ValidationResult.ValidationResultBuilder.class));
        assertDoesNotThrow(() -> underTest.validateCloudStorageAndHandleTimeout(sdxCluster, envResponse, stackV4Request));
        verify(sdxNotificationService, times(1)).send(eq(SDX_VALIDATION_FAILED_AND_SKIPPED), any(), any(SdxCluster.class));
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAzureAndDiskEncryptionSetIdAndNoEncryptionParameters() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.AZURE.name());
        envResponse.setAzure(AzureEnvironmentParameters.builder()
                .withResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                        .withDiskEncryptionSetId(DISK_ENCRYPTION_SET_ID)
                        .withEncryptionKeyUrl(ENCRYPTION_KEY)
                        .build())
                .build());
        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request = instanceGroupV4Request.getTemplate();
        instanceTemplateV4Request.createAzure();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verifyAzureEncryption(instanceTemplateV4Request, EncryptionType.CUSTOM, DISK_ENCRYPTION_SET_ID, ENCRYPTION_KEY);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAzureAndDiskEncryptionSetIdAndTwoInstanceGroups() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setAzure(AzureEnvironmentParameters.builder()
                .withResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                        .withDiskEncryptionSetId(DISK_ENCRYPTION_SET_ID)
                        .withEncryptionKeyUrl(ENCRYPTION_KEY)
                        .build())
                .build());
        envResponse.setCloudPlatform(CloudPlatform.AZURE.name());

        InstanceGroupV4Request instanceGroupV4Request1 = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request1 = instanceGroupV4Request1.getTemplate();

        InstanceGroupV4Request instanceGroupV4Request2 = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request2 = instanceGroupV4Request2.getTemplate();
        instanceTemplateV4Request2.createAzure().setEncryption(createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, ENCRYPTION_KEY));

        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request1, instanceGroupV4Request2));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);
        verifyAzureEncryption(instanceTemplateV4Request1, EncryptionType.CUSTOM, DISK_ENCRYPTION_SET_ID, ENCRYPTION_KEY);
        verifyAzureEncryption(instanceTemplateV4Request2, EncryptionType.CUSTOM, DISK_ENCRYPTION_SET_ID, ENCRYPTION_KEY);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAzureAndEncryptionAtHostAndNoInstanceGroups() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setAccountId(ACCOUNT_ID);
        envResponse.setCloudPlatform(CloudPlatform.AZURE.name());

        List<InstanceGroupV4Request> instanceGroups = new ArrayList<>();
        when(stackV4Request.getInstanceGroups()).thenReturn(instanceGroups);

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        assertThat(instanceGroups).isEmpty();
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAzureAndEncryptionAtHostAndNoInstanceTemplateParameters() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setAccountId(ACCOUNT_ID);
        envResponse.setCloudPlatform(CloudPlatform.AZURE.name());

        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));
        when(hostEncryptionCalculator.hostEncryptionRequired(any())).thenReturn(true);

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verifyAzureEncryptionForEncryptionAtHost(instanceGroupV4Request.getTemplate(), Boolean.TRUE);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAzureAndEncryptionAtHostAndNoEncryptionParameters() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setAccountId(ACCOUNT_ID);
        envResponse.setCloudPlatform(CloudPlatform.AZURE.name());
        when(hostEncryptionCalculator.hostEncryptionRequired(any())).thenReturn(true);
        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request = instanceGroupV4Request.getTemplate();
        instanceTemplateV4Request.createAzure();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verifyAzureEncryptionForEncryptionAtHost(instanceGroupV4Request.getTemplate(), Boolean.TRUE);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAzureAndEncryptionAtHostAndTwoInstanceGroups() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setAccountId(ACCOUNT_ID);
        envResponse.setCloudPlatform(CloudPlatform.AZURE.name());
        when(hostEncryptionCalculator.hostEncryptionRequired(any())).thenReturn(true);

        InstanceGroupV4Request instanceGroupV4Request1 = createInstanceGroupV4Request();

        InstanceGroupV4Request instanceGroupV4Request2 = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request2 = instanceGroupV4Request2.getTemplate();
        instanceTemplateV4Request2.createAzure().setEncryption(createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, ENCRYPTION_KEY));

        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request1, instanceGroupV4Request2));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);
        verifyAzureEncryptionForEncryptionAtHost(instanceGroupV4Request1.getTemplate(), Boolean.TRUE);
        verifyAzureEncryptionForEncryptionAtHost(instanceGroupV4Request2.getTemplate(), Boolean.TRUE);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenGcpAndEncryptionKeyIsNotPresent() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.GCP.name());

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verify(stackV4Request, never()).getInstanceGroups();
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenGcpAndEncryptionKeyAndNoInstanceGroups() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.GCP.name());
        envResponse.setGcp(GcpEnvironmentParameters.builder()
                .withResourceEncryptionParameters(GcpResourceEncryptionParameters.builder()
                        .withEncryptionKey(ENCRYPTION_KEY)
                        .build())
                .build());

        List<InstanceGroupV4Request> instanceGroups = new ArrayList<>();
        when(stackV4Request.getInstanceGroups()).thenReturn(instanceGroups);

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        assertThat(instanceGroups).isEmpty();
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenGcpAndEncryptionKeyAndNoInstanceTemplateParameters() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.GCP.name());
        envResponse.setGcp(GcpEnvironmentParameters.builder()
                .withResourceEncryptionParameters(GcpResourceEncryptionParameters.builder()
                        .withEncryptionKey(ENCRYPTION_KEY)
                        .build())
                .build());
        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verifyGcpEncryption(instanceGroupV4Request.getTemplate(), EncryptionType.CUSTOM, ENCRYPTION_KEY);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenGcpAndEncryptionKeydAndNoEncryptionParameters() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.GCP.name());
        envResponse.setGcp(GcpEnvironmentParameters.builder()
                .withResourceEncryptionParameters(GcpResourceEncryptionParameters.builder()
                        .withEncryptionKey(ENCRYPTION_KEY)
                        .build())
                .build());
        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request = instanceGroupV4Request.getTemplate();
        instanceTemplateV4Request.createGcp();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verifyGcpEncryption(instanceTemplateV4Request, EncryptionType.CUSTOM, ENCRYPTION_KEY);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenGcpAndEncryptionKeyAndTwoInstanceGroups() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setGcp(GcpEnvironmentParameters.builder()
                .withResourceEncryptionParameters(GcpResourceEncryptionParameters.builder()
                        .withEncryptionKey(ENCRYPTION_KEY)
                        .build())
                .build());
        envResponse.setCloudPlatform(CloudPlatform.GCP.name());

        InstanceGroupV4Request instanceGroupV4Request1 = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request1 = instanceGroupV4Request1.getTemplate();

        InstanceGroupV4Request instanceGroupV4Request2 = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request2 = instanceGroupV4Request2.getTemplate();
        instanceTemplateV4Request2.createGcp().setEncryption(createGcpEncryptionV4Parameters(EncryptionType.CUSTOM, ENCRYPTION_KEY));

        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request1, instanceGroupV4Request2));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);
        verifyGcpEncryption(instanceTemplateV4Request1, EncryptionType.CUSTOM, ENCRYPTION_KEY);
        verifyGcpEncryption(instanceTemplateV4Request2, EncryptionType.CUSTOM, ENCRYPTION_KEY);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAwsAndNoInstanceGroups() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.AWS.name());
        List<InstanceGroupV4Request> instanceGroups = new ArrayList<>();
        when(stackV4Request.getInstanceGroups()).thenReturn(instanceGroups);

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        assertThat(instanceGroups).isEmpty();
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAwsAndNoInstanceTemplateParameters() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.AWS.name());
        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verifyAwsEncryption(instanceGroupV4Request.getTemplate(), EncryptionType.DEFAULT);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAwsAndNoEncryptionParameters() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.AWS.name());
        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request = instanceGroupV4Request.getTemplate();
        instanceTemplateV4Request.createAws();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verifyAwsEncryption(instanceTemplateV4Request, EncryptionType.DEFAULT);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAwsAndCustomEncryptionParameters() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.AWS.name());
        envResponse.setAws(AwsEnvironmentParameters.builder()
                .withAwsDiskEncryptionParameters(AwsDiskEncryptionParameters.builder()
                        .withEncryptionKeyArn(AWS_ENCRYPTION_KEY)
                        .build())
                .build());
        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verifyAwsEncryption(instanceGroupV4Request.getTemplate(), EncryptionType.CUSTOM, AWS_ENCRYPTION_KEY);

    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("encryptionTypeDataProvider")
    void setupInstanceVolumeEncryptionTestWhenAwsAndEncryptionParameters(String testCaseName, EncryptionType encryptionType, String encryptionKey,
            EncryptionType encryptionTypeExpected, String encryptionKeyExpected) {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.AWS.name());
        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request = instanceGroupV4Request.getTemplate();
        if (encryptionKey == null) {
            instanceTemplateV4Request.createAws().setEncryption(createAwsEncryptionV4Parameters(encryptionType));
        } else {
            instanceTemplateV4Request.createAws().setEncryption(createAwsEncryptionV4Parameters(encryptionType, encryptionKey));
        }
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        if (encryptionKeyExpected == null) {
            verifyAwsEncryption(instanceTemplateV4Request, encryptionTypeExpected);
        } else {
            verifyAwsEncryption(instanceTemplateV4Request, encryptionTypeExpected, encryptionKeyExpected);
        }
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAwsAndTwoInstanceGroupsAndEncryptionTypesNoneCustom() {
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setCloudPlatform(CloudPlatform.AWS.name());
        InstanceGroupV4Request instanceGroupV4Request1 = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request1 = instanceGroupV4Request1.getTemplate();
        instanceTemplateV4Request1.createAws().setEncryption(createAwsEncryptionV4Parameters(EncryptionType.NONE));

        InstanceGroupV4Request instanceGroupV4Request2 = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request2 = instanceGroupV4Request2.getTemplate();
        instanceTemplateV4Request2.createAws().setEncryption(createAwsEncryptionV4Parameters(EncryptionType.CUSTOM, ENCRYPTION_KEY));

        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request1, instanceGroupV4Request2));

        underTest.setupInstanceVolumeEncryption(stackV4Request, envResponse);

        verifyAwsEncryption(instanceTemplateV4Request1, EncryptionType.NONE);
        verifyAwsEncryption(instanceTemplateV4Request2, EncryptionType.CUSTOM, ENCRYPTION_KEY);
    }

    private InstanceGroupV4Request createInstanceGroupV4Request() {
        InstanceGroupV4Request instanceGroupV4Request = new InstanceGroupV4Request();
        instanceGroupV4Request.setTemplate(new InstanceTemplateV4Request());
        return instanceGroupV4Request;
    }

    private void verifyAwsEncryption(InstanceTemplateV4Request instanceTemplateV4Request, EncryptionType expectedEncryptionType) {
        verifyAwsEncryption(instanceTemplateV4Request, expectedEncryptionType, null);
    }

    private void verifyAwsEncryption(InstanceTemplateV4Request instanceTemplateV4Request, EncryptionType expectedEncryptionType, String expectedEncryptionKey) {
        AwsInstanceTemplateV4Parameters aws = instanceTemplateV4Request.getAws();
        assertThat(aws).isNotNull();
        AwsEncryptionV4Parameters encryption = aws.getEncryption();
        assertThat(encryption).isNotNull();
        assertThat(encryption.getType()).isEqualTo(expectedEncryptionType);
        assertThat(encryption.getKey()).isEqualTo(expectedEncryptionKey);
    }

    private AwsEncryptionV4Parameters createAwsEncryptionV4Parameters(EncryptionType encryptionType) {
        return createAwsEncryptionV4Parameters(encryptionType, null);
    }

    private AwsEncryptionV4Parameters createAwsEncryptionV4Parameters(EncryptionType encryptionType, String encryptionKey) {
        AwsEncryptionV4Parameters awsEncryptionV4Parameters = new AwsEncryptionV4Parameters();
        awsEncryptionV4Parameters.setType(encryptionType);
        awsEncryptionV4Parameters.setKey(encryptionKey);
        return awsEncryptionV4Parameters;
    }

    private AzureEncryptionV4Parameters createAzureEncryptionV4Parameters(EncryptionType encryptionType, String encryptionKey) {
        AzureEncryptionV4Parameters azureEncryptionV4Parameters = new AzureEncryptionV4Parameters();
        azureEncryptionV4Parameters.setType(encryptionType);
        azureEncryptionV4Parameters.setKey(encryptionKey);
        return azureEncryptionV4Parameters;
    }

    private void verifyAzureEncryption(InstanceTemplateV4Request instanceTemplateV4Request, EncryptionType expectedEncryptionType,
            String expectedDiskEncryptionSetId, String expectedEncryptionKeyUrl) {
        AzureInstanceTemplateV4Parameters azure = instanceTemplateV4Request.getAzure();
        assertThat(azure).isNotNull();
        AzureEncryptionV4Parameters encryption = azure.getEncryption();
        assertThat(encryption).isNotNull();
        assertThat(encryption.getType()).isEqualTo(expectedEncryptionType);
        assertThat(encryption.getKey()).isEqualTo(expectedEncryptionKeyUrl);
        assertThat(encryption.getDiskEncryptionSetId()).isEqualTo(expectedDiskEncryptionSetId);
    }

    private void verifyAzureEncryptionForEncryptionAtHost(InstanceTemplateV4Request instanceTemplateV4Request, Boolean expectedIsEncryptionAtHost) {
        AzureInstanceTemplateV4Parameters azure = instanceTemplateV4Request.getAzure();
        assertThat(azure).isNotNull();
        AzureEncryptionV4Parameters encryption = azure.getEncryption();
        assertThat(encryption).isNotNull();
        assertThat(encryption.getEncryptionAtHostEnabled()).isEqualTo(expectedIsEncryptionAtHost);
    }

    private GcpEncryptionV4Parameters createGcpEncryptionV4Parameters(EncryptionType encryptionType, String encryptionKey) {
        GcpEncryptionV4Parameters gcpEncryptionV4Parameters = new GcpEncryptionV4Parameters();
        gcpEncryptionV4Parameters.setType(encryptionType);
        gcpEncryptionV4Parameters.setKey(encryptionKey);
        return gcpEncryptionV4Parameters;
    }

    private void verifyGcpEncryption(InstanceTemplateV4Request instanceTemplateV4Request, EncryptionType expectedEncryptionType,
            String expectedEncryptionKey) {
        GcpInstanceTemplateV4Parameters gcp = instanceTemplateV4Request.getGcp();
        assertThat(gcp).isNotNull();
        GcpEncryptionV4Parameters encryption = gcp.getEncryption();
        assertThat(encryption).isNotNull();
        assertThat(encryption.getType()).isEqualTo(expectedEncryptionType);
        assertThat(encryption.getKey()).isEqualTo(expectedEncryptionKey);
    }

    private StackV4Request getStackV4Request(SdxCluster sdxCluster, Boolean awsNative) {
        StackV4Request stackV4Request = new StackV4Request();
        if (awsNative) {
            stackV4Request.setVariant("AWS_NATIVE");
        }
        stackV4Request.setInstanceGroups(new ArrayList<>());
        sdxCluster.setStackRequest(stackV4Request);
        return stackV4Request;
    }

    private SdxCluster getSdxCluster(Boolean multiAz) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("sdxName");
        sdxCluster.setEnvCrn("envcrn");
        sdxCluster.setEnableMultiAz(multiAz);
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);
        return sdxCluster;
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponse(Boolean govCloud) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EnvironmentAuthenticationResponse environmentAuthenticationResponse = new EnvironmentAuthenticationResponse();
        environmentAuthenticationResponse.setLoginUserName("cb");
        environmentAuthenticationResponse.setPublicKey("pubkey");
        detailedEnvironmentResponse.setAuthentication(environmentAuthenticationResponse);
        detailedEnvironmentResponse.setTags(new TagResponse());
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        detailedEnvironmentResponse.setAccountId(ACCOUNT_ID);
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setGovCloud(govCloud);
        detailedEnvironmentResponse.setCredential(credentialResponse);
        return detailedEnvironmentResponse;
    }

}