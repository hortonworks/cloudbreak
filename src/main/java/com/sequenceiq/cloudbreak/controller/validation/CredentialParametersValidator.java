package com.sequenceiq.cloudbreak.controller.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;

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
                valid = validateParams(request.getParameters(), context, requiredAWSParams);
                break;
            case AZURE:
                valid = validateParams(request.getParameters(), context, requiredAzureParams);
                break;
            case GCC:
                valid = validateParams(request.getParameters(), context, requiredGccParams);
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

}
