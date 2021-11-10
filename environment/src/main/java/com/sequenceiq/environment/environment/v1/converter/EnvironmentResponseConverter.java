package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.cloudbreak.util.SecurityGroupSeparator.getSecurityGroupIds;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.yarn.YarnEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentDeletionType;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TagResponse;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialViewConverter;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyResponseConverter;

@Component
public class EnvironmentResponseConverter {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EnvironmentResponseConverter.class);

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    private final RegionConverter regionConverter;

    private final CredentialViewConverter credentialViewConverter;

    private final ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter;

    private final FreeIpaConverter freeIpaConverter;

    private final TelemetryApiConverter telemetryApiConverter;

    private final BackupConverter backupConverter;

    private final NetworkDtoToResponseConverter networkDtoToResponseConverter;

    public EnvironmentResponseConverter(CredentialToCredentialV1ResponseConverter credentialConverter,
            RegionConverter regionConverter, CredentialViewConverter credentialViewConverter,
            ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter,
            FreeIpaConverter freeIpaConverter, TelemetryApiConverter telemetryApiConverter,
            BackupConverter backupConverter, NetworkDtoToResponseConverter networkDtoToResponseConverter) {
        this.credentialConverter = credentialConverter;
        this.regionConverter = regionConverter;
        this.credentialViewConverter = credentialViewConverter;
        this.proxyConfigToProxyResponseConverter = proxyConfigToProxyResponseConverter;
        this.freeIpaConverter = freeIpaConverter;
        this.telemetryApiConverter = telemetryApiConverter;
        this.backupConverter = backupConverter;
        this.networkDtoToResponseConverter = networkDtoToResponseConverter;
    }

    public DetailedEnvironmentResponse dtoToDetailedResponse(EnvironmentDto environmentDto) {
        DetailedEnvironmentResponse.Builder builder = DetailedEnvironmentResponse.builder()
                .withCrn(environmentDto.getResourceCrn())
                .withName(environmentDto.getName())
                .withDescription(environmentDto.getDescription())
                .withCloudPlatform(environmentDto.getCloudPlatform())
                .withCredential(credentialConverter.convert(environmentDto.getCredential()))
                .withEnvironmentStatus(environmentDto.getStatus().getResponseStatus())
                .withLocation(locationDtoToResponse(environmentDto.getLocation()))
                .withCreateFreeIpa(environmentDto.getFreeIpaCreation().getCreate())
                .withFreeIpa(freeIpaConverter.convert(environmentDto.getFreeIpaCreation()))
                .withRegions(regionConverter.convertRegions(environmentDto.getRegions()))
                .withCreator(environmentDto.getCreator())
                .withAuthentication(authenticationDtoToResponse(environmentDto.getAuthentication()))
                .withStatusReason(environmentDto.getStatusReason())
                .withCreated(environmentDto.getCreated())
                .withTag(getIfNotNull(environmentDto.getTags(), this::environmentTagsToTagResponse))
                .withTelemetry(telemetryApiConverter.convert(environmentDto.getTelemetry()))
                .withBackup(backupConverter.convert(environmentDto.getBackup()))
                .withTunnel(environmentDto.getExperimentalFeatures().getTunnel())
                .withIdBrokerMappingSource(environmentDto.getExperimentalFeatures().getIdBrokerMappingSource())
                .withCloudStorageValidation(environmentDto.getExperimentalFeatures().getCloudStorageValidation())
                .withAdminGroupName(environmentDto.getAdminGroupName())
                .withAws(getIfNotNull(environmentDto.getParameters(), this::awsEnvParamsToAwsEnvironmentParams))
                .withAzure(getIfNotNull(environmentDto.getParameters(), this::azureEnvParamsToAzureEnvironmentParams))
                .withYarn(getIfNotNull(environmentDto.getParameters(), this::yarnEnvParamsToYarnEnvironmentParams))
                .withGcp(getIfNotNull(environmentDto.getParameters(), this::gcpEnvParamsToGcpEnvironmentParams))
                .withParentEnvironmentCrn(environmentDto.getParentEnvironmentCrn())
                .withParentEnvironmentName(environmentDto.getParentEnvironmentName())
                .withParentEnvironmentCloudPlatform(environmentDto.getParentEnvironmentCloudPlatform())
                .withEnvironmentServiceVersion(environmentDto.getEnvironmentServiceVersion())
                .withDeletionType(deletionType(environmentDto.getDeletionType()))
                .withCcmV2TlsType(environmentDto.getExperimentalFeatures().getCcmV2TlsType())
                .withEnvironmentDomain(environmentDto.getDomain());

        NullUtil.doIfNotNull(environmentDto.getProxyConfig(),
                proxyConfig -> builder.withProxyConfig(proxyConfigToProxyResponseConverter.convert(environmentDto.getProxyConfig())));
        NullUtil.doIfNotNull(environmentDto.getNetwork(),
                network -> builder.withNetwork(networkDtoToResponse(network, environmentDto.getExperimentalFeatures().getTunnel(), true)));
        NullUtil.doIfNotNull(environmentDto.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessDtoToResponse(securityAccess)));
        return builder.build();
    }

    private EnvironmentNetworkResponse networkDtoToResponse(NetworkDto network, Tunnel tunnel, boolean detailedResponse) {
        return networkDtoToResponseConverter.convert(network, tunnel, detailedResponse);
    }

    public SimpleEnvironmentResponse dtoToSimpleResponse(EnvironmentDto environmentDto) {
        SimpleEnvironmentResponse.Builder builder = SimpleEnvironmentResponse.builder()
                .withCrn(environmentDto.getResourceCrn())
                .withName(environmentDto.getName())
                .withDescription(environmentDto.getDescription())
                .withCloudPlatform(environmentDto.getCloudPlatform())
                .withCredentialView(credentialViewConverter.convert(environmentDto.getCredentialView()))
                .withEnvironmentStatus(environmentDto.getStatus().getResponseStatus())
                .withCreator(environmentDto.getCreator())
                .withLocation(locationDtoToResponse(environmentDto.getLocation()))
                .withCreateFreeIpa(environmentDto.getFreeIpaCreation().getCreate())
                .withFreeIpa(freeIpaConverter.convert(environmentDto.getFreeIpaCreation()))
                .withStatusReason(environmentDto.getStatusReason())
                .withCreated(environmentDto.getCreated())
                .withTunnel(environmentDto.getExperimentalFeatures().getTunnel())
                .withAdminGroupName(environmentDto.getAdminGroupName())
                .withTag(getIfNotNull(environmentDto.getTags(), this::environmentTagsToTagResponse))
                .withTelemetry(telemetryApiConverter.convert(environmentDto.getTelemetry()))
                .withBackup(backupConverter.convert(environmentDto.getBackup()))
                .withRegions(regionConverter.convertRegions(environmentDto.getRegions()))
                .withAws(getIfNotNull(environmentDto.getParameters(), this::awsEnvParamsToAwsEnvironmentParams))
                .withAzure(getIfNotNull(environmentDto.getParameters(), this::azureEnvParamsToAzureEnvironmentParams))
                .withYarn(getIfNotNull(environmentDto.getParameters(), this::yarnEnvParamsToYarnEnvironmentParams))
                .withGcp(getIfNotNull(environmentDto.getParameters(), this::gcpEnvParamsToGcpEnvironmentParams))
                .withDeletionType(deletionType(environmentDto.getDeletionType()))
                .withParentEnvironmentName(environmentDto.getParentEnvironmentName())
                .withCcmV2TlsType(environmentDto.getExperimentalFeatures().getCcmV2TlsType())
                .withEnvironmentDomain(environmentDto.getDomain());

        NullUtil.doIfNotNull(environmentDto.getProxyConfig(),
                proxyConfig -> builder.withProxyConfig(proxyConfigToProxyResponseConverter.convertToView(environmentDto.getProxyConfig())));
        NullUtil.doIfNotNull(environmentDto.getNetwork(),
                network -> builder.withNetwork(networkDtoToResponse(network, environmentDto.getExperimentalFeatures().getTunnel(), false)));
        return builder.build();
    }

    private EnvironmentAuthenticationResponse authenticationDtoToResponse(AuthenticationDto authenticationDto) {
        return EnvironmentAuthenticationResponse.builder()
                .withLoginUserName(authenticationDto.getLoginUserName())
                .withPublicKey(authenticationDto.getPublicKey())
                .withPublicKeyId(authenticationDto.getPublicKeyId())
                .build();
    }

    private SecurityAccessResponse securityAccessDtoToResponse(SecurityAccessDto securityAccess) {
        return SecurityAccessResponse.builder()
                .withCidr(securityAccess.getCidr())
                .withSecurityGroupIdForKnox(securityAccess.getSecurityGroupIdForKnox())
                .withSecurityGroupIdsForKnox(getSecurityGroupIds(securityAccess.getSecurityGroupIdForKnox()))
                .withDefaultSecurityGroupId(securityAccess.getDefaultSecurityGroupId())
                .withDefaultSecurityGroupIds(getSecurityGroupIds(securityAccess.getDefaultSecurityGroupId()))
                .build();
    }

    private AwsEnvironmentParameters awsEnvParamsToAwsEnvironmentParams(ParametersDto parameters) {
        AwsDiskEncryptionParametersDto awsDiskEncryptionParametersDto = Optional.ofNullable(parameters.getAwsParametersDto())
                .map(AwsParametersDto::getAwsDiskEncryptionParametersDto)
                .orElse(null);
        return Optional.ofNullable(parameters.getAwsParametersDto())
                .map(aws -> AwsEnvironmentParameters.builder()
                        .withS3guard(getIfNotNull(aws, this::awsParametersToS3guardParam))
                        .withAwsDiskEncryptionParameters(getIfNotNull(awsDiskEncryptionParametersDto, this::awsParametersToAwsDiskEncryptionParameters))
                        .build())
                .orElse(null);
    }

    private AzureEnvironmentParameters azureEnvParamsToAzureEnvironmentParams(ParametersDto parameters) {
        AzureResourceGroupDto resourceGroupDto = Optional.ofNullable(parameters.getAzureParametersDto())
                .map(AzureParametersDto::getAzureResourceGroupDto)
                .filter(rgDto -> Objects.nonNull(rgDto.getResourceGroupUsagePattern()))
                .filter(rgDto -> Objects.nonNull(rgDto.getResourceGroupCreation()))
                .orElse(null);
        AzureResourceEncryptionParametersDto resourceEncryptionParametersDto = Optional.ofNullable(parameters.getAzureParametersDto())
                .map(AzureParametersDto::getAzureResourceEncryptionParametersDto)
                .orElse(null);
        return AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(getIfNotNull(resourceGroupDto, this::azureParametersToAzureResourceGroup))
                .withResourceEncryptionParameters(getIfNotNull(resourceEncryptionParametersDto, this::azureParametersToAzureResourceEncryptionParameters))
                .build();
    }

    private YarnEnvironmentParameters yarnEnvParamsToYarnEnvironmentParams(ParametersDto parameters) {
        return Optional.ofNullable(parameters.getYarnParametersDto())
                .map(yarn -> YarnEnvironmentParameters.builder()
                        .build())
                .orElse(null);
    }

    private GcpEnvironmentParameters gcpEnvParamsToGcpEnvironmentParams(ParametersDto parameters) {
        GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersDto = Optional.ofNullable(parameters.getGcpParametersDto())
                .map(GcpParametersDto::getGcpResourceEncryptionParametersDto)
                .orElse(null);

        return Optional.ofNullable(parameters.getGcpParametersDto())
                .map(gcp -> GcpEnvironmentParameters.builder()
                        .withResourceEncryptionParameters(getIfNotNull(gcpResourceEncryptionParametersDto, this::gcpParametersToGcpResourceEncryptionParameters))
                        .build())
                .orElse(null);
    }

    private S3GuardRequestParameters awsParametersToS3guardParam(AwsParametersDto awsParametersDto) {
        return S3GuardRequestParameters.builder()
                .withDynamoDbTableName(awsParametersDto.getS3GuardTableName())
                .build();
    }

    private AwsDiskEncryptionParameters awsParametersToAwsDiskEncryptionParameters(AwsDiskEncryptionParametersDto awsDiskEncryptionParametersDto) {
        return AwsDiskEncryptionParameters.builder()
                .withEncryptionKeyArn(awsDiskEncryptionParametersDto.getEncryptionKeyArn())
                .build();
    }

    private AzureResourceGroup azureParametersToAzureResourceGroup(AzureResourceGroupDto azureResourceGroupDto) {
        return AzureResourceGroup.builder()
                .withName(azureResourceGroupDto.getName())
                .withResourceGroupUsage(resourceGroupUsagePatternToResourceGroupUsage(azureResourceGroupDto.getResourceGroupUsagePattern()))
                .build();
    }

    private AzureResourceEncryptionParameters azureParametersToAzureResourceEncryptionParameters(
            AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto) {
        return AzureResourceEncryptionParameters.builder()
                .withEncryptionKeyUrl(azureResourceEncryptionParametersDto.getEncryptionKeyUrl())
                .withDiskEncryptionSetId(azureResourceEncryptionParametersDto.getDiskEncryptionSetId())
                .withEncryptionKeyResourceGroupName(azureResourceEncryptionParametersDto.getEncryptionKeyResourceGroupName())
                .build();
    }

    private GcpResourceEncryptionParameters gcpParametersToGcpResourceEncryptionParameters(
            GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersDto) {
        return GcpResourceEncryptionParameters.builder()
                .withEncryptionKey(gcpResourceEncryptionParametersDto.getEncryptionKey())
                .build();
    }

    private ResourceGroupUsage resourceGroupUsagePatternToResourceGroupUsage(ResourceGroupUsagePattern resourceGroupUsagePattern) {
        switch (resourceGroupUsagePattern) {
            case USE_SINGLE:
                return ResourceGroupUsage.SINGLE;
            case USE_MULTIPLE:
                return ResourceGroupUsage.MULTIPLE;
            case USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT:
                return ResourceGroupUsage.SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT;
            default:
                throw new BadRequestException("Unknown resource group usage pattern: %s" + resourceGroupUsagePattern);
        }
    }

    private EnvironmentDeletionType deletionType(com.sequenceiq.environment.environment.EnvironmentDeletionType deletionType) {
        if (deletionType == null) {
            LOGGER.debug("Environment deletion type is not filled, falling back to NONE");
            return  EnvironmentDeletionType.NONE;
        }
        switch (deletionType) {
            case NONE:
                return EnvironmentDeletionType.NONE;
            case SIMPLE:
                return EnvironmentDeletionType.SIMPLE;
            case FORCE:
                return EnvironmentDeletionType.FORCE;
            default:
                throw new BadRequestException("Unknown deletion type: %s" + deletionType);
        }
    }

    private TagResponse environmentTagsToTagResponse(EnvironmentTags tags) {
        TagResponse tagResponse = new TagResponse();
        tagResponse.setDefaults(tags.getDefaultTags());
        tagResponse.setUserDefined(tags.getUserDefinedTags());
        return tagResponse;
    }

    private LocationResponse locationDtoToResponse(LocationDto locationDto) {
        return LocationResponse.LocationResponseBuilder.aLocationResponse()
                .withName(locationDto.getName())
                .withDisplayName(locationDto.getDisplayName())
                .withLatitude(locationDto.getLatitude())
                .withLongitude(locationDto.getLongitude())
                .build();
    }
}
