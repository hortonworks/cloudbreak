package com.sequenceiq.freeipa.service.rotation.salt.contextprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.SaltSecurityConfigService;
import com.sequenceiq.freeipa.service.orchestrator.FreeIpaSaltPingService;
import com.sequenceiq.freeipa.service.orchestrator.SaltPingFailedException;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class SaltMasterKeyPairRotationContextProviderTest {

    private static final Long STACK_ID = 1L;

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final String PRIVATE_KEY_VAULT_PATH = "secret/privateKeyPath";

    private static final String NEW_PRIVATE_KEY = privateKey();

    private static final String OLD_PRIVATE_KEY = privateKey();

    @InjectMocks
    private SaltMasterKeyPairRotationContextProvider underTest;

    @Mock
    private StackService stackService;

    @Mock
    private SaltSecurityConfigService saltSecurityConfigService;

    @Mock
    private BootstrapService bootstrapService;

    @Mock
    private FreeIpaSaltPingService freeIpaSaltPingService;

    @Mock
    private Stack stack;

    @Spy
    private SaltSecurityConfig saltSecurityConfig;

    private static String privateKey() {
        return BaseEncoding.base64().encode(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()).getBytes());
    }

    @BeforeEach
    void setUp() {
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
    }

    @Test
    void testSaltMasterKeyPairContextProviderProvidesAllContextData() {
        saltSecurityConfig.setSaltMasterPrivateKeyVault(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(ENVIRONMENT_CRN);

        assertInstanceOf(VaultRotationContext.class, contexts.get(CommonSecretRotationStep.VAULT));
        assertInstanceOf(CustomJobRotationContext.class, contexts.get(CommonSecretRotationStep.CUSTOM_JOB));
    }

    @Test
    void testSaltMasterKeyPairContextProviderWhenKeyPairIsMissingFromDatabase() {
        doAnswer(invocation -> {
            SaltSecurityConfig saltSecurityConfig = invocation.getArgument(0, SaltSecurityConfig.class);
            saltSecurityConfig.setSaltMasterPrivateKeyVault(new Secret(saltSecurityConfig.getSaltMasterPrivateKeyVault(), PRIVATE_KEY_VAULT_PATH));
            return saltSecurityConfig;
        }).when(saltSecurityConfigService).save(any());
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(ENVIRONMENT_CRN);

        assertInstanceOf(VaultRotationContext.class, contexts.get(CommonSecretRotationStep.VAULT));
        assertInstanceOf(CustomJobRotationContext.class, contexts.get(CommonSecretRotationStep.CUSTOM_JOB));
        ArgumentCaptor<SaltSecurityConfig> saltSecurityConfigArgumentCaptor = ArgumentCaptor.forClass(SaltSecurityConfig.class);
        verify(saltSecurityConfigService, times(1)).save(saltSecurityConfigArgumentCaptor.capture());
        assertNotNull(saltSecurityConfigArgumentCaptor.getValue().getSaltMasterPrivateKeyVault());
        assertNotNull(saltSecurityConfigArgumentCaptor.getValue().getSaltMasterPublicKey());
    }

    @Test
    void testSaltMasterKeyPairCustomJobRotationPhase() throws CloudbreakOrchestratorException {
        saltSecurityConfig.setSaltMasterPrivateKeyVault(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        when(stack.getId()).thenReturn(STACK_ID);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(ENVIRONMENT_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);
        customJobRotationContext.getRotationJob().get().run();

        verify(stackService, times(1)).getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString());
        verify(saltSecurityConfigService, never()).save(any());
        verify(bootstrapService, times(1)).bootstrap(eq(STACK_ID), eq(Boolean.TRUE));
    }

    @Test
    void testSaltMasterKeyPairCustomJobRollbackPhase() throws CloudbreakOrchestratorException {
        saltSecurityConfig.setSaltMasterPrivateKeyVault(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        when(stack.getId()).thenReturn(STACK_ID);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(ENVIRONMENT_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);
        customJobRotationContext.getRollbackJob().get().run();

        verify(stackService, times(1)).getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString());
        verify(saltSecurityConfigService, never()).save(any());
        verify(bootstrapService, times(1)).bootstrap(eq(STACK_ID), eq(Boolean.TRUE));
    }

    @Test
    void testSaltMasterKeyPairCustomJobRollbackPhaseFailed() throws CloudbreakOrchestratorException {
        saltSecurityConfig.setSaltMasterPrivateKeyVault(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        when(stack.getId()).thenReturn(STACK_ID);
        doThrow(new CloudbreakOrchestratorFailedException("very serious error")).when(bootstrapService).bootstrap(eq(STACK_ID), eq(Boolean.TRUE));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(ENVIRONMENT_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> customJobRotationContext.getRollbackJob().get().run());

        assertEquals("very serious error", secretRotationException.getMessage());
        verify(stackService, times(1)).getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString());
        verify(saltSecurityConfigService, never()).save(any());
        verify(bootstrapService, times(1)).bootstrap(eq(STACK_ID), eq(Boolean.TRUE));
    }

    @Test
    void testSaltMasterKeyPairCustomJobPreValidatePhaseWhenStoppedInstanceExists() throws SaltPingFailedException {
        saltSecurityConfig.setSaltMasterPrivateKeyVault(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        doThrow(new SaltPingFailedException("ping error", new RuntimeException("error"))).when(freeIpaSaltPingService).saltPing(eq(stack));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(ENVIRONMENT_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);

        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> customJobRotationContext.getPreValidateJob().get().run());

        verify(stackService, times(2)).getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString());
        assertEquals("ping error", secretRotationException.getMessage());
    }

    @Test
    void testSaltMasterKeyPairCustomJobPostValidatePhaseWhenStoppedInstanceExists() throws SaltPingFailedException {
        saltSecurityConfig.setSaltMasterPrivateKeyVault(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        doThrow(new SaltPingFailedException("ping error", new RuntimeException("error"))).when(freeIpaSaltPingService).saltPing(eq(stack));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(ENVIRONMENT_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);

        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> customJobRotationContext.getPostValidateJob().get().run());

        verify(stackService, times(2)).getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString());
        assertEquals("ping error", secretRotationException.getMessage());
    }

    @Test
    void testSaltMasterKeyPairCustomJobFinalizePhase() {
        saltSecurityConfig.setSaltMasterPrivateKeyVault(new Secret(OLD_PRIVATE_KEY, PRIVATE_KEY_VAULT_PATH));
        saltSecurityConfig.setSaltMasterPublicKey("salt master public key");
        when(stack.getId()).thenReturn(STACK_ID);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(ENVIRONMENT_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);
        customJobRotationContext.getFinalizeJob().get().run();

        verify(stackService, times(2)).getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString());
        ArgumentCaptor<SaltSecurityConfig> saltSecurityConfigArgumentCaptor = ArgumentCaptor.forClass(SaltSecurityConfig.class);
        verify(saltSecurityConfigService, times(1)).save(saltSecurityConfigArgumentCaptor.capture());
        SaltSecurityConfig savedSaltSecurityConfig = saltSecurityConfigArgumentCaptor.getValue();
        assertNull(savedSaltSecurityConfig.getLegacySaltMasterPublicKey());
    }

    private InstanceMetaData instance(String instanceId, InstanceStatus instanceStatus) {
        InstanceMetaData instanceMetadata = new InstanceMetaData();
        instanceMetadata.setInstanceId(instanceId);
        instanceMetadata.setInstanceStatus(instanceStatus);
        return instanceMetadata;
    }
}