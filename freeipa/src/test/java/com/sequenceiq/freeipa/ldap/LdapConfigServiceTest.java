package com.sequenceiq.freeipa.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
public class LdapConfigServiceTest {
    private static final String ENVIRONMENT_ID = "environmentId";

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:accountId:ldapconfig:4c5ba74b-c35e-45e9-9f47-123456789876";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private LdapConfigRepository ldapConfigRepository;

    @Mock
    private CrnService crnService;

    @Mock
    private Clock clock;

    @Mock
    private LdapConfigValidator ldapConfigValidator;

    @InjectMocks
    private LdapConfigService underTest;

    @Test
    public void testCreateKerberosConfig() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setEnvironmentCrn(ENVIRONMENT_ID);
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(crnService.createCrn(ACCOUNT_ID, CrnResourceDescriptor.LDAP)).thenReturn(RESOURCE_CRN);
        Mockito.when(ldapConfigRepository.save(ldapConfig)).thenReturn(ldapConfig);
        // WHEN
        underTest.createLdapConfig(ldapConfig);
        // THEN
        assertNotNull(ldapConfig.getResourceCrn());
    }

    @Test
    public void testCreateKerberosConfigWhenThereIsAlreadyExisted() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setEnvironmentCrn(ENVIRONMENT_ID);
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(Optional.of(new LdapConfig()));

        BadRequestException ex = Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.createLdapConfig(ldapConfig);
        });
        assertEquals("LdapConfig in the [accountId] account's [environmentId] environment is already exists", ex.getMessage());
        // WHEN
        // THEN BadRequestException has to be thrown
    }

    @Test
    public void testGet() {
        // GIVEN
        LdapConfig expectedLdapConfig = new LdapConfig();
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(Optional.of(expectedLdapConfig));
        // WHEN
        LdapConfig actualResult = underTest.get(ENVIRONMENT_ID);
        // THEN
        assertEquals(expectedLdapConfig, actualResult);
    }

    @Test
    public void testGetNotFound() {
        // GIVEN
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> {
            // WHEN
            underTest.get(ENVIRONMENT_ID);
        });
        assertEquals("LdapConfig for environment 'environmentId' not found.", ex.getMessage());
    }

    @Test
    public void testDelete() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(Optional.of(ldapConfig));
        // WHEN
        underTest.delete(ENVIRONMENT_ID);
        // THEN
        Mockito.verify(ldapConfigRepository).save(ldapConfig);
    }

    @Test
    public void testDeleteNotFound() {
        // GIVEN
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> {
            // WHEN
            underTest.delete(ENVIRONMENT_ID);
        });
        assertEquals("LdapConfig for environment 'environmentId' not found.", ex.getMessage());
    }

    @Test
    public void testTestConnectionWithEnvironmentId() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        Mockito.when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(Optional.of(ldapConfig));
        // WHEN
        String actualResult = underTest.testConnection(ENVIRONMENT_ID, null);
        // THEN
        assertEquals("connected", actualResult);
    }

    @Test
    public void testTestConnectionWithLdapConfig() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        // WHEN
        String actualResult = underTest.testConnection(null, ldapConfig);
        // THEN
        assertEquals("connected", actualResult);
    }

    @Test
    public void testTestConnectionWithLdapConfigShouldThrowBadRequest() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        Mockito.doThrow(new BadRequestException("connection failed")).when(ldapConfigValidator).validateLdapConnection(ldapConfig);
        // WHEN
        String actualResult = underTest.testConnection(null, ldapConfig);
        // THEN
        assertEquals("connection failed", actualResult);
    }
}
