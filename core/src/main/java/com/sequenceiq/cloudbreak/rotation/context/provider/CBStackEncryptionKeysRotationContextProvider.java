package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyRotationRequest;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
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
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CBStackEncryptionKeysRotationContextProvider implements RotationContextProvider {

    @Inject
    private StackService stackService;

    @Inject
    private StackEncryptionService stackEncryptionService;

    @Inject
    private EncryptionKeyService encryptionKeyService;

    @Inject
    private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        Stack stack = stackService.getByCrn(resourceCrn);
        if (!AWS_NATIVE_GOV_VARIANT.variant().getValue().equals(stack.getPlatformVariant())) {
            throw new BadRequestException("Stack encryption key rotation is only available on AWS Gov environments.");
        }

        CustomJobRotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> {
                    DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
                    if (!environment.isEnableSecretEncryption()) {
                        throw new SecretRotationException("Stack encryption key rotation is only available on environments with secret encryption enabled.");
                    }
                })
                .withRotationJob(() -> {
                    StackEncryption stackEncryption = stackEncryptionService.getStackEncryption(stack.getId());
                    EncryptionResources encryptionResources = encryptionKeyService.getEncryptionResources(stack);
                    CloudInformationDecorator cloudInformationDecorator = cloudInformationDecoratorProvider.getForStack(stack);
                    rotateStackEncryptionKeys(stackEncryption, encryptionResources, stack, cloudInformationDecorator);
                })
                .build();
        return Map.of(CUSTOM_JOB, customJobRotationContext);
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.STACK_ENCRYPTION_KEYS;
    }

    private void rotateStackEncryptionKeys(StackEncryption stackEncryption, EncryptionResources encryptionResources, Stack stack,
            CloudInformationDecorator cloudInformationDecorator) {
        CloudResource luksKey = resourceRetriever.findByResourceReferencesAndStatusAndTypeAndStack(
                List.of(stackEncryption.getEncryptionKeyLuks()), CommonStatus.CREATED,
                cloudInformationDecorator.getLuksEncryptionKeyResourceType(), stack.getId()).getFirst();
        CloudResource cloudSecretManagerKey = resourceRetriever.findByResourceReferencesAndStatusAndTypeAndStack(
                List.of(stackEncryption.getEncryptionKeyCloudSecretManager()), CommonStatus.CREATED,
                cloudInformationDecorator.getCloudSecretManagerEncryptionKeyResourceType(), stack.getId()).getFirst();
        CloudContext cloudContext = getCloudContext(stack);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn()));

        encryptionResources.rotateEncryptionKey(EncryptionKeyRotationRequest.builder()
                .withCloudResources(List.of(luksKey, cloudSecretManagerKey))
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .build());
    }

    private CloudContext getCloudContext(Stack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        return CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withWorkspaceId(stack.getWorkspaceId())
                .withTenantId(stack.getTenantId())
                .withLocation(location)
                .build();
    }
}
