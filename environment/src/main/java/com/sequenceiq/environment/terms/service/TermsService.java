package com.sequenceiq.environment.terms.service;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.environment.api.v1.terms.model.TermType;
import com.sequenceiq.environment.exception.TermsAlreadySetException;
import com.sequenceiq.environment.marketplace.domain.Terms;
import com.sequenceiq.environment.terms.repository.TermsRepository;

@Service
public class TermsService {

    @Inject
    private TermsRepository termsRepository;

    @Inject
    private TransactionService transactionService;

    /**
     * Returns the acceptance status of the given term type for the given account.
     * @param accountId the ID of the account
     * @param termType the type of the given term type
     * @return  the acceptance status of the given term type
     */

    public Boolean get(String accountId, TermType termType) {
        Optional<Terms> termsOptional = termsRepository.findByAccountIdAndTermType(accountId, termType);
        return termsOptional.map(Terms::isAccepted).orElse(Boolean.FALSE);
    }

    /**
     * Updates or creates the given term type acceptance for the given account.
     * @param accepted the acceptance status of the given term type
     * @param termType the type of the given term type
     * @param accountId the ID of the account
     */

    public void updateOrCreate(Boolean accepted, TermType termType, String accountId) {
        try {
            transactionService.required(() -> {
                Optional<Terms> termsOptional = termsRepository.findByAccountIdAndTermType(accountId, termType);
                termsOptional.ifPresentOrElse(terms -> {
                    terms.setAccepted(accepted);
                    terms.setTermType(termType);
                    termsRepository.save(terms);
                }, () -> {
                    Terms terms = new Terms();
                    terms.setAccepted(accepted);
                    terms.setAccountId(accountId);
                    terms.setTermType(termType);
                    termsRepository.save(terms);
                });
            });
        } catch (DataIntegrityViolationException | TransactionService.TransactionExecutionException e) {
            throw new TermsAlreadySetException("Terms acceptance is modified already. Please retry later", e);
        }
    }
}
