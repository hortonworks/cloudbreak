package com.sequenceiq.environment.tags.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.repository.AccountTagRepository;

@Service
public class AccountTagService {

    private final AccountTagRepository accountTagRepository;

    private final CostTagging costTagging;

    private final EntitlementService entitlementService;

    private final RegionAwareCrnGenerator regionAwareCrnGenerator;

    public AccountTagService(AccountTagRepository accountTagRepository, CostTagging costTagging, EntitlementService entitlementService,
            RegionAwareCrnGenerator regionAwareCrnGenerator) {
        this.accountTagRepository = accountTagRepository;
        this.costTagging = costTagging;
        this.entitlementService = entitlementService;
        this.regionAwareCrnGenerator = regionAwareCrnGenerator;
    }

    public Set<AccountTag> get(String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> accountTagRepository.findAllInAccount(accountId));
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
            throw new ForbiddenException("Access denied", e);
        }
    }

    public Map<String, String> generate(String accountId, EnvironmentDto environmentDto) {
        try {
            Map<String, String> accountTagsMap = accountTagRepository.findAllInAccount(accountId)
                    .stream()
                    .collect(Collectors.toMap(AccountTag::getTagKey, AccountTag::getTagValue));
            boolean internalTenant = entitlementService.internalTenant(accountId);

            CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder.builder()
                    .withCreatorCrn(environmentDto.getCreator())
                    .withEnvironmentCrn(environmentDto.getResourceCrn())
                    .withAccountId(environmentDto.getAccountId())
                    .withPlatform(environmentDto.getCloudPlatform())
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withIsInternalTenant(internalTenant)
                    .withUserName(getUserFromCrn(environmentDto.getCreator()))
                    .withAccountTags(accountTagsMap)
                    .build();
            return costTagging.generateAccountTags(request);
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenException("Access denied", e);
        }
    }

    private String getUserFromCrn(String crn) {
        return Optional.ofNullable(Crn.fromString(crn)).map(Crn::getUserId).orElse(null);
    }

    private String createCRN(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.ACCOUNT_TAG, accountId);
    }
}
