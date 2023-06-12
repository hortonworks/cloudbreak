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
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto.Builder;
import com.sequenceiq.environment.environment.dto.credential.CredentialDetailsConverter;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaInstanceCountByGroupProvider;
import com.sequenceiq.environment.environment.service.recipe.EnvironmentRecipeService;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.service.AccountTagService;
import com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService;
import com.sequenceiq.environment.tags.v1.converter.AccountTagToAccountTagResponsesConverter;

@Component
public class EnvironmentDtoConverter {

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap;

    private final AuthenticationDtoConverter authenticationDtoConverter;

    private final EntitlementService entitlementService;

    private final AccountTagService accountTagService;

    private final DefaultInternalAccountTagService defaultInternalAccountTagService;

    private final AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter;

    private final CostTagging costTagging;

    private final CrnUserDetailsService crnUserDetailsService;

    private final EnvironmentRecipeService environmentRecipeService;

    private final FreeIpaInstanceCountByGroupProvider ipaInstanceCountByGroupProvider;

    private final CredentialDetailsConverter credentialDetailsConverter;

    public EnvironmentDtoConverter(Map<CloudPlatform,
            EnvironmentNetworkConverter> environmentNetworkConverterMap,
            Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap,
            AuthenticationDtoConverter authenticationDtoConverter,
            CostTagging costTagging,
            EntitlementService entitlementService,
            DefaultInternalAccountTagService defaultInternalAccountTagService,
            AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter,
            AccountTagService accountTagService,
            CrnUserDetailsService crnUserDetailsService,
            EnvironmentRecipeService environmentRecipeService,
            FreeIpaInstanceCountByGroupProvider ipaInstanceCountByGroupProvider,
            CredentialDetailsConverter credentialDetailsConverter) {
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.environmentParamsConverterMap = environmentParamsConverterMap;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.costTagging = costTagging;
        this.entitlementService = entitlementService;
        this.accountTagService = accountTagService;
        this.defaultInternalAccountTagService = defaultInternalAccountTagService;
        this.accountTagToAccountTagResponsesConverter = accountTagToAccountTagResponsesConverter;
        this.crnUserDetailsService = crnUserDetailsService;
        this.environmentRecipeService = environmentRecipeService;
        this.ipaInstanceCountByGroupProvider = ipaInstanceCountByGroupProvider;
        this.credentialDetailsConverter = credentialDetailsConverter;
    }

