package com.sequenceiq.cloudbreak.controller.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;

public class CredentialParametersValidator implements ConstraintValidator<ValidCredentialRequest, CredentialRequest> {

    private static final String KEYSTONE_VERSION_V2 = "cb-keystone-v2";
    private static final String KEYSTONE_VERSION_V3 = "cb-keystone-v3";
    private static final String KEYSTONE_V3_DEFAULT_SCOPE = "cb-keystone-v3-default-scope";
    private static final String KEYSTONE_V3_DOMAIN_SCOPE = "cb-keystone-v3-domain-scope";
    private static final String KEYSTONE_V3_PROJECT_SCOPE = "cb-keystone-v3-project-scope";
    private static final String NULL_STRING = "null";

    @Inject
    private List<ParameterValidator> parameterValidators;

    private List<TemplateParam> requiredAWSParams = new ArrayList<>();
    private List<TemplateParam> requiredAzureParams = new ArrayList<>();
    private List<TemplateParam> requiredGcpParams = new ArrayList<>();
    private List<TemplateParam> requiredOpenStackParams = new ArrayList<>();

    @Override
    public void initialize(ValidCredentialRequest constraintAnnotation) {
        for (AWSCredentialParam param : AWSCredentialParam.values()) {
            requiredAWSParams.add(param);
        }
        for (RequiredAzureCredentialParam param : RequiredAzureCredentialParam.values()) {
            requiredAzureParams.add(param);
        }
        for (GcpCredentialParam param : GcpCredentialParam.values()) {
            requiredGcpParams.add(param);
        }
        for (OpenStackCredentialParam param : OpenStackCredentialParam.values()) {
            requiredOpenStackParams.add(param);
        }
    }

    @Override
    public boolean isValid(CredentialRequest request, ConstraintValidatorContext context) {
        boolean valid = true;
        switch (request.getCloudPlatform()) {
            case AWS:
                valid = validateParams(request.getParameters(), context, requiredAWSParams);
                break;
            case AZURE:
                valid = validateParams(request.getParameters(), context, requiredAzureParams);
                break;
            case GCP:
                valid = validateParams(request.getParameters(), context, requiredGcpParams);
                break;
            case OPENSTACK:
                valid = validateOpenStackParams(request.getParameters(), context, requiredOpenStackParams);
                break;
            default:
                break;
        }
        return valid;
    }

    private boolean validateParams(Map<String, Object> params, ConstraintValidatorContext context, List<TemplateParam> paramConstraints) {
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(params, context, paramConstraints)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateOpenStackParams(Map<String, Object> params, ConstraintValidatorContext context, List<TemplateParam> paramConstraints) {
        boolean valid = true;
        String keystoneVersion = String.valueOf(params.get(OpenStackCredentialParam.KEYSTONE_VERSION.getName()));
        if (keystoneVersion.equals(NULL_STRING) || keystoneVersion.equals(KEYSTONE_VERSION_V2)) {
            valid = validateParams(params, context, paramConstraints)
                    && validateOpenStackV2Params(params, context, paramConstraints);
        } else if (keystoneVersion.equals(KEYSTONE_VERSION_V3)) {
            valid = validateParams(params, context, paramConstraints)
                    && validateOpenStackV3Params(params, context, paramConstraints);
        } else {
            return false;
        }
        return valid;
    }

    private boolean validateOpenStackV2Params(Map<String, Object> params, ConstraintValidatorContext context, List<TemplateParam> paramConstraints) {
        boolean valid = true;
        if (!params.containsKey(OpenStackCredentialParam.TENANT_NAME.getName())) {
            addConstraintViolation(OpenStackCredentialParam.TENANT_NAME.getName(), context);
            valid = false;
        }
        return valid;
    }

    private boolean validateOpenStackV3Params(Map<String, Object> params, ConstraintValidatorContext context, List<TemplateParam> paramConstraints) {
        boolean valid;
        if (!params.containsKey(OpenStackCredentialParam.KEYSTONE_AUTH_SCOPE.getName())) {
            addConstraintViolation(OpenStackCredentialParam.KEYSTONE_AUTH_SCOPE.getName(), context);
            valid = false;
        } else {
            valid = validateV3Scopes(params, context);
        }
        return valid;
    }

    private boolean validateV3Scopes(Map<String, Object> params, ConstraintValidatorContext context) {
        boolean valid = true;
        switch (String.valueOf(params.get(OpenStackCredentialParam.KEYSTONE_AUTH_SCOPE.getName()))) {
            case KEYSTONE_V3_DEFAULT_SCOPE:
                if (!params.containsKey(OpenStackCredentialParam.USER_DOMAIN.getName())) {
                    addConstraintViolation(OpenStackCredentialParam.USER_DOMAIN.getName(), context);
                    valid = false;
                }
                break;
            case KEYSTONE_V3_DOMAIN_SCOPE:
                if (!params.containsKey(OpenStackCredentialParam.USER_DOMAIN.getName())) {
                    addConstraintViolation(OpenStackCredentialParam.USER_DOMAIN.getName(), context);
                    valid = false;
                }
                if (!params.containsKey(OpenStackCredentialParam.DOMAIN_NAME.getName())) {
                    addConstraintViolation(OpenStackCredentialParam.DOMAIN_NAME.getName(), context);
                    valid = false;
                }
                break;
            case KEYSTONE_V3_PROJECT_SCOPE:
                if (!params.containsKey(OpenStackCredentialParam.USER_DOMAIN.getName())) {
                    addConstraintViolation(OpenStackCredentialParam.USER_DOMAIN.getName(), context);
                    valid = false;
                }
                if (!params.containsKey(OpenStackCredentialParam.PROJECT_NAME.getName())) {
                    addConstraintViolation(OpenStackCredentialParam.PROJECT_NAME.getName(), context);
                    valid = false;
                }
                if (!params.containsKey(OpenStackCredentialParam.PROJECT_DOMAIN_NAME.getName())) {
                    addConstraintViolation(OpenStackCredentialParam.PROJECT_DOMAIN_NAME.getName(), context);
                    valid = false;
                }
                break;
            default:
                valid = false;
        }
        return valid;
    }

    private void addConstraintViolation(String key, ConstraintValidatorContext context) {
        String message = String.format("%s is required.", key);
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("parameters")
                .addBeanNode().inIterable().atKey(key)
                .addConstraintViolation();
    }
}
