package com.sequenceiq.environment.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.environment.marketplace.domain.Terms;
import com.sequenceiq.environment.marketplace.repository.TermsRepository;

@ExtendWith(MockitoExtension.class)
public class AzureMarketplaceTermsServiceTest {

    @Mock
    private TermsRepository termsRepository;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @InjectMocks
    private AzureMarketplaceTermsService azureMarketplaceTermsService;

    @BeforeEach
    public void setUp() {
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(mock(RegionAwareInternalCrnGenerator.class));
    }

    @Test
    public void testGetTermsAccepted() {
        String accountId = "testAccountId";
        Terms terms = new Terms();
        terms.setAccountId(accountId);
        terms.setAccepted(true);

        when(termsRepository.findByAccountId(accountId)).thenReturn(Optional.of(terms));

        Boolean result = azureMarketplaceTermsService.get(accountId);

        assertEquals(true, result);
        verify(regionAwareInternalCrnGeneratorFactory.iam(), times(1)).getInternalCrnForServiceAsString();
        verify(termsRepository, times(1)).findByAccountId(accountId);
    }

    @Test
    public void testGetTermsNotAccepted() {
        String accountId = "testAccountId";

        when(termsRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        Boolean result = azureMarketplaceTermsService.get(accountId);

        assertEquals(false, result);
        verify(regionAwareInternalCrnGeneratorFactory.iam(), times(1)).getInternalCrnForServiceAsString();
        verify(termsRepository, times(1)).findByAccountId(accountId);
    }

    @Test
    public void testUpdateOrCreateTermsExists() {
        String accountId = "testAccountId";
        Terms terms = new Terms();
        terms.setAccountId(accountId);
        terms.setAccepted(true);

        when(termsRepository.findByAccountId(accountId)).thenReturn(Optional.of(terms));

        azureMarketplaceTermsService.updateOrCreate(false, accountId);

        verify(termsRepository, times(1)).findByAccountId(accountId);
        verify(termsRepository, times(1)).save(terms);
    }

    @Test
    public void testUpdateOrCreateTermsDoesNotExist() {
        String accountId = "testAccountId";

        when(termsRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        azureMarketplaceTermsService.updateOrCreate(true, accountId);

        verify(termsRepository, times(1)).findByAccountId(accountId);
        verify(termsRepository, times(1)).save(any());
    }

    @Test
    public void testUpdateOrCreateDataIntegrityViolationException() {
        String accountId = "testAccountId";
        Terms terms = new Terms();
        terms.setAccountId(accountId);
        terms.setAccepted(true);

        when(termsRepository.findByAccountId(accountId)).thenReturn(Optional.of(terms));
        doThrow(DataIntegrityViolationException.class).when(termsRepository).save(terms);

        assertThrows(AccessDeniedException.class, () -> azureMarketplaceTermsService.updateOrCreate(false, accountId));

        verify(termsRepository, times(1)).findByAccountId(accountId);
        verify(termsRepository, times(1)).save(terms);
    }
}
