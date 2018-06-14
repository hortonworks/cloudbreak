package com.sequenceiq.cloudbreak.validation;

import static com.sequenceiq.cloudbreak.type.OpenStackCredentialV3ScopeConstants.CB_KEYSTONE_V3_DEFAULT_SCOPE;
import static com.sequenceiq.cloudbreak.type.OpenStackCredentialVersionConstants.CB_KEYSTONE_V3;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.type.OpenStackCredentialRequestKeys;

public class CredentialRequestValidator implements ConstraintValidator<ValidCredentialRequest, CredentialRequest> {

    private static final String CLOUD_OPENSTACK = "OPENSTACK";

    @Override
    public void initialize(ValidCredentialRequest constraintAnnotation) {

    }

    @Override
    public boolean isValid(CredentialRequest value, ConstraintValidatorContext context) {

        if (CLOUD_OPENSTACK.equals(value.getCloudPlatform())
                && CB_KEYSTONE_V3.equals(getParameterOrEmpty(OpenStackCredentialRequestKeys.VERSION, value))
                && CB_KEYSTONE_V3_DEFAULT_SCOPE.equals(getParameterOrEmpty(OpenStackCredentialRequestKeys.SCOPE, value))) {

            String message = "OpenStack Keystone V3 credentials with default scope cannot be created";
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("Keystone V3 credential scope")
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            return false;
        }

        return true;
    }

    private String getParameterOrEmpty(OpenStackCredentialRequestKeys key, CredentialRequest request) {
        Object found = request.getParameters().get(key.getValue());
        return found instanceof String ? (String) found : "";
    }
}