    public EnvironmentViewDto environmentViewToViewDto(EnvironmentView environmentView) {
        EnvironmentViewDto.Builder builder = EnvironmentViewDto.builder()
                .withId(environmentView.getId())
                .withResourceCrn(environmentView.getResourceCrn())
                .withName(environmentView.getName())
                .withOriginalName(environmentView.getOriginalName())
                .withDescription(environmentView.getDescription())
                .withAccountId(environmentView.getAccountId())
                .withArchived(environmentView.isArchived())
                .withCloudPlatform(environmentView.getCloudPlatform())
                .withCredentialView(environmentView.getCredential())
                .withDeletionTimestamp(environmentView.getDeletionTimestamp())
                .withLocationDto(environmentViewToLocationDto(environmentView))
                .withRegions(environmentView.getRegionSet())
                .withTelemetry(environmentView.getTelemetry())
                .withBackup(environmentView.getBackup())
                .withEnvironmentStatus(environmentView.getStatus())
                .withCreator(environmentView.getCreator())
                .withAuthentication(authenticationDtoConverter.authenticationToDto(environmentView.getAuthentication()))
                .withFreeIpaCreation(environmentViewToFreeIpaCreationDto(environmentView))
                .withCreated(environmentView.getCreated())
                .withStatusReason(environmentView.getStatusReason())
                .withExperimentalFeatures(environmentView.getExperimentalFeaturesJson())
                .withTags(environmentView.getEnvironmentTags())
                .withSecurityAccess(environmentToSecurityAccessDto(environmentView.getCidr(), environmentView.getSecurityGroupIdForKnox(),
                        environmentView.getDefaultSecurityGroupId()))
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
        EnvironmentDto.Builder builder = EnvironmentDto.builder()
                .withId(environment.getId())
                .withResourceCrn(environment.getResourceCrn())
                .withName(environment.getName())
                .withOriginalName(environment.getOriginalName())
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
                        environment.getDefaultSecurityGroupId()))
                .withAdminGroupName(environment.getAdminGroupName())
                .withProxyConfig(environment.getProxyConfig())
                .withEnvironmentDeletionType(environment.getDeletionType())
                .withEnvironmentServiceVersion(environment.getEnvironmentServiceVersion())
                .withEnvironmentDomain(environment.getDomain());

        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        builder.withCredentialDetails(credentialDetailsConverter.credentialToCredentialDetails(cloudPlatform, environment.getCredential()));
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
        environment.setOriginalName(creationDto.getName());
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
        environment.setCreateFreeIpa(creationDto.getFreeIpaCreation().isCreate());
        environment.setFreeIpaInstanceCountByGroup(creationDto.getFreeIpaCreation().getInstanceCountByGroup());
        environment.setFreeIpaInstanceType(creationDto.getFreeIpaCreation().getInstanceType());
        environment.setFreeIpaImageCatalog(creationDto.getFreeIpaCreation().getImageCatalog());
        environment.setFreeIpaEnableMultiAz(creationDto.getFreeIpaCreation().isEnableMultiAz());
        environment.setDeletionType(NONE);
        environment.setFreeIpaImageId(creationDto.getFreeIpaCreation().getImageId());
        environment.setFreeIpaImageOs(creationDto.getFreeIpaCreation().getImageOs());
        environment.setAdminGroupName(creationDto.getAdminGroupName());
        environment.setCreated(System.currentTimeMillis());
        environment.setTags(getTags(creationDto));
        environment.setExperimentalFeaturesJson(creationDto.getExperimentalFeatures());
        setRegions(creationDto, environment);
        return environment;
    }

    public NetworkDto networkToNetworkDto(Environment environment) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        NetworkDto result = null;
        if (environment.getNetwork() != null) {
            result = environmentNetworkConverterMap.get(cloudPlatform).convertToDto(environment.getNetwork());
        }
        return result;
    }

    private void setRegions(EnvironmentCreationDto creationDto, Environment environment) {
        Set<Region> regions = creationDto.getRegions().stream().map(r -> {
            Region region = new Region();
            region.setName(r);
            return region;
        }).collect(Collectors.toSet());
        environment.setRegions(regions);
    }

    public LocationDto environmentToLocationDto(Environment environment) {
        return LocationDto.builder()
                .withName(environment.getLocation())
                .withDisplayName(environment.getLocationDisplayName())
                .withLongitude(environment.getLongitude())
                .withLatitude(environment.getLatitude())
                .build();
    }

    private LocationDto environmentViewToLocationDto(EnvironmentView environment) {
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
        Set<String> recipesForEnvironment = environmentRecipeService.getRecipes(environment.getId());
        FreeIpaCreationDto freeIpaCreationDto = getFreeIpaCreationDto(environment.isCreateFreeIpa(), environment.getFreeIpaInstanceCountByGroup(),
                environment.getFreeIpaInstanceType(), environment.getFreeIpaImageCatalog(), environment.getFreeIpaImageId(),
                environment.getFreeIpaImageOs(), environment.isFreeIpaEnableMultiAz());
        freeIpaCreationDto.setRecipes(recipesForEnvironment);
        freeIpaCreationDto.setAws(getFreeIpaAwsParameters(environment.getCloudPlatform(), environment.getParameters()));
        return freeIpaCreationDto;
    }

    private FreeIpaCreationDto environmentViewToFreeIpaCreationDto(EnvironmentView environment) {
        FreeIpaCreationDto freeIpaCreationDto = getFreeIpaCreationDto(environment.isCreateFreeIpa(), environment.getFreeIpaInstanceCountByGroup(),
                environment.getFreeIpaInstanceType(), environment.getFreeIpaImageCatalog(), environment.getFreeIpaImageId(),
                environment.getFreeIpaImageOs(), environment.isFreeIpaEnableMultiAz());
        freeIpaCreationDto.setRecipes(environment.getFreeipaRecipes());
        freeIpaCreationDto.setAws(getFreeIpaAwsParameters(environment.getCloudPlatform(), environment.getParameters()));
        return freeIpaCreationDto;
    }

    private FreeIpaCreationAwsParametersDto getFreeIpaAwsParameters(String cloudPlatform, BaseParameters parameters) {
        if (cloudPlatform.equals(CloudPlatform.AWS.name()) && Objects.nonNull(parameters)) {
            AwsParameters awsParameters = (AwsParameters) parameters;
            return FreeIpaCreationAwsParametersDto.builder()
                    .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                            .withPercentage(awsParameters.getFreeIpaSpotPercentage())
                            .withMaxPrice(awsParameters.getFreeIpaSpotMaxPrice())
                            .build())
                    .build();
        } else {
            return null;
        }
    }

    private FreeIpaCreationDto getFreeIpaCreationDto(boolean createFreeIpa, Integer freeIpaInstanceCountByGroup, String freeIpaInstanceType,
            String freeIpaImageCatalog, String freeIpaImageId, String freeIpaImageOs, boolean freeIpaEnableMultiAz) {
        Integer ipaInstanceCountByGroup = Optional.ofNullable(freeIpaInstanceCountByGroup).orElse(ipaInstanceCountByGroupProvider.getDefaultInstanceCount());
        Builder builder = FreeIpaCreationDto.builder(ipaInstanceCountByGroup)
                .withCreate(createFreeIpa);
        builder.withInstanceType(freeIpaInstanceType);
        builder.withImageCatalog(freeIpaImageCatalog);
        builder.withImageId(freeIpaImageId);
        builder.withImageOs(freeIpaImageOs);
        builder.withEnableMultiAz(freeIpaEnableMultiAz);
        return builder.build();
    }

}
