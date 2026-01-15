package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.cloudbreak.util.SecurityGroupSeparator.getSecurityGroupIds;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.AzureExternalizedComputeParams;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.yarn.YarnEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.CreateEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentDeletionType;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.ExternalizedComputeClusterResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TagResponse;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialViewConverter;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileToEncryptionProfileResponseConverter;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.CreateEnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentViewDto;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentResponseConverter.class);

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    private final RegionConverter regionConverter;

    private final CredentialViewConverter credentialViewConverter;

    private final ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter;

    private final FreeIpaConverter freeIpaConverter;

    private final TelemetryApiConverter telemetryApiConverter;

    private final BackupConverter backupConverter;

    private final NetworkDtoToResponseConverter networkDtoToResponseConverter;

    private final DataServicesConverter dataServicesConverter;

    private final EncryptionProfileToEncryptionProfileResponseConverter encryptionProfileResponseConverter;

    public EnvironmentResponseConverter(CredentialToCredentialV1ResponseConverter credentialConverter,
            RegionConverter regionConverter, CredentialViewConverter credentialViewConverter,
            ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter,
            FreeIpaConverter freeIpaConverter, TelemetryApiConverter telemetryApiConverter,
            BackupConverter backupConverter, NetworkDtoToResponseConverter networkDtoToResponseConverter,
            DataServicesConverter dataServicesConverter,
            EncryptionProfileToEncryptionProfileResponseConverter encryptionProfileResponseConverter) {
        this.credentialConverter = credentialConverter;
        this.regionConverter = regionConverter;
        this.credentialViewConverter = credentialViewConverter;
        this.proxyConfigToProxyResponseConverter = proxyConfigToProxyResponseConverter;
        this.freeIpaConverter = freeIpaConverter;
        this.telemetryApiConverter = telemetryApiConverter;
        this.backupConverter = backupConverter;
        this.networkDtoToResponseConverter = networkDtoToResponseConverter;
        this.dataServicesConverter = dataServicesConverter;
        this.encryptionProfileResponseConverter = encryptionProfileResponseConverter;
    }

    public CreateEnvironmentResponse dtoToCreateResponse(CreateEnvironmentDto environmentDto) {
        return dtoToDetailedResponseBuilder(CreateEnvironmentResponse.builder(), environmentDto.getEnvironmentDto())
                .withFlowIdentifier(environmentDto.getFlowIdentifier())
                .build();
    }

    public DetailedEnvironmentResponse dtoToDetailedResponse(EnvironmentDto environmentDto) {
        return dtoToDetailedResponseBuilder(DetailedEnvironmentResponse.builder(), environmentDto)
                .build();
    }

    public <B extends DetailedEnvironmentResponse.Builder> B dtoToDetailedResponseBuilder(B builder, EnvironmentDto environmentDto) {
        builder.withCrn(environmentDto.getResourceCrn())
                .withOriginalName(environmentDto.getOriginalName())
                .withName(environmentDto.getName())
                .withDescription(environmentDto.getDescription())
                .withCloudPlatform(environmentDto.getCloudPlatform())
                .withCredential(credentialConverter.convert(environmentDto.getCredential()))
                .withEnvironmentStatus(environmentDto.getStatus().getResponseStatus())
                .withLocation(locationDtoToResponse(environmentDto.getLocation()))
                .withCreateFreeIpa(environmentDto.getFreeIpaCreation().isCreate())
                .withFreeIpa(freeIpaConverter.convert(environmentDto.getFreeIpaCreation()))
                .withRegions(regionConverter.convertRegions(environmentDto.getRegions()))
                .withCreator(environmentDto.getCreator())
                .withAuthentication(authenticationDtoToResponse(environmentDto.getAuthentication()))
                .withStatusReason(environmentDto.getStatusReason())
                .withCreated(environmentDto.getCreated())
                .withTag(getIfNotNull(environmentDto.getTags(), this::environmentTagsToTagResponse))
                .withTelemetry(telemetryApiConverter.convert(environmentDto.getTelemetry(), environmentDto.getAccountId()))
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
                .withAccountId(environmentDto.getAccountId())
                .withEnvironmentDomain(environmentDto.getDomain())
                .withDataServices(dataServicesConverter.convertToResponse(environmentDto.getDataServices()))
                .withEnableSecretEncryption(environmentDto.isEnableSecretEncryption())
                .withEnableComputeCluster(environmentDto.isEnableComputeCluster())
                .withEnvironmentType(environmentDto.getEnvironmentType() != null ? environmentDto.getEnvironmentType().toString() :  null)
                .withRemoteEnvironmentCrn(environmentDto.getRemoteEnvironmentCrn())
                .withEncryptionProfileCrn(environmentDto.getEncryptionProfileCrn());

        NullUtil.doIfNotNull(environmentDto.getProxyConfig(),
                proxyConfig -> builder.withProxyConfig(proxyConfigToProxyResponseConverter.convert(environmentDto.getProxyConfig())));
        NullUtil.doIfNotNull(environmentDto.getNetwork(),
                network -> builder.withNetwork(networkDtoToResponse(network, environmentDto.getExperimentalFeatures().getTunnel(), true)));
        NullUtil.doIfNotNull(environmentDto.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessDtoToResponse(securityAccess)));
        NullUtil.doIfNotNull(environmentDto.getExternalizedComputeCluster(),
                externalizedComputeCluster -> builder.withExternalizedComputeCluster(externalizedComputeClusterDtoToResponse(externalizedComputeCluster,
                        environmentDto.getNetwork())));
        return builder;
    }

    private ExternalizedComputeClusterResponse externalizedComputeClusterDtoToResponse(ExternalizedComputeClusterDto externalizedComputeClusterDto,
            NetworkDto networkDto) {
        if (externalizedComputeClusterDto != null) {
            Set<String> workerNodeSubnetIds = getWorkerNodeSubnetIds(externalizedComputeClusterDto, networkDto);
            return ExternalizedComputeClusterResponse.newBuilder()
                    .withPrivateCluster(externalizedComputeClusterDto.isPrivateCluster())
                    .withKubeApiAuthorizedIpRanges(externalizedComputeClusterDto.getKubeApiAuthorizedIpRanges())
                    .withOutboundType(externalizedComputeClusterDto.getOutboundType())
                    .withWorkerNodeSubnetIds(workerNodeSubnetIds)
                    .withAzure(AzureExternalizedComputeParams.newBuilder()
                            .withOutboundType(externalizedComputeClusterDto.getOutboundType())
                            .build())
                    .build();
        } else {
            return null;
        }
    }

    private Set<String> getWorkerNodeSubnetIds(ExternalizedComputeClusterDto externalizedComputeClusterDto, NetworkDto networkDto) {
        Set<String> workerNodeSubnetIds = externalizedComputeClusterDto.getWorkerNodeSubnetIds();
        if (CollectionUtils.isEmpty(workerNodeSubnetIds) && networkDto != null) {
            workerNodeSubnetIds = networkDto.getSubnetIds();
        }
        return workerNodeSubnetIds;
    }

    private EnvironmentNetworkResponse networkDtoToResponse(NetworkDto network, Tunnel tunnel, boolean detailedResponse) {
        return networkDtoToResponseConverter.convert(network, tunnel, detailedResponse);
    }

    public SimpleEnvironmentResponse dtoToSimpleResponse(EnvironmentViewDto environmentViewDto) {
        SimpleEnvironmentResponse.Builder builder = SimpleEnvironmentResponse.builder()
                .withCrn(environmentViewDto.getResourceCrn())
                .withOriginalName(environmentViewDto.getOriginalName())
                .withName(environmentViewDto.getName())
                .withDescription(environmentViewDto.getDescription())
                .withCloudPlatform(environmentViewDto.getCloudPlatform())
                .withCredentialView(credentialViewConverter.convert(environmentViewDto.getCredentialView()))
                .withEnvironmentStatus(environmentViewDto.getStatus().getResponseStatus())
                .withCreator(environmentViewDto.getCreator())
                .withLocation(locationDtoToResponse(environmentViewDto.getLocation()))
                .withCreateFreeIpa(environmentViewDto.getFreeIpaCreation().isCreate())
                .withFreeIpa(freeIpaConverter.convert(environmentViewDto.getFreeIpaCreation()))
                .withStatusReason(environmentViewDto.getStatusReason())
                .withCreated(environmentViewDto.getCreated())
                .withTunnel(environmentViewDto.getExperimentalFeatures().getTunnel())
                .withAdminGroupName(environmentViewDto.getAdminGroupName())
                .withTag(getIfNotNull(environmentViewDto.getTags(), this::environmentTagsToTagResponse))
                .withTelemetry(telemetryApiConverter.convert(environmentViewDto.getTelemetry(), environmentViewDto.getAccountId()))
                .withBackup(backupConverter.convert(environmentViewDto.getBackup()))
                .withRegions(regionConverter.convertRegions(environmentViewDto.getRegions()))
                .withAws(getIfNotNull(environmentViewDto.getParameters(), this::awsEnvParamsToAwsEnvironmentParams))
                .withAzure(getIfNotNull(environmentViewDto.getParameters(), this::azureEnvParamsToAzureEnvironmentParams))
                .withYarn(getIfNotNull(environmentViewDto.getParameters(), this::yarnEnvParamsToYarnEnvironmentParams))
                .withGcp(getIfNotNull(environmentViewDto.getParameters(), this::gcpEnvParamsToGcpEnvironmentParams))
                .withDeletionType(deletionType(environmentViewDto.getDeletionType()))
                .withParentEnvironmentName(environmentViewDto.getParentEnvironmentName())
                .withCcmV2TlsType(environmentViewDto.getExperimentalFeatures().getCcmV2TlsType())
                .withEnvironmentDomain(environmentViewDto.getDomain())
                .withDataServices(dataServicesConverter.convertToResponse(environmentViewDto.getDataServices()))
                .withEnableSecretEncryption(environmentViewDto.isEnableSecretEncryption())
                .withEnableComputeCluster(environmentViewDto.isEnableComputeCluster())
                .withEnvironmentType(environmentViewDto.getEnvironmentType() != null ? environmentViewDto.getEnvironmentType().toString() : null)
                .withRemoteEnvironmentCrn(environmentViewDto.getRemoteEnvironmentCrn())
                .withEncryptionProfileCrn(environmentViewDto.getEncryptionProfileCrn());

        NullUtil.doIfNotNull(environmentViewDto.getProxyConfig(),
                proxyConfig -> builder.withProxyConfig(proxyConfigToProxyResponseConverter.convertToView(environmentViewDto.getProxyConfig())));
        NullUtil.doIfNotNull(environmentViewDto.getNetwork(),
                network -> builder.withNetwork(networkDtoToResponse(network, environmentViewDto.getExperimentalFeatures().getTunnel(), false)));
        return builder.build();
    }

    public SimpleEnvironmentResponse dtoToSimpleResponse(EnvironmentDto environmentDto, boolean withNetwork, boolean withFreeIPA) {
        SimpleEnvironmentResponse.Builder builder = SimpleEnvironmentResponse.builder()
                .withCrn(environmentDto.getResourceCrn())
                .withOriginalName(environmentDto.getOriginalName())
                .withName(environmentDto.getName())
                .withDescription(environmentDto.getDescription())
                .withCloudPlatform(environmentDto.getCloudPlatform())
                .withCredentialView(credentialViewConverter.convertResponse(environmentDto.getCredential()))
                .withEnvironmentStatus(environmentDto.getStatus().getResponseStatus())
                .withCreator(environmentDto.getCreator())
                .withLocation(locationDtoToResponse(environmentDto.getLocation()))
                .withCreateFreeIpa(environmentDto.getFreeIpaCreation().isCreate())
                .withStatusReason(environmentDto.getStatusReason())
                .withCreated(environmentDto.getCreated())
                .withTunnel(environmentDto.getExperimentalFeatures().getTunnel())
                .withAdminGroupName(environmentDto.getAdminGroupName())
                .withTag(getIfNotNull(environmentDto.getTags(), this::environmentTagsToTagResponse))
                .withTelemetry(telemetryApiConverter.convert(environmentDto.getTelemetry(), environmentDto.getAccountId()))
                .withBackup(backupConverter.convert(environmentDto.getBackup()))
                .withRegions(regionConverter.convertRegions(environmentDto.getRegions()))
                .withAws(getIfNotNull(environmentDto.getParameters(), this::awsEnvParamsToAwsEnvironmentParams))
                .withAzure(getIfNotNull(environmentDto.getParameters(), this::azureEnvParamsToAzureEnvironmentParams))
                .withYarn(getIfNotNull(environmentDto.getParameters(), this::yarnEnvParamsToYarnEnvironmentParams))
                .withGcp(getIfNotNull(environmentDto.getParameters(), this::gcpEnvParamsToGcpEnvironmentParams))
                .withDeletionType(deletionType(environmentDto.getDeletionType()))
                .withParentEnvironmentName(environmentDto.getParentEnvironmentName())
                .withCcmV2TlsType(environmentDto.getExperimentalFeatures().getCcmV2TlsType())
                .withEnvironmentDomain(environmentDto.getDomain())
                .withDataServices(dataServicesConverter.convertToResponse(environmentDto.getDataServices()))
                .withEnableSecretEncryption(environmentDto.isEnableSecretEncryption())
                .withEnableComputeCluster(environmentDto.isEnableComputeCluster())
                .withEnvironmentType(environmentDto.getEnvironmentType() != null ? environmentDto.getEnvironmentType().toString() : null)
                .withRemoteEnvironmentCrn(environmentDto.getRemoteEnvironmentCrn())
                .withEncryptionProfileCrn(environmentDto.getEncryptionProfileCrn());

        NullUtil.doIfNotNull(environmentDto.getProxyConfig(),
                proxyConfig -> builder.withProxyConfig(proxyConfigToProxyResponseConverter.convertToView(environmentDto.getProxyConfig())));
        if (withNetwork) {
            NullUtil.doIfNotNull(environmentDto.getNetwork(),
                    network -> builder.withNetwork(networkDtoToResponse(network, environmentDto.getExperimentalFeatures().getTunnel(), false)));
        }
        if (withFreeIPA) {
            builder.withFreeIpa(freeIpaConverter.convert(environmentDto.getFreeIpaCreation()));
        }
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
                .withUserManagedIdentity(azureResourceEncryptionParametersDto.getUserManagedIdentity())
                .withEnableHostEncryption(azureResourceEncryptionParametersDto.getEnableHostEncryption())
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
        return switch (resourceGroupUsagePattern) {
            case USE_SINGLE -> ResourceGroupUsage.SINGLE;
            case USE_MULTIPLE -> ResourceGroupUsage.MULTIPLE;
            case USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT -> ResourceGroupUsage.SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT;
        };
    }

    private EnvironmentDeletionType deletionType(com.sequenceiq.environment.environment.EnvironmentDeletionType deletionType) {
        if (deletionType == null) {
            LOGGER.debug("Environment deletion type is not filled, falling back to NONE");
            return EnvironmentDeletionType.NONE;
        }
        return switch (deletionType) {
            case NONE -> EnvironmentDeletionType.NONE;
            case SIMPLE -> EnvironmentDeletionType.SIMPLE;
            case FORCE -> EnvironmentDeletionType.FORCE;
        };
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
