package com.sequenceiq.cloudbreak.controller.validation.stack;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateV4RequestValidator;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.PlatformResourceClientService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class StackV4RequestValidator implements Validator<StackV4Request> {

    @Inject
    private InstanceTemplateV4RequestValidator templateRequestValidator;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private PlatformResourceClientService platformResourceClientService;

    @Override
    public ValidationResult validate(StackV4Request subject) {
        ValidationResultBuilder validationBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(subject.getInstanceGroups())) {
            validationBuilder.error("Stack request must contain instance groups.");
        }
        validateTemplates(subject, validationBuilder);
        validateEncryptionKey(subject, validationBuilder);
        return validationBuilder.build();
    }

    private void validateTemplates(StackV4Request stackRequest, ValidationResultBuilder resultBuilder) {
        stackRequest.getInstanceGroups()
                .stream()
                .map(i -> templateRequestValidator.validate(i.getTemplate()))
                .reduce(ValidationResult::merge)
                .ifPresent(resultBuilder::merge);
    }

    private void validateEncryptionKey(StackV4Request stackRequest, ValidationResultBuilder validationBuilder) {
        if (StringUtils.isEmpty(stackRequest.getEnvironmentCrn())) {
            validationBuilder.error("Environment CRN cannot be null or empty.");
            return;
        }
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stackRequest.getEnvironmentCrn());
        stackRequest.getInstanceGroups().stream()
                .filter(request -> isEncryptionTypeSetUp(request.getTemplate()))
                .filter(request -> {
                    EncryptionType valueForTypeKey = getEncryptionType(request.getTemplate());
                    return EncryptionType.CUSTOM.equals(valueForTypeKey);
                })
                .forEach(request -> {
                    checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(request, environment.getCredential().getName(),
                            stackRequest.getPlacement().getRegion(), validationBuilder);
                });
    }

    private EncryptionType getEncryptionType(InstanceTemplateV4Request template) {
        if (template.getAws() != null && template.getAws().getEncryption() != null && template.getAws().getEncryption().getType() != null) {
            return template.getAws().getEncryption().getType();
        }
        if (template.getGcp() != null && template.getGcp().getEncryption() != null && template.getGcp().getEncryption().getType() != null) {
            return template.getGcp().getEncryption().getType();
        }
        return null;
    }

    private void checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(InstanceGroupV4Request instanceGroupRequest,
        String credentialName, String region, ValidationResultBuilder validationBuilder) {
        Optional<CloudEncryptionKeys> keys = getEncryptionKeysWithExceptionHandling(credentialName, region);
        if (keys.isPresent() && !keys.get().getCloudEncryptionKeys().isEmpty()) {
            if (getEncryptionKey(instanceGroupRequest.getTemplate()) == null) {
                validationBuilder.error("There is no encryption key provided but CUSTOM type is given for encryption.");
            } else if (keys.get().getCloudEncryptionKeys().stream().map(CloudEncryptionKey::getName)
                    .noneMatch(s -> s.equals(getEncryptionKey(instanceGroupRequest.getTemplate())))) {
                validationBuilder.error("The provided encryption key does not exists in the given region's encryption key list for this credential.");
            }
        }
    }

    private String getEncryptionKey(InstanceTemplateV4Request template) {
        if (template.getAws() != null && template.getAws().getEncryption() != null && template.getAws().getEncryption().getKey() != null) {
            return template.getAws().getEncryption().getKey();
        }
        if (template.getGcp() != null && template.getGcp().getEncryption() != null && template.getGcp().getEncryption().getKey() != null) {
            return template.getGcp().getEncryption().getKey();
        }
        return null;
    }

    private Optional<CloudEncryptionKeys> getEncryptionKeysWithExceptionHandling(String credentialName, String region) {
        try {
            CloudEncryptionKeys cloudEncryptionKeys = platformResourceClientService.getEncryptionKeys(credentialName, region);
            return Optional.ofNullable(cloudEncryptionKeys);
        } catch (RuntimeException ignore) {
            return Optional.empty();
        }
    }

    private boolean isEncryptionTypeSetUp(InstanceTemplateV4Request template) {
        return getEncryptionType(template) != null;
    }
}
