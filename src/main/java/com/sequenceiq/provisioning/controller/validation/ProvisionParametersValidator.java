package com.sequenceiq.provisioning.controller.validation;

import java.util.Arrays;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.provisioning.controller.json.ProvisionRequest;

/**
 * Validates a provision request. Because different parameters belong to
 * different cloud platforms, they must be validated accordingly. This class
 * checks if the required parameters are present and if they match the required
 * format.
 */
public class ProvisionParametersValidator implements ConstraintValidator<ValidProvisionRequest, ProvisionRequest> {

    // TODO: add other required params
    List<String> requiredAWSParams = Arrays.asList("roleArn", "region", "sshKey");

    @Override
    public void initialize(ValidProvisionRequest constraintAnnotation) {
    }

    @Override
    public boolean isValid(ProvisionRequest request, ConstraintValidatorContext context) {
        boolean valid = true;
        switch (request.getCloudPlatform()) {
        case AWS:
            for (String param : requiredAWSParams) {
                if (!request.getParameters().containsKey(param)) {
                    addParameterConstraintViolation(context, param, String.format("%s is required.", param));
                    valid = false;
                }
            }
            // TODO: validate instanceType, sshLocation with regex
            break;
        case AZURE:
            // TODO
            break;
        }
        return valid;

    }

    private void addParameterConstraintViolation(ConstraintValidatorContext context, String key, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("parameters")
                .addBeanNode().inIterable().atKey(key)
                .addConstraintViolation();
    }
}
