package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.google.common.io.BaseEncoding;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.context.saltboot.SaltBootConfigRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.saltboot.SaltBootUpdateConfiguration;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class SaltBootRotationContextProviderTest {

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn1";

    private static final String SALT_BOOT_PASSWORD_VAULT_PATH = "secret/passwordPath";

    private static final String SALT_BOOT_PRIVATE_KEY_VAULT_PATH = "secret/privateKeyPath";

    private static final String OLD_PASSWORD = "oldPassword";

    private static final String NEW_PASSWORD = "newPassword";

    private static final String OLD_PRIVATE_KEY = newPrivateKey();

    private static final String NEW_PRIVATE_KEY = newPrivateKey();

    @Mock
    private StackDtoService stackService;

    @Mock
    private SecretService secretService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

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

    @Mock
    private InstanceMetadataView instanceMetadataView;

    @BeforeEach
    public void setUp() {
        when(stackService.getByCrn(anyString())).thenReturn(stack);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(instanceMetadataView.getPrivateIp()).thenReturn("0.0.0.0");
        when(instanceMetadataView.getDiscoveryFQDN()).thenReturn("host1");
        when(stack.getAllAvailableInstances()).thenReturn(List.of(instanceMetadataView));
        when(securityConfig.getSaltSecurityConfig()).thenReturn(saltSecurityConfig);
        when(saltBootPassword.getSecret()).thenReturn(SALT_BOOT_PASSWORD_VAULT_PATH);
        when(saltSecurityConfig.getSaltBootPasswordSecret()).thenReturn(saltBootPassword);
        when(saltBootPrivateKey.getSecret()).thenReturn(SALT_BOOT_PRIVATE_KEY_VAULT_PATH);
        when(saltSecurityConfig.getSaltBootSignPrivateKeySecret()).thenReturn(saltBootPrivateKey);
    }

    @Test
    public void testSaltBootContextProviderProvidesAllContextData() {
        when(secretService.getRotation(SALT_BOOT_PASSWORD_VAULT_PATH)).thenReturn(new RotationSecret(NEW_PASSWORD, OLD_PASSWORD));
        when(secretService.getRotation(SALT_BOOT_PRIVATE_KEY_VAULT_PATH))
                .thenReturn(new RotationSecret(NEW_PRIVATE_KEY, OLD_PRIVATE_KEY));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);

        assertInstanceOf(VaultRotationContext.class, contexts.get(CommonSecretRotationStep.VAULT));
        assertInstanceOf(CustomJobRotationContext.class, contexts.get(CommonSecretRotationStep.CUSTOM_JOB));
        assertInstanceOf(SaltBootConfigRotationContext.class, contexts.get(CommonSecretRotationStep.SALTBOOT_CONFIG));
        assertInstanceOf(UserDataRotationContext.class, contexts.get(CommonSecretRotationStep.USER_DATA));

        SaltBootConfigRotationContext saltBootConfigRotationContext = (SaltBootConfigRotationContext) contexts.get(CommonSecretRotationStep.SALTBOOT_CONFIG);
        SaltBootUpdateConfiguration serviceUpdateConfiguration = saltBootConfigRotationContext.getServiceUpdateConfiguration();
        assertEquals("security-config.yml", serviceUpdateConfiguration.configFile());
        assertEquals("/etc/salt-bootstrap", serviceUpdateConfiguration.configFolder());
        assertEquals("newPassword", serviceUpdateConfiguration.newSaltBootPassword());
        assertEquals("oldPassword", serviceUpdateConfiguration.oldSaltBootPassword());
        assertEquals(List.of("saltboot.stop-saltboot", "saltboot.start-saltboot"), serviceUpdateConfiguration.serviceRestartActions());
        assertEquals(Set.of("host1"), serviceUpdateConfiguration.targetFqdns());
        assertEquals(Set.of("0.0.0.0"), serviceUpdateConfiguration.targetPrivateIps());
    }

    private static String newPrivateKey() {
        return BaseEncoding.base64().encode(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()).getBytes());
    }
}