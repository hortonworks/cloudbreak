package com.sequenceiq.provisioning.controller.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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

    @Resource
    private Map<ValidatorType, ParameterValidator> parameterValidators;

    private List<TemplateParam> awsParams = new ArrayList<>();
    private List<TemplateParam> azureParams = new ArrayList<>();

    @Override
    public void initialize(ValidProvisionRequest constraintAnnotation) {
        for (RequiredAwsTemplateParam param : RequiredAwsTemplateParam.values()) {
            awsParams.add(param);
        }
        for (RequiredAzureTemplateParam param : RequiredAzureTemplateParam.values()) {
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
        return parameterValidators.get(ValidatorType.REQUIRED).validate(request.getParameters(), context, azureParams)
                && parameterValidators.get(ValidatorType.CLASS).validate(request.getParameters(), context, azureParams);
    }

    private boolean validateAWSParams(TemplateJson request, ConstraintValidatorContext context) {
        // TODO: validate instanceType, sshLocation, etc.. with regex
        return parameterValidators.get(ValidatorType.REQUIRED).validate(request.getParameters(), context, awsParams)
                && parameterValidators.get(ValidatorType.CLASS).validate(request.getParameters(), context, awsParams);
    }

}
