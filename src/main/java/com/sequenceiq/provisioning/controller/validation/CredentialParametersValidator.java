package com.sequenceiq.provisioning.controller.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.provisioning.controller.json.CredentialJson;

public class CredentialParametersValidator implements ConstraintValidator<ValidCredentialRequest, CredentialJson> {

    @Resource
    private Map<ValidatorType, ParameterValidator> parameterValidators;

    private List<TemplateParam> requiredAWSParams = new ArrayList<>();
    private List<TemplateParam> requiredAzureParams = new ArrayList<>();

    @Override
    public void initialize(ValidCredentialRequest constraintAnnotation) {
        for (RequiredAWSCredentialParam param : RequiredAWSCredentialParam.values()) {
            requiredAWSParams.add(param);
        }
        for (RequiredAzureCredentialParam param : RequiredAzureCredentialParam.values()) {
            requiredAzureParams.add(param);
        }

    }

    @Override
    public boolean isValid(CredentialJson request, ConstraintValidatorContext context) {
        boolean valid = true;
        switch (request.getCloudPlatform()) {
            case AWS:
                valid = validateAWSParams(request, context);
                break;
            case AZURE:
                valid = validateAzureParams(request, context);
                break;
            default:
                break;
        }
        return valid;
    }

    private boolean validateAzureParams(CredentialJson request, ConstraintValidatorContext context) {
        return parameterValidators.get(ValidatorType.REQUIRED).validate(request.getParameters(), context, requiredAzureParams);
    }

    private boolean validateAWSParams(CredentialJson request, ConstraintValidatorContext context) {
        return parameterValidators.get(ValidatorType.REQUIRED).validate(request.getParameters(), context, requiredAWSParams);
    }

}
