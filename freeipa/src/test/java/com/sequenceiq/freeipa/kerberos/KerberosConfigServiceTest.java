package com.sequenceiq.freeipa.kerberos;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.freeipa.util.CrnService;

@RunWith(MockitoJUnitRunner.class)
public class KerberosConfigServiceTest {
    private static final String ENVIRONMENT_ID = "environmentId";

    private static final String ACCOUNT_ID = "accountId";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private KerberosConfigRepository kerberosConfigRepository;

    @Mock
    private CrnService crnService;

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
        Mockito.when(kerberosConfigRepository.save(kerberosConfig)).thenReturn(kerberosConfig);
        // WHEN
        underTest.createKerberosConfig(kerberosConfig);
        // THEN
        Assert.assertNotNull(kerberosConfig.getResourceCrn());
    }

    @Test
    public void testCreateKerberosConfigWhenThereIsAlreadyExisted() {
        // GIVEN
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setEnvironmentCrn(ENVIRONMENT_ID);
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNull(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(Optional.of(new KerberosConfig()));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("environment is already exists");
        // WHEN
        underTest.createKerberosConfig(kerberosConfig);
        // THEN BadRequestException has to be thrown
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
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("KerberosConfig for environment");
        // WHEN
        underTest.get(ENVIRONMENT_ID);
        // THEN NotFoundException has to be thrown
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
        Mockito.verify(kerberosConfigRepository).delete(expectedKerberosConfig);
    }

    @Test
    public void testDeleteNotFound() {
        // GIVEN
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("KerberosConfig for environment");
        // WHEN
        underTest.delete(ENVIRONMENT_ID);
        // THEN NotFoundException has to be thrown
    }
}
