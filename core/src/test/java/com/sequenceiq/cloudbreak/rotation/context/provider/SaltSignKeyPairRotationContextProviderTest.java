package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import org.testcontainers.shaded.com.google.common.io.BaseEncoding;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
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
    private SaltSecurityConfigService saltSecurityConfigService;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private SecretRotationSaltService secretRotationSaltService;

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
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);

        assertInstanceOf(VaultRotationContext.class, contexts.get(CommonSecretRotationStep.VAULT));
        assertInstanceOf(CustomJobRotationContext.class, contexts.get(CommonSecretRotationStep.CUSTOM_JOB));
    }

    @Test
    void testSaltSignKeyPairCustomJobRotationPhase() throws CloudbreakException {
        saltSecurityConfig.setSaltSignPrivateKey(OLD_PRIVATE_KEY);
        when(stack.getId()).thenReturn(STACK_ID);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);
        customJobRotationContext.getRotationJob().get().run();

        verify(stackDtoService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(clusterBootstrapper, times(1)).bootstrapMachines(eq(STACK_ID), eq(Boolean.TRUE));
    }

    @Test
    void testSaltSignKeyPairCustomJobRollbackPhase() throws CloudbreakException {
        saltSecurityConfig.setSaltSignPrivateKey(OLD_PRIVATE_KEY);
        when(stack.getId()).thenReturn(STACK_ID);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);
        customJobRotationContext.getRollbackJob().get().run();

        verify(stackDtoService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(clusterBootstrapper, times(1)).bootstrapMachines(eq(STACK_ID), eq(Boolean.TRUE));
    }

    @Test
    void testSaltSignKeyPairCustomJobRollbackPhaseFailed() throws CloudbreakException {
        saltSecurityConfig.setSaltSignPrivateKey(OLD_PRIVATE_KEY);
        when(stack.getId()).thenReturn(STACK_ID);
        doThrow(new CloudbreakException("very serious error")).when(clusterBootstrapper).bootstrapMachines(eq(STACK_ID), eq(Boolean.TRUE));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> customJobRotationContext.getRollbackJob().get().run());

        assertEquals("very serious error", secretRotationException.getMessage());
        verify(stackDtoService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(clusterBootstrapper, times(1)).bootstrapMachines(eq(STACK_ID), eq(Boolean.TRUE));
    }

    @Test
    void testSaltSignKeyPairCustomJobPreValidatePhaseNodeMissingPingResponse() throws CloudbreakOrchestratorFailedException {
        saltSecurityConfig.setSaltSignPrivateKey(OLD_PRIVATE_KEY);
        GatewayConfig primaryGatewayConfig = new GatewayConfig("conn", "public", "private", 1234, "instance1", Boolean.FALSE);
        doThrow(new SecretRotationException("Salt ping failed")).when(secretRotationSaltService).validateSalt(eq(stack));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);

        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> customJobRotationContext.getPreValidateJob().get().run());

        verify(stackDtoService, times(2)).getByCrn(eq(RESOURCE_CRN));
        assertEquals("Salt ping failed", secretRotationException.getMessage());
    }

    @Test
    void testSaltSignKeyPairCustomJobPostValidatePhaseNodeMissingPingResponse() throws CloudbreakOrchestratorFailedException {
        saltSecurityConfig.setSaltSignPrivateKey(OLD_PRIVATE_KEY);
        GatewayConfig primaryGatewayConfig = new GatewayConfig("conn", "public", "private", 1234, "instance1", Boolean.FALSE);
        doThrow(new SecretRotationException("Salt ping failed")).when(secretRotationSaltService).validateSalt(eq(stack));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);

        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> customJobRotationContext.getPostValidateJob().get().run());

        verify(stackDtoService, times(2)).getByCrn(eq(RESOURCE_CRN));
        assertEquals("Salt ping failed", secretRotationException.getMessage());
    }

    @Test
    void testSaltSignKeyPairCustomJobFinalizePhase() {
        saltSecurityConfig.setSaltSignPrivateKey(OLD_PRIVATE_KEY);
        saltSecurityConfig.setSaltSignPublicKey("salt sign public key");
        when(stack.getId()).thenReturn(STACK_ID);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.get(CommonSecretRotationStep.CUSTOM_JOB);
        customJobRotationContext.getFinalizeJob().get().run();

        verify(stackDtoService, times(2)).getByCrn(eq(RESOURCE_CRN));
        ArgumentCaptor<SaltSecurityConfig> saltSecurityConfigArgumentCaptor = ArgumentCaptor.forClass(SaltSecurityConfig.class);
        verify(saltSecurityConfigService, times(1)).save(saltSecurityConfigArgumentCaptor.capture());
        SaltSecurityConfig savedSaltSecurityConfig = saltSecurityConfigArgumentCaptor.getValue();
        assertNull(savedSaltSecurityConfig.getLegacySaltSignPublicKey());
    }

    private InstanceMetadataView instance(String instanceId, boolean reachable) {
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        lenient().when(instanceMetadataView.getInstanceId()).thenReturn(instanceId);
        when(instanceMetadataView.isReachable()).thenReturn(reachable);
        return instanceMetadataView;
    }
}