package com.sequenceiq.freeipa.service.rotation.lukspassphrase.contextprovider;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AWS_DEFAULT_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.rotation.ExitCriteriaProvider;
import com.sequenceiq.freeipa.service.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeipaLUKSVolumePassphraseRotationContextProviderTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @InjectMocks
    private FreeipaLUKSVolumePassphraseRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        Stack stack = mock(Stack.class);
        when(stack.getEnvironmentCrn()).thenReturn(RESOURCE_CRN);
        when(stack.getPlatformvariant()).thenReturn(AWS_NATIVE_GOV_VARIANT.variant().getValue());
        InstanceMetaData instance1 = mock(InstanceMetaData.class);
        InstanceMetaData instance2 = mock(InstanceMetaData.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        ExitCriteriaModel exitCriteriaModel = mock(ExitCriteriaModel.class);
        when(exitCriteriaProvider.get(stack)).thenReturn(exitCriteriaModel);
        when(instance1.getDiscoveryFQDN()).thenReturn("instance1");
        when(instance1.isAvailable()).thenReturn(true);
        when(instance2.getDiscoveryFQDN()).thenReturn("instance2");
        when(instance2.isAvailable()).thenReturn(true);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instance1, instance2));
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(RESOURCE_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);

        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        assertEquals(2, result.size());

        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) result.get(CommonSecretRotationStep.CUSTOM_JOB);
        assertEquals(RESOURCE_CRN, customJobRotationContext.getResourceCrn());
        assertDoesNotThrow(() -> customJobRotationContext.getPreValidateJob().get().run());

        SaltStateApplyRotationContext saltStateApplyRotationContext = (SaltStateApplyRotationContext) result.get(FreeIpaSecretRotationStep.SALT_STATE_APPLY);
        assertEquals(RESOURCE_CRN, saltStateApplyRotationContext.getResourceCrn());
        assertEquals(gatewayConfig, saltStateApplyRotationContext.getGatewayConfig());
        assertThat(saltStateApplyRotationContext.getTargets()).hasSameElementsAs(Set.of("instance1", "instance2"));
        assertEquals(exitCriteriaModel, saltStateApplyRotationContext.getExitCriteriaModel());
        assertThat(saltStateApplyRotationContext.getStates()).containsExactly("rotateluks");
        assertThat(saltStateApplyRotationContext.getCleanupStates().get()).containsExactly("rotateluks/finalize");
        assertThat(saltStateApplyRotationContext.getRollBackStates().get()).containsExactly("rotateluks/rollback");
    }

    @Test
    void testGetContextsWhenNotAllInstancesAreAvailable() {
        Stack stack = mock(Stack.class);
        when(stack.getEnvironmentCrn()).thenReturn(RESOURCE_CRN);
        when(stack.getPlatformvariant()).thenReturn(AWS_NATIVE_GOV_VARIANT.variant().getValue());
        InstanceMetaData instance1 = mock(InstanceMetaData.class);
        InstanceMetaData instance2 = mock(InstanceMetaData.class);
        lenient().when(instance1.isAvailable()).thenReturn(true);
        when(instance2.isAvailable()).thenReturn(false);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instance1, instance2));
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(RESOURCE_CRN, ACCOUNT_ID)).thenReturn(stack);

        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        assertEquals(2, result.size());

        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) result.get(CommonSecretRotationStep.CUSTOM_JOB);
        assertEquals(RESOURCE_CRN, customJobRotationContext.getResourceCrn());
        assertThrows(CloudbreakRuntimeException.class, () -> customJobRotationContext.getPreValidateJob().get().run());
    }

    @Test
    void testGetContextsWhenNotGovVariant() {
        Stack stack = mock(Stack.class);
        when(stack.getPlatformvariant()).thenReturn(AWS_DEFAULT_VARIANT.getValue());
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(RESOURCE_CRN, ACCOUNT_ID)).thenReturn(stack);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.getContexts(RESOURCE_CRN));
        assertEquals("LUKS passphrase rotation is only available on AWS Gov environments.", exception.getMessage());
    }

    @Test
    void testGetSecret() {
        assertEquals(FreeIpaSecretType.LUKS_VOLUME_PASSPHRASE, underTest.getSecret());
    }
}
