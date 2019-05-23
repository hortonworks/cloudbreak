package com.sequenceiq.freeipa.ldap;

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
public class LdapConfigServiceTest {
    private static final String ENVIRONMENT_ID = "environmentId";

    private static final String ACCOUNT_ID = "accountId";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private LdapConfigRepository ldapConfigRepository;

    @Mock
    private UserCrnService userCrnService;

    @Mock
    private LdapConfigValidator ldapConfigValidator;

    @InjectMocks
    private LdapConfigService underTest;

    @Test
    public void testCreateKerberosConfig() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setEnvironmentId(ENVIRONMENT_ID);
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(ldapConfigRepository.save(ldapConfig)).thenReturn(ldapConfig);
        // WHEN
        underTest.createLdapConfig(ldapConfig);
        // THEN
        Assert.assertNotNull(ldapConfig.getResourceCrn());
    }

    @Test
    public void testCreateKerberosConfigWhenThereIsAlreadyExisted() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setEnvironmentId(ENVIRONMENT_ID);
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(ldapConfigRepository.findByAccountIdAndEnvironmentId(ACCOUNT_ID, ENVIRONMENT_ID)).thenReturn(Optional.of(new LdapConfig()));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("environment is already exists");
        // WHEN
        underTest.createLdapConfig(ldapConfig);
        // THEN BadRequestException has to be thrown
    }

    @Test
    public void testGet() {
        // GIVEN
        LdapConfig expectedLdapConfig = new LdapConfig();
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(ldapConfigRepository.findByAccountIdAndEnvironmentId(ACCOUNT_ID, ENVIRONMENT_ID)).thenReturn(Optional.of(expectedLdapConfig));
        // WHEN
        LdapConfig actualResult = underTest.get(ENVIRONMENT_ID);
        // THEN
        Assert.assertEquals(expectedLdapConfig, actualResult);
    }

    @Test
    public void testGetNotFound() {
        // GIVEN
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("LdapConfig for environment");
        // WHEN
        underTest.get(ENVIRONMENT_ID);
        // THEN NotFoundException has to be thrown
    }

    @Test
    public void testDelete() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(ldapConfigRepository.findByAccountIdAndEnvironmentId(ACCOUNT_ID, ENVIRONMENT_ID)).thenReturn(Optional.of(ldapConfig));
        // WHEN
        underTest.delete(ENVIRONMENT_ID);
        // THEN
        Mockito.verify(ldapConfigRepository).delete(ldapConfig);
    }

    @Test
    public void testDeleteNotFound() {
        // GIVEN
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("LdapConfig for environment");
        // WHEN
        underTest.delete(ENVIRONMENT_ID);
        // THEN NotFoundException has to be thrown
    }

    @Test
    public void testTestConnectionWithEnvironmentId() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        Mockito.when(userCrnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(ldapConfigRepository.findByAccountIdAndEnvironmentId(ACCOUNT_ID, ENVIRONMENT_ID)).thenReturn(Optional.of(ldapConfig));
        // WHEN
        String actualResult = underTest.testConnection(ENVIRONMENT_ID, null);
        // THEN
        Assert.assertEquals("connected", actualResult);
    }

    @Test
    public void testTestConnectionWithLdapConfig() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        // WHEN
        String actualResult = underTest.testConnection(null, ldapConfig);
        // THEN
        Assert.assertEquals("connected", actualResult);
    }

    @Test
    public void testTestConnectionWithLdapConfigShouldThrowBadRequest() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        Mockito.doThrow(new BadRequestException("connection failed")).when(ldapConfigValidator).validateLdapConnection(ldapConfig);
        // WHEN
        String actualResult = underTest.testConnection(null, ldapConfig);
        // THEN
        Assert.assertEquals("connection failed", actualResult);
    }
}
