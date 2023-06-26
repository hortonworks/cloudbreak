package com.sequenceiq.cloudbreak.rotation.saltboot;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.rotation.ServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.context.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class SaltBootRotationContextProviderTest {

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn1";

    private static final String SALT_BOOT_PASSWORD_VAULT_PATH = "secret/passwordPath";

    private static final String SALT_BOOT_PRIVATE_KEY_VAULT_PATH = "secret/privateKeyPath";

    @Mock
    private StackDtoService stackService;

    @InjectMocks
    private SaltBootRotationContextProvider underTest;

    @Mock
    private StackDto stack;

    @Mock
    private SecurityConfig securityConfig;

    @Mock
    private SaltSecurityConfig saltSecurityConfig;

    @Mock
    private Secret saltBootPassword;

    @Mock
    private Secret saltBootPrivateKey;

    @BeforeEach
    public void setUp() {
        when(stackService.getByCrn(anyString())).thenReturn(stack);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(securityConfig.getSaltSecurityConfig()).thenReturn(saltSecurityConfig);
        when(saltBootPassword.getSecret()).thenReturn(SALT_BOOT_PASSWORD_VAULT_PATH);
        when(saltSecurityConfig.getSaltBootPasswordSecret()).thenReturn(saltBootPassword);
        when(saltBootPrivateKey.getSecret()).thenReturn(SALT_BOOT_PRIVATE_KEY_VAULT_PATH);
        when(saltSecurityConfig.getSaltBootSignPrivateKeySecret()).thenReturn(saltBootPrivateKey);
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