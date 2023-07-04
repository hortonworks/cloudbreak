package com.sequenceiq.freeipa.service.rotation;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.rotation.ServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.context.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.userdata.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.vault.VaultRotationContext;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.rotation.saltboot.SaltBootRotationContextProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class SaltBootRotationContextProviderTest {

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn1";

    private static final String SALT_BOOT_PASSWORD_VAULT_PATH = "secret/passwordPath";

    private static final String SALT_BOOT_PRIVATE_KEY_VAULT_PATH = "secret/privateKeyPath";

    @Mock
    private StackService stackService;

    @Mock
    private SecurityConfigService securityConfigService;

    @InjectMocks
    private SaltBootRotationContextProvider underTest;

    @Mock
    private Stack stack;

    @Mock
    private SecurityConfig securityConfig;

    @Mock
    private SaltSecurityConfig saltSecurityConfig;

    @BeforeEach
    public void setUp() {
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack);
        when(securityConfigService.findOneByStack(any())).thenReturn(securityConfig);
        when(securityConfig.getSaltSecurityConfig()).thenReturn(saltSecurityConfig);
        when(saltSecurityConfig.getSaltBootPasswordVaultSecret()).thenReturn(SALT_BOOT_PASSWORD_VAULT_PATH);
        when(saltSecurityConfig.getSaltBootSignPrivateKeyVaultSecret()).thenReturn(SALT_BOOT_PRIVATE_KEY_VAULT_PATH);
    }

    @Test
    public void testSaltBootContextProviderProvidesAllContextData() {
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);

        assertInstanceOf(VaultRotationContext.class, contexts.get(CommonSecretRotationStep.VAULT));
        assertInstanceOf(CustomJobRotationContext.class, contexts.get(CommonSecretRotationStep.CUSTOM_JOB));
        assertInstanceOf(ServiceConfigRotationContext.class, contexts.get(CommonSecretRotationStep.SERVICE_CONFIG));
        assertInstanceOf(UserDataRotationContext.class, contexts.get(CommonSecretRotationStep.USER_DATA));
    }

}