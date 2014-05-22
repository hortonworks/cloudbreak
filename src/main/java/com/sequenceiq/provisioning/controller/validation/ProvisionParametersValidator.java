package com.sequenceiq.provisioning.controller.validation;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.TemplateJson;

/**
 * Validates a provision request. Because different parameters belong to
 * different cloud platforms, they must be validated accordingly. This class
 * checks if the required parameters are present and if they match the required
 * format.
 */
@Component
public class ProvisionParametersValidator implements ConstraintValidator<ValidProvisionRequest, TemplateJson> {

    @Autowired
    private RequiredParametersValidator requiredParametersValidator;

    private List<String> requiredAWSParams = new ArrayList<>();
    private List<String> requiredAzureParams = new ArrayList<>();

    @Override
    public void initialize(ValidProvisionRequest constraintAnnotation) {
        for (RequiredAwsTemplateParam param : RequiredAwsTemplateParam.values()) {
            requiredAWSParams.add(param.getName());
        }
        // TODO: required Azure params
    }

    @Override
    public boolean isValid(TemplateJson request, ConstraintValidatorContext context) {
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

    private boolean validateAzureParams(TemplateJson request, ConstraintValidatorContext context) {
        return requiredParametersValidator.validate(request.getParameters(), context, requiredAzureParams);
    }

    private boolean validateAWSParams(TemplateJson request, ConstraintValidatorContext context) {
        // TODO: validate instanceType, sshLocation, etc.. with regex
        return requiredParametersValidator.validate(request.getParameters(), context, requiredAWSParams);
    }

}
