package com.sequenceiq.cloudbreak.controller.validation;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import org.springframework.beans.factory.annotation.Autowired;

public class CredentialParametersValidator implements ConstraintValidator<ValidCredentialRequest, CredentialRequest> {

    @Autowired
    private List<ParameterValidator> parameterValidators;

    private List<TemplateParam> requiredAWSParams = new ArrayList<>();
    private List<TemplateParam> requiredAzureParams = new ArrayList<>();
    private List<TemplateParam> requiredGccParams = new ArrayList<>();


    @Override
    public void initialize(ValidCredentialRequest constraintAnnotation) {
        for (AWSCredentialParam param : AWSCredentialParam.values()) {
            requiredAWSParams.add(param);
        }
        for (RequiredAzureCredentialParam param : RequiredAzureCredentialParam.values()) {
            requiredAzureParams.add(param);
        }
        for (GccCredentialParam param : GccCredentialParam.values()) {
            requiredGccParams.add(param);
        }
    }

    @Override
    public boolean isValid(CredentialRequest request, ConstraintValidatorContext context) {
        boolean valid = true;
        switch (request.getCloudPlatform()) {
            case AWS:
                valid = validateAWSParams(request, context);
                break;
            case AZURE:
                valid = validateAzureParams(request, context);
                break;
            case GCC:
                valid = validateGccParams(request, context);
                break;
            default:
                break;
        }
        return valid;
    }

    private boolean validateGccParams(CredentialRequest request, ConstraintValidatorContext context) {
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, requiredGccParams)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateAzureParams(CredentialRequest request, ConstraintValidatorContext context) {
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, requiredAzureParams)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateAWSParams(CredentialRequest request, ConstraintValidatorContext context) {
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, requiredAWSParams)) {
                return false;
            }
        }
        return true;
    }

}
