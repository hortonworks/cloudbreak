package com.sequenceiq.environment.tags.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.common.api.tag.model.Tags;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.repository.AccountTagRepository;

@Service
public class AccountTagService {

    private final AccountTagRepository accountTagRepository;

    private final CostTagging costTagging;

    private final EntitlementService entitlementService;

    public AccountTagService(AccountTagRepository accountTagRepository, CostTagging costTagging, EntitlementService entitlementService) {
        this.accountTagRepository = accountTagRepository;
        this.costTagging = costTagging;
        this.entitlementService = entitlementService;
    }

    public Set<AccountTag> get(String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> accountTagRepository.findAllInAccount(accountId));
    }

    public List<AccountTag> create(List<AccountTag> accountTags, String accountId) {
        try {
            accountTagRepository.arhiveAll(accountId);

            List<AccountTag> result = new ArrayList<>();
            for (AccountTag accountTag : accountTags) {
                accountTag.setAccountId(accountId);
                accountTag.setArchived(false);
                accountTag.setResourceCrn(createCRN(accountId));
                result.add(accountTagRepository.save(accountTag));
            }
            return result;
        } catch (DataIntegrityViolationException e) {
            throw new AccessDeniedException("Access denied", e);
        }
    }

    public Tags generate(String accountId, EnvironmentDto environmentDto) {
        try {
            Map<String, String> accountTagsMap = accountTagRepository.findAllInAccount(accountId)
                    .stream()
                    .collect(Collectors.toMap(AccountTag::getTagKey, AccountTag::getTagValue));
            boolean internalTenant = entitlementService.internalTenant(environmentDto.getCreator(), accountId);

            CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder.builder()
                    .withCreatorCrn(environmentDto.getCreator())
                    .withEnvironmentCrn(environmentDto.getResourceCrn())
                    .withAccountId(environmentDto.getAccountId())
                    .withPlatform(environmentDto.getCloudPlatform())
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withIsInternalTenant(internalTenant)
                    .withUserName(getUserFromCrn(environmentDto.getCreator()))
                    .withAccountTags(new Tags(accountTagsMap))
                    .build();
            return costTagging.generateAccountTags(request);
        } catch (DataIntegrityViolationException e) {
            throw new AccessDeniedException("Access denied", e);
        }
    }

    private String getUserFromCrn(String crn) {
        return Optional.ofNullable(Crn.fromString(crn)).map(Crn::getUserId).orElse(null);
    }

    private String createCRN(String accountId) {
        return Crn.builder()
                .setService(Crn.Service.ACCOUNTTAG)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.ACCOUNT_TAG)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }
}
