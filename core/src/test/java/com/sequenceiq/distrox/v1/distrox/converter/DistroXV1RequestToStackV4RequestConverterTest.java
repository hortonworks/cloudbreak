package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.converter.v4.stacks.TelemetryConverter;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.tags.TagsV1Request;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@ExtendWith(MockitoExtension.class)
class DistroXV1RequestToStackV4RequestConverterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private DistroXAuthenticationToStaAuthenticationConverter authenticationConverter;

    @Mock
    private DistroXImageToImageSettingsConverter imageConverter;

    @Mock
    private DistroXClusterToClusterConverter clusterConverter;

    @Mock
    private InstanceGroupV1ToInstanceGroupV4Converter instanceGroupConverter;

    @Mock
    private NetworkV1ToNetworkV4Converter networkConverter;

    @Mock
    private DistroXParameterConverter stackParameterConverter;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private SdxConverter sdxConverter;

    @Mock
    private CredentialResponse credentialResponse;

    @Mock
    private TelemetryConverter telemetryConverter;

    @Mock
    private SecurityV1RequestToSecurityV4RequestConverter securityV1RequestToSecurityV4RequestConverter;

    @Mock
    private SdxClientService sdxClientService;

    @Mock
    private DistroXDatabaseRequestToStackDatabaseRequestConverter databaseRequestConverter;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ProviderPreferencesService providerPreferencesService;

    @InjectMocks
    private DistroXV1RequestToStackV4RequestConverter underTest;

    @Test
    void convert() {
        when(environmentClientService.getByName(anyString())).thenReturn(createAwsEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());
        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.FALSE);

        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        databaseRequest.setDatabaseEngineVersion("13");
        source.setExternalDatabase(databaseRequest);
        StackV4Request convert = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));
        assertThat(convert.getExternalDatabase()).isNotNull();
        assertThat(convert.getExternalDatabase().getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);
        assertThat(convert.getExternalDatabase().getDatabaseEngineVersion()).isEqualTo("13");

        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.TRUE);
        convert = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));
        assertThat(convert.getVariant()).isEqualTo("AWS_NATIVE");
    }

    @Test
    void testConvertWithDisabledAwsNativeEntitlementAndRuntimeVersionThatMakesAwsNativeVariantDefault() {
        DetailedEnvironmentResponse environmentResponse = createAwsEnvironment();
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentResponse.getCredential().getGovCloud()).thenReturn(false);
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());
        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.FALSE);
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setRuntime("7.3.1");
        when(sdxClientService.getByEnvironmentCrn(anyString())).thenReturn(List.of(sdxClusterResponse));

        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        databaseRequest.setDatabaseEngineVersion("13");
        source.setExternalDatabase(databaseRequest);
        StackV4Request convert = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));
        assertThat(convert.getExternalDatabase()).isNotNull();
        assertThat(convert.getExternalDatabase().getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);
        assertThat(convert.getExternalDatabase().getDatabaseEngineVersion()).isEqualTo("13");

        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.FALSE);
        convert = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));
        assertThat(convert.getVariant()).isEqualTo("AWS_NATIVE");
    }

    @Test
    void testConvertWithAwsNativeEntitlementAndRuntimeVersionButVariantSetByUser() {
        DetailedEnvironmentResponse environmentResponse = createAwsEnvironment();
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentResponse.getCredential().getGovCloud()).thenReturn(false);
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());
        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.TRUE);
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setRuntime("7.3.1");
        when(sdxClientService.getByEnvironmentCrn(anyString())).thenReturn(List.of(sdxClusterResponse));
        when(providerPreferencesService.cloudConstantByName(any())).thenReturn(Optional.of(new AwsMockConstants()));

        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        source.setVariant("AWS");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        databaseRequest.setDatabaseEngineVersion("13");
        source.setExternalDatabase(databaseRequest);

        StackV4Request convert = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));

        assertThat(convert.getExternalDatabase()).isNotNull();
        assertThat(convert.getExternalDatabase().getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);
        assertThat(convert.getExternalDatabase().getDatabaseEngineVersion()).isEqualTo("13");
        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.FALSE);
        convert = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));
        assertThat(convert.getVariant()).isEqualTo("AWS");
    }

    @Test
    void testConvertWithAwsNativeVariantIsWrong() {
        DetailedEnvironmentResponse environmentResponse = createAwsEnvironment();
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentResponse.getCredential().getGovCloud()).thenReturn(false);
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());
        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.TRUE);
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setRuntime("7.3.1");
        when(sdxClientService.getByEnvironmentCrn(anyString())).thenReturn(List.of(sdxClusterResponse));
        when(providerPreferencesService.cloudConstantByName(any())).thenReturn(Optional.of(new AwsMockConstants()));

        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        source.setVariant("AWS_NOT_VARIANT");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        databaseRequest.setDatabaseEngineVersion("13");
        source.setExternalDatabase(databaseRequest);
        Exception exception = assertThrows(BadRequestException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));
        });
        assertThat(exception.getMessage()).isEqualTo("Variant AWS_NOT_VARIANT is not supported for cloud platform AWS. " +
                "Supported Variants are: AWS, AWS_NATIVE_GOV, AWS_NATIVE");
    }

    @Test
    void testConvertWithDisabledAwsNativeEntitlementAndRuntimeVersionForAwsGov() {
        DetailedEnvironmentResponse environmentResponse = createAwsEnvironment();
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentResponse.getCredential().getGovCloud()).thenReturn(true);
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());
        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.FALSE);
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setRuntime("7.3.1");
        when(sdxClientService.getByEnvironmentCrn(anyString())).thenReturn(List.of(sdxClusterResponse));

        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        source.setVariant("AWS_NATIVE_GOV");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        databaseRequest.setDatabaseEngineVersion("13");
        source.setExternalDatabase(databaseRequest);
        StackV4Request convert = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));
        assertThat(convert.getExternalDatabase()).isNotNull();
        assertThat(convert.getExternalDatabase().getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);
        assertThat(convert.getExternalDatabase().getDatabaseEngineVersion()).isEqualTo("13");

        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.FALSE);
        convert = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));
        assertThat(convert.getVariant()).isEqualTo("AWS_NATIVE_GOV");
    }

    @Test
    void convertAvailabilityZoneComesFromEnv() {
        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);

        DetailedEnvironmentResponse env = createAwsEnvironment();
        env.getNetwork().setSubnetMetas(Map.of("SubnetMeta", createCloudSubnet()));

        when(environmentClientService.getByName(source.getEnvironmentName())).thenReturn(env);
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());
        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.FALSE);

        StackV4Request convert = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));

        assertThat(convert.getExternalDatabase()).isNotNull();
        assertThat(convert.getExternalDatabase().getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);

        when(entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(anyString())).thenReturn(Boolean.TRUE);
        convert = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));
        assertThat(convert.getVariant()).isEqualTo("AWS_NATIVE");
    }

    @ParameterizedTest
    @EnumSource(EnvironmentStatus.class)
    void testStackV4RequestToDistroXV1RequestRegardlessOfTheStateOfTheEnvironment(EnvironmentStatus status) {
        StackV4Request source = new StackV4Request();
        source.setName("SomeStack");
        source.setEnvironmentCrn("SomeEnvCrn");

        DetailedEnvironmentResponse env = createAwsEnvironment();
        env.setCrn(source.getEnvironmentCrn());
        env.setEnvironmentStatus(status);

        when(environmentClientService.getByCrn(source.getEnvironmentCrn())).thenReturn(env);

        DistroXV1Request result = assertDoesNotThrow(() -> underTest.convert(source));

        verify(environmentClientService, times(1)).getByCrn(any());
        verify(environmentClientService, times(1)).getByCrn(source.getEnvironmentCrn());

        assertEquals(env.getName(), result.getEnvironmentName());
    }

    @Test
    void testStackV4RequestToDistroXV1RequestWhenEnvironmentCrnIsNullThenNoCallHappensTowardsTheEnvironmentClientServiceAndNoEnvNameSetHappens() {
        StackV4Request source = new StackV4Request();
        source.setName("SomeStack");
        source.setEnvironmentCrn(null);

        DistroXV1Request result = assertDoesNotThrow(() -> underTest.convert(source));

        verify(environmentClientService, never()).getByCrn(any());

        assertNull(result.getEnvironmentName());
    }

    @Test
    void convertStackRequest() {
        when(databaseRequestConverter.convert(any(DatabaseRequest.class))).thenReturn(createDistroXDatabaseRequest());

        StackV4Request source = new StackV4Request();
        source.setName("stackname");
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);
        DistroXV1Request convert = underTest.convert(source);
        assertThat(convert.getExternalDatabase()).isNotNull();
        assertThat(convert.getExternalDatabase().getAvailabilityType()).isEqualTo(DistroXDatabaseAvailabilityType.HA);
    }

    @Test
    void testWhenTagsProvidedTheyWillBePassedForStackConversion() {
        when(databaseRequestConverter.convert(any(DatabaseRequest.class))).thenReturn(createDistroXDatabaseRequest());

        StackV4Request source = new StackV4Request();
        source.setName("stackname");
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);
        TagsV4Request tags = createTagsV4Request();
        source.setTags(tags);

        DistroXV1Request result = underTest.convert(source);
        assertThat(result.getExternalDatabase()).isNotNull();
        assertThat(result.getExternalDatabase().getAvailabilityType()).isEqualTo(DistroXDatabaseAvailabilityType.HA);
        checkTagsV4WithV1(tags, result.getTags());
    }

    @Test
    public void testGetNetworkWhenNetworkIsNullInInstanceGroups() {
        NetworkV1Request networkRequest = new NetworkV1Request();
        AwsNetworkV1Parameters aws = new AwsNetworkV1Parameters();
        aws.setSubnetId("sub1");
        networkRequest.setAws(aws);

        Set<InstanceGroupV1Request> instanceGroups = new HashSet<>();
        instanceGroups.add(new InstanceGroupV1Request());
        instanceGroups.add(new InstanceGroupV1Request());

        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("ANY");

        NetworkV4Request networkV4Request = new NetworkV4Request();
        ArgumentCaptor<Pair<NetworkV1Request, DetailedEnvironmentResponse>> networkConverterCaptor = ArgumentCaptor.forClass(Pair.class);

        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(networkV4Request);

        NetworkV4Request actual = underTest.getNetwork(networkRequest, environment, instanceGroups);

        verify(networkConverter).convertToNetworkV4Request(networkConverterCaptor.capture());

        assertEquals(networkV4Request, actual);
        Pair<NetworkV1Request, DetailedEnvironmentResponse> captured = networkConverterCaptor.getValue();
        assertEquals("sub1", captured.getKey().getAws().getSubnetId());
    }

    @Test
    public void testGetNetworkWhenAwsIsNullInInstanceGroupsNetwork() {
        NetworkV1Request networkRequest = new NetworkV1Request();
        AwsNetworkV1Parameters aws = new AwsNetworkV1Parameters();
        aws.setSubnetId("sub1");
        networkRequest.setAws(aws);

        Set<InstanceGroupV1Request> instanceGroups = new HashSet<>();
        InstanceGroupV1Request ig1 = new InstanceGroupV1Request();
        ig1.setNetwork(new InstanceGroupNetworkV1Request());
        InstanceGroupV1Request ig2 = new InstanceGroupV1Request();
        ig2.setNetwork(new InstanceGroupNetworkV1Request());
        instanceGroups.add(ig1);
        instanceGroups.add(ig2);

        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("ANY");

        NetworkV4Request networkV4Request = new NetworkV4Request();
        ArgumentCaptor<Pair<NetworkV1Request, DetailedEnvironmentResponse>> networkConverterCaptor = ArgumentCaptor.forClass(Pair.class);

        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(networkV4Request);

        NetworkV4Request actual = underTest.getNetwork(networkRequest, environment, instanceGroups);

        verify(networkConverter).convertToNetworkV4Request(networkConverterCaptor.capture());

        assertEquals(networkV4Request, actual);
        Pair<NetworkV1Request, DetailedEnvironmentResponse> captured = networkConverterCaptor.getValue();
        assertEquals("sub1", captured.getKey().getAws().getSubnetId());
    }

    @Test
    public void testGetNetworkWhenSubnetIdsIsNullInInstanceGroupsNetworkAws() {
        NetworkV1Request networkRequest = new NetworkV1Request();
        AwsNetworkV1Parameters aws = new AwsNetworkV1Parameters();
        aws.setSubnetId("sub1");
        networkRequest.setAws(aws);

        Set<InstanceGroupV1Request> instanceGroups = new HashSet<>();
        InstanceGroupV1Request ig1 = new InstanceGroupV1Request();
        InstanceGroupNetworkV1Request network = new InstanceGroupNetworkV1Request();
        network.setAws(new InstanceGroupAwsNetworkV1Parameters());
        ig1.setNetwork(network);
        InstanceGroupV1Request ig2 = new InstanceGroupV1Request();
        InstanceGroupNetworkV1Request network1 = new InstanceGroupNetworkV1Request();
        network1.setAws(new InstanceGroupAwsNetworkV1Parameters());
        ig2.setNetwork(network1);
        instanceGroups.add(ig1);
        instanceGroups.add(ig2);

        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("ANY");

        NetworkV4Request networkV4Request = new NetworkV4Request();
        ArgumentCaptor<Pair<NetworkV1Request, DetailedEnvironmentResponse>> networkConverterCaptor = ArgumentCaptor.forClass(Pair.class);

        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(networkV4Request);

        NetworkV4Request actual = underTest.getNetwork(networkRequest, environment, instanceGroups);

        verify(networkConverter).convertToNetworkV4Request(networkConverterCaptor.capture());

        assertEquals(networkV4Request, actual);
        Pair<NetworkV1Request, DetailedEnvironmentResponse> captured = networkConverterCaptor.getValue();
        assertEquals("sub1", captured.getKey().getAws().getSubnetId());
    }

    @Test
    public void testGetNetworkWhenHasOneSubnetIdInInstanceGroupsNetworkAws() {
        NetworkV1Request networkRequest = new NetworkV1Request();
        AwsNetworkV1Parameters aws = new AwsNetworkV1Parameters();
        aws.setSubnetId("sub1");
        networkRequest.setAws(aws);

        Set<InstanceGroupV1Request> instanceGroups = new HashSet<>();
        InstanceGroupV1Request ig1 = new InstanceGroupV1Request();
        InstanceGroupNetworkV1Request network = new InstanceGroupNetworkV1Request();
        InstanceGroupAwsNetworkV1Parameters awsNetworkV1Parameters = new InstanceGroupAwsNetworkV1Parameters();
        awsNetworkV1Parameters.setSubnetIds(List.of("subnet1"));
        network.setAws(awsNetworkV1Parameters);
        ig1.setNetwork(network);
        InstanceGroupV1Request ig2 = new InstanceGroupV1Request();
        InstanceGroupNetworkV1Request network1 = new InstanceGroupNetworkV1Request();
        InstanceGroupAwsNetworkV1Parameters awsNetworkV1Parameters1 = new InstanceGroupAwsNetworkV1Parameters();
        network1.setAws(awsNetworkV1Parameters1);
        ig2.setNetwork(network1);
        instanceGroups.add(ig1);
        instanceGroups.add(ig2);

        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("ANY");

        NetworkV4Request networkV4Request = new NetworkV4Request();
        ArgumentCaptor<Pair<NetworkV1Request, DetailedEnvironmentResponse>> networkConverterCaptor = ArgumentCaptor.forClass(Pair.class);

        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(networkV4Request);

        NetworkV4Request actual = underTest.getNetwork(networkRequest, environment, instanceGroups);

        verify(networkConverter).convertToNetworkV4Request(networkConverterCaptor.capture());

        assertEquals(networkV4Request, actual);
        Pair<NetworkV1Request, DetailedEnvironmentResponse> captured = networkConverterCaptor.getValue();
        assertEquals("subnet1", captured.getKey().getAws().getSubnetId());
    }

    @Test
    public void testGetNetworkWhenHasSameSubnetIdInInstanceGroupsNetworkAws() {
        NetworkV1Request networkRequest = new NetworkV1Request();
        AwsNetworkV1Parameters aws = new AwsNetworkV1Parameters();
        aws.setSubnetId("sub1");
        networkRequest.setAws(aws);

        Set<InstanceGroupV1Request> instanceGroups = new HashSet<>();
        InstanceGroupV1Request ig1 = new InstanceGroupV1Request();
        InstanceGroupNetworkV1Request network = new InstanceGroupNetworkV1Request();
        InstanceGroupAwsNetworkV1Parameters awsNetworkV1Parameters = new InstanceGroupAwsNetworkV1Parameters();
        awsNetworkV1Parameters.setSubnetIds(List.of("subnet1"));
        network.setAws(awsNetworkV1Parameters);
        ig1.setNetwork(network);
        InstanceGroupV1Request ig2 = new InstanceGroupV1Request();
        InstanceGroupNetworkV1Request network1 = new InstanceGroupNetworkV1Request();
        InstanceGroupAwsNetworkV1Parameters awsNetworkV1Parameters1 = new InstanceGroupAwsNetworkV1Parameters();
        awsNetworkV1Parameters1.setSubnetIds(List.of("subnet1"));
        network1.setAws(awsNetworkV1Parameters1);
        ig2.setNetwork(network1);
        instanceGroups.add(ig1);
        instanceGroups.add(ig2);

        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("ANY");

        NetworkV4Request networkV4Request = new NetworkV4Request();
        ArgumentCaptor<Pair<NetworkV1Request, DetailedEnvironmentResponse>> networkConverterCaptor = ArgumentCaptor.forClass(Pair.class);

        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(networkV4Request);

        NetworkV4Request actual = underTest.getNetwork(networkRequest, environment, instanceGroups);

        verify(networkConverter).convertToNetworkV4Request(networkConverterCaptor.capture());

        assertEquals(networkV4Request, actual);
        Pair<NetworkV1Request, DetailedEnvironmentResponse> captured = networkConverterCaptor.getValue();
        assertEquals("subnet1", captured.getKey().getAws().getSubnetId());
    }

    @Test
    public void testGetNetworkWhenHasDiffSubnetIdInInstanceGroupsNetworkAws() {
        NetworkV1Request networkRequest = new NetworkV1Request();
        AwsNetworkV1Parameters aws = new AwsNetworkV1Parameters();
        aws.setSubnetId("sub1");
        networkRequest.setAws(aws);

        Set<InstanceGroupV1Request> instanceGroups = new HashSet<>();
        InstanceGroupV1Request ig1 = new InstanceGroupV1Request();
        InstanceGroupNetworkV1Request network = new InstanceGroupNetworkV1Request();
        InstanceGroupAwsNetworkV1Parameters awsNetworkV1Parameters = new InstanceGroupAwsNetworkV1Parameters();
        awsNetworkV1Parameters.setSubnetIds(List.of("subnet1"));
        network.setAws(awsNetworkV1Parameters);
        ig1.setNetwork(network);
        InstanceGroupV1Request ig2 = new InstanceGroupV1Request();
        InstanceGroupNetworkV1Request network1 = new InstanceGroupNetworkV1Request();
        InstanceGroupAwsNetworkV1Parameters awsNetworkV1Parameters1 = new InstanceGroupAwsNetworkV1Parameters();
        awsNetworkV1Parameters1.setSubnetIds(List.of("subnet2"));
        network1.setAws(awsNetworkV1Parameters1);
        ig2.setNetwork(network1);
        instanceGroups.add(ig1);
        instanceGroups.add(ig2);

        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("ANY");

        NetworkV4Request networkV4Request = new NetworkV4Request();
        ArgumentCaptor<Pair<NetworkV1Request, DetailedEnvironmentResponse>> networkConverterCaptor = ArgumentCaptor.forClass(Pair.class);

        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(networkV4Request);

        NetworkV4Request actual = underTest.getNetwork(networkRequest, environment, instanceGroups);

        verify(networkConverter).convertToNetworkV4Request(networkConverterCaptor.capture());

        assertEquals(networkV4Request, actual);
        Pair<NetworkV1Request, DetailedEnvironmentResponse> captured = networkConverterCaptor.getValue();
        assertEquals("sub1", captured.getKey().getAws().getSubnetId());
    }

    @Test
    void testJavaVersionInStackRequestPassedToDistroXRequest() {
        StackV4Request source = new StackV4Request();
        source.setJavaVersion(11);

        DistroXV1Request result = underTest.convert(source);

        assertThat(result.getJavaVersion()).isEqualTo(11);
    }

    @Test
    void testJavaVersionInDistroXRequestPassedToStackRequest() {
        when(environmentClientService.getByName(anyString())).thenReturn(createAwsEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());

        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("env");
        source.setJavaVersion(11);

        StackV4Request result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));

        assertThat(result.getJavaVersion()).isEqualTo(11);
    }

    @Test
    void testArchitecture() {
        when(environmentClientService.getByName(anyString())).thenReturn(createAwsEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());

        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("env");
        source.setArchitecture(Architecture.ARM64.getName());

        StackV4Request result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));

        assertThat(result.getArchitecture()).isEqualTo(Architecture.ARM64.getName());
    }

    private void checkTagsV4WithV1(TagsV4Request input, TagsV1Request result) {
        assertEquals(input.getUserDefined().size(), result.getUserDefined().size());
        assertEquals(input.getApplication().size(), result.getApplication().size());
        assertEquals(input.getDefaults().size(), result.getDefaults().size());

        checkTagMap(input.getUserDefined(), result.getUserDefined());
        checkTagMap(input.getApplication(), result.getApplication());
        checkTagMap(input.getDefaults(), result.getDefaults());
    }

    private void checkTagMap(Map<String, String> input, Map<String, String> result) {
        input.forEach((k, v) -> {
            assertTrue(result.containsKey(k));
            assertEquals(v, result.get(k));
        });
    }

    private DatabaseRequest createDatabaseRequest() {
        DatabaseRequest request = new DatabaseRequest();
        request.setAvailabilityType(DatabaseAvailabilityType.HA);
        request.setDatabaseEngineVersion("13");
        return request;
    }

    private DistroXDatabaseRequest createDistroXDatabaseRequest() {
        DistroXDatabaseRequest request = new DistroXDatabaseRequest();
        request.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        return request;
    }

    private NetworkV4Request createAwsNetworkV4Request() {
        NetworkV4Request network = new NetworkV4Request();
        network.setAws(createAwsNetworkV4Parameters());
        return network;
    }

    private TagsV4Request createTagsV4Request() {
        TagsV4Request r = new TagsV4Request();
        r.setUserDefined(Map.of("apple", "tree"));
        r.setDefaults(Map.of("default", "fruit"));
        r.setApplication(Map.of("peach", "tree"));
        return r;
    }

    private AwsNetworkV4Parameters createAwsNetworkV4Parameters() {
        AwsNetworkV4Parameters awsNetwork = new AwsNetworkV4Parameters();
        awsNetwork.setSubnetId("mysubnetid");
        awsNetwork.setVpcId("myvpc");
        return awsNetwork;
    }

    private DetailedEnvironmentResponse createAwsEnvironment() {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        env.setCloudPlatform("AWS");
        env.setCrn("environmentCrn");
        env.setNetwork(createAwsNetwork());
        env.setName("SomeAwesomeEnv");
        env.setCredential(credentialResponse);
        env.setRegions(createCompactRegionResponse());
        return env;
    }

    private CompactRegionResponse createCompactRegionResponse() {
        CompactRegionResponse regionResponse = new CompactRegionResponse();
        regionResponse.setNames(List.of("myregion"));
        return regionResponse;
    }

    private EnvironmentNetworkResponse createAwsNetwork() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setAws(createAwsNetworkParams());
        network.setSubnetIds(Set.of("mysubnetid"));
        return network;
    }

    private CloudSubnet createCloudSubnet() {
        CloudSubnet cs = new CloudSubnet();
        cs.setType(SubnetType.PUBLIC);
        cs.setName("someCloudSubnet");
        cs.setId("123");
        cs.setIgwAvailable(true);
        cs.setMapPublicIpOnLaunch(false);
        cs.setPrivateSubnet(false);
        cs.setCidr("0.0.0.0/0");
        return cs;
    }

    private EnvironmentNetworkAwsParams createAwsNetworkParams() {
        EnvironmentNetworkAwsParams awsNetwork = new EnvironmentNetworkAwsParams();
        awsNetwork.setVpcId("myvpc");
        return awsNetwork;
    }

    public class AwsMockConstants implements CloudConstant {

        @Override
        public String[] variants() {
            return AwsConstants.VARIANTS;
        }

        @Override
        public Platform platform() {
            return AwsConstants.AWS_PLATFORM;
        }

        @Override
        public Variant variant() {
            return AwsConstants.AWS_DEFAULT_VARIANT;
        }
    }

}