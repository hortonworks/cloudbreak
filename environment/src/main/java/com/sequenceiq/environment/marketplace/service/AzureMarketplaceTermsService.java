package com.sequenceiq.environment.marketplace.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.environment.marketplace.domain.Terms;
import com.sequenceiq.environment.marketplace.repository.TermsRepository;

@Service
public class AzureMarketplaceTermsService {

    private final TermsRepository termsRepository;

    private final RegionAwareCrnGenerator regionAwareCrnGenerator;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AzureMarketplaceTermsService(TermsRepository termsRepository, RegionAwareCrnGenerator regionAwareCrnGenerator,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.termsRepository = termsRepository;
        this.regionAwareCrnGenerator = regionAwareCrnGenerator;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public Boolean get(String accountId) {
        Optional<Terms> termsOptional = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> termsRepository.findFirstByAccountId(accountId));
        return termsOptional.map(Terms::isAccepted).orElse(Boolean.FALSE);
    }

    public void updateOrCreate(Boolean accepted, String accountId) {
        try {
            Optional<Terms> termsOptional = termsRepository.findFirstByAccountId(accountId);
            termsOptional.ifPresentOrElse(terms -> {
                terms.setAccepted(accepted);
                termsRepository.save(terms);
            }, () -> {
                Terms terms = new Terms();
                terms.setAccepted(accepted);
                terms.setAccountId(accountId);
                terms.setResourceCrn(createCRN(accountId));
                termsRepository.save(terms);
            });
        } catch (DataIntegrityViolationException e) {
            throw new AccessDeniedException("Access denied", e);
        }
    }

    private String createCRN(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.ACCOUNT_TAG, accountId);
    }
}
