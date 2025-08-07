package com.sequenceiq.environment.terms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.environment.api.v1.terms.model.TermType;
import com.sequenceiq.environment.exception.TermsAlreadySetException;
import com.sequenceiq.environment.marketplace.domain.Terms;
import com.sequenceiq.environment.terms.repository.TermsRepository;

@ExtendWith(MockitoExtension.class)
public class TermsServiceTest {

    @Mock
    private TermsRepository termsRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TermsService termsService;

    private final TermType termType = TermType.AZURE_MARKETPLACE_IMAGE_TERMS;

    @BeforeEach
    void init() throws TransactionService.TransactionExecutionException {
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
    }

    @Test
    public void testGetTermsAccepted() {
        String accountId = "testAccountId";
        Terms terms = new Terms();
        terms.setAccountId(accountId);
        terms.setAccepted(true);

        when(termsRepository.findByAccountIdAndTermType(accountId, termType)).thenReturn(Optional.of(terms));

        Boolean result = termsService.get(accountId, termType);

        assertEquals(true, result);
        verify(termsRepository, times(1)).findByAccountIdAndTermType(accountId, termType);
    }

    @Test
    public void testGetTermsNotAccepted() {
        String accountId = "testAccountId";

        when(termsRepository.findByAccountIdAndTermType(accountId, termType)).thenReturn(Optional.empty());

        Boolean result = termsService.get(accountId, termType);

        assertEquals(false, result);
        verify(termsRepository, times(1)).findByAccountIdAndTermType(accountId, termType);
    }

    @Test
    public void testUpdateOrCreateTermsExists() {
        String accountId = "testAccountId";
        Terms terms = new Terms();
        terms.setAccountId(accountId);
        terms.setAccepted(true);

        when(termsRepository.findByAccountIdAndTermType(accountId, termType)).thenReturn(Optional.of(terms));

        termsService.updateOrCreate(false, termType, accountId);

        verify(termsRepository, times(1)).findByAccountIdAndTermType(accountId, termType);
        verify(termsRepository, times(1)).save(terms);
    }

    @Test
    public void testUpdateOrCreateTermsDoesNotExist() {
        String accountId = "testAccountId";

        when(termsRepository.findByAccountIdAndTermType(accountId, termType)).thenReturn(Optional.empty());

        termsService.updateOrCreate(true, termType, accountId);

        verify(termsRepository, times(1)).findByAccountIdAndTermType(accountId, termType);
        verify(termsRepository, times(1)).save(any());
    }

    @Test
    public void testUpdateOrCreateDataIntegrityViolationException() {
        String accountId = "testAccountId";
        Terms terms = new Terms();
        terms.setAccountId(accountId);
        terms.setAccepted(true);

        when(termsRepository.findByAccountIdAndTermType(accountId, termType)).thenReturn(Optional.of(terms));
        doThrow(DataIntegrityViolationException.class).when(termsRepository).save(terms);

        assertThrows(TermsAlreadySetException.class, () -> termsService.updateOrCreate(false, termType, accountId));

        verify(termsRepository, times(1)).findByAccountIdAndTermType(accountId, termType);
        verify(termsRepository, times(1)).save(terms);
    }

    @Test
    public void testUpdateOrCreateTransactionExecutionException() throws TransactionService.TransactionExecutionException {
        String accountId = "testAccountId";

        doThrow(TransactionService.TransactionExecutionException.class).when(transactionService).required(any(Runnable.class));

        assertThrows(TermsAlreadySetException.class, () -> termsService.updateOrCreate(false, termType, accountId));

        verify(transactionService, times(1)).required(any(Runnable.class));
    }

    @Test
    public void testGetWithTermTypeAccepted() {
        String accountId = "testAccountId";
        TermType termType = TermType.AZURE_MARKETPLACE_IMAGE_TERMS;
        Terms terms = new Terms();
        terms.setAccountId(accountId);
        terms.setAccepted(true);
        terms.setTermType(termType);

        when(termsRepository.findByAccountIdAndTermType(accountId, termType)).thenReturn(Optional.of(terms));

        Boolean result = termsService.get(accountId, termType);

        assertEquals(true, result);
        verify(termsRepository, times(1)).findByAccountIdAndTermType(accountId, termType);
    }

    @Test
    public void testGetWithTermTypeNotAccepted() {
        String accountId = "testAccountId";
        TermType termType = TermType.AZURE_DEFAULT_OUTBOUND_TERMS;

        when(termsRepository.findByAccountIdAndTermType(accountId, termType)).thenReturn(Optional.empty());

        Boolean result = termsService.get(accountId, termType);

        assertEquals(false, result);
        verify(termsRepository, times(1)).findByAccountIdAndTermType(accountId, termType);
    }

    @Test
    public void testUpdateOrCreateWithTermTypeExists() {
        String accountId = "testAccountId";
        TermType termType = TermType.AZURE_MARKETPLACE_IMAGE_TERMS;
        Terms terms = new Terms();
        terms.setAccountId(accountId);
        terms.setAccepted(true);
        terms.setTermType(termType);

        when(termsRepository.findByAccountIdAndTermType(accountId, termType)).thenReturn(Optional.of(terms));

        termsService.updateOrCreate(false, termType, accountId);

        verify(termsRepository, times(1)).findByAccountIdAndTermType(accountId, termType);
        verify(termsRepository, times(1)).save(terms);
        assertEquals(false, terms.isAccepted());
        assertEquals(termType, terms.getTermType());
    }

    @Test
    public void testUpdateOrCreateWithTermTypeDoesNotExist() {
        String accountId = "testAccountId";
        TermType termType = TermType.AZURE_DEFAULT_OUTBOUND_TERMS;

        when(termsRepository.findByAccountIdAndTermType(accountId, termType)).thenReturn(Optional.empty());

        termsService.updateOrCreate(true, termType, accountId);

        verify(termsRepository, times(1)).findByAccountIdAndTermType(accountId, termType);
        verify(termsRepository, times(1)).save(any(Terms.class));
    }

    @Test
    public void testUpdateOrCreateWithTermTypeDataIntegrityViolationException() {
        String accountId = "testAccountId";
        TermType termType = TermType.AZURE_MARKETPLACE_IMAGE_TERMS;
        Terms terms = new Terms();
        terms.setAccountId(accountId);
        terms.setAccepted(true);
        terms.setTermType(termType);

        when(termsRepository.findByAccountIdAndTermType(accountId, termType)).thenReturn(Optional.of(terms));
        doThrow(DataIntegrityViolationException.class).when(termsRepository).save(terms);

        assertThrows(TermsAlreadySetException.class, () -> termsService.updateOrCreate(false, termType, accountId));

        verify(termsRepository, times(1)).findByAccountIdAndTermType(accountId, termType);
        verify(termsRepository, times(1)).save(terms);
    }

    @Test
    public void testUpdateOrCreateWithTermTypeTransactionExecutionException() throws TransactionService.TransactionExecutionException {
        String accountId = "testAccountId";
        TermType termType = TermType.AZURE_DEFAULT_OUTBOUND_TERMS;

        doThrow(TransactionService.TransactionExecutionException.class).when(transactionService).required(any(Runnable.class));

        assertThrows(TermsAlreadySetException.class, () -> termsService.updateOrCreate(true, termType, accountId));

        verify(transactionService, times(1)).required(any(Runnable.class));
    }
}
