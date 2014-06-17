package com.sequenceiq.cloudbreak.controller.validation;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;

public class CredentialParametersValidator implements ConstraintValidator<ValidCredentialRequest, CredentialJson> {

    @Autowired
    private List<ParameterValidator> parameterValidators;

    private List<TemplateParam> requiredAWSParams = new ArrayList<>();
    private List<TemplateParam> requiredAzureParams = new ArrayList<>();

    @Override
    public void initialize(ValidCredentialRequest constraintAnnotation) {
        for (AWSCredentialParam param : AWSCredentialParam.values()) {
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
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, requiredAzureParams)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateAWSParams(CredentialJson request, ConstraintValidatorContext context) {
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, requiredAWSParams)) {
                return false;
            }
        }
        return true;
    }

}
