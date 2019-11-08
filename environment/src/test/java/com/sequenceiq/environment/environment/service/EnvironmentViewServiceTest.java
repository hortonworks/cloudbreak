package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.util.TestConstants.CRN;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.repository.EnvironmentViewRepository;

@ExtendWith(SpringExtension.class)
class EnvironmentViewServiceTest {

    @Inject
    private EnvironmentViewService environmentViewServiceUnderTest;

    @MockBean
    private EnvironmentViewRepository environmentViewRepository;

    @MockBean
    private ConversionService conversionService;

    @Test
    void findByNamesInAccount() {
        Set<EnvironmentView> queryResult = Set.of();
        when(environmentViewRepository
                .findAllByNameInAndAccountIdAndArchivedIsFalse(anyCollection(), eq(ACCOUNT_ID)))
                .thenReturn(queryResult);
        assertEquals(queryResult, environmentViewServiceUnderTest.findByNamesInAccount(Set.of("name1", "name2"), ACCOUNT_ID),
                "findByNamesInAccount expected to give back the same result as repository mock (findAllByNameInAndAccountId)");
    }

    @Test
    void findByNamesInAccountEmptyNameList() {
        assertEquals(Set.of(), environmentViewServiceUnderTest.findByNamesInAccount(Set.of(), ACCOUNT_ID),
                "without name list the result should be empty");
    }

    @Test
    void findByResourceCrnsInAccount() {
        Set<EnvironmentView> queryResult = Set.of();
        when(environmentViewRepository
                .findAllByResourceCrnInAndAccountIdAndArchivedIsFalse(anyCollection(), eq(ACCOUNT_ID)))
                .thenReturn(queryResult);
        assertEquals(queryResult, environmentViewServiceUnderTest.findByResourceCrnsInAccount(Set.of("crn1", "crn2"), ACCOUNT_ID),
                "findByResourceCrnsInAccount expected to give back the same result as repository mock (findAllByResourceCrnInAndAccountId)");
    }

    @Test
    void findByResourceCrnsInAccountEmptyNameList() {
        assertEquals(Set.of(), environmentViewServiceUnderTest.findByResourceCrnsInAccount(Set.of(), ACCOUNT_ID),
                "without name list the result should be empty");
    }

    @Test
    void listByAccountId() {
        environmentViewServiceUnderTest.listByAccountId(ACCOUNT_ID);
        verify(environmentViewRepository).findAllByAccountId(eq(ACCOUNT_ID));
    }

    @Test
    void findAllByAccountId() {
        environmentViewServiceUnderTest.findAllByAccountId(ACCOUNT_ID);
        verify(environmentViewRepository).findAllByAccountId(eq(ACCOUNT_ID));
    }

    @Test
    void findAllByCredentialId() {
        environmentViewServiceUnderTest.findAllByCredentialId(1L);
        verify(environmentViewRepository).findAllByCredentialIdAndArchivedIsFalse(eq(1L));
    }

    @Test
    void getIdByName() {
        long value = 1L;
        when(environmentViewRepository
                .getIdByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(value);
        assertEquals(value, environmentViewServiceUnderTest.getIdByName(ENVIRONMENT_NAME, ACCOUNT_ID));
    }

    @Test
    void getIdByNameNotFound() {
        when(environmentViewRepository
                .getIdByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(null);
        assertThrows(NotFoundException.class, () -> environmentViewServiceUnderTest.getIdByName(ENVIRONMENT_NAME, ACCOUNT_ID));
    }

    @Test
    void getIdByCrn() {
        long value = 1L;
        when(environmentViewRepository
                .getIdByResourceCrnAndAccountIdAndArchivedIsFalse(eq(CRN), eq(ACCOUNT_ID))).thenReturn(value);
        assertEquals(value, environmentViewServiceUnderTest.getIdByCrn(CRN, ACCOUNT_ID));

    }

    @Test
    void getIdByCrnNotFound() {
        when(environmentViewRepository
                .getIdByResourceCrnAndAccountIdAndArchivedIsFalse(eq(CRN), eq(ACCOUNT_ID))).thenReturn(null);
        assertThrows(NotFoundException.class, () -> environmentViewServiceUnderTest.getIdByCrn(CRN, ACCOUNT_ID));
    }

    @Configuration
    @Import(EnvironmentViewService.class)
    static class Config {
    }
}