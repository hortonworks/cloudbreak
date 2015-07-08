package com.sequenceiq.cloudbreak.controller.validation;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;

/**
 * Validates a provision request. Because different parameters belong to
 * different cloud platforms, they must be validated accordingly. This class
 * checks if the required parameters are present and if they match the required
 * format.
 */
@Component
public class TemplateParametersValidator implements ConstraintValidator<ValidProvisionRequest, TemplateRequest> {

    @Inject
    private List<ParameterValidator> parameterValidators;

    private List<TemplateParam> awsParams = new ArrayList<>();
    private List<TemplateParam> azureParams = new ArrayList<>();
    private List<TemplateParam> gcpParams = new ArrayList<>();
    private List<TemplateParam> openStackParams = new ArrayList<>();

    @Override
    public void initialize(ValidProvisionRequest constraintAnnotation) {
        for (AwsTemplateParam param : AwsTemplateParam.values()) {
            awsParams.add(param);
        }
        for (AzureTemplateParam param : AzureTemplateParam.values()) {
            azureParams.add(param);
        }
        for (GcpTemplateParam param : GcpTemplateParam.values()) {
            gcpParams.add(param);
        }
        for (OpenStackTemplateParam param : OpenStackTemplateParam.values()) {
            openStackParams.add(param);
        }
    }

    @Override
    public boolean isValid(TemplateRequest request, ConstraintValidatorContext context) {
        boolean valid = true;
        switch (request.getCloudPlatform()) {
            case AWS:
                valid = validateAWSParams(request, context);
                break;
            case AZURE:
                valid = validateAzureParams(request, context);
                break;
            case GCP:
                valid = validateGcpParams(request, context);
                break;
            case OPENSTACK:
                valid = validateOpenStackParams(request, context);
                break;
            default:
                break;
        }
        return valid;
    }

    private boolean validateAzureParams(TemplateRequest request, ConstraintValidatorContext context) {
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, azureParams)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateGcpParams(TemplateRequest request, ConstraintValidatorContext context) {
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, gcpParams)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateOpenStackParams(TemplateRequest request, ConstraintValidatorContext context) {
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, openStackParams)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateAWSParams(TemplateRequest request, ConstraintValidatorContext context) {
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.validate(request.getParameters(), context, awsParams)) {
                return false;
            }
        }
        return true;
    }

}
