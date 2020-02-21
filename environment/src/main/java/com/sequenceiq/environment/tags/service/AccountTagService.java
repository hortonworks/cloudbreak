package com.sequenceiq.environment.tags.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.repository.AccountTagRepository;

@Service
public class AccountTagService {

    private final AccountTagRepository accountTagRepository;

    public AccountTagService(AccountTagRepository accountTagRepository) {
        this.accountTagRepository = accountTagRepository;
    }

    public Set<AccountTag> get(String accountId) {
        return accountTagRepository.findAllInAccount(accountId);
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
