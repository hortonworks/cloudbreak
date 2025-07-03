package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.cloudbreak.common.request.CreatorClientConstants.CALLER_ID_NOT_FOUND;
import static com.sequenceiq.cloudbreak.common.request.CreatorClientConstants.CDP_CALLER_ID_HEADER;
import static com.sequenceiq.cloudbreak.common.request.CreatorClientConstants.USER_AGENT_HEADER;
import static com.sequenceiq.cloudbreak.common.request.HeaderValueProvider.getHeaderOrItsFallbackValueOrDefault;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.CidrUtil;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.api.v1.environment.model.AzureExternalizedComputeParams;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentLoadBalancerUpdateRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.ExternalizedComputeCreateRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.HybridEnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.UpdateAzureResourceEncryptionParametersRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentCrnResponse;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.TunnelConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.EnvironmentHybridDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.UpdateAzureResourceEncryptionDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
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
import com.sequenceiq.environment.proxy.v1.converter.ProxyRequestToProxyConfigConverter;
import com.sequenceiq.environment.telemetry.service.AccountTelemetryService;

@Component
public class EnvironmentApiConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentApiConverter.class);

    private static final String DEFAULT_CIDR = "0.0.0.0/0";

    private final CredentialService credentialService;

    private final TelemetryApiConverter telemetryApiConverter;

    private final BackupConverter backupConverter;

    private final AccountTelemetryService accountTelemetryService;

    private final TunnelConverter tunnelConverter;

    private final FreeIpaConverter freeIpaConverter;

    private final NetworkRequestToDtoConverter networkRequestToDtoConverter;

    private final ProxyRequestToProxyConfigConverter proxyRequestToProxyConfigConverter;

    private final RegionAwareCrnGenerator regionAwareCrnGenerator;

    private final DataServicesConverter dataServicesConverter;

    private final ExternalizedComputeService externalizedComputeService;

    public EnvironmentApiConverter(TelemetryApiConverter telemetryApiConverter,
            BackupConverter backupConverter,
            TunnelConverter tunnelConverter,
            AccountTelemetryService accountTelemetryService,
            CredentialService credentialService,
            FreeIpaConverter freeIpaConverter,
            NetworkRequestToDtoConverter networkRequestToDtoConverter,
            ProxyRequestToProxyConfigConverter proxyRequestToProxyConfigConverter,
            RegionAwareCrnGenerator regionAwareCrnGenerator,
            DataServicesConverter dataServicesConverter,
            ExternalizedComputeService externalizedComputeService) {
        this.backupConverter = backupConverter;
        this.telemetryApiConverter = telemetryApiConverter;
        this.accountTelemetryService = accountTelemetryService;
        this.tunnelConverter = tunnelConverter;
        this.credentialService = credentialService;
        this.freeIpaConverter = freeIpaConverter;
        this.networkRequestToDtoConverter = networkRequestToDtoConverter;
        this.proxyRequestToProxyConfigConverter = proxyRequestToProxyConfigConverter;
        this.regionAwareCrnGenerator = regionAwareCrnGenerator;
        this.dataServicesConverter = dataServicesConverter;
        this.externalizedComputeService = externalizedComputeService;
    }

    public EnvironmentCreationDto initCreationDto(EnvironmentRequest request) {
        LOGGER.debug("Creating EnvironmentCreationDto from EnvironmentRequest: {}", request);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String cloudPlatform = credentialService.getCloudPlatformByCredential(request.getCredentialName(), accountId, ENVIRONMENT);
        EnvironmentType environmentType = null;
        if (request.getEnvironmentType() != null) {
            try {
                environmentType = EnvironmentType.valueOf(request.getEnvironmentType());
            } catch (IllegalArgumentException ie) {
                throw new BadRequestException(String.format("%s is not a valid value for Environment Type", request.getEnvironmentType()));
            }
        }
        EnvironmentCreationDto.Builder builder = EnvironmentCreationDto.builder()
                .withAccountId(accountId)
                .withCreator(ThreadBasedUserCrnProvider.getUserCrn())
                .withName(request.getName())
                .withDescription(request.getDescription())
                .withCloudPlatform(cloudPlatform)
                .withCredential(request)
                .withCreated(System.currentTimeMillis())
                .withFreeIpaCreation(freeIpaConverter.convert(request.getFreeIpa(), accountId, cloudPlatform))
                .withExternalizedComputeCluster(requestToExternalizedComputeClusterDto(request.getExternalizedComputeCreateRequest(), accountId))
                .withLocation(locationRequestToDto(request.getLocation()))
                .withTelemetry(telemetryApiConverter.convert(request.getTelemetry(),
                        accountTelemetryService.getOrDefault(accountId), accountId))
                .withBackup(request.getBackup() != null && isNotEmpty(request.getBackup().getStorageLocation()) ?
                        backupConverter.convert(request.getBackup()) : backupConverter.convert(request.getTelemetry()))
                .withRegions(locationRequestToRegions(request.getLocation(), cloudPlatform))
                .withAuthentication(authenticationRequestToDto(request.getAuthentication()))
                .withAdminGroupName(request.getAdminGroupName())
                .withTags(request.getTags())
                .withCrn(createCrn(ThreadBasedUserCrnProvider.getAccountId()))
                .withExperimentalFeatures(ExperimentalFeatures.builder()
                        .withIdBrokerMappingSource(request.getIdBrokerMappingSource())
                        .withCloudStorageValidation(request.getCloudStorageValidation())
                        .withTunnel(tunnelConverter.convert(request.getTunnel()))
                        .withOverrideTunnel(request.getOverrideTunnel())
                        .withCcmV2TlsType(request.getCcmV2TlsType())
                        .build())
                .withParameters(paramsToParametersDto(request, cloudPlatform))
                .withParentEnvironmentName(request.getParentEnvironmentName())
                .withProxyConfigName(request.getProxyConfigName())
                .withDataServices(dataServicesConverter.convertToDto(request.getDataServices()))
                .withCreatorClient(getHeaderOrItsFallbackValueOrDefault(USER_AGENT_HEADER, CDP_CALLER_ID_HEADER, CALLER_ID_NOT_FOUND))
                .withEnvironmentType(environmentType);

        NullUtil.doIfNotNull(request.getNetwork(), network -> {
            NetworkDto networkDto = networkRequestToDto(network);
            networkRequestToDtoConverter.setDefaultAvailabilityZonesIfNeeded(networkDto);
            builder.withNetwork(networkDto);
        });
        NullUtil.doIfNotNull(request.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessRequestToDto(securityAccess)));

        // TODO temporary until CCM not really integrated
        if (request.getSecurityAccess() == null && !CloudPlatform.GCP.name().equals(cloudPlatform)) {
            SecurityAccessDto securityAccess = SecurityAccessDto.builder()
                    .withCidr(DEFAULT_CIDR)
                    .build();
            builder.withSecurityAccess(securityAccess);
        }

        return builder.build();
    }

    public ExternalizedComputeClusterDto requestToExternalizedComputeClusterDto(ExternalizedComputeCreateRequest externalizedCompute, String accountId) {
        ExternalizedComputeClusterDto.Builder builder = ExternalizedComputeClusterDto.builder();
        if (externalizedCompute != null && externalizedCompute.isCreate()) {
            externalizedComputeService.externalizedComputeValidation(accountId);
            builder.withCreate(externalizedCompute.isCreate())
                    .withPrivateCluster(externalizedCompute.isPrivateCluster());
            AzureExternalizedComputeParams azure = externalizedCompute.getAzure();
            if (azure != null && StringUtils.hasText(azure.getOutboundType())) {
                validateAzureExternalizedComputeParams(azure);
                builder.withOutboundType(azure.getOutboundType().toLowerCase());
            } else if (StringUtils.hasText(externalizedCompute.getOutboundType())) {
                //TODO: this branch needs to be removed after the next cdpcli release
                builder.withOutboundType(externalizedCompute.getOutboundType().toLowerCase());
            }
            if (StringUtils.hasText(externalizedCompute.getKubeApiAuthorizedIpRanges())) {
                builder.withKubeApiAuthorizedIpRanges(CidrUtil.cidrSet(externalizedCompute.getKubeApiAuthorizedIpRanges()));
            }
            builder.withWorkerNodeSubnetIds(externalizedCompute.getWorkerNodeSubnetIds());
        }
        return builder.build();
    }

    private NetworkDto networkRequestToDto(EnvironmentNetworkRequest network) {
        return networkRequestToDtoConverter.convert(network);
    }

    private String createCrn(@Nonnull String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.ENVIRONMENT, accountId);
    }

    private void validateAzureExternalizedComputeParams(AzureExternalizedComputeParams params) {
        if (!"udr".equalsIgnoreCase(params.getOutboundType())) {
                throw new BadRequestException(String.format("Azure Outbound type '%s' is not supported", params.getOutboundType()));
        }
    }

    private ParametersDto paramsToParametersDto(EnvironmentRequest request, String cloudPlatform) {
        if (AWS.name().equals(cloudPlatform)) {
            return awsParamsToParametersDto(request.getAws(), Optional.ofNullable(request.getFreeIpa()).map(AttachedFreeIpaRequest::getAws).orElse(null));
        } else if (AZURE.name().equals(cloudPlatform)) {
            return azureParamsToParametersDto(request.getAzure());
        } else if (GCP.name().equals(cloudPlatform)) {
            return gcpParamsToParametersDto(request.getGcp());
        }
        return null;
    }

    private ParametersDto awsParamsToParametersDto(AwsEnvironmentParameters aws, AwsFreeIpaParameters awsFreeIpa) {
        if (Objects.isNull(aws) && Objects.isNull(awsFreeIpa)) {
            return null;
        }
        return ParametersDto.builder()
                .withAwsParametersDto(awsParamsToAwsParameters(aws, awsFreeIpa))
                .build();
    }

    private ParametersDto azureParamsToParametersDto(AzureEnvironmentParameters azureEnvironmentParameters) {
        if (Objects.isNull(azureEnvironmentParameters)) {
            return ParametersDto.builder()
                    .withAzureParametersDto(AzureParametersDto.builder()
                            .withAzureResourceGroupDto(buildDefaultResourceGroupDto())
                            .build())
                    .build();
        }
        return ParametersDto.builder()
                .withAzureParametersDto(azureParamsToAzureParametersDto(azureEnvironmentParameters))
                .build();
    }

    private ParametersDto gcpParamsToParametersDto(GcpEnvironmentParameters gcpEnvironmentParameters) {
        if (Objects.isNull(gcpEnvironmentParameters)) {
            return ParametersDto.builder()
                    .withGcpParametersDto(GcpParametersDto.builder()
                            .build())
                    .build();
        }
        return ParametersDto.builder()
                .withGcpParametersDto(gcpParamsToGcpParametersDto(gcpEnvironmentParameters))
                .build();
    }

    private AwsParametersDto awsParamsToAwsParameters(AwsEnvironmentParameters aws, AwsFreeIpaParameters awsFreeIpa) {
        AwsParametersDto.Builder builder = AwsParametersDto.builder()
                .withAwsDiskEncryptionParametersDto(Optional.ofNullable(aws)
                        .map(AwsEnvironmentParameters::getAwsDiskEncryptionParameters)
                        .filter(awsDiskEncryptionParameters -> Objects.nonNull(awsDiskEncryptionParameters.getEncryptionKeyArn()))
                        .map(this::awsDiskEncryptionParametersToAwsDiskEncryptionParametersDto)
                        .orElse(null));
        Optional.ofNullable(awsFreeIpa)
                .map(AwsFreeIpaParameters::getSpot)
                .ifPresent(awsFreeIpaSpotParameters -> {
                    builder.withFreeIpaSpotPercentage(awsFreeIpaSpotParameters.getPercentage())
                            .withFreeIpaSpotMaxPrice(awsFreeIpaSpotParameters.getMaxPrice());
                });

        return builder.build();
    }

    private AwsDiskEncryptionParametersDto awsDiskEncryptionParametersToAwsDiskEncryptionParametersDto(
            AwsDiskEncryptionParameters awsDiskEncryptionParameters) {
        AwsDiskEncryptionParametersDto.Builder awsDiskEncryptionParametersDto = AwsDiskEncryptionParametersDto.builder()
                .withEncryptionKeyArn(Optional.ofNullable(awsDiskEncryptionParameters)
                        .map(AwsDiskEncryptionParameters::getEncryptionKeyArn)
                        .orElse(null));
        return awsDiskEncryptionParametersDto.build();
    }

    private AzureParametersDto azureParamsToAzureParametersDto(AzureEnvironmentParameters azureEnvironmentParameters) {
        return AzureParametersDto.builder()
                .withAzureResourceGroupDto(
                        Optional.ofNullable(azureEnvironmentParameters)
                                .map(AzureEnvironmentParameters::getResourceGroup)
                                .filter(resourceGroup -> Objects.nonNull(resourceGroup.getResourceGroupUsage())
                                        || !StringUtils.isEmpty(resourceGroup.getName()))
                                .map(this::azureResourceGroupToAzureResourceGroupDto)
                                .orElse(buildDefaultResourceGroupDto())
                )
                .withAzureResourceEncryptionParametersDto(
                        Optional.ofNullable(azureEnvironmentParameters)
                                .map(AzureEnvironmentParameters::getResourceEncryptionParameters)
                                .map(this::azureResourceEncryptionParametersToAzureEncryptionParametersDto)
                                .orElse(null)
                )
                .build();
    }

    private GcpParametersDto gcpParamsToGcpParametersDto(GcpEnvironmentParameters gcpEnvironmentParameters) {
        return GcpParametersDto.builder()
                .withGcpResourceEncryptionParametersDto(
                        Optional.ofNullable(gcpEnvironmentParameters)
                                .map(GcpEnvironmentParameters::getGcpResourceEncryptionParameters)
                                .filter(gcpResourceEncryptionParameters -> Objects.nonNull(gcpResourceEncryptionParameters.getEncryptionKey()))
                                .map(this::gcpResourceEncryptionParametersToGcpEncryptionParametersDto)
                                .orElse(null)
                )
                .build();
    }

    private AzureResourceEncryptionParametersDto azureResourceEncryptionParametersToAzureEncryptionParametersDto(
            AzureResourceEncryptionParameters azureResourceEncryptionParameters) {
        AzureResourceEncryptionParametersDto.Builder azureResourceEncryptionParametersDto =
                AzureResourceEncryptionParametersDto.builder()
                        .withEnableHostEncryption(azureResourceEncryptionParameters.isEnableHostEncryption());
        if (Objects.nonNull(azureResourceEncryptionParameters.getEncryptionKeyUrl())) {
            azureResourceEncryptionParametersDto
                    .withEncryptionKeyUrl(azureResourceEncryptionParameters.getEncryptionKeyUrl())
                    .withEncryptionKeyResourceGroupName(azureResourceEncryptionParameters.getEncryptionKeyResourceGroupName());
        }
        if (Objects.nonNull(azureResourceEncryptionParameters.getUserManagedIdentity())) {
            azureResourceEncryptionParametersDto
                    .withUserManagedIdentity(azureResourceEncryptionParameters.getUserManagedIdentity());
        }
        return azureResourceEncryptionParametersDto.build();
    }

    private GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersToGcpEncryptionParametersDto(
            GcpResourceEncryptionParameters gcpResourceEncryptionParameters) {
        GcpResourceEncryptionParametersDto.Builder gcpResourceEncryptionParametersDto = GcpResourceEncryptionParametersDto.builder()
                .withEncryptionKey(gcpResourceEncryptionParameters.getEncryptionKey());
        return gcpResourceEncryptionParametersDto.build();
    }

    private AzureResourceGroupDto buildDefaultResourceGroupDto() {
        return AzureResourceGroupDto.builder()
                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE)
                .build();
    }

    private AzureResourceGroupDto azureResourceGroupToAzureResourceGroupDto(AzureResourceGroup azureResourceGroup) {
        return AzureResourceGroupDto.builder()
                .withName(azureResourceGroup.getName())
                .withResourceGroupUsagePattern(resourceGroupUsageToResourceGroupUsagePattern(azureResourceGroup.getResourceGroupUsage()))
                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                .build();
    }

    private ResourceGroupUsagePattern resourceGroupUsageToResourceGroupUsagePattern(ResourceGroupUsage resourceGroupUsage) {
        if (Objects.nonNull(resourceGroupUsage)) {
            return switch (resourceGroupUsage) {
                case SINGLE -> ResourceGroupUsagePattern.USE_SINGLE;
                case MULTIPLE -> ResourceGroupUsagePattern.USE_MULTIPLE;
                case SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT -> ResourceGroupUsagePattern.USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT;
            };
        } else {
            return null;
        }
    }

    private LocationDto locationRequestToDto(LocationRequest location) {
        return LocationDto.builder()
                .withName(location.getName())
                .withLatitude(location.getLatitude())
                .withLongitude(location.getLongitude())
                .withDisplayName(location.getName())
                .build();
    }

    private Set<String> locationRequestToRegions(LocationRequest location, String cloudPlatform) {
        if (CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform)) {
            return Set.of();
        }
        return Set.of(location.getName());
    }

    private AuthenticationDto authenticationRequestToDto(EnvironmentAuthenticationRequest authentication) {
        AuthenticationDto.Builder builder = AuthenticationDto.builder();
        if (authentication != null && (StringUtils.hasText(authentication.getPublicKey()) || StringUtils.hasText(authentication.getPublicKeyId()))) {
            String publicKey = getPublicKey(authentication);
            builder.withLoginUserName(authentication.getLoginUserName())
                    .withPublicKey(publicKey)
                    .withPublicKeyId(nullIfEmpty(authentication.getPublicKeyId()))
                    .withManagedKey(Objects.nonNull(publicKey));
        }
        return builder.build();
    }

    private String getPublicKey(EnvironmentAuthenticationRequest authentication) {
        String publicKey = nullIfEmpty(authentication.getPublicKey());
        return Optional.ofNullable(publicKey).map(key -> key.replaceAll("\n", "")).map(String::trim).orElse(null);
    }

    private String nullIfEmpty(String value) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        return null;
    }

    private SecurityAccessDto securityAccessRequestToDto(SecurityAccessRequest securityAccess) {
        return SecurityAccessDto.builder()
                .withCidr(securityAccess.getCidr())
                .withSecurityGroupIdForKnox(securityAccess.getSecurityGroupIdForKnox())
                .withDefaultSecurityGroupId(securityAccess.getDefaultSecurityGroupId())
                .build();
    }

    private EnvironmentHybridDto hybridEnvironmentRequest(HybridEnvironmentRequest hybridEnvironmentRequest) {
        return EnvironmentHybridDto.builder()
                .withRemoteEnvironmentCrn(hybridEnvironmentRequest.getRemoteEnvironmentCrn())
                .build();
    }

    public EnvironmentEditDto initEditDto(Environment currentEnvironmentFromDb, EnvironmentEditRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentEditDto.Builder builder = EnvironmentEditDto.builder()
                .withDescription(request.getDescription())
                .withAccountId(accountId)
                .withUserDefinedTags(request.getTags())
                .withCreator(currentEnvironmentFromDb.getCreator())
                .withCloudPlatform(currentEnvironmentFromDb.getCloudPlatform())
                .withCrn(currentEnvironmentFromDb.getResourceCrn())
                .withIdBrokerMappingSource(request.getIdBrokerMappingSource())
                .withCloudStorageValidation(request.getCloudStorageValidation())
                .withAdminGroupName(request.getAdminGroupName())
                .withFreeipaNodeCount(request.getFreeIpaNodeCount());
        NullUtil.doIfNotNull(request.getNetwork(), network -> builder.withNetwork(networkRequestToDto(network)));
        NullUtil.doIfNotNull(request.getAuthentication(), authentication -> builder.withAuthentication(authenticationRequestToDto(authentication)));
        NullUtil.doIfNotNull(request.getTelemetry(), telemetryRequest -> builder.withTelemetry(telemetryApiConverter.convertForEdit(
                currentEnvironmentFromDb.getTelemetry(), request.getTelemetry(), accountTelemetryService.getOrDefault(accountId), accountId)));
        NullUtil.doIfNotNull(request.getBackup(), backupRequest -> builder.withBackup(backupConverter.convert(request.getBackup())));
        NullUtil.doIfNotNull(request.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessRequestToDto(securityAccess)));
        NullUtil.doIfNotNull(request.getHybridEnvironment(), hybridEnvironmentRequest ->
                builder.withHybridEnvironment(hybridEnvironmentRequest(hybridEnvironmentRequest)));
        NullUtil.doIfNotNull(request.getAws(), awsParams -> builder.withParameters(awsParamsToParametersDto(awsParams, null)));
        NullUtil.doIfNotNull(request.getAzure(), azureParams -> builder.withParameters(azureParamsToParametersDto(azureParams)));
        NullUtil.doIfNotNull(request.getProxy(), proxyRequest -> builder.withProxyConfig(proxyRequestToProxyConfigConverter.convert(proxyRequest)));
        NullUtil.doIfNotNull(request.getDataServices(), dataServices -> builder.withDataServices(dataServicesConverter.convertToDto(dataServices)));
        return builder.build();
    }

    public EnvironmentChangeCredentialDto convertEnvironmentChangeCredentialDto(EnvironmentChangeCredentialRequest request) {
        return EnvironmentChangeCredentialDto.builder()
                .withCredentialName(request.getCredential() != null ? request.getCredential().getName() : request.getCredentialName())
                .build();
    }

    public UpdateAzureResourceEncryptionDto convertUpdateAzureResourceEncryptionDto(UpdateAzureResourceEncryptionParametersRequest request) {
        return UpdateAzureResourceEncryptionDto.builder()
                .withAzureResourceEncryptionParametersDto(
                        azureResourceEncryptionParametersToAzureEncryptionParametersDto(request.getAzureResourceEncryptionParameters()))
                .build();
    }

    public EnvironmentCrnResponse crnResponse(String environmentName, String crn) {
        EnvironmentCrnResponse response = new EnvironmentCrnResponse();
        response.setEnvironmentName(environmentName);
        response.setEnvironmentCrn(crn);
        return response;
    }

    public EnvironmentFeatures convertToEnvironmentTelemetryFeatures(FeaturesRequest featuresRequest) {
        EnvironmentFeatures features = new EnvironmentFeatures();
        features.setWorkloadAnalytics(featuresRequest.getWorkloadAnalytics());
        features.setCloudStorageLogging(featuresRequest.getCloudStorageLogging());
        return features;
    }

    public EnvironmentLoadBalancerDto initLoadBalancerDto(EnvironmentLoadBalancerUpdateRequest request) {
        return EnvironmentLoadBalancerDto.builder()
                .withEndpointAccessGateway(request.getPublicEndpointAccessGateway())
                .withEndpointGatewaySubnetIds(request.getSubnetIds())
                .build();
    }
}
