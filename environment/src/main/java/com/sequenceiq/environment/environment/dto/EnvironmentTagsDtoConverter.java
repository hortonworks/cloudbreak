package com.sequenceiq.environment.environment.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.tag.AccountTagValidationFailed;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
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

    public EnvironmentTagsDtoConverter(CostTagging costTagging,
            EntitlementService entitlementService,
            DefaultInternalAccountTagService defaultInternalAccountTagService,
            AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter,
            AccountTagService accountTagService,
            CrnUserDetailsService crnUserDetailsService) {
        this.costTagging = costTagging;
        this.entitlementService = entitlementService;
        this.accountTagService = accountTagService;
        this.defaultInternalAccountTagService = defaultInternalAccountTagService;
        this.accountTagToAccountTagResponsesConverter = accountTagToAccountTagResponsesConverter;
        this.crnUserDetailsService = crnUserDetailsService;
    }

    public Json getTags(EnvironmentCreationDto creationDto) {
        return getTags(creationDto.getAccountId(),
                creationDto.getCreator(),
                creationDto.getCrn(),
                creationDto.getCloudPlatform(),
                creationDto.getTags());
    }

    public Json getTags(EnvironmentEditDto editDto) {
        return getTags(editDto.getAccountId(),
                editDto.getCreator(),
                editDto.getCrn(),
                editDto.getCloudPlatform(),
                editDto.getUserDefinedTags());
    }

    private Json getTags(String accountId, String creator, String crn, String cloudPlatform, Map<String, String> userDefinedTags) {
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
            Map<String, String> defaultTags = costTagging.prepareDefaultTags(request);
            return new Json(new EnvironmentTags(Objects.requireNonNullElseGet(userDefinedTags, HashMap::new), defaultTags));
        } catch (AccountTagValidationFailed aTVF) {
            throw new BadRequestException(aTVF.getMessage());
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert dynamic userDefinedTags. " + e.getMessage(), e);
        }
    }

    private String getUserNameFromCrn(String crn) {
        return crnUserDetailsService.loadUserByUsername(crn).getUsername();
    }

}
