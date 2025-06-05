package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AWS_DEFAULT_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class CBLUKSVolumePassphraseRotationContextProviderTest {

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @InjectMocks
    private CBLUKSVolumePassphraseRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        StackDto stack = mock(StackDto.class);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(stack.getPlatformVariant()).thenReturn(AWS_NATIVE_GOV_VARIANT.variant().getValue());
        InstanceMetadataView instance1 = mock(InstanceMetadataView.class);
        InstanceMetadataView instance2 = mock(InstanceMetadataView.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        ExitCriteriaModel exitCriteriaModel = mock(ExitCriteriaModel.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stack);
        when(exitCriteriaProvider.get(stack)).thenReturn(exitCriteriaModel);
        when(instance1.getDiscoveryFQDN()).thenReturn("instance1");
        when(instance1.isReachable()).thenReturn(true);
        when(instance2.getDiscoveryFQDN()).thenReturn("instance2");
        when(instance2.isReachable()).thenReturn(true);
        when(stack.getNotDeletedInstanceMetaData()).thenReturn(List.of(instance1, instance2));
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(true)
                .build());

        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        assertEquals(2, result.size());

        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) result.get(CommonSecretRotationStep.CUSTOM_JOB);
        assertEquals(RESOURCE_CRN, customJobRotationContext.getResourceCrn());
        assertDoesNotThrow(customJobRotationContext.getPreValidateJob().get()::run);

        SaltStateApplyRotationContext saltStateApplyRotationContext = (SaltStateApplyRotationContext) result.get(CloudbreakSecretRotationStep.SALT_STATE_APPLY);
        assertEquals(RESOURCE_CRN, saltStateApplyRotationContext.getResourceCrn());
        assertEquals(gatewayConfig, saltStateApplyRotationContext.getGatewayConfig());
        assertThat(saltStateApplyRotationContext.getTargets()).hasSameElementsAs(Set.of("instance1", "instance2"));
        assertEquals(exitCriteriaModel, saltStateApplyRotationContext.getExitCriteriaModel());
        assertThat(saltStateApplyRotationContext.getStates()).containsExactly("rotateluks");
        assertThat(saltStateApplyRotationContext.getCleanupStates().get()).containsExactly("rotateluks/finalize");
        assertThat(saltStateApplyRotationContext.getRollBackStates().get()).containsExactly("rotateluks/rollback");
    }

    @Test
    void testGetContextsWhenSecretEncryptionIsNotEnabled() {
        StackDto stack = mock(StackDto.class);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(stack.getPlatformVariant()).thenReturn(AWS_NATIVE_GOV_VARIANT.variant().getValue());
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stack);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(false)
                .build());

        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        assertEquals(2, result.size());

        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) result.get(CommonSecretRotationStep.CUSTOM_JOB);
        assertEquals(RESOURCE_CRN, customJobRotationContext.getResourceCrn());
        assertThrows(SecretRotationException.class, customJobRotationContext.getPreValidateJob().get()::run,
                "LUKS passphrase rotation is only available on environments with secret encryption enabled.");
    }

    @Test
    void testGetContextsWhenNotAllInstancesAreAvailable() {
        StackDto stack = mock(StackDto.class);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(stack.getPlatformVariant()).thenReturn(AWS_NATIVE_GOV_VARIANT.variant().getValue());
        InstanceMetadataView instance1 = mock(InstanceMetadataView.class);
        InstanceMetadataView instance2 = mock(InstanceMetadataView.class);
        lenient().when(instance1.isReachable()).thenReturn(true);
        when(instance2.isReachable()).thenReturn(false);
        when(stack.getNotDeletedInstanceMetaData()).thenReturn(List.of(instance1, instance2));
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stack);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(true)
                .build());

        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        assertEquals(2, result.size());

        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) result.get(CommonSecretRotationStep.CUSTOM_JOB);
        assertEquals(RESOURCE_CRN, customJobRotationContext.getResourceCrn());
        assertThrows(SecretRotationException.class, customJobRotationContext.getPreValidateJob().get()::run,
                "All instances of the stack need to be in a reachable state before starting the 'LUKS_VOLUME_PASSPHRASE' rotation.");
    }

    @Test
    void testGetContextsWhenNotGovVariant() {
        StackDto stack = mock(StackDto.class);
        when(stack.getPlatformVariant()).thenReturn(AWS_DEFAULT_VARIANT.getValue());
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stack);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.getContexts(RESOURCE_CRN),
                "LUKS passphrase rotation is only available on AWS Gov environments.");
    }

    @Test
    void testGetSecret() {
        assertEquals(CloudbreakSecretType.LUKS_VOLUME_PASSPHRASE, underTest.getSecret());
    }
}
