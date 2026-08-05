package com.sequenceiq.environment.environment.dto;

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
import com.sequenceiq.cloudbreak.tag.AccountTagValidationFailed;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.UserDefinedTagValidator;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.service.AccountTagService;
import com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService;
import com.sequenceiq.environment.tags.v1.converter.AccountTagToAccountTagResponsesConverter;

@Component
public class EnvironmentTagsDtoConverter {

    private final EntitlementService entitlementService;

    private final AccountTagService accountTagService;

    private final DefaultInternalAccountTagService defaultInternalAccountTagService;

    private final AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter;

    private final CostTagging costTagging;

    private final CrnUserDetailsService crnUserDetailsService;

    private final UserDefinedTagValidator userDefinedTagValidator;

    public EnvironmentTagsDtoConverter(CostTagging costTagging,
            EntitlementService entitlementService,
            DefaultInternalAccountTagService defaultInternalAccountTagService,
            AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter,
            AccountTagService accountTagService,
            CrnUserDetailsService crnUserDetailsService,
            UserDefinedTagValidator userDefinedTagValidator) {
        this.costTagging = costTagging;
        this.entitlementService = entitlementService;
        this.accountTagService = accountTagService;
        this.defaultInternalAccountTagService = defaultInternalAccountTagService;
        this.accountTagToAccountTagResponsesConverter = accountTagToAccountTagResponsesConverter;
        this.crnUserDetailsService = crnUserDetailsService;
        this.userDefinedTagValidator = userDefinedTagValidator;
    }

    public Json getTags(EnvironmentCreationDto creationDto) {
        return getTags(creationDto.getAccountId(),
                creationDto.getCreator(),
                creationDto.getCrn(),
                creationDto.getCloudPlatform(),
                creationDto.getTags());
    }

    public ValidationResult validateUserDefinedTagsAgainstDefaultTags(EnvironmentCreationDto creationDto) {
        Map<String, String> userDefinedTags = creationDto.getTags();
        if (userDefinedTags == null || userDefinedTags.isEmpty()) {
            return ValidationResult.empty();
        }
        Map<String, String> defaultTags = resolveDefaultTags(creationDto.getAccountId(),
                creationDto.getCreator(),
                creationDto.getCrn(),
                creationDto.getCloudPlatform(),
                userDefinedTags);
        return userDefinedTagValidator.validateAgainstDefaultTags(userDefinedTags, defaultTags);
    }

    public ValidationResult validateUserDefinedTagsAgainstDefaultTags(EnvironmentEditDto editDto, EnvironmentTags environmentTags) {
        Map<String, String> userDefinedTags = editDto.getUserDefinedTags();
        if (userDefinedTags == null || userDefinedTags.isEmpty()) {
            return ValidationResult.empty();
        }
        Map<String, String> defaultTags = Optional.ofNullable(environmentTags)
                .map(EnvironmentTags::getDefaultTags)
                .orElse(Map.of());
        return userDefinedTagValidator.validateAgainstDefaultTags(userDefinedTags, defaultTags);
    }

    public Json getTags(EnvironmentEditDto editDto, EnvironmentTags environmentTags) {
        Map<String, String> existingUserDefinedTags = Optional.ofNullable(environmentTags)
                .map(EnvironmentTags::getUserDefinedTags)
                .orElse(Map.of());
        Map<String, String> mergedUserDefinedTags = mergeTags(existingUserDefinedTags, editDto.getUserDefinedTags());
        Map<String, String> existingDefaultTags = Optional.ofNullable(environmentTags)
                .map(EnvironmentTags::getDefaultTags)
                .orElse(Map.of());
        return new Json(new EnvironmentTags(mergedUserDefinedTags, new HashMap<>(existingDefaultTags)));
    }

    private Map<String, String> mergeTags(Map<String, String> existingTags, Map<String, String> newTags) {
        Map<String, String> mergedTags = existingTags != null ? new HashMap<>(existingTags) : new HashMap<>();
        mergedTags.putAll(Optional.ofNullable(newTags).orElse(Map.of()));
        return mergedTags;
    }

    private Json getTags(String accountId, String creator, String crn, String cloudPlatform, Map<String, String> userDefinedTags) {
        Map<String, String> defaultTags = resolveDefaultTags(accountId, creator, crn, cloudPlatform, userDefinedTags);
        return new Json(new EnvironmentTags(Objects.requireNonNullElseGet(userDefinedTags, HashMap::new), defaultTags));
    }

    private Map<String, String> resolveDefaultTags(String accountId, String creator, String crn, String cloudPlatform,
            Map<String, String> userDefinedTags) {
        boolean internalTenant = entitlementService.internalTenant(accountId);
        Set<AccountTag> accountTags = accountTagService.get(accountId);
        List<AccountTagResponse> accountTagResponses = accountTags.stream()
                .map(accountTagToAccountTagResponsesConverter::convert)
                .collect(Collectors.toList());
        defaultInternalAccountTagService.merge(accountTagResponses);
        Map<String, String> accountTagsMap = accountTagResponses
                .stream()
                .collect(Collectors.toMap(AccountTagResponse::getKey, AccountTagResponse::getValue));
        CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder.builder()
                .withCreatorCrn(creator)
                .withEnvironmentCrn(crn)
                .withAccountId(accountId)
                .withPlatform(cloudPlatform)
                .withResourceCrn(crn)
                .withIsInternalTenant(internalTenant)
                .withUserName(getUserNameFromCrn(creator))
                .withAccountTags(accountTagsMap)
                .withUserDefinedTags(userDefinedTags)
                .build();
        try {
            return costTagging.prepareDefaultTags(request);
        } catch (AccountTagValidationFailed aTVF) {
            throw new BadRequestException(aTVF.getMessage());
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert dynamic userDefinedTags. " + e.getMessage(), e);
        }
    }

    private String getUserNameFromCrn(String crn) {
        return crnUserDetailsService.getUmsUser(crn).getUsername();
    }

}
