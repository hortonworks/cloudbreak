package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.yarn.YarnEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
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
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.dto.AzureParametersDto;
import com.sequenceiq.environment.parameters.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyResponseConverter;

@Component
public class EnvironmentResponseConverter {

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    private final RegionConverter regionConverter;

    private final CredentialViewConverter credentialViewConverter;

    private final ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter;

    private final FreeIpaConverter freeIpaConverter;

    private final TelemetryApiConverter telemetryApiConverter;

    private final NetworkDtoToResponseConverter networkDtoToResponseConverter;

    public EnvironmentResponseConverter(CredentialToCredentialV1ResponseConverter credentialConverter,
            RegionConverter regionConverter, CredentialViewConverter credentialViewConverter,
            ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter,
            FreeIpaConverter freeIpaConverter, TelemetryApiConverter telemetryApiConverter,
            NetworkDtoToResponseConverter networkDtoToResponseConverter) {
        this.credentialConverter = credentialConverter;
        this.regionConverter = regionConverter;
        this.credentialViewConverter = credentialViewConverter;
        this.proxyConfigToProxyResponseConverter = proxyConfigToProxyResponseConverter;
        this.freeIpaConverter = freeIpaConverter;
        this.telemetryApiConverter = telemetryApiConverter;
        this.networkDtoToResponseConverter = networkDtoToResponseConverter;
    }

    public DetailedEnvironmentResponse dtoToDetailedResponse(EnvironmentDto environmentDto) {
        DetailedEnvironmentResponse.Builder builder = DetailedEnvironmentResponse.Builder.builder()
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
                .withParentEnvironmentCloudPlatform(environmentDto.getParentEnvironmentCloudPlatform());

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
                .withRegions(regionConverter.convertRegions(environmentDto.getRegions()))
                .withAws(getIfNotNull(environmentDto.getParameters(), this::awsEnvParamsToAwsEnvironmentParams))
                .withAzure(getIfNotNull(environmentDto.getParameters(), this::azureEnvParamsToAzureEnvironmentParams))
                .withYarn(getIfNotNull(environmentDto.getParameters(), this::yarnEnvParamsToYarnEnvironmentParams))
                .withGcp(getIfNotNull(environmentDto.getParameters(), this::gcpEnvParamsToGcpEnvironmentParams))
                .withParentEnvironmentName(environmentDto.getParentEnvironmentName());

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
                .withDefaultSecurityGroupId(securityAccess.getDefaultSecurityGroupId())
                .build();
    }

    private AwsEnvironmentParameters awsEnvParamsToAwsEnvironmentParams(ParametersDto parameters) {
        return Optional.ofNullable(parameters.getAwsParametersDto())
                .map(aws -> AwsEnvironmentParameters.builder()
                        .withS3guard(getIfNotNull(aws, this::awsParametersToS3guardParam))
                        .build())
                .orElse(null);
    }

    private AzureEnvironmentParameters azureEnvParamsToAzureEnvironmentParams(ParametersDto parameters) {
        return Optional.ofNullable(parameters.getAzureParametersDto())
                .map(AzureParametersDto::getAzureResourceGroupDto)
                .filter(resourceGroupDto -> Objects.nonNull(resourceGroupDto.getResourceGroupUsagePattern()))
                .filter(resourceGroupDto -> Objects.nonNull(resourceGroupDto.getResourceGroupCreation()))
                .map(resourceGroupDto -> AzureEnvironmentParameters.builder()
                        .withAzureResourceGroup(getIfNotNull(resourceGroupDto, this::azureParametersToAzureResourceGroup))
                        .build())
                .orElse(null);
    }

    private YarnEnvironmentParameters yarnEnvParamsToYarnEnvironmentParams(ParametersDto parameters) {
        return Optional.ofNullable(parameters.getYarnParametersDto())
                .map(yarn -> YarnEnvironmentParameters.builder()
                        .build())
                .orElse(null);
    }

    private GcpEnvironmentParameters gcpEnvParamsToGcpEnvironmentParams(ParametersDto parameters) {
        return Optional.ofNullable(parameters.getGcpParametersDto())
                .map(yarn -> GcpEnvironmentParameters.builder()
                        .build())
                .orElse(null);
    }

    private S3GuardRequestParameters awsParametersToS3guardParam(AwsParametersDto awsParametersDto) {
        return S3GuardRequestParameters.builder()
                .withDynamoDbTableName(awsParametersDto.getS3GuardTableName())
                .build();
    }

    private AzureResourceGroup azureParametersToAzureResourceGroup(AzureResourceGroupDto azureResourceGroupDto) {
        return AzureResourceGroup.builder()
                .withName(azureResourceGroupDto.getName())
                .withResourceGroupUsage(resourceGroupUsagePatternToResourceGroupUsage(azureResourceGroupDto.getResourceGroupUsagePattern()))
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
                    throw new RuntimeException("Unknown resource group usage pattern: %s" + resourceGroupUsagePattern);
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
