package com.sequenceiq.freeipa.service.rotation.ldapbindpassword.contextprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.service.binduser.LdapBindUserNameProvider;

@ExtendWith(MockitoExtension.class)
class FreeIpaLdapBindPasswordRotationContextProviderTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:tenant:user:5678";

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private LdapBindUserNameProvider userNameProvider;

    @InjectMocks
    private FreeIpaLdapBindPasswordRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        LdapConfig ldapConfig = mock(LdapConfig.class);
        when(ldapConfig.getBindPasswordSecret()).thenReturn("password");
        when(ldapConfig.getClusterName()).thenReturn("clustername");
        when(userNameProvider.createBindUserName(any())).thenReturn("ldapuser");
        when(ldapConfigService.find(any(), any(), any())).thenReturn(Optional.of(ldapConfig));
        Map<SecretRotationStep, ? extends RotationContext> contexts = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.getContextsWithProperties("crn", Map.of("CLUSTER_NAME", "clustername")));
        assertEquals(2, contexts.size());
    }
}