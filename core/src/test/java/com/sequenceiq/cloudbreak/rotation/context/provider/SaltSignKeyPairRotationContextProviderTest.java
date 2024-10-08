package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.google.common.io.BaseEncoding;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class SaltSignKeyPairRotationContextProviderTest {

    private static final Long STACK_ID = 1L;

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String PRIVATE_KEY_VAULT_PATH = "secret/privateKeyPath";

    private static final String NEW_PRIVATE_KEY = privateKey();

    private static final String OLD_PRIVATE_KEY = privateKey();

    @InjectMocks
    private SaltSignKeyPairRotationContextProvider underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Mock
    private SaltSecurityConfigService saltSecurityConfigService;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private StackDto stack;

    @Spy
    private SaltSecurityConfig saltSecurityConfig;

    private static String privateKey() {
        return BaseEncoding.base64().encode(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()).getBytes());
    }

    @BeforeEach
    void setUp() {
        when(stackDtoService.getByCrn(anyString())).thenReturn(stack);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
    }

    @Test
    void testSaltSignKeyPairContextProviderProvidesAllContextData() {
        saltSecurityConfig.setSaltSignPrivateKey(OLD_PRIVATE_KEY);
        when(saltSecurityConfig.getSaltSignPrivateKeySecret()).thenReturn(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);

        assertInstanceOf(VaultRotationContext.class, contexts.get(CommonSecretRotationStep.VAULT));
        assertInstanceOf(CustomJobRotationContext.class, contexts.get(CommonSecretRotationStep.CUSTOM_JOB));
    }

    @Test
    void testSaltSignKeyPairCustomJobRotationPhase() throws CloudbreakException {
        saltSecurityConfig.setSaltSignPrivateKey(OLD_PRIVATE_KEY);
        when(saltSecurityConfig.getSaltSignPrivateKeySecret()).thenReturn(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        when(uncachedSecretServiceForRotation.getRotation(PRIVATE_KEY_VAULT_PATH)).thenReturn(new RotationSecret(NEW_PRIVATE_KEY, OLD_PRIVATE_KEY));
        when(stack.getId()).thenReturn(STACK_ID);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);
        customJobRotationContext.getRotationJob().get().run();

        verify(stackDtoService, times(2)).getByCrn(eq(RESOURCE_CRN));
        ArgumentCaptor<SaltSecurityConfig> saltSecurityConfigArgumentCaptor = ArgumentCaptor.forClass(SaltSecurityConfig.class);
        verify(saltSecurityConfigService, times(1)).save(saltSecurityConfigArgumentCaptor.capture());
        assertNotNull(saltSecurityConfigArgumentCaptor.getValue());
        assertEquals(PkiUtil.calculatePemPublicKeyInBase64(NEW_PRIVATE_KEY), saltSecurityConfigArgumentCaptor.getValue().getSaltSignPublicKey());
        verify(clusterBootstrapper, times(1)).bootstrapMachines(eq(STACK_ID), eq(Boolean.TRUE));
    }

    @Test
    void testSaltSignKeyPairCustomJobRollbackPhase() throws CloudbreakException {
        saltSecurityConfig.setSaltSignPrivateKey(OLD_PRIVATE_KEY);
        when(saltSecurityConfig.getSaltSignPrivateKeySecret()).thenReturn(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        when(uncachedSecretServiceForRotation.getRotation(PRIVATE_KEY_VAULT_PATH)).thenReturn(new RotationSecret(NEW_PRIVATE_KEY, OLD_PRIVATE_KEY));
        when(stack.getId()).thenReturn(STACK_ID);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);
        customJobRotationContext.getRollbackJob().get().run();

        verify(stackDtoService, times(2)).getByCrn(eq(RESOURCE_CRN));
        ArgumentCaptor<SaltSecurityConfig> saltSecurityConfigArgumentCaptor = ArgumentCaptor.forClass(SaltSecurityConfig.class);
        verify(saltSecurityConfigService, times(1)).save(saltSecurityConfigArgumentCaptor.capture());
        assertNotNull(saltSecurityConfigArgumentCaptor.getValue());
        assertEquals(PkiUtil.calculatePemPublicKeyInBase64(OLD_PRIVATE_KEY), saltSecurityConfigArgumentCaptor.getValue().getSaltSignPublicKey());
        verify(clusterBootstrapper, times(1)).bootstrapMachines(eq(STACK_ID), eq(Boolean.TRUE));
    }

    @Test
    void testSaltSignKeyPairCustomJobPreValidatePhaseWhenStoppedInstanceExists() {
        saltSecurityConfig.setSaltSignPrivateKey(OLD_PRIVATE_KEY);
        when(saltSecurityConfig.getSaltSignPrivateKeySecret()).thenReturn(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        List<InstanceMetadataView> instances = List.of(instance("instance1", false), instance("instance2", true));
        when(stack.getAllAvailableInstances()).thenReturn(instances);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);

        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> customJobRotationContext.getPreValidateJob().get().run());

        verify(stackDtoService, times(2)).getByCrn(eq(RESOURCE_CRN));
        assertEquals("Unreachable instances found: [instance1], salt sign key rotation is not possible!", secretRotationException.getMessage());
    }

    private InstanceMetadataView instance(String instanceId, boolean reachable) {
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        lenient().when(instanceMetadataView.getInstanceId()).thenReturn(instanceId);
        when(instanceMetadataView.isReachable()).thenReturn(reachable);
        return instanceMetadataView;
    }
}