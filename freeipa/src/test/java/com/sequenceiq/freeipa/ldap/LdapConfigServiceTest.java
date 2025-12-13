package com.sequenceiq.freeipa.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
public class LdapConfigServiceTest {
    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

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
        ldapConfig.setEnvironmentCrn(ENVIRONMENT_CRN);
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        when(crnService.createCrn(ACCOUNT_ID, CrnResourceDescriptor.LDAP)).thenReturn(RESOURCE_CRN);
        when(ldapConfigRepository.save(ldapConfig)).thenReturn(ldapConfig);
        // WHEN
        underTest.createLdapConfig(ldapConfig);
        // THEN
        assertNotNull(ldapConfig.getResourceCrn());
    }

    @Test
    public void testCreateKerberosConfigWhenThereIsAlreadyExisted() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setEnvironmentCrn(ENVIRONMENT_CRN);
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        when(ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.of(new LdapConfig()));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            underTest.createLdapConfig(ldapConfig);
        });
        assertEquals("LdapConfig in the [accountId] account's "
                + "[crn:cdp:environments:us-west-1:accountId:environment:e438a2db-d650-4132-ae62-242c5ba2f784] environment is already exists", ex.getMessage());
        // WHEN
        // THEN BadRequestException has to be thrown
    }

    @Test
    public void testGet() {
        // GIVEN
        LdapConfig expectedLdapConfig = new LdapConfig();
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        when(ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.of(expectedLdapConfig));
        // WHEN
        LdapConfig actualResult = underTest.get(ENVIRONMENT_CRN);
        // THEN
        assertEquals(expectedLdapConfig, actualResult);
    }

    @Test
    public void testGetNotFound() {
        // GIVEN
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            // WHEN
            underTest.get(ENVIRONMENT_CRN);
        });
        assertEquals("LdapConfig for environment "
                + "'crn:cdp:environments:us-west-1:accountId:environment:e438a2db-d650-4132-ae62-242c5ba2f784' not found.", ex.getMessage());
    }

    @Test
    public void testDelete() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        when(ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.of(ldapConfig));
        // WHEN
        underTest.delete(ENVIRONMENT_CRN);
        // THEN
        verify(ldapConfigRepository).save(ldapConfig);
    }

    @Test
    public void testDeleteNotFound() {
        // GIVEN
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            // WHEN
            underTest.delete(ENVIRONMENT_CRN);
        });
        assertEquals("LdapConfig for environment "
                + "'crn:cdp:environments:us-west-1:accountId:environment:e438a2db-d650-4132-ae62-242c5ba2f784' not found.", ex.getMessage());
    }

    @Test
    public void testTestConnectionWithEnvironmentId() {
        // GIVEN
        LdapConfig ldapConfig = new LdapConfig();
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        when(ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.of(ldapConfig));
        // WHEN
        String actualResult = underTest.testConnection(ENVIRONMENT_CRN, null);
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
        doThrow(new BadRequestException("connection failed")).when(ldapConfigValidator).validateLdapConnection(ldapConfig);
        // WHEN
        String actualResult = underTest.testConnection(null, ldapConfig);
        // THEN
        assertEquals("connection failed", actualResult);
    }

    @Test
    void testEnvironmentLevelLdapConfigExists() {
        when(ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.of(new LdapConfig()));

        boolean result = underTest.doesEnvironmentLevelLdapConfigExists(Crn.safeFromString(ENVIRONMENT_CRN));

        assertTrue(result);
    }

    @Test
    void testEnvironmentLevelLdapConfigNotExists() {
        when(ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_CRN))
                .thenReturn(Optional.empty());

        boolean result = underTest.doesEnvironmentLevelLdapConfigExists(Crn.safeFromString(ENVIRONMENT_CRN));

        assertFalse(result);
    }
}
