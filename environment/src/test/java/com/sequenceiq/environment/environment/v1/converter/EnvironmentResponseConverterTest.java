package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialViewResponse;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentBaseResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyViewResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialViewConverter;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.dto.AzureParametersDto;
import com.sequenceiq.environment.parameters.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyResponseConverter;

@ExtendWith(SpringExtension.class)
public class EnvironmentResponseConverterTest {

    private static final String REGION = "us-west";

    @InjectMocks
    private EnvironmentResponseConverter underTest;

    @Mock
    private CredentialToCredentialV1ResponseConverter credentialConverter;

    @Mock
    private RegionConverter regionConverter;

    @Mock
    private CredentialViewConverter credentialViewConverter;

    @Mock
    private ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter;

    @Mock
    private FreeIpaConverter freeIpaConverter;

    @Mock
    private TelemetryApiConverter telemetryApiConverter;

    @Mock
    private NetworkDtoToResponseConverter networkDtoToResponseConverter;

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE"})
    void testDtoToDetailedResponse(CloudPlatform cloudPlatform) {
        EnvironmentDto environment = createEnvironmentDto(cloudPlatform);
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        FreeIpaResponse freeIpaResponse = mock(FreeIpaResponse.class);
        CompactRegionResponse compactRegionResponse = mock(CompactRegionResponse.class);
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        ProxyResponse proxyResponse = mock(ProxyResponse.class);
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);

        when(credentialConverter.convert(environment.getCredential())).thenReturn(credentialResponse);
        when(freeIpaConverter.convert(environment.getFreeIpaCreation())).thenReturn(freeIpaResponse);
        when(regionConverter.convertRegions(environment.getRegions())).thenReturn(compactRegionResponse);
        when(telemetryApiConverter.convert(environment.getTelemetry())).thenReturn(telemetryResponse);
        when(proxyConfigToProxyResponseConverter.convert(environment.getProxyConfig())).thenReturn(proxyResponse);
        when(networkDtoToResponseConverter.convert(environment.getNetwork(), environment.getExperimentalFeatures().getTunnel()))
                .thenReturn(environmentNetworkResponse);

        DetailedEnvironmentResponse actual = underTest.dtoToDetailedResponse(environment);

        assertEquals(environment.getResourceCrn(), actual.getCrn());
        assertEquals(environment.getName(), actual.getName());
        assertEquals(environment.getDescription(), actual.getDescription());
        assertEquals(environment.getCloudPlatform(), actual.getCloudPlatform());
        assertEquals(credentialResponse, actual.getCredential());
        assertEquals(environment.getStatus().getResponseStatus(), actual.getEnvironmentStatus());
        assertLocation(environment.getLocation(), actual.getLocation());
        assertTrue(actual.getCreateFreeIpa());
        assertEquals(freeIpaResponse, actual.getFreeIpa());
        assertEquals(compactRegionResponse, actual.getRegions());
        assertEquals(environment.getCreator(), actual.getCreator());
        assertAuthentication(environment.getAuthentication(), actual.getAuthentication());
        assertEquals(environment.getStatusReason(), actual.getStatusReason());
        assertEquals(environment.getCreated(), actual.getCreated());
        assertEquals(environment.getTags().getUserDefinedTags(), actual.getTags().getUserDefined());
        assertEquals(environment.getTags().getDefaultTags(), actual.getTags().getDefaults());
        assertEquals(telemetryResponse, actual.getTelemetry());
        assertEquals(environment.getExperimentalFeatures().getTunnel(), actual.getTunnel());
        assertEquals(environment.getExperimentalFeatures().getIdBrokerMappingSource(), actual.getIdBrokerMappingSource());
        assertEquals(environment.getExperimentalFeatures().getCloudStorageValidation(), actual.getCloudStorageValidation());
        assertEquals(environment.getAdminGroupName(), actual.getAdminGroupName());
        assertParameters(environment, actual, cloudPlatform);
        assertEquals(environment.getParentEnvironmentCrn(), actual.getParentEnvironmentCrn());
        assertEquals(environment.getParentEnvironmentName(), actual.getParentEnvironmentName());
        assertEquals(environment.getParentEnvironmentCloudPlatform(), actual.getParentEnvironmentCloudPlatform());
        assertEquals(proxyResponse, actual.getProxyConfig());
        assertEquals(environmentNetworkResponse, actual.getNetwork());
        assertSecurityAccess(environment.getSecurityAccess(), actual.getSecurityAccess());

