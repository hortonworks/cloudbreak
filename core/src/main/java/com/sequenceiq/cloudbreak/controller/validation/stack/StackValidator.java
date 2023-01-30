package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.LEGACY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE_DATALAKE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateValidator;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.PlatformResourceClientService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class StackValidator {

    @Inject
    private InstanceTemplateValidator templateRequestValidator;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private PlatformResourceClientService platformResourceClientService;

    @Inject
    private EntitlementService entitlementService;

    public void validate(Stack subject, ValidationResult.ValidationResultBuilder validationBuilder) {
        if (CollectionUtils.isEmpty(subject.getInstanceGroups())) {
            validationBuilder.error("Stack request must contain instance groups.");
        }
        validateTemplates(subject, validationBuilder);
        validateEncryptionKey(subject, validationBuilder);
        validateVariant(subject, validationBuilder);
    }

    private void validateVariant(Stack source, ValidationResultBuilder validationBuilder) {
        String variant = source.getPlatformVariant();
        boolean awsNativeEnabled =
                entitlementService.awsNativeEnabled(Crn.safeFromString(source.getResourceCrn()).getAccountId());
        boolean awsNativeDatalakeEnabled = entitlementService.awsNativeDataLakeEnabled(Crn.safeFromString(source.getResourceCrn()).getAccountId());
        if ((WORKLOAD.equals(source.getType()) || LEGACY.equals(source.getType()))
                && AWS_NATIVE_VARIANT.variant().value().equals(variant) && !awsNativeEnabled) {
            validationBuilder.error(String.format("%s entitlement was not granted to your tenant. "
                    + "Please get in contact with Cloudera support to request it.",
                    CDP_CB_AWS_NATIVE.name()));
        }
        if (DATALAKE.equals(source.getType()) && AWS_NATIVE_VARIANT.variant().value().equals(variant) && !awsNativeDatalakeEnabled) {
            validationBuilder.error(String.format("%s entitlement was not granted to your tenant. "
                            + "Please get in contact with Cloudera support to request it.",
                    CDP_CB_AWS_NATIVE_DATALAKE.name()));
        }
    }

    private void validateTemplates(Stack stack, ValidationResultBuilder resultBuilder) {
        stack.getInstanceGroups()
                .stream()
                .map(i -> templateRequestValidator.validate(i.getTemplate()))
                .reduce(ValidationResult::merge)
                .ifPresent(resultBuilder::merge);
    }

    private void validateEncryptionKey(Stack stack, ValidationResultBuilder validationBuilder) {
        if (StringUtils.isEmpty(stack.getEnvironmentCrn())) {
            validationBuilder.error("Environment CRN cannot be null or empty.");
            return;
        }
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        stack.getInstanceGroups().stream()
                .filter(request -> isEncryptionTypeSetUp(request.getTemplate()))
                .filter(request -> {
                    EncryptionType valueForTypeKey = getEncryptionType(request.getTemplate());
                    return EncryptionType.CUSTOM.equals(valueForTypeKey);
                })
                .forEach(request -> {
                    checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(request, environment.getCrn(),
                            stack.getRegion(), validationBuilder);
                });
    }

    private EncryptionType getEncryptionType(Template template) {
        try {
            if (template.getCloudPlatform() != null && template.getAttributes() != null) {
                if (template.getCloudPlatform().equals(CloudPlatform.AWS.name())) {
                    AwsInstanceTemplateV4Parameters parameters = template.getAttributes().get(AwsInstanceTemplateV4Parameters.class);
                    if (parameters != null && parameters.getEncryption() != null) {
                        return parameters.getEncryption().getType();
                    }
                } else if (template.getCloudPlatform().equals(CloudPlatform.GCP.name())) {
                    GcpInstanceTemplateV4Parameters parameters = template.getAttributes().get(GcpInstanceTemplateV4Parameters.class);
                    if (parameters != null && parameters.getEncryption() != null) {
                        return parameters.getEncryption().getType();
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private void checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(InstanceGroup instanceGroup,
        String envCrn, String region, ValidationResultBuilder validationBuilder) {
        Optional<CloudEncryptionKeys> keys = getEncryptionKeysWithExceptionHandling(envCrn, region);
        if (keys.isPresent() && !keys.get().getCloudEncryptionKeys().isEmpty()) {
            if (getEncryptionKey(instanceGroup.getTemplate()) == null) {
                validationBuilder.error("There is no encryption key provided but CUSTOM type is given for encryption.");
            } else if (keys.get().getCloudEncryptionKeys().stream().map(CloudEncryptionKey::getName)
                    .noneMatch(s -> s.equals(getEncryptionKey(instanceGroup.getTemplate())))) {
                validationBuilder.error("The provided encryption key does not exists in the given region's encryption key list for this credential.");
            }
        }
    }

    private String getEncryptionKey(Template template) {
        try {
            if (template.getCloudPlatform() != null && template.getAttributes() != null) {
                if (template.getCloudPlatform().equals(CloudPlatform.AWS.name())) {
                    AwsInstanceTemplateV4Parameters parameters = template.getAttributes().get(AwsInstanceTemplateV4Parameters.class);
                    if (parameters != null && parameters.getEncryption() != null) {
                        return parameters.getEncryption().getKey();
                    }
                } else if (template.getCloudPlatform().equals(CloudPlatform.GCP.name())) {
                    GcpInstanceTemplateV4Parameters parameters = template.getAttributes().get(GcpInstanceTemplateV4Parameters.class);
                    if (parameters != null && parameters.getEncryption() != null) {
                        return parameters.getEncryption().getKey();
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private Optional<CloudEncryptionKeys> getEncryptionKeysWithExceptionHandling(String envCrn, String region) {
        try {
            CloudEncryptionKeys cloudEncryptionKeys = platformResourceClientService.getEncryptionKeys(envCrn, region);
            return Optional.ofNullable(cloudEncryptionKeys);
        } catch (RuntimeException ignore) {
            return Optional.empty();
        }
    }

    private boolean isEncryptionTypeSetUp(Template template) {
        return getEncryptionType(template) != null;
    }
}
