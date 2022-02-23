package com.sequenceiq.environment.environment.dto;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;
import static com.sequenceiq.environment.environment.EnvironmentDeletionType.NONE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.tag.AccountTagValidationFailed;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.service.AccountTagService;
import com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService;
import com.sequenceiq.environment.tags.v1.converter.AccountTagToAccountTagResponsesConverter;

@Component
public class EnvironmentDtoConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDtoConverter.class);

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap;

    private final AuthenticationDtoConverter authenticationDtoConverter;

    private final EntitlementService entitlementService;

    private final AccountTagService accountTagService;

    private final DefaultInternalAccountTagService defaultInternalAccountTagService;

    private final AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter;

    private final CostTagging costTagging;

    private final CrnUserDetailsService crnUserDetailsService;

    public EnvironmentDtoConverter(Map<CloudPlatform,
            EnvironmentNetworkConverter> environmentNetworkConverterMap,
            Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap,
            AuthenticationDtoConverter authenticationDtoConverter,
            CostTagging costTagging,
            EntitlementService entitlementService,
            DefaultInternalAccountTagService defaultInternalAccountTagService,
            AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter,
            AccountTagService accountTagService,
            CrnUserDetailsService crnUserDetailsService) {
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.environmentParamsConverterMap = environmentParamsConverterMap;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.costTagging = costTagging;
        this.entitlementService = entitlementService;
        this.accountTagService = accountTagService;
        this.defaultInternalAccountTagService = defaultInternalAccountTagService;
        this.accountTagToAccountTagResponsesConverter = accountTagToAccountTagResponsesConverter;
        this.crnUserDetailsService = crnUserDetailsService;
    }

    public EnvironmentViewDto environmentViewToViewDto(EnvironmentView environmentView) {
        EnvironmentViewDto.EnvironmentViewDtoBuilder builder = EnvironmentViewDto.builder()
                .withId(environmentView.getId())
                .withResourceCrn(environmentView.getResourceCrn())
                .withName(environmentView.getName())
                .withDescription(environmentView.getDescription())
                .withAccountId(environmentView.getAccountId())
                .withArchived(environmentView.isArchived())
                .withCloudPlatform(environmentView.getCloudPlatform())
                .withCredentialView(environmentView.getCredential())
                .withDeletionTimestamp(environmentView.getDeletionTimestamp())
                .withLocationDto(environmentToLocationDto(environmentView))
                .withRegions(environmentView.getRegionSet())
                .withTelemetry(environmentView.getTelemetry())
                .withBackup(environmentView.getBackup())
                .withEnvironmentStatus(environmentView.getStatus())
                .withCreator(environmentView.getCreator())
                .withAuthentication(authenticationDtoConverter.authenticationToDto(environmentView.getAuthentication()))
                .withFreeIpaCreation(environmentToFreeIpaCreationDto(environmentView))
                .withCreated(environmentView.getCreated())
                .withStatusReason(environmentView.getStatusReason())
                .withExperimentalFeatures(environmentView.getExperimentalFeaturesJson())
                .withTags(environmentView.getEnvironmentTags())
                .withSecurityAccess(environmentToSecurityAccessDto(environmentView.getCidr(), environmentView.getSecurityGroupIdForKnox(),
                        environmentView.getSecurityGroupIdForKnox()))
                .withAdminGroupName(environmentView.getAdminGroupName())
                .withProxyConfig(environmentView.getProxyConfig())
                .withEnvironmentDeletionType(environmentView.getDeletionType())
                .withEnvironmentServiceVersion(environmentView.getEnvironmentServiceVersion())
                .withEnvironmentDomain(environmentView.getDomain());

        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environmentView.getCloudPlatform());
        doIfNotNull(environmentView.getParameters(), parameters -> builder.withParameters(
                environmentParamsConverterMap.get(cloudPlatform).convertToDto(parameters)));
        doIfNotNull(environmentView.getNetwork(), network -> builder.withNetwork(
                environmentNetworkConverterMap.get(cloudPlatform).convertToDto(network)));
        doIfNotNull(environmentView.getParentEnvironment(), parentEnvironment -> builder
                .withParentEnvironmentCrn(parentEnvironment.getResourceCrn())
                .withParentEnvironmentName(parentEnvironment.getName())
                .withParentEnvironmentCloudPlatform(parentEnvironment.getCloudPlatform()));
        return builder.build();
    }

    public EnvironmentDto environmentToDto(Environment environment) {
        EnvironmentDto.EnvironmentDtoBuilder builder = EnvironmentDto.builder()
                .withId(environment.getId())
                .withResourceCrn(environment.getResourceCrn())
                .withName(environment.getName())
                .withDescription(environment.getDescription())
                .withAccountId(environment.getAccountId())
                .withArchived(environment.isArchived())
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(environment.getCredential())
                .withDeletionTimestamp(environment.getDeletionTimestamp())
                .withLocationDto(environmentToLocationDto(environment))
                .withRegions(environment.getRegionSet())
                .withTelemetry(environment.getTelemetry())
                .withBackup(environment.getBackup())
                .withEnvironmentStatus(environment.getStatus())
                .withCreator(environment.getCreator())
                .withAuthentication(authenticationDtoConverter.authenticationToDto(environment.getAuthentication()))
                .withFreeIpaCreation(environmentToFreeIpaCreationDto(environment))
                .withCreated(environment.getCreated())
                .withStatusReason(environment.getStatusReason())
                .withExperimentalFeatures(environment.getExperimentalFeaturesJson())
                .withTags(environment.getEnvironmentTags())
                .withSecurityAccess(environmentToSecurityAccessDto(environment.getCidr(), environment.getSecurityGroupIdForKnox(),
                        environment.getSecurityGroupIdForKnox()))
                .withAdminGroupName(environment.getAdminGroupName())
                .withProxyConfig(environment.getProxyConfig())
                .withEnvironmentDeletionType(environment.getDeletionType())
                .withEnvironmentServiceVersion(environment.getEnvironmentServiceVersion())
                .withEnvironmentDomain(environment.getDomain());

        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        doIfNotNull(environment.getParameters(), parameters -> builder.withParameters(
                environmentParamsConverterMap.get(cloudPlatform).convertToDto(parameters)));
        doIfNotNull(environment.getNetwork(), network -> builder.withNetwork(
                environmentNetworkConverterMap.get(cloudPlatform).convertToDto(network)));
        doIfNotNull(environment.getParentEnvironment(), parentEnvironment -> builder
                .withParentEnvironmentCrn(parentEnvironment.getResourceCrn())
                .withParentEnvironmentName(parentEnvironment.getName())
                .withParentEnvironmentCloudPlatform(parentEnvironment.getCloudPlatform()));
        return builder.build();
    }

    public Environment creationDtoToEnvironment(EnvironmentCreationDto creationDto) {
        Environment environment = new Environment();
        environment.setAccountId(creationDto.getAccountId());
        environment.setCreator(creationDto.getCreator());
        environment.setName(creationDto.getName());
        environment.setArchived(false);
        environment.setCloudPlatform(creationDto.getCloudPlatform());
        environment.setDescription(creationDto.getDescription());
        LocationDto location = creationDto.getLocation();
        environment.setLatitude(location.getLatitude());
        environment.setLongitude(location.getLongitude());
        environment.setLocation(location.getName());
        environment.setTelemetry(creationDto.getTelemetry());
        environment.setBackup(creationDto.getBackup());
        environment.setLocationDisplayName(location.getDisplayName());
        environment.setStatus(EnvironmentStatus.CREATION_INITIATED);
        environment.setStatusReason(null);
        environment.setCreateFreeIpa(creationDto.getFreeIpaCreation().getCreate());
        environment.setFreeIpaInstanceCountByGroup(creationDto.getFreeIpaCreation().getInstanceCountByGroup());
        environment.setFreeIpaImageCatalog(creationDto.getFreeIpaCreation().getImageCatalog());
        environment.setFreeIpaEnableMultiAz(creationDto.getFreeIpaCreation().isEnableMultiAz());
        environment.setDeletionType(NONE);
        environment.setFreeIpaImageId(creationDto.getFreeIpaCreation().getImageId());
        environment.setAdminGroupName(creationDto.getAdminGroupName());
        environment.setCreated(System.currentTimeMillis());
        environment.setTags(getTags(creationDto));
        environment.setExperimentalFeaturesJson(creationDto.getExperimentalFeatures());
        setLocation(creationDto, environment, location);
        return environment;
    }

    private void setLocation(EnvironmentCreationDto creationDto, Environment environment, LocationDto location) {
        Set<Region> regions = creationDto.getRegions().stream().map(r -> {
            Region region = new Region();
            region.setName(r);
            return region;
        }).collect(Collectors.toSet());
        environment.setRegions(regions);
        environment.setLocation(location.getName());
        environment.setLocationDisplayName(location.getDisplayName());
        environment.setLongitude(location.getLongitude());
        environment.setLatitude(location.getLatitude());
    }

    public LocationDto environmentToLocationDto(Environment environment) {
        return LocationDto.builder()
                .withName(environment.getLocation())
                .withDisplayName(environment.getLocationDisplayName())
                .withLongitude(environment.getLongitude())
                .withLatitude(environment.getLatitude())
                .build();
    }

    public LocationDto environmentToLocationDto(EnvironmentView environment) {
        return LocationDto.builder()
                .withName(environment.getLocation())
                .withDisplayName(environment.getLocationDisplayName())
                .withLongitude(environment.getLongitude())
                .withLatitude(environment.getLatitude())
                .build();
    }

    private String getUserNameFromCrn(String crn) {
        return crnUserDetailsService.loadUserByUsername(crn).getUsername();
    }

    private SecurityAccessDto environmentToSecurityAccessDto(String cidr, String securityGroupIdForKnox, String defaultSecurityGroupId) {
        return SecurityAccessDto.builder()
                .withCidr(cidr)
                .withSecurityGroupIdForKnox(securityGroupIdForKnox)
                .withDefaultSecurityGroupId(defaultSecurityGroupId)
                .build();
    }

    private Json getTags(EnvironmentCreationDto creationDto) {
        boolean internalTenant = entitlementService.internalTenant(creationDto.getAccountId());
        Map<String, String> userDefinedTags = creationDto.getTags();
        Set<AccountTag> accountTags = accountTagService.get(creationDto.getAccountId());
        List<AccountTagResponse> accountTagResponses = accountTags.stream()
                .map(accountTagToAccountTagResponsesConverter::convert)
                .collect(Collectors.toList());
        defaultInternalAccountTagService.merge(accountTagResponses);
        Map<String, String> accountTagsMap = accountTagResponses
                .stream()
                .collect(Collectors.toMap(AccountTagResponse::getKey, AccountTagResponse::getValue));
        CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder.builder()
                .withCreatorCrn(creationDto.getCreator())
                .withEnvironmentCrn(creationDto.getCrn())
                .withAccountId(creationDto.getAccountId())
                .withPlatform(creationDto.getCloudPlatform())
                .withResourceCrn(creationDto.getCrn())
                .withIsInternalTenant(internalTenant)
                .withUserName(getUserNameFromCrn(creationDto.getCreator()))
                .withAccountTags(accountTagsMap)
                .withUserDefinedTags(userDefinedTags)
                .build();

        try {
            Map<String, String> defaultTags = costTagging.prepareDefaultTags(request);
            return new Json(new EnvironmentTags(Objects.requireNonNullElseGet(userDefinedTags, HashMap::new), defaultTags));
        } catch (AccountTagValidationFailed aTVF) {
            throw new BadRequestException(aTVF.getMessage());
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert dynamic tags. " + e.getMessage(), e);
        }
    }

    private FreeIpaCreationDto environmentToFreeIpaCreationDto(Environment environment) {
        return getFreeIpaCreationDto(environment.isCreateFreeIpa(), environment.getFreeIpaInstanceCountByGroup(), environment.getFreeIpaImageCatalog(),
                environment.getFreeIpaImageId(), environment.isFreeIpaEnableMultiAz(), environment.getCloudPlatform(),
                (AwsParameters) environment.getParameters());
    }

    private FreeIpaCreationDto environmentToFreeIpaCreationDto(EnvironmentView environment) {
        return getFreeIpaCreationDto(environment.isCreateFreeIpa(), environment.getFreeIpaInstanceCountByGroup(), environment.getFreeIpaImageCatalog(),
                environment.getFreeIpaImageId(), environment.isFreeIpaEnableMultiAz(), environment.getCloudPlatform(),
                (AwsParameters) environment.getParameters());
    }

    private FreeIpaCreationDto getFreeIpaCreationDto(boolean createFreeIpa, Integer freeIpaInstanceCountByGroup, String freeIpaImageCatalog,
            String freeIpaImageId, boolean freeIpaEnableMultiAz, String cloudPlatform, AwsParameters parameters) {
        FreeIpaCreationDto.Builder builder = FreeIpaCreationDto.builder()
                .withCreate(createFreeIpa);
        Optional.ofNullable(freeIpaInstanceCountByGroup).ifPresent(builder::withInstanceCountByGroup);
        builder.withImageCatalog(freeIpaImageCatalog);
        builder.withImageId(freeIpaImageId);
        builder.withEnableMultiAz(freeIpaEnableMultiAz);
        if (cloudPlatform.equals(CloudPlatform.AWS.name())) {
            if (Objects.nonNull(parameters)) {
                builder.withAws(FreeIpaCreationAwsParametersDto.builder()
                        .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                .withPercentage(parameters.getFreeIpaSpotPercentage())
                                .withMaxPrice(parameters.getFreeIpaSpotMaxPrice())
                                .build())
                        .build());
            }
        }

        return builder.build();
    }
}
