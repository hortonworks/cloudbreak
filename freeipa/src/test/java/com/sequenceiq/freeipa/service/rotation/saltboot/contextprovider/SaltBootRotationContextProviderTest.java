package com.sequenceiq.freeipa.service.rotation.saltboot.contextprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.rotation.ExitCriteriaProvider;
import com.sequenceiq.freeipa.service.rotation.saltboot.context.SaltBootConfigRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class SaltBootRotationContextProviderTest {

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn1";

    private static final String SALT_BOOT_PASSWORD_VAULT_PATH = "secret/passwordPath";

    private static final String SALT_BOOT_PRIVATE_KEY_VAULT_PATH = "secret/privateKeyPath";

    private static final String OLD_PASSWORD = "oldPassword";

    private static final String NEW_PASSWORD = "newPassword";

    private static final String OLD_PRIVATE_KEY = PkiUtil.generatePemPrivateKeyInBase64();

    private static final String NEW_PRIVATE_KEY = PkiUtil.generatePemPrivateKeyInBase64();

    @Mock
    private StackService stackService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @InjectMocks
    private SaltBootRotationContextProvider underTest;

    @Mock
    private Stack stack;

    @Mock
    private SecurityConfig securityConfig;

    @Mock
    private SaltSecurityConfig saltSecurityConfig;

    @Mock
    private InstanceMetaData instanceMetaData;

    @BeforeEach
    public void setUp() {
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack);
        lenient().when(instanceMetaData.getPrivateIp()).thenReturn("0.0.0.0");
        lenient().when(instanceMetaData.getDiscoveryFQDN()).thenReturn("host1");
        lenient().when(instanceMetaData.isAvailable()).thenReturn(true);
        lenient().when(stack.getAllInstanceMetaDataList()).thenReturn(List.of(instanceMetaData));
        lenient().when(securityConfigService.findOneByStack(any())).thenReturn(securityConfig);
        lenient().when(securityConfig.getSaltSecurityConfig()).thenReturn(saltSecurityConfig);
        lenient().when(saltSecurityConfig.getSaltBootPasswordVaultSecret()).thenReturn(SALT_BOOT_PASSWORD_VAULT_PATH);
        lenient().when(saltSecurityConfig.getSaltBootSignPrivateKeyVaultSecret()).thenReturn(SALT_BOOT_PRIVATE_KEY_VAULT_PATH);
    }

    @Test
    void testSaltBootContextProviderProvidesAllContextDataWithGCP() {
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.GCP.name());
        when(uncachedSecretServiceForRotation.getRotation(SALT_BOOT_PASSWORD_VAULT_PATH)).thenReturn(new RotationSecret(NEW_PASSWORD, OLD_PASSWORD));
        when(uncachedSecretServiceForRotation.getRotation(SALT_BOOT_PRIVATE_KEY_VAULT_PATH))
                .thenReturn(new RotationSecret(NEW_PRIVATE_KEY, OLD_PRIVATE_KEY));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);

        assertInstanceOf(VaultRotationContext.class, contexts.get(CommonSecretRotationStep.VAULT));
        assertInstanceOf(CustomJobRotationContext.class, contexts.get(CommonSecretRotationStep.CUSTOM_JOB));
        assertInstanceOf(SaltBootConfigRotationContext.class, contexts.get(CommonSecretRotationStep.SALTBOOT_CONFIG));
        assertInstanceOf(UserDataRotationContext.class, contexts.get(CommonSecretRotationStep.USER_DATA));

        SaltBootConfigRotationContext saltBootConfigRotationContext = (SaltBootConfigRotationContext) contexts.get(CommonSecretRotationStep.SALTBOOT_CONFIG);
        SaltBootUpdateConfiguration serviceUpdateConfiguration = saltBootConfigRotationContext.getServiceUpdateConfiguration();
        assertEquals("rotated-security-config.yml", serviceUpdateConfiguration.configFile());
        assertEquals("/etc/salt-bootstrap", serviceUpdateConfiguration.configFolder());
        assertEquals("newPassword", serviceUpdateConfiguration.newSaltBootPassword());
        assertEquals("oldPassword", serviceUpdateConfiguration.oldSaltBootPassword());
        assertEquals(List.of("saltboot.restart-rotated-saltboot"), serviceUpdateConfiguration.serviceRestartActions());
        assertEquals(Set.of("host1"), serviceUpdateConfiguration.targetFqdns());
        assertEquals(Set.of("0.0.0.0"), serviceUpdateConfiguration.targetPrivateIps());
    }

    @Test
    public void testSaltBootContextProviderProvidesAllContextData() {
        when(uncachedSecretServiceForRotation.getRotation(SALT_BOOT_PASSWORD_VAULT_PATH)).thenReturn(new RotationSecret(NEW_PASSWORD, OLD_PASSWORD));
        when(uncachedSecretServiceForRotation.getRotation(SALT_BOOT_PRIVATE_KEY_VAULT_PATH))
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
        assertEquals(List.of("saltboot.restart-rotated-saltboot"), serviceUpdateConfiguration.serviceRestartActions());
        assertEquals(Set.of("host1"), serviceUpdateConfiguration.targetFqdns());
        assertEquals(Set.of("0.0.0.0"), serviceUpdateConfiguration.targetPrivateIps());
    }
}