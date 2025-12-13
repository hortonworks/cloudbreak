package com.sequenceiq.freeipa.kerberos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
public class KerberosConfigServiceTest {
    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:accountId:kerberosconfig:4c5ba74b-c35e-45e9-9f47-123456789876";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private KerberosConfigRepository kerberosConfigRepository;

    @Mock
    private CrnService crnService;

    @Mock
    private Clock clockService;

    @InjectMocks
    private KerberosConfigService underTest;

    @Test
    public void name() {
        List<Integer> numbers = List.of(1, 3, 1, 4, 5, 6, 3, 2);
        final long c = numbers.stream()
                .collect(Collectors.toMap(n -> n, n -> 1, (l, r) -> l + r))
                .values().stream()
                .filter(v -> v > 1)
                .count();
        assertEquals(2, c);
    }

    @Test
    public void testCreateKerberosConfig() {
        // GIVEN
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setEnvironmentCrn(ENVIRONMENT_CRN);
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        when(crnService.createCrn(ACCOUNT_ID, CrnResourceDescriptor.KERBEROS)).thenReturn(RESOURCE_CRN);
        when(kerberosConfigRepository.save(kerberosConfig)).thenReturn(kerberosConfig);
        // WHEN
        underTest.createKerberosConfig(kerberosConfig);
        // THEN
        assertNotNull(kerberosConfig.getResourceCrn());
    }

    @Test
    public void testCreateKerberosConfigWhenThereIsAlreadyExisted() {
        // GIVEN
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setEnvironmentCrn(ENVIRONMENT_CRN);
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.of(new KerberosConfig()));
        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            // WHEN
            underTest.createKerberosConfig(kerberosConfig);
        });
        assertEquals("KerberosConfig in the [accountId] account's "
                + "[crn:cdp:environments:us-west-1:accountId:environment:e438a2db-d650-4132-ae62-242c5ba2f784] environment is already exists", ex.getMessage());
    }

    @Test
    public void testGet() {
        // GIVEN
        KerberosConfig expectedKerberosConfig = new KerberosConfig();
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.of(expectedKerberosConfig));
        // WHEN
        KerberosConfig actualResult = underTest.get(ENVIRONMENT_CRN);
        // THEN
        assertEquals(expectedKerberosConfig, actualResult);
    }

    @Test
    public void testFindAllInEnvironment() {
        // GIVEN
        KerberosConfig expectedKerberosConfig1 = new KerberosConfig();
        KerberosConfig expectedKerberosConfig2 = new KerberosConfig();
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(List.of(expectedKerberosConfig1, expectedKerberosConfig2));
        // WHEN
        List<KerberosConfig> actualResults = underTest.findAllInEnvironment(ENVIRONMENT_CRN);
        // THEN
        assertEquals(List.of(expectedKerberosConfig1, expectedKerberosConfig2), actualResults);
    }

    @Test
    public void testSaveAll() {
        // GIVEN
        KerberosConfig expectedKerberosConfig1 = new KerberosConfig();
        KerberosConfig expectedKerberosConfig2 = new KerberosConfig();
        List<KerberosConfig> kerberosConfigs = List.of(expectedKerberosConfig1, expectedKerberosConfig2);
        when(kerberosConfigRepository.saveAll(any())).thenReturn(kerberosConfigs);
        // WHEN
        List<KerberosConfig> actualResults = underTest.saveAll(List.of(expectedKerberosConfig1, expectedKerberosConfig2));
        // THEN
        assertEquals(kerberosConfigs, actualResults);
    }

    @Test
    public void testGetNotFound() {
        // GIVEN
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            // WHEN
            underTest.get(ENVIRONMENT_CRN);
            // THEN NotFoundException has to be thrown
        });
        assertEquals("KerberosConfig for environment "
                + "'crn:cdp:environments:us-west-1:accountId:environment:e438a2db-d650-4132-ae62-242c5ba2f784' not found.", ex.getMessage());
    }

    @Test
    public void testDelete() {
        // GIVEN
        KerberosConfig expectedKerberosConfig = new KerberosConfig();
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.of(expectedKerberosConfig));
        // WHEN
        underTest.delete(ENVIRONMENT_CRN);
        // THEN
        verify(kerberosConfigRepository).save(expectedKerberosConfig);
    }

    @Test
    public void testDeleteNotFound() {
        // GIVEN
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            // WHEN
            underTest.delete(ENVIRONMENT_CRN);
        });
        assertEquals("KerberosConfig for environment "
                + "'crn:cdp:environments:us-west-1:accountId:environment:e438a2db-d650-4132-ae62-242c5ba2f784' not found.", ex.getMessage());
    }

    @Test
    void testEnvironmentLevelLdapConfigExists() {
        when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.of(new KerberosConfig()));

        boolean result = underTest.doesEnvironmentLevelKerberosConfigExists(Crn.safeFromString(ENVIRONMENT_CRN));

        assertTrue(result);
    }

    @Test
    void testEnvironmentLevelLdapConfigNotExists() {
        when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.empty());

        boolean result = underTest.doesEnvironmentLevelKerberosConfigExists(Crn.safeFromString(ENVIRONMENT_CRN));

        assertFalse(result);
    }
}