        verify(credentialConverter).convert(environment.getCredential());
        verify(freeIpaConverter).convert(environment.getFreeIpaCreation());
        verify(regionConverter).convertRegions(environment.getRegions());
        verify(telemetryApiConverter).convert(environment.getTelemetry());
        verify(proxyConfigToProxyResponseConverter).convert(environment.getProxyConfig());
        verify(networkDtoToResponseConverter).convert(environment.getNetwork(), environment.getExperimentalFeatures().getTunnel());
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE"})
    void testDtoToSimpleResponse(CloudPlatform cloudPlatform) {
        EnvironmentDto environment = createEnvironmentDto(cloudPlatform);
        CredentialViewResponse credentialResponse = mock(CredentialViewResponse.class);
        FreeIpaResponse freeIpaResponse = mock(FreeIpaResponse.class);
        CompactRegionResponse compactRegionResponse = mock(CompactRegionResponse.class);
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        ProxyViewResponse proxyResponse = mock(ProxyViewResponse.class);
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);

        when(credentialViewConverter.convert(environment.getCredentialView())).thenReturn(credentialResponse);
        when(freeIpaConverter.convert(environment.getFreeIpaCreation())).thenReturn(freeIpaResponse);
        when(regionConverter.convertRegions(environment.getRegions())).thenReturn(compactRegionResponse);
        when(telemetryApiConverter.convert(environment.getTelemetry())).thenReturn(telemetryResponse);
        when(proxyConfigToProxyResponseConverter.convertToView(environment.getProxyConfig())).thenReturn(proxyResponse);
        when(networkDtoToResponseConverter.convert(environment.getNetwork(), environment.getExperimentalFeatures().getTunnel()))
                .thenReturn(environmentNetworkResponse);

        SimpleEnvironmentResponse actual = underTest.dtoToSimpleResponse(environment);

        assertEquals(environment.getResourceCrn(), actual.getCrn());
        assertEquals(environment.getName(), actual.getName());
        assertEquals(environment.getDescription(), actual.getDescription());
        assertEquals(environment.getCloudPlatform(), actual.getCloudPlatform());
        assertEquals(credentialResponse, actual.getCredential());
        assertEquals(environment.getStatus().getResponseStatus(), actual.getEnvironmentStatus());
        assertEquals(environment.getCreator(), actual.getCreator());
        assertLocation(environment.getLocation(), actual.getLocation());
        assertTrue(actual.getCreateFreeIpa());
        assertEquals(freeIpaResponse, actual.getFreeIpa());
        assertEquals(environment.getStatusReason(), actual.getStatusReason());
        assertEquals(environment.getCreated(), actual.getCreated());
        assertEquals(environment.getExperimentalFeatures().getTunnel(), actual.getTunnel());
        assertEquals(environment.getAdminGroupName(), actual.getAdminGroupName());
        assertEquals(environment.getTags().getUserDefinedTags(), actual.getTags().getUserDefined());
        assertEquals(environment.getTags().getDefaultTags(), actual.getTags().getDefaults());
        assertEquals(telemetryResponse, actual.getTelemetry());
        assertEquals(compactRegionResponse, actual.getRegions());
        assertParameters(environment, actual, cloudPlatform);
        assertEquals(environment.getParentEnvironmentName(), actual.getParentEnvironmentName());
        assertEquals(proxyResponse, actual.getProxyConfig());
        assertEquals(environmentNetworkResponse, actual.getNetwork());

