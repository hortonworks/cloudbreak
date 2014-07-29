package com.sequenceiq.cloudbreak.controller.validation;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.TemplateJson;

/**
 * Validates a provision request. Because different parameters belong to
 * different cloud platforms, they must be validated accordingly. This class
 * checks if the required parameters are present and if they match the required
 * format.
 */
@Component
public class ProvisionParametersValidator implements ConstraintValidator<ValidProvisionRequest, TemplateJson> {

    @Autowired
    private List<ParameterValidator> parameterValidators;

    private List<TemplateParam> awsParams = new ArrayList<>();
    private List<TemplateParam> azureParams = new ArrayList<>();

    @Override
    public void initialize(ValidProvisionRequest constraintAnnotation) {
        for (AwsTemplateParam param : AwsTemplateParam.values()) {
            awsParams.add(param);
        }
        for (AzureTemplateParam param : AzureTemplateParam.values()) {
            azureParams.add(param);
        }
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
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, azureParams)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateAWSParams(TemplateJson request, ConstraintValidatorContext context) {
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, awsParams)) {
                return false;
            }
        }
        return true;
    }

}
