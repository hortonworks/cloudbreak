package com.sequenceiq.cloudbreak.validation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.tuple.Pair;

import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;

public abstract class AbstractCrnValidator<T> implements ConstraintValidator<ValidCrn, T> {

    private CrnResourceDescriptor[] resourceDescriptors;

    private ValidCrn.Effect effect;

    @Override
    public void initialize(ValidCrn constraintAnnotation) {
        resourceDescriptors = constraintAnnotation.resource();
        effect = constraintAnnotation.effect();
    }

    @Override
    public boolean isValid(T req, ConstraintValidatorContext constraintValidatorContext) {
        constraintValidatorContext.disableDefaultConstraintViolation();
        if (crnInputIsEmpty(req)) {
            return true;
        }
        if (crnInputIsInvalid(req)) {
            String errorMessage = getInvalidCrnErrorMessage(req);
            addValidationErrorMessage(errorMessage, constraintValidatorContext);
            return false;
        }
        if (resourceDescriptors.length != 0 && crnInputHasInvalidServiceOrResourceType(req, effect)) {
            Set<Pair> serviceAndResourceTypePairs = Arrays.stream(resourceDescriptors)
                    .map(CrnResourceDescriptor::createServiceAndResourceTypePair)
                    .collect(Collectors.toSet());
            String errorMessage = getErrorMessageIfServiceOrResourceTypeInvalid(req, serviceAndResourceTypePairs, effect);
            addValidationErrorMessage(errorMessage, constraintValidatorContext);
            return false;
        }
        return true;
    }

    public CrnResourceDescriptor[] getResourceDescriptors() {
        return resourceDescriptors;
    }

    public ValidCrn.Effect getEffect() {
        return effect;
    }

    protected abstract String getErrorMessageIfServiceOrResourceTypeInvalid(T req, Set<Pair> serviceAndResourceTypePairs, ValidCrn.Effect effect);

    protected abstract boolean crnInputHasInvalidServiceOrResourceType(T req, ValidCrn.Effect effect);

    protected abstract String getInvalidCrnErrorMessage(T req);

    protected abstract boolean crnInputIsInvalid(T req);

    protected abstract boolean crnInputIsEmpty(T req);

    private void addValidationErrorMessage(String errorMessage, ConstraintValidatorContext context) {
        context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
    }
}
