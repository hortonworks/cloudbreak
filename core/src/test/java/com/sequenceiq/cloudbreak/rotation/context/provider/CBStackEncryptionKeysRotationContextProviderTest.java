package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AWS_DEFAULT_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyRotationRequest;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.encryption.CloudInformationDecorator;
import com.sequenceiq.cloudbreak.service.encryption.CloudInformationDecoratorProvider;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class CBStackEncryptionKeysRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final Long STACK_ID = 1L;

    private static final String REGION = "region";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String LUKS_KEY_ARN = "luksKeyArn";

    private static final String CLOUD_SECRET_MANAGER_KEY_ARN = "cloudSecretManagerKeyArn";

    @Mock
    private StackService stackService;

    @Mock
    private StackEncryptionService stackEncryptionService;

    @Mock
    private EncryptionKeyService encryptionKeyService;

    @Mock
    private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;

    @Mock
    private CloudInformationDecorator cloudInformationDecorator;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @InjectMocks
    private CBStackEncryptionKeysRotationContextProvider underTest;

    @Captor
    private ArgumentCaptor<EncryptionKeyRotationRequest> encryptionKeyRotationRequestCaptor;

    @Test
    void testGetContexts() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getRegion()).thenReturn(REGION);
        when(stack.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stack.getPlatformVariant()).thenReturn(AWS_NATIVE_GOV_VARIANT.variant().getValue());
        when(stackService.getByCrn(RESOURCE_CRN)).thenReturn(stack);
        StackEncryption stackEncryption = mock(StackEncryption.class);
        when(stackEncryption.getEncryptionKeyLuks()).thenReturn(LUKS_KEY_ARN);
        when(stackEncryption.getEncryptionKeyCloudSecretManager()).thenReturn(CLOUD_SECRET_MANAGER_KEY_ARN);
        when(stackEncryptionService.getStackEncryption(STACK_ID)).thenReturn(stackEncryption);
        EncryptionResources encryptionResources = mock(EncryptionResources.class);
        when(encryptionKeyService.getEncryptionResources(stack)).thenReturn(encryptionResources);
        when(cloudInformationDecoratorProvider.getForStack(stack)).thenReturn(cloudInformationDecorator);
        when(cloudInformationDecorator.getLuksEncryptionKeyResourceType()).thenReturn(ResourceType.AWS_KMS_KEY);
        when(cloudInformationDecorator.getCloudSecretManagerEncryptionKeyResourceType()).thenReturn(ResourceType.AWS_KMS_KEY);
        CloudResource luksCloudResource = mock(CloudResource.class);
        when(resourceRetriever.findByResourceReferencesAndStatusAndTypeAndStack(
                List.of(LUKS_KEY_ARN), CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID))
                .thenReturn(List.of(luksCloudResource));
        CloudResource cloudSecretManagerCloudResource = mock(CloudResource.class);
        when(resourceRetriever.findByResourceReferencesAndStatusAndTypeAndStack(
                List.of(CLOUD_SECRET_MANAGER_KEY_ARN), CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID))
                .thenReturn(List.of(cloudSecretManagerCloudResource));
        Credential credential = mock(Credential.class);
        when(credentialClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(credential);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(true)
                .build());

        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        assertEquals(1, result.size());

        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) result.get(CommonSecretRotationStep.CUSTOM_JOB);
        assertDoesNotThrow(customJobRotationContext.getPreValidateJob().get()::run);
        assertDoesNotThrow(customJobRotationContext.getRotationJob().get()::run);
        verify(encryptionResources).rotateEncryptionKey(encryptionKeyRotationRequestCaptor.capture());
        EncryptionKeyRotationRequest encryptionKeyRotationRequest = encryptionKeyRotationRequestCaptor.getValue();
        assertThat(encryptionKeyRotationRequest.cloudResources()).containsExactlyInAnyOrder(luksCloudResource, cloudSecretManagerCloudResource);
        assertThat(encryptionKeyRotationRequest.cloudContext().getLocation().getRegion().value()).isEqualTo(REGION);
        assertThat(encryptionKeyRotationRequest.cloudContext().getLocation().getAvailabilityZone().value()).isEqualTo(AVAILABILITY_ZONE);
        assertThat(encryptionKeyRotationRequest.cloudCredential()).isEqualTo(cloudCredential);
    }

    @Test
    void testGetContextsWhenSecretEncryptionIsNotEnabled() {
        Stack stack = mock(Stack.class);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stack.getPlatformVariant()).thenReturn(AWS_NATIVE_GOV_VARIANT.variant().getValue());
        when(stackService.getByCrn(RESOURCE_CRN)).thenReturn(stack);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(false)
                .build());

        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        assertEquals(1, result.size());

        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) result.get(CommonSecretRotationStep.CUSTOM_JOB);
        assertThrows(SecretRotationException.class, customJobRotationContext.getPreValidateJob().get()::run,
                "Stack encryption key rotation is only available on environments with secret encryption enabled.");
    }

    @Test
    void testGetContextsWhenNotGovVariant() {
        Stack stack = mock(Stack.class);
        when(stack.getPlatformVariant()).thenReturn(AWS_DEFAULT_VARIANT.getValue());
        when(stackService.getByCrn(RESOURCE_CRN)).thenReturn(stack);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.getContexts(RESOURCE_CRN),
                "Stack encryption key rotation is only available on AWS Gov environments.");
    }

    @Test
    void testGetSecret() {
        assertThat(underTest.getSecret()).isEqualTo(CloudbreakSecretType.STACK_ENCRYPTION_KEYS);
    }
}
