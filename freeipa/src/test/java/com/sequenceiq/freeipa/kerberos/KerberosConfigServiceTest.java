package com.sequenceiq.freeipa.kerberos;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
public class KerberosConfigServiceTest {
    private static final String ENVIRONMENT_ID = "environmentId";

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
        kerberosConfig.setEnvironmentCrn(ENVIRONMENT_ID);
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(crnService.createCrn(ACCOUNT_ID, Crn.ResourceType.KERBEROS)).thenReturn(RESOURCE_CRN);
        Mockito.when(kerberosConfigRepository.save(kerberosConfig)).thenReturn(kerberosConfig);
        // WHEN
        underTest.createKerberosConfig(kerberosConfig);
        // THEN
        Assertions.assertNotNull(kerberosConfig.getResourceCrn());
    }

    @Test
    public void testCreateKerberosConfigWhenThereIsAlreadyExisted() {
        // GIVEN
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setEnvironmentCrn(ENVIRONMENT_ID);
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNull(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(Optional.of(new KerberosConfig()));
        BadRequestException ex = Assertions.assertThrows(BadRequestException.class, () -> {
            // WHEN
            underTest.createKerberosConfig(kerberosConfig);
        });
        assertEquals("KerberosConfig in the [accountId] account's [environmentId] environment is already exists", ex.getMessage());
    }

    @Test
    public void testGet() {
        // GIVEN
        KerberosConfig expectedKerberosConfig = new KerberosConfig();
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNull(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(Optional.of(expectedKerberosConfig));
        // WHEN
        KerberosConfig actualResult = underTest.get(ENVIRONMENT_ID);
        // THEN
        assertEquals(expectedKerberosConfig, actualResult);
    }

    @Test
    public void testGetNotFound() {
        // GIVEN
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> {
            // WHEN
            underTest.get(ENVIRONMENT_ID);
            // THEN NotFoundException has to be thrown
        });
        assertEquals("KerberosConfig for environment 'environmentId' not found.", ex.getMessage());

    }

    @Test
    public void testDelete() {
        // GIVEN
        KerberosConfig expectedKerberosConfig = new KerberosConfig();
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNull(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(Optional.of(expectedKerberosConfig));
        // WHEN
        underTest.delete(ENVIRONMENT_ID);
        // THEN
        Mockito.verify(kerberosConfigRepository).save(expectedKerberosConfig);
    }

    @Test
    public void testDeleteNotFound() {
        // GIVEN
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> {
            // WHEN
            underTest.delete(ENVIRONMENT_ID);
        });
        assertEquals("KerberosConfig for environment 'environmentId' not found.", ex.getMessage());
    }
}
