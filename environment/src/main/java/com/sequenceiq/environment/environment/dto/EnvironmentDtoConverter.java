package com.sequenceiq.environment.environment.dto;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.tag.AccountTagValidationFailed;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.environment.credential.v1.converter.CredentialViewConverter;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.service.AccountTagService;

@Component
public class EnvironmentDtoConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDtoConverter.class);

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap;

    private final AuthenticationDtoConverter authenticationDtoConverter;

    private final CredentialViewConverter credentialViewConverter;

    private final EntitlementService entitlementService;

    private final AccountTagService accountTagService;

    private final CostTagging costTagging;

    public EnvironmentDtoConverter(Map<CloudPlatform,
            EnvironmentNetworkConverter> environmentNetworkConverterMap,
            Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap,
            AuthenticationDtoConverter authenticationDtoConverter,
            CredentialViewConverter credentialViewConverter,
            CostTagging costTagging,
            EntitlementService entitlementService,
            AccountTagService accountTagService) {
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.environmentParamsConverterMap = environmentParamsConverterMap;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.credentialViewConverter = credentialViewConverter;
        this.costTagging = costTagging;
        this.entitlementService = entitlementService;
        this.accountTagService = accountTagService;
    }

    public EnvironmentDto environmentToDto(Environment environment) {
        EnvironmentDto.Builder builder = EnvironmentDto.builder()
                .withId(environment.getId())
                .withResourceCrn(environment.getResourceCrn())
                .withName(environment.getName())
                .withDescription(environment.getDescription())
                .withAccountId(environment.getAccountId())
                .withArchived(environment.isArchived())
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(environment.getCredential())
                .withCredentialView(credentialViewConverter.convert(environment.getCredential()))
                .withDeletionTimestamp(environment.getDeletionTimestamp())
                .withLocationDto(environmentToLocationDto(environment))
                .withRegions(environment.getRegionSet())
                .withTelemetry(environment.getTelemetry())
                .withEnvironmentStatus(environment.getStatus())
                .withCreator(environment.getCreator())
                .withAuthentication(authenticationDtoConverter.authenticationToDto(environment.getAuthentication()))
                .withFreeIpaCreation(environmentToFreeIpaCreationDto(environment))
                .withCreated(environment.getCreated())
                .withStatusReason(environment.getStatusReason())
                .withExperimentalFeatures(environment.getExperimentalFeaturesJson())
                .withTags(environment.getEnvironmentTags())
                .withSecurityAccess(environmentToSecurityAccessDto(environment))
                .withAdminGroupName(environment.getAdminGroupName());

        doIfNotNull(environment.getParameters(), parameters -> builder.withParameters(
                environmentParamsConverterMap.get(CloudPlatform.valueOf(environment.getCloudPlatform())).convertToDto(parameters)));
        doIfNotNull(environment.getNetwork(), network -> builder.withNetwork(
                environmentNetworkConverterMap.get(CloudPlatform.valueOf(environment.getCloudPlatform())).convertToDto(network)));
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
        environment.setLatitude(creationDto.getLocation().getLatitude());
        environment.setLongitude(creationDto.getLocation().getLongitude());
        environment.setLocation(creationDto.getLocation().getName());
        environment.setTelemetry(creationDto.getTelemetry());
        environment.setLocationDisplayName(creationDto.getLocation().getDisplayName());
        environment.setStatus(EnvironmentStatus.CREATION_INITIATED);
        environment.setCreateFreeIpa(creationDto.getFreeIpaCreation().getCreate());
        environment.setFreeIpaInstanceCountByGroup(creationDto.getFreeIpaCreation().getInstanceCountByGroup());
        environment.setAdminGroupName(creationDto.getAdminGroupName());
        environment.setCreated(System.currentTimeMillis());
        environment.setTags(getTags(creationDto));
        environment.setExperimentalFeaturesJson(creationDto.getExperimentalFeatures());
        return environment;
    }

    public LocationDto environmentToLocationDto(Environment environment) {
        return LocationDto.builder()
                .withName(environment.getLocation())
                .withDisplayName(environment.getLocationDisplayName())
                .withLongitude(environment.getLongitude())
                .withLatitude(environment.getLatitude())
                .build();
    }

    private String getUserFromCrn(String crn) {
        return Optional.ofNullable(Crn.fromString(crn)).map(Crn::getUserId).orElse(null);
    }

    private SecurityAccessDto environmentToSecurityAccessDto(Environment environment) {
        return SecurityAccessDto.builder()
                .withCidr(environment.getCidr())
                .withSecurityGroupIdForKnox(environment.getSecurityGroupIdForKnox())
                .withDefaultSecurityGroupId(environment.getDefaultSecurityGroupId())
                .build();
    }

    private Json getTags(EnvironmentCreationDto creationDto) {
        boolean internalTenant = entitlementService.internalTenant(creationDto.getCreator(), creationDto.getAccountId());
        Map<String, String> userDefinedTags = creationDto.getTags();
        Map<String, String> accountTags = accountTagService.get(creationDto.getAccountId())
                .stream()
                .collect(Collectors.toMap(AccountTag::getTagKey, AccountTag::getTagValue));
        CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder.builder()
                .withCreatorCrn(creationDto.getCreator())
                .withEnvironmentCrn(creationDto.getCrn())
                .withAccountId(creationDto.getAccountId())
                .withPlatform(creationDto.getCloudPlatform())
                .withResourceCrn(creationDto.getCrn())
                .withIsInternalTenant(internalTenant)
                .withUserName(getUserFromCrn(creationDto.getCreator()))
                .withAccountTags(accountTags)
                .withUserDefinedTags(userDefinedTags)
                .build();

        try {
            Map<String, String> defaultTags = costTagging.prepareDefaultTags(request);
            return new Json(new EnvironmentTags(Objects.requireNonNullElseGet(userDefinedTags, HashMap::new), defaultTags));
        } catch (AccountTagValidationFailed aTVF) {
            throw new BadRequestException(aTVF.getMessage());
        } catch (Exception ignored) {
            throw new BadRequestException("Failed to convert dynamic tags.");
        }
    }

    private FreeIpaCreationDto environmentToFreeIpaCreationDto(Environment environment) {
        FreeIpaCreationDto.Builder builder = FreeIpaCreationDto.builder()
                .withCreate(environment.isCreateFreeIpa());
        Optional.ofNullable(environment.getFreeIpaInstanceCountByGroup()).ifPresent(builder::withInstanceCountByGroup);

        if (environment.getCloudPlatform().equals(CloudPlatform.AWS.name())) {
            AwsParameters awsParameters = (AwsParameters) environment.getParameters();
            if (Objects.nonNull(awsParameters)) {
                builder.withAws(FreeIpaCreationAwsParametersDto.builder()
                        .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                .withPercentage(awsParameters.getFreeIpaSpotPercentage())
                                .build())
                        .build());
            }
        }

        return builder.build();
    }
}
