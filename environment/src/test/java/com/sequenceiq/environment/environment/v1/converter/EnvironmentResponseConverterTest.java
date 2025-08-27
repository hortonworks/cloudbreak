package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialViewResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DataServicesResponse;
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
import com.sequenceiq.environment.credential.domain.CredentialView;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialViewConverter;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileToEncryptionProfileResponseConverter;
import com.sequenceiq.environment.environment.EnvironmentDeletionType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoBase;
import com.sequenceiq.environment.environment.dto.EnvironmentViewDto;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupCreation;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.domain.ProxyConfigView;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyResponseConverter;

@ExtendWith(SpringExtension.class)
class EnvironmentResponseConverterTest {

    private static final String REGION = "us-west";

    private static final int FREE_IPA_INSTANCE_COUNT_BY_GROUP = 2;

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
    private BackupConverter backupConverter;

    @Mock
    private NetworkDtoToResponseConverter networkDtoToResponseConverter;

    @Mock
    private DataServicesConverter dataServicesConverter;

    @Mock
    private EncryptionProfileToEncryptionProfileResponseConverter encryptionProfileResponseConverter;

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE", "GCP"})
    void testDtoToDetailedResponse(CloudPlatform cloudPlatform) {
        EnvironmentDto environment = createEnvironmentDtoBuilder(cloudPlatform).build();
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        FreeIpaResponse freeIpaResponse = mock(FreeIpaResponse.class);
        CompactRegionResponse compactRegionResponse = mock(CompactRegionResponse.class);
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        BackupResponse backupResponse = mock(BackupResponse.class);
        ProxyResponse proxyResponse = mock(ProxyResponse.class);
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        DataServicesResponse dataServicesResponse = mock(DataServicesResponse.class);
        String encryptionProfileName = "epName";

        when(credentialConverter.convert(environment.getCredential())).thenReturn(credentialResponse);
        when(freeIpaConverter.convert(environment.getFreeIpaCreation())).thenReturn(freeIpaResponse);
        when(regionConverter.convertRegions(environment.getRegions())).thenReturn(compactRegionResponse);
        when(telemetryApiConverter.convert(eq(environment.getTelemetry()), any())).thenReturn(telemetryResponse);
        when(backupConverter.convert(environment.getBackup())).thenReturn(backupResponse);
        when(proxyConfigToProxyResponseConverter.convert(environment.getProxyConfig())).thenReturn(proxyResponse);
        when(networkDtoToResponseConverter.convert(environment.getNetwork(), environment.getExperimentalFeatures().getTunnel(), true))
                .thenReturn(environmentNetworkResponse);
        when(dataServicesConverter.convertToResponse(environment.getDataServices())).thenReturn(dataServicesResponse);

        DetailedEnvironmentResponse actual = underTest.dtoToDetailedResponse(environment);

        assertEquals(environment.getResourceCrn(), actual.getCrn());
        assertEquals(environment.getName(), actual.getName());
        assertEquals(environment.getOriginalName(), actual.getOriginalName());
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
        assertEquals(environment.getExperimentalFeatures().getCcmV2TlsType(), actual.getCcmV2TlsType());
        assertEquals(environment.getAdminGroupName(), actual.getAdminGroupName());
        assertParameters(environment, actual, cloudPlatform);
        assertEquals(environment.getParentEnvironmentCrn(), actual.getParentEnvironmentCrn());
        assertEquals(environment.getParentEnvironmentName(), actual.getParentEnvironmentName());
        assertEquals(environment.getParentEnvironmentCloudPlatform(), actual.getParentEnvironmentCloudPlatform());
        assertEquals(proxyResponse, actual.getProxyConfig());
        assertEquals(environmentNetworkResponse, actual.getNetwork());
        assertEquals(dataServicesResponse, actual.getDataServices());
        assertSecurityAccess(environment.getSecurityAccess(), actual.getSecurityAccess());
        assertThat(actual.isEnableSecretEncryption()).isTrue();
        assertThat(actual.isEnableComputeCluster()).isTrue();
        assertEquals(environment.getEnvironmentType().toString(), actual.getEnvironmentType());
        assertEquals(environment.getRemoteEnvironmentCrn(), actual.getRemoteEnvironmentCrn());

        verify(credentialConverter).convert(environment.getCredential());
        verify(freeIpaConverter).convert(environment.getFreeIpaCreation());
        verify(regionConverter).convertRegions(environment.getRegions());
        verify(telemetryApiConverter).convert(eq(environment.getTelemetry()), any());
        verify(proxyConfigToProxyResponseConverter).convert(environment.getProxyConfig());
        verify(networkDtoToResponseConverter).convert(environment.getNetwork(), environment.getExperimentalFeatures().getTunnel(), true);
        verify(dataServicesConverter).convertToResponse(environment.getDataServices());
        verify(dataServicesConverter).convertToResponse(environment.getDataServices());
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE"})
    void testDtoToDetailedResponseWhenComputeClusterEnabled(CloudPlatform cloudPlatform) {
        EnvironmentDto environment = createEnvironmentDtoBuilder(cloudPlatform)
                .withExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                        .withCreate(true)
                        .withKubeApiAuthorizedIpRanges(Set.of("10.0.0.0/8", "172.0.0.0/16"))
                        .withPrivateCluster(true)
                        .withOutboundType("outbound")
                        .build())
                .build();
        environment.setEnvironmentType(null);
        environment.setRemoteEnvironmentCrn(null);
        environment.setEncryptionProfileName(null);
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        FreeIpaResponse freeIpaResponse = mock(FreeIpaResponse.class);
        CompactRegionResponse compactRegionResponse = mock(CompactRegionResponse.class);
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        BackupResponse backupResponse = mock(BackupResponse.class);
        ProxyResponse proxyResponse = mock(ProxyResponse.class);
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        DataServicesResponse dataServicesResponse = mock(DataServicesResponse.class);
        EncryptionProfileResponse encryptionProfileResponse = mock(EncryptionProfileResponse.class);

        when(credentialConverter.convert(environment.getCredential())).thenReturn(credentialResponse);
        when(freeIpaConverter.convert(environment.getFreeIpaCreation())).thenReturn(freeIpaResponse);
        when(regionConverter.convertRegions(environment.getRegions())).thenReturn(compactRegionResponse);
        when(telemetryApiConverter.convert(eq(environment.getTelemetry()), any())).thenReturn(telemetryResponse);
        when(backupConverter.convert(environment.getBackup())).thenReturn(backupResponse);
        when(proxyConfigToProxyResponseConverter.convert(environment.getProxyConfig())).thenReturn(proxyResponse);
        when(networkDtoToResponseConverter.convert(environment.getNetwork(), environment.getExperimentalFeatures().getTunnel(), true))
                .thenReturn(environmentNetworkResponse);
        when(dataServicesConverter.convertToResponse(environment.getDataServices())).thenReturn(dataServicesResponse);

        DetailedEnvironmentResponse actual = underTest.dtoToDetailedResponse(environment);

        assertEquals(environment.getResourceCrn(), actual.getCrn());
        assertEquals(environment.getName(), actual.getName());
        assertEquals(environment.getOriginalName(), actual.getOriginalName());
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
        assertEquals(environment.getExperimentalFeatures().getCcmV2TlsType(), actual.getCcmV2TlsType());
        assertEquals(environment.getAdminGroupName(), actual.getAdminGroupName());
        assertParameters(environment, actual, cloudPlatform);
        assertEquals(environment.getParentEnvironmentCrn(), actual.getParentEnvironmentCrn());
        assertEquals(environment.getParentEnvironmentName(), actual.getParentEnvironmentName());
        assertEquals(environment.getParentEnvironmentCloudPlatform(), actual.getParentEnvironmentCloudPlatform());
        assertEquals(proxyResponse, actual.getProxyConfig());
        assertEquals(environmentNetworkResponse, actual.getNetwork());
        assertEquals(dataServicesResponse, actual.getDataServices());
        assertSecurityAccess(environment.getSecurityAccess(), actual.getSecurityAccess());
        assertThat(actual.isEnableSecretEncryption()).isTrue();
        assertThat(actual.isEnableComputeCluster()).isTrue();
        assertThat(actual.getExternalizedComputeCluster().getWorkerNodeSubnetIds()).contains("subnet1");
        assertThat(actual.getExternalizedComputeCluster().getKubeApiAuthorizedIpRanges()).contains("10.0.0.0/8", "172.0.0.0/16");
        assertTrue(actual.getExternalizedComputeCluster().isPrivateCluster());
        assertEquals("outbound", actual.getExternalizedComputeCluster().getOutboundType());
        assertEquals("outbound", actual.getExternalizedComputeCluster().getAzure().getOutboundType());
        assertNull(actual.getEnvironmentType());
        assertNull(actual.getRemoteEnvironmentCrn());
        assertNull(actual.getEncryptionProfileName());

        verify(credentialConverter).convert(environment.getCredential());
        verify(freeIpaConverter).convert(environment.getFreeIpaCreation());
        verify(regionConverter).convertRegions(environment.getRegions());
        verify(telemetryApiConverter).convert(eq(environment.getTelemetry()), any());
        verify(proxyConfigToProxyResponseConverter).convert(environment.getProxyConfig());
        verify(networkDtoToResponseConverter).convert(environment.getNetwork(), environment.getExperimentalFeatures().getTunnel(), true);
        verify(dataServicesConverter).convertToResponse(environment.getDataServices());
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE", "GCP"})
    void testDtoToSimpleResponse(CloudPlatform cloudPlatform) {
        EnvironmentDto environmentDto = createEnvironmentDtoBuilder(cloudPlatform).build();
        CredentialViewResponse credentialResponse = mock(CredentialViewResponse.class);
        FreeIpaResponse freeIpaResponse = mock(FreeIpaResponse.class);
        CompactRegionResponse compactRegionResponse = mock(CompactRegionResponse.class);
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        ProxyViewResponse proxyResponse = mock(ProxyViewResponse.class);
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        DataServicesResponse dataServicesResponse = mock(DataServicesResponse.class);
        String encryptionProfileName = "epName";

        when(credentialViewConverter.convertResponse(environmentDto.getCredential())).thenReturn(credentialResponse);
        when(freeIpaConverter.convert(environmentDto.getFreeIpaCreation())).thenReturn(freeIpaResponse);
        when(regionConverter.convertRegions(environmentDto.getRegions())).thenReturn(compactRegionResponse);
        when(telemetryApiConverter.convert(eq(environmentDto.getTelemetry()), any())).thenReturn(telemetryResponse);
        when(proxyConfigToProxyResponseConverter.convertToView(environmentDto.getProxyConfig())).thenReturn(proxyResponse);
        when(networkDtoToResponseConverter.convert(environmentDto.getNetwork(), environmentDto.getExperimentalFeatures().getTunnel(), false))
                .thenReturn(environmentNetworkResponse);
        when(dataServicesConverter.convertToResponse(environmentDto.getDataServices())).thenReturn(dataServicesResponse);

        SimpleEnvironmentResponse actual = underTest.dtoToSimpleResponse(environmentDto, true, true);

        assertEquals(environmentDto.getResourceCrn(), actual.getCrn());
        assertEquals(environmentDto.getName(), actual.getName());
        assertEquals(environmentDto.getOriginalName(), actual.getOriginalName());
        assertEquals(environmentDto.getDescription(), actual.getDescription());
        assertEquals(environmentDto.getCloudPlatform(), actual.getCloudPlatform());
        assertEquals(credentialResponse, actual.getCredential());
        assertEquals(environmentDto.getStatus().getResponseStatus(), actual.getEnvironmentStatus());
        assertEquals(environmentDto.getCreator(), actual.getCreator());
        assertLocation(environmentDto.getLocation(), actual.getLocation());
        assertTrue(actual.getCreateFreeIpa());
        assertEquals(freeIpaResponse, actual.getFreeIpa());
        assertEquals(environmentDto.getStatusReason(), actual.getStatusReason());
        assertEquals(environmentDto.getCreated(), actual.getCreated());
        assertEquals(environmentDto.getExperimentalFeatures().getTunnel(), actual.getTunnel());
        assertEquals(environmentDto.getExperimentalFeatures().getCcmV2TlsType(), actual.getCcmV2TlsType());
        assertEquals(environmentDto.getAdminGroupName(), actual.getAdminGroupName());
        assertEquals(environmentDto.getTags().getUserDefinedTags(), actual.getTags().getUserDefined());
        assertEquals(environmentDto.getTags().getDefaultTags(), actual.getTags().getDefaults());
        assertEquals(telemetryResponse, actual.getTelemetry());
        assertEquals(compactRegionResponse, actual.getRegions());
        assertParameters(environmentDto, actual, cloudPlatform);
        assertEquals(environmentDto.getParentEnvironmentName(), actual.getParentEnvironmentName());
        assertEquals(proxyResponse, actual.getProxyConfig());
        assertEquals(environmentNetworkResponse, actual.getNetwork());
        assertEquals(dataServicesResponse, actual.getDataServices());
        assertThat(actual.isEnableSecretEncryption()).isTrue();
        assertThat(actual.isEnableComputeCluster()).isTrue();
        assertEquals(environmentDto.getEnvironmentType().toString(), actual.getEnvironmentType());
        assertEquals(environmentDto.getRemoteEnvironmentCrn(), actual.getRemoteEnvironmentCrn());

        verify(credentialViewConverter).convertResponse(environmentDto.getCredential());
        verify(freeIpaConverter).convert(environmentDto.getFreeIpaCreation());
        verify(regionConverter).convertRegions(environmentDto.getRegions());
        verify(telemetryApiConverter).convert(eq(environmentDto.getTelemetry()), any());
        verify(proxyConfigToProxyResponseConverter).convertToView(environmentDto.getProxyConfig());
        verify(networkDtoToResponseConverter).convert(environmentDto.getNetwork(), environmentDto.getExperimentalFeatures().getTunnel(), false);
        verify(dataServicesConverter).convertToResponse(environmentDto.getDataServices());
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE", "GCP"})
    void testViewDtoToSimpleResponse(CloudPlatform cloudPlatform) {
        EnvironmentViewDto environmentViewDto = createEnvironmentViewDto(cloudPlatform);
        CredentialViewResponse credentialResponse = mock(CredentialViewResponse.class);
        FreeIpaResponse freeIpaResponse = mock(FreeIpaResponse.class);
        CompactRegionResponse compactRegionResponse = mock(CompactRegionResponse.class);
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        ProxyViewResponse proxyResponse = mock(ProxyViewResponse.class);
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        DataServicesResponse dataServicesResponse = mock(DataServicesResponse.class);
        String encryptionProfileName = "epName";

        when(credentialViewConverter.convert(environmentViewDto.getCredentialView())).thenReturn(credentialResponse);
        when(freeIpaConverter.convert(environmentViewDto.getFreeIpaCreation())).thenReturn(freeIpaResponse);
        when(regionConverter.convertRegions(environmentViewDto.getRegions())).thenReturn(compactRegionResponse);
        when(telemetryApiConverter.convert(eq(environmentViewDto.getTelemetry()), any())).thenReturn(telemetryResponse);
        when(proxyConfigToProxyResponseConverter.convertToView(environmentViewDto.getProxyConfig())).thenReturn(proxyResponse);
        when(networkDtoToResponseConverter.convert(environmentViewDto.getNetwork(), environmentViewDto.getExperimentalFeatures().getTunnel(), false))
                .thenReturn(environmentNetworkResponse);
        when(dataServicesConverter.convertToResponse(environmentViewDto.getDataServices())).thenReturn(dataServicesResponse);

        SimpleEnvironmentResponse actual = underTest.dtoToSimpleResponse(environmentViewDto);

        assertEquals(environmentViewDto.getResourceCrn(), actual.getCrn());
        assertEquals(environmentViewDto.getName(), actual.getName());
        assertEquals(environmentViewDto.getOriginalName(), actual.getOriginalName());
        assertEquals(environmentViewDto.getDescription(), actual.getDescription());
        assertEquals(environmentViewDto.getCloudPlatform(), actual.getCloudPlatform());
        assertEquals(credentialResponse, actual.getCredential());
        assertEquals(environmentViewDto.getStatus().getResponseStatus(), actual.getEnvironmentStatus());
        assertEquals(environmentViewDto.getCreator(), actual.getCreator());
        assertLocation(environmentViewDto.getLocation(), actual.getLocation());
        assertTrue(actual.getCreateFreeIpa());
        assertEquals(freeIpaResponse, actual.getFreeIpa());
        assertEquals(environmentViewDto.getStatusReason(), actual.getStatusReason());
        assertEquals(environmentViewDto.getCreated(), actual.getCreated());
        assertEquals(environmentViewDto.getExperimentalFeatures().getTunnel(), actual.getTunnel());
        assertEquals(environmentViewDto.getExperimentalFeatures().getCcmV2TlsType(), actual.getCcmV2TlsType());
        assertEquals(environmentViewDto.getAdminGroupName(), actual.getAdminGroupName());
        assertEquals(environmentViewDto.getTags().getUserDefinedTags(), actual.getTags().getUserDefined());
        assertEquals(environmentViewDto.getTags().getDefaultTags(), actual.getTags().getDefaults());
        assertEquals(telemetryResponse, actual.getTelemetry());
        assertEquals(compactRegionResponse, actual.getRegions());
        assertParameters(environmentViewDto, actual, cloudPlatform);
        assertEquals(environmentViewDto.getParentEnvironmentName(), actual.getParentEnvironmentName());
        assertEquals(proxyResponse, actual.getProxyConfig());
        assertEquals(environmentNetworkResponse, actual.getNetwork());
        assertEquals(dataServicesResponse, actual.getDataServices());
        assertThat(actual.isEnableSecretEncryption()).isTrue();
        assertThat(actual.isEnableComputeCluster()).isTrue();
        assertEquals(environmentViewDto.getEnvironmentType().toString(), actual.getEnvironmentType());
        assertEquals(environmentViewDto.getRemoteEnvironmentCrn(), actual.getRemoteEnvironmentCrn());

        verify(credentialViewConverter).convert(environmentViewDto.getCredentialView());
        verify(freeIpaConverter).convert(environmentViewDto.getFreeIpaCreation());
        verify(regionConverter).convertRegions(environmentViewDto.getRegions());
        verify(telemetryApiConverter).convert(eq(environmentViewDto.getTelemetry()), any());
        verify(proxyConfigToProxyResponseConverter).convertToView(environmentViewDto.getProxyConfig());
        verify(networkDtoToResponseConverter).convert(environmentViewDto.getNetwork(), environmentViewDto.getExperimentalFeatures().getTunnel(), false);
        verify(dataServicesConverter).convertToResponse(environmentViewDto.getDataServices());
    }

    private void assertParameters(EnvironmentDtoBase environment, EnvironmentBaseResponse actual, CloudPlatform cloudPlatform) {
        if (AWS.equals(cloudPlatform)) {
            assertEquals(environment.getParameters().getAwsParametersDto().getAwsDiskEncryptionParametersDto().getEncryptionKeyArn(),
                    actual.getAws().getAwsDiskEncryptionParameters().getEncryptionKeyArn());
        } else if (AZURE.equals(cloudPlatform)) {
            assertAzureParameters(environment.getParameters().getAzureParametersDto(), actual.getAzure());
        } else if (GCP.equals(cloudPlatform)) {
            assertGcpParameters(environment.getParameters().getGcpParametersDto(), actual.getGcp());
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
        assertEquals(azureParametersDto.getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl(),
                azureEnvironmentParameters.getResourceEncryptionParameters().getEncryptionKeyUrl());
        assertEquals("dummy-des-id", azureEnvironmentParameters.getResourceEncryptionParameters().getDiskEncryptionSetId());
        assertEquals("dummyResourceGroupName", azureEnvironmentParameters.getResourceEncryptionParameters().getEncryptionKeyResourceGroupName());
    }

    private void assertGcpParameters(GcpParametersDto gcpParametersDto, GcpEnvironmentParameters gcpEnvironmentParameters) {
        assertNotNull(gcpEnvironmentParameters);
        assertEquals(gcpParametersDto.getGcpResourceEncryptionParametersDto().getEncryptionKey(),
                gcpEnvironmentParameters.getGcpResourceEncryptionParameters().getEncryptionKey());
    }

    private EnvironmentDto.Builder createEnvironmentDtoBuilder(CloudPlatform cloudPlatform) {
        return EnvironmentDto.builder()
                .withProxyConfig(new ProxyConfig())
                .withCredential(new Credential())
                .withResourceCrn("resource-crn")
                .withName("my-env")
                .withOriginalName("my-env")
                .withDescription("Test environment.")
                .withCloudPlatform("AWS")
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
                .withNetwork(NetworkDto.builder().withSubnetMetas(Map.of("subnet1",
                        new CloudSubnet.Builder()
                                .id("subnet1-id")
                                .name("subnet1")
                                .build()))
                        .build())
                .withSecurityAccess(createSecurityAccess())
                .withEnvironmentDeletionType(EnvironmentDeletionType.FORCE)
                .withEnableSecretEncryption(true)
                .withEnableComputeCluster(true)
                .withEnvironmentType(EnvironmentType.HYBRID)
                .withRemoteEnvironmentCrn("remoteEnvironmentCrn")
                .withEncryptionProfileName("encryptionProfName");
    }

    private EnvironmentViewDto createEnvironmentViewDto(CloudPlatform cloudPlatform) {
        return EnvironmentViewDto.builder()
                .withProxyConfig(new ProxyConfigView())
                .withCredentialView(new CredentialView())
                .withResourceCrn("resource-crn")
                .withName("my-env")
                .withOriginalName("my-env")
                .withDescription("Test environment.")
                .withCloudPlatform("AWS")
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
                .withNetwork(NetworkDto.builder().build())
                .withSecurityAccess(createSecurityAccess())
                .withEnvironmentDeletionType(EnvironmentDeletionType.FORCE)
                .withEnableSecretEncryption(true)
                .withEnableComputeCluster(true)
                .withEnvironmentType(EnvironmentType.HYBRID)
                .withRemoteEnvironmentCrn("remoteEnvironmentCrn")
                .withEncryptionProfileName("epName")
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
        } else if (GCP.equals(cloudPlatform)) {
            return createGcpParameters();
        } else {
            throw new RuntimeException("CloudPlatform " + cloudPlatform + " is not supported.");
        }
    }

    private ParametersDto createAzureParameters() {
        return ParametersDto.builder()
                .withAzureParametersDto(AzureParametersDto.builder()
                        .withAzureResourceGroupDto(
                                AzureResourceGroupDto.builder()
                                        .withName("my-resource-group-name")
                                        .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                        .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                        .build())
                        .withAzureResourceEncryptionParametersDto(
                                AzureResourceEncryptionParametersDto.builder()
                                        .withEncryptionKeyUrl("dummy-key-url")
                                        .withDiskEncryptionSetId("dummy-des-id")
                                        .withEncryptionKeyResourceGroupName("dummyResourceGroupName")
                                        .build())
                        .build())
                .build();
    }

    private ParametersDto createGcpParameters() {
        return ParametersDto.builder()
                .withGcpParametersDto(GcpParametersDto.builder()
                        .withGcpResourceEncryptionParametersDto(
                                GcpResourceEncryptionParametersDto.builder()
                                        .withEncryptionKey("dummy-encryption-key")
                                        .build())
                        .build())
                .build();
    }

    private ParametersDto createAwsParameters() {
        return ParametersDto.builder()
                .withAwsParametersDto(AwsParametersDto.builder()
                        .withAwsDiskEncryptionParametersDto(AwsDiskEncryptionParametersDto.builder()
                                .withEncryptionKeyArn("dummy-key-arn")
                                .build())
                        .build())
                .build();
    }

    private ExperimentalFeatures createExperimentalFeatures() {
        return ExperimentalFeatures.builder()
                .withTunnel(Tunnel.CCM)
                .withIdBrokerMappingSource(IdBrokerMappingSource.IDBMMS)
                .withCloudStorageValidation(CloudStorageValidation.ENABLED)
                .withCcmV2TlsType(CcmV2TlsType.ONE_WAY_TLS)
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
        return FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP)
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
