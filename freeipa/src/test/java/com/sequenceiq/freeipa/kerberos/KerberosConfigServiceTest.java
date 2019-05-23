package com.sequenceiq.freeipa.kerberos;

import java.util.Optional;

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
import com.sequenceiq.freeipa.util.UserCrnService;

@RunWith(MockitoJUnitRunner.class)
public class KerberosConfigServiceTest {
    private static final String ENVIRONMENT_ID = "environmentId";

    private static final String ACCOUNT_ID = "accountId";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private KerberosConfigRepository kerberosConfigRepository;

    @Mock
    private UserCrnService userCrnService;

    @InjectMocks
    private KerberosConfigService underTest;

    @Test
    public void testCreateKerberosConfig() {
        // GIVEN
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setEnvironmentId(ENVIRONMENT_ID);
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
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
        kerberosConfig.setEnvironmentId(ENVIRONMENT_ID);
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(kerberosConfigRepository.findByAccountIdAndEnvironmentId(ACCOUNT_ID, ENVIRONMENT_ID)).thenReturn(Optional.of(new KerberosConfig()));
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
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(kerberosConfigRepository.findByAccountIdAndEnvironmentId(ACCOUNT_ID, ENVIRONMENT_ID)).thenReturn(Optional.of(expectedKerberosConfig));
        // WHEN
        KerberosConfig actualResult = underTest.get(ENVIRONMENT_ID);
        // THEN
        Assert.assertEquals(expectedKerberosConfig, actualResult);
    }

    @Test
    public void testGetNotFound() {
        // GIVEN
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
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
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(kerberosConfigRepository.findByAccountIdAndEnvironmentId(ACCOUNT_ID, ENVIRONMENT_ID)).thenReturn(Optional.of(expectedKerberosConfig));
        // WHEN
        underTest.delete(ENVIRONMENT_ID);
        // THEN
        Mockito.verify(kerberosConfigRepository).delete(expectedKerberosConfig);
    }

    @Test
    public void testDeleteNotFound() {
        // GIVEN
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("KerberosConfig for environment");
        // WHEN
        underTest.delete(ENVIRONMENT_ID);
        // THEN NotFoundException has to be thrown
    }
}
