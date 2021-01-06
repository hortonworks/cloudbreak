package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto.EnvironmentChangeCredentialDtoBuilder.anEnvironmentChangeCredentialDto;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentCrnResponse;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.TunnelConverter;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto.Builder;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupCreation;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.telemetry.service.AccountTelemetryService;

@Component
public class EnvironmentApiConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentApiConverter.class);

    private final CredentialService credentialService;

    private final TelemetryApiConverter telemetryApiConverter;

    private final AccountTelemetryService accountTelemetryService;

    private final TunnelConverter tunnelConverter;

    private final FreeIpaConverter freeIpaConverter;

    private final NetworkRequestToDtoConverter networkRequestToDtoConverter;

    public EnvironmentApiConverter(TelemetryApiConverter telemetryApiConverter,
            TunnelConverter tunnelConverter,
            AccountTelemetryService accountTelemetryService,
            CredentialService credentialService,
            FreeIpaConverter freeIpaConverter,
            NetworkRequestToDtoConverter networkRequestToDtoConverter) {
        this.telemetryApiConverter = telemetryApiConverter;
        this.accountTelemetryService = accountTelemetryService;
        this.tunnelConverter = tunnelConverter;
        this.credentialService = credentialService;
        this.freeIpaConverter = freeIpaConverter;
        this.networkRequestToDtoConverter = networkRequestToDtoConverter;
    }

    public EnvironmentCreationDto initCreationDto(EnvironmentRequest request) {
        LOGGER.debug("Creating EnvironmentCreationDto from EnvironmentRequest: {}", request);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String cloudPlatform = credentialService.getCloudPlatformByCredential(request.getCredentialName(), accountId, ENVIRONMENT);
        Builder builder = EnvironmentCreationDto.builder()
                .withAccountId(accountId)
                .withCreator(ThreadBasedUserCrnProvider.getUserCrn())
                .withName(request.getName())
                .withDescription(request.getDescription())
                .withCloudPlatform(cloudPlatform)
                .withCredential(request)
                .withCreated(System.currentTimeMillis())
                .withFreeIpaCreation(freeIpaConverter.convert(request.getFreeIpa()))
                .withLocation(locationRequestToDto(request.getLocation()))
                .withTelemetry(telemetryApiConverter.convert(request.getTelemetry(),
                        accountTelemetryService.getOrDefault(accountId).getFeatures()))
                .withRegions(locationRequestToRegions(request.getLocation(), cloudPlatform))
                .withAuthentication(authenticationRequestToDto(request.getAuthentication()))
                .withAdminGroupName(request.getAdminGroupName())
                .withTags(request.getTags())
                .withCrn(createCrn(ThreadBasedUserCrnProvider.getAccountId()))
                .withExperimentalFeatures(ExperimentalFeatures.builder()
                        .withIdBrokerMappingSource(request.getIdBrokerMappingSource())
                        .withCloudStorageValidation(request.getCloudStorageValidation())
                        .withTunnel(tunnelConverter.convert(request.getTunnel()))
                        .build())
                .withParameters(paramsToParametersDto(request, cloudPlatform))
                .withParentEnvironmentName(request.getParentEnvironmentName())
                .withProxyConfigName(request.getProxyConfigName());

        NullUtil.doIfNotNull(request.getNetwork(), network -> builder.withNetwork(networkRequestToDto(network)));
        NullUtil.doIfNotNull(request.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessRequestToDto(securityAccess)));

        // TODO temporary until CCM not really integrated
        if (request.getSecurityAccess() == null && !CloudPlatform.GCP.name().equals(cloudPlatform)) {
            SecurityAccessDto securityAccess = SecurityAccessDto.builder()
                    .withCidr("0.0.0.0/0")
                    .build();
            builder.withSecurityAccess(securityAccess);
        }
        return builder.build();
    }

    private NetworkDto networkRequestToDto(EnvironmentNetworkRequest network) {
        return networkRequestToDtoConverter.convert(network);
    }

    private String createCrn(@Nonnull String accountId) {
        return Crn.builder(CrnResourceDescriptor.ENVIRONMENT)
                .setAccountId(accountId)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    private ParametersDto paramsToParametersDto(EnvironmentRequest request, String cloudPlatform) {
        if (AWS.name().equals(cloudPlatform)) {
            return awsParamsToParametersDto(request.getAws(), Optional.ofNullable(request.getFreeIpa()).map(AttachedFreeIpaRequest::getAws).orElse(null));
        } else if (AZURE.name().equals(cloudPlatform)) {
            return azureParamsToParametersDto(request.getAzure());
        }
        return null;
    }

    private ParametersDto awsParamsToParametersDto(AwsEnvironmentParameters aws, AwsFreeIpaParameters awsFreeIpa) {
        if (Objects.isNull(aws) && Objects.isNull(awsFreeIpa)) {
            return null;
        }
        return ParametersDto.builder()
                .withAwsParameters(awsParamsToAwsParameters(aws, awsFreeIpa))
                .build();
    }

    private ParametersDto azureParamsToParametersDto(AzureEnvironmentParameters azureEnvironmentParameters) {
        if (Objects.isNull(azureEnvironmentParameters)) {
            return ParametersDto.builder()
                    .withAzureParameters(AzureParametersDto.builder()
                            .withResourceGroup(buildDefaultResourceGroupDto())
                            .build())
                    .build();
        }
        return ParametersDto.builder()
                .withAzureParameters(azureParamsToAzureParametersDto(azureEnvironmentParameters))
                .build();
    }

    private AwsParametersDto awsParamsToAwsParameters(AwsEnvironmentParameters aws, AwsFreeIpaParameters awsFreeIpa) {
        AwsParametersDto.Builder builder = AwsParametersDto.builder()
                .withDynamoDbTableName(Optional.ofNullable(aws)
                        .map(AwsEnvironmentParameters::getS3guard)
                        .map(S3GuardRequestParameters::getDynamoDbTableName)
                        .orElse(null));
        Optional.ofNullable(awsFreeIpa)
                .map(AwsFreeIpaParameters::getSpot)
                .ifPresent(awsFreeIpaSpotParameters -> {
                    builder.withFreeIpaSpotPercentage(awsFreeIpaSpotParameters.getPercentage())
                            .withFreeIpaSpotMaxPrice(awsFreeIpaSpotParameters.getMaxPrice());
                });
        return builder.build();
    }

    private AzureParametersDto azureParamsToAzureParametersDto(AzureEnvironmentParameters azureEnvironmentParameters) {
        return AzureParametersDto.builder()
                .withResourceGroup(
                        Optional.ofNullable(azureEnvironmentParameters)
                                .map(AzureEnvironmentParameters::getResourceGroup)
                                .filter(resourceGroup -> Objects.nonNull(resourceGroup.getResourceGroupUsage())
                                        || !StringUtils.isEmpty(resourceGroup.getName()))
                                .map(this::azureResourceGroupToAzureResourceGroupDto)
                                .orElse(buildDefaultResourceGroupDto())
                ).build();

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
            switch (resourceGroupUsage) {
                case SINGLE:
                    return ResourceGroupUsagePattern.USE_SINGLE;
                case MULTIPLE:
                    return ResourceGroupUsagePattern.USE_MULTIPLE;
                case SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT:
                    return ResourceGroupUsagePattern.USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT;
                default:
                    throw new RuntimeException("Unknown usage pattern: %s" + resourceGroupUsage);
            }
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

    public EnvironmentEditDto initEditDto(EnvironmentEditRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentEditDto.EnvironmentEditDtoBuilder builder = EnvironmentEditDto.builder()
                .withDescription(request.getDescription())
                .withAccountId(accountId)
                .withIdBrokerMappingSource(request.getIdBrokerMappingSource())
                .withCloudStorageValidation(request.getCloudStorageValidation())
                .withAdminGroupName(request.getAdminGroupName());
        NullUtil.doIfNotNull(request.getNetwork(), network -> builder.withNetwork(networkRequestToDto(network)));
        NullUtil.doIfNotNull(request.getAuthentication(), authentication -> builder.withAuthentication(authenticationRequestToDto(authentication)));
        NullUtil.doIfNotNull(request.getTelemetry(), telemetryRequest -> builder.withTelemetry(telemetryApiConverter.convert(request.getTelemetry(),
                accountTelemetryService.getOrDefault(accountId).getFeatures())));
        NullUtil.doIfNotNull(request.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessRequestToDto(securityAccess)));
        NullUtil.doIfNotNull(request.getAws(), awsParams -> builder.withParameters(awsParamsToParametersDto(awsParams, null)));
        return builder.build();
    }

    public EnvironmentChangeCredentialDto convertEnvironmentChangeCredentialDto(EnvironmentChangeCredentialRequest request) {
        return anEnvironmentChangeCredentialDto()
                .withCredentialName(request.getCredential() != null ? request.getCredential().getName() : request.getCredentialName())
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
        features.setClusterLogsCollection(featuresRequest.getClusterLogsCollection());
        return features;
    }
}
