package com.sequenceiq.freeipa.service.rotation.ldapbindpassword.contextprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.rotation.FreeIpaRotationAdditionalParameters;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.binduser.UserSyncBindUserService;

@ExtendWith(MockitoExtension.class)
class FreeIpaLdapBindPasswordRotationContextProviderTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:tenant:environment:envCrn1-postfix";

    private static final String CLUSTER_NAME = "clusterName";

    private static final String BIND_PASSWORD_SECRET = "bindPasswordSecret";

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private UserSyncBindUserService userSyncBindUserService;

    @InjectMocks
    private FreeIpaLdapBindPasswordRotationContextProvider underTest;

    @Mock
    private LdapConfig ldapConfig;

    @Test
    void testGetContextsWithoutClusterNameAndUserSync() {
        SecretRotationException sre = assertThrows(SecretRotationException.class, () -> underTest.getContextsWithProperties(ENVIRONMENT_CRN, null));

        assertEquals("FreeIpa ldap bind password rotation failed, either CLUSTER_NAME or USER_SYNC should be set in the request.", sre.getMessage());
        verify(ldapConfigService, never()).find(any(), any(), any());
    }

    @Test
    void testGetContextsWithoutClusterNameAndUserSyncTrue() {
        Map<String, String> additionalProperties = Map.of(FreeIpaRotationAdditionalParameters.ROTATE_USER_SYNC_USER.name(), "true");
        when(ldapConfigService.find(any(), any(), any())).thenReturn(Optional.of(ldapConfig));
        when(ldapConfig.getBindPasswordSecret()).thenReturn(BIND_PASSWORD_SECRET);
        when(userSyncBindUserService.createUserSyncBindUserPostfix(ENVIRONMENT_CRN)).thenReturn("usersync-postfix");

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContextsWithProperties(ENVIRONMENT_CRN, additionalProperties);

        assertEquals(2, contexts.size());
        assertTrue(FreeIpaSecretType.FREEIPA_LDAP_BIND_PASSWORD.getSteps().stream().allMatch(contexts::containsKey));
        verify(ldapConfigService, times(1)).find(eq(ENVIRONMENT_CRN), eq("tenant"), eq("usersync-postfix"));
    }

    @Test
    void testGetContextsWithoutClusterNameAndUserSyncBad() {
        Map<String, String> additionalProperties = Map.of(FreeIpaRotationAdditionalParameters.ROTATE_USER_SYNC_USER.name(), "bad");

        SecretRotationException sre = assertThrows(SecretRotationException.class,
                () -> underTest.getContextsWithProperties(ENVIRONMENT_CRN, additionalProperties));

        assertEquals("FreeIpa ldap bind password rotation failed, either CLUSTER_NAME or USER_SYNC should be set in the request.", sre.getMessage());
        verify(ldapConfigService, never()).find(any(), any(), any());
    }

    @Test
    void testGetContextsWithoutClusterNameAndUserSyncBothSet() {
        Map<String, String> additionalProperties = Map.of(FreeIpaRotationAdditionalParameters.CLUSTER_NAME.name(), CLUSTER_NAME,
                FreeIpaRotationAdditionalParameters.ROTATE_USER_SYNC_USER.name(), "true");

        SecretRotationException sre = assertThrows(SecretRotationException.class,
                () -> underTest.getContextsWithProperties(ENVIRONMENT_CRN, additionalProperties));

        assertEquals("For FreeIpa LDAP bind password rotation CLUSTER_NAME and USER_SYNC cannot be both set together.", sre.getMessage());
        verify(ldapConfigService, never()).find(any(), any(), any());
    }

    @Test
    void testGetContextsWithClusterNameExceptionIfLdapConfigNotFound() {
        Map<String, String> additionalProperties = Map.of(FreeIpaRotationAdditionalParameters.CLUSTER_NAME.name(), CLUSTER_NAME);
        SecretRotationException sre = assertThrows(SecretRotationException.class,
                () -> underTest.getContextsWithProperties(ENVIRONMENT_CRN, additionalProperties));

        assertEquals("FreeIpa ldap bind password rotation failed, cannot found ldap config for user: clusterName", sre.getMessage());
        verify(ldapConfigService, times(1)).find(eq(ENVIRONMENT_CRN), eq("tenant"), eq(CLUSTER_NAME));
    }

    @Test
    void testGetContextsWithClusterName() {
        when(ldapConfigService.find(any(), any(), any())).thenReturn(Optional.of(ldapConfig));
        when(ldapConfig.getBindPasswordSecret()).thenReturn(BIND_PASSWORD_SECRET);

        Map<String, String> additionalProperties = Map.of(FreeIpaRotationAdditionalParameters.CLUSTER_NAME.name(), CLUSTER_NAME);
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContextsWithProperties(ENVIRONMENT_CRN, additionalProperties);

        assertEquals(2, contexts.size());
        assertTrue(FreeIpaSecretType.FREEIPA_LDAP_BIND_PASSWORD.getSteps().stream().allMatch(contexts::containsKey));
        verify(ldapConfigService, times(1)).find(ENVIRONMENT_CRN, "tenant", CLUSTER_NAME);
    }
}