package com.sequenceiq.environment.environment.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.service.AccountTagService;
import com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService;
import com.sequenceiq.environment.tags.v1.converter.AccountTagToAccountTagResponsesConverter;

@Service
public class EnvironmentTagProvider {

    private final EntitlementService entitlementService;

    private final AccountTagService accountTagService;

    private final DefaultInternalAccountTagService defaultInternalAccountTagService;

    private final AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter;

    private final CostTagging costTagging;

    private final CrnUserDetailsService crnUserDetailsService;

    public EnvironmentTagProvider(EntitlementService entitlementService,
            AccountTagService accountTagService,
            DefaultInternalAccountTagService defaultInternalAccountTagService,
            AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter,
            CostTagging costTagging,
            CrnUserDetailsService crnUserDetailsService) {
        this.entitlementService = entitlementService;
        this.accountTagService = accountTagService;
        this.defaultInternalAccountTagService = defaultInternalAccountTagService;
        this.accountTagToAccountTagResponsesConverter = accountTagToAccountTagResponsesConverter;
        this.costTagging = costTagging;
        this.crnUserDetailsService = crnUserDetailsService;
    }

    public Map<String, String> getTags(EnvironmentDto environmentDto, String resourceCrn) {
        Map<String, String> userDefinedTags = environmentDto.getTags().getUserDefinedTags();
        Map<String, String> accountTags = getAccountTags(environmentDto.getAccountId());
        Map<String, String> defaultTags = getDefaultTags(environmentDto, userDefinedTags, accountTags, resourceCrn);

        CDPTagMergeRequest mergeRequest = CDPTagMergeRequest.Builder
                .builder()
                .withEnvironmentTags(environmentDto.getTags().getUserDefinedTags())
                .withPlatform(environmentDto.getCloudPlatform())
                .withRequestTags(defaultTags)
                .build();

        return costTagging.mergeTags(mergeRequest);
    }

    private Map<String, String> getAccountTags(String accountId) {
        Set<AccountTag> accountTags = accountTagService.get(accountId);
        List<AccountTagResponse> accountTagResponses = accountTags.stream()
                .map(a -> accountTagToAccountTagResponsesConverter.convert(a))
                .collect(Collectors.toList());
        defaultInternalAccountTagService.merge(accountTagResponses);
        return accountTagResponses
                .stream()
                .collect(Collectors.toMap(AccountTagResponse::getKey, AccountTagResponse::getValue));
    }

    private Map<String, String> getDefaultTags(EnvironmentDto environmentDto, Map<String, String> userDefinedTags,
            Map<String, String> accountTagsMap, String resourceCrn) {
        boolean internalTenant = entitlementService.internalTenant(environmentDto.getAccountId());
        CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder.builder()
                .withCreatorCrn(environmentDto.getCreator())
                .withEnvironmentCrn(environmentDto.getResourceCrn())
                .withAccountId(environmentDto.getAccountId())
                .withPlatform(environmentDto.getCloudPlatform())
                .withResourceCrn(resourceCrn)
                .withIsInternalTenant(internalTenant)
                .withUserName(getUserNameFromCrn(environmentDto.getCreator()))
                .withAccountTags(accountTagsMap)
                .withUserDefinedTags(userDefinedTags)
                .build();
        return costTagging.prepareDefaultTags(request);
    }

    private String getUserNameFromCrn(String crn) {
        return crnUserDetailsService.loadUserByUsername(crn).getUsername();
    }
}
