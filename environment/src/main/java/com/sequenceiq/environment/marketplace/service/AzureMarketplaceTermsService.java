package com.sequenceiq.environment.marketplace.service;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.environment.marketplace.domain.Terms;
import com.sequenceiq.environment.marketplace.repository.TermsRepository;

@Service
public class AzureMarketplaceTermsService {

    @Inject
    private TermsRepository termsRepository;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Boolean get(String accountId) {
        Optional<Terms> termsOptional = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> termsRepository.findByAccountId(accountId));
        return termsOptional.map(Terms::isAccepted).orElse(Boolean.FALSE);
    }

    public void updateOrCreate(Boolean accepted, String accountId) {
        try {
            Optional<Terms> termsOptional = termsRepository.findByAccountId(accountId);
            termsOptional.ifPresentOrElse(terms -> {
                terms.setAccepted(accepted);
                termsRepository.save(terms);
            }, () -> {
                Terms terms = new Terms();
                terms.setAccepted(accepted);
                terms.setAccountId(accountId);
                termsRepository.save(terms);
            });
        } catch (DataIntegrityViolationException e) {
            throw new AccessDeniedException("Access denied", e);
        }
    }
}