        verify(credentialViewConverter).convert(environment.getCredentialView());
        verify(freeIpaConverter).convert(environment.getFreeIpaCreation());
        verify(regionConverter).convertRegions(environment.getRegions());
        verify(telemetryApiConverter).convert(environment.getTelemetry());
        verify(proxyConfigToProxyResponseConverter).convertToView(environment.getProxyConfig());
        verify(networkDtoToResponseConverter).convert(environment.getNetwork(), environment.getExperimentalFeatures().getTunnel());
    }

    private void assertParameters(EnvironmentDto environment, EnvironmentBaseResponse actual, CloudPlatform cloudPlatform) {
        if (AWS.equals(cloudPlatform)) {
            assertEquals(environment.getParameters().getAwsParametersDto().getS3GuardTableName(), actual.getAws().getS3guard().getDynamoDbTableName());
        } else {
            assertAzureParameters(environment.getParameters().getAzureParametersDto(), actual.getAzure());
        }
    }

    private void assertLocation(LocationDto location, LocationResponse actual) {
        assertEquals(location.getName(), actual.getName());
        assertEquals(location.getDisplayName(), actual.getDisplayName());
        assertEquals(location.getLatitude(), actual.getLatitude());
        assertEquals(location.getLongitude(), actual.getLongitude());
    }

    private void assertAuthentication(AuthenticationDto authentication, EnvironmentAuthenticationResponse actual) {
        assertEquals(authentication.getPublicKey(), actual.getPublicKey());
        assertEquals(authentication.getPublicKeyId(), actual.getPublicKeyId());
        assertEquals(authentication.getLoginUserName(), actual.getLoginUserName());
    }

    private void assertSecurityAccess(SecurityAccessDto securityAccess, SecurityAccessResponse actual) {
        assertEquals(securityAccess.getCidr(), actual.getCidr());
        assertEquals(securityAccess.getDefaultSecurityGroupId(), actual.getDefaultSecurityGroupId());
        assertEquals(securityAccess.getSecurityGroupIdForKnox(), actual.getSecurityGroupIdForKnox());
    }

    private void assertAzureParameters(AzureParametersDto azureParametersDto, AzureEnvironmentParameters azureEnvironmentParameters) {
        assertNotNull(azureEnvironmentParameters);
        assertEquals(azureParametersDto.getAzureResourceGroupDto().getName(), azureEnvironmentParameters.getResourceGroup().getName());
    }

    private EnvironmentDto createEnvironmentDto(CloudPlatform cloudPlatform) {
        return EnvironmentDto.builder()
                .withResourceCrn("resource-crn")
                .withName("my-env")
                .withDescription("Test environment.")
                .withCloudPlatform("AWS")
                .withCredential(new Credential())
                .withEnvironmentStatus(EnvironmentStatus.AVAILABLE)
                .withLocationDto(createLocationDto())
                .withFreeIpaCreation(createFreeIpaCreationDto())
                .withRegions(Set.of(createRegion()))
                .withCreator("cloudbreak-user")
                .withAuthentication(createAuthenticationDto())
                .withStatusReason("status reason")
                .withCreated(123L)
                .withTags(createTags())
                .withTelemetry(new EnvironmentTelemetry())
                .withExperimentalFeatures(createExperimentalFeatures())
                .withAdminGroupName("admin group")
                .withParameters(createParametersDto(cloudPlatform))
                .withParentEnvironmentCrn("environment crn")
                .withParentEnvironmentName("parent-env")
                .withParentEnvironmentCloudPlatform("AWS")
                .withProxyConfig(new ProxyConfig())
                .withNetwork(NetworkDto.builder().build())
                .withSecurityAccess(createSecurityAccess())
                .build();
    }

    private SecurityAccessDto createSecurityAccess() {
        return SecurityAccessDto.builder()
                .withCidr("1.1.1.1/16")
                .withDefaultSecurityGroupId("group-id")
                .withSecurityGroupIdForKnox("knox-group")
                .build();
    }

    private ParametersDto createParametersDto(CloudPlatform cloudPlatform) {
        if (AWS.equals(cloudPlatform)) {
            return createAwsParameters();
        } else if (AZURE.equals(cloudPlatform)) {
            return createAzureParameters();
        } else {
            throw new RuntimeException("CloudPlatform " + cloudPlatform + " is not supported.");
        }
    }

    private ParametersDto createAzureParameters() {
        return ParametersDto.builder()
                .withAwsParameters(AwsParametersDto.builder()
                        .withDynamoDbTableName("my-table")
                        .build())
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(
                                AzureResourceGroupDto.builder()
                                        .withName("my-resource-group-name")
                                        .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                        .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                        .build())
                        .build())
                .build();
    }

    private ParametersDto createAwsParameters() {
        return ParametersDto.builder()
                .withAwsParameters(AwsParametersDto.builder()
                        .withDynamoDbTableName("my-table")
                        .build())
                .build();
    }

    private ExperimentalFeatures createExperimentalFeatures() {
        return ExperimentalFeatures.builder()
                .withTunnel(Tunnel.CCM)
                .withIdBrokerMappingSource(IdBrokerMappingSource.IDBMMS)
                .withCloudStorageValidation(CloudStorageValidation.ENABLED)
                .build();
    }

    private EnvironmentTags createTags() {
        return new EnvironmentTags(Map.of("user", "cloudbreak"), Map.of("department", "finance"));
    }

    private AuthenticationDto createAuthenticationDto() {
        return AuthenticationDto.builder()
                .withLoginUserName("cloudbreak")
                .withPublicKey("public key")
                .withPublicKeyId("public key id")
                .build();
    }

    private Region createRegion() {
        Region region = new Region();
        region.setName(REGION);
        return region;
    }

    private FreeIpaCreationDto createFreeIpaCreationDto() {
        return FreeIpaCreationDto.builder()
                .withCreate(true)
                .build();
    }

    private LocationDto createLocationDto() {
        return LocationDto.builder()
                .withName(REGION)
                .withDisplayName("US West")
                .withLatitude(123.4)
                .withLongitude(567.8)
                .build();
    }

}