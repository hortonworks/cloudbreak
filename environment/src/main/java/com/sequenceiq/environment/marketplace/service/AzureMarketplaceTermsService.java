package com.sequenceiq.environment.marketplace.service;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.environment.exception.MarketplaceTermsAlreadySetException;
import com.sequenceiq.environment.marketplace.domain.Terms;
import com.sequenceiq.environment.marketplace.repository.TermsRepository;

@Service
public class AzureMarketplaceTermsService {

    @Inject
    private TermsRepository termsRepository;

    @Inject
    private TransactionService transactionService;

    public Boolean get(String accountId) {
        Optional<Terms> termsOptional = termsRepository.findByAccountId(accountId);
        return termsOptional.map(Terms::isAccepted).orElse(Boolean.FALSE);
    }

    public void updateOrCreate(Boolean accepted, String accountId) {
        try {
            transactionService.required(() -> {
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
            });
        } catch (DataIntegrityViolationException | TransactionService.TransactionExecutionException e) {
            throw new MarketplaceTermsAlreadySetException("Marketplace terms acceptance is modified already. Please retry later", e);
        }
    }
}
