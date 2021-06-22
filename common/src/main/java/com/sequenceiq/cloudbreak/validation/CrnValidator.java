package com.sequenceiq.cloudbreak.validation;

import java.util.Arrays;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class CrnValidator extends AbstractCrnValidator<String> {

    @Override
    protected String getErrorMessageIfServiceOrResourceTypeInvalid(String req, Set<Pair> serviceAndResourceTypePairs, ValidCrn.Effect effect) {
        return String.format("Crn provided: %s has invalid resource type or service type. " +
                "%s service type / resource type pairs: %s", req, effect.getName(), Joiner.on(",").join(serviceAndResourceTypePairs));
    }

    @Override
    protected boolean crnInputHasInvalidServiceOrResourceType(String req, ValidCrn.Effect effect) {
        Crn requestCrn = Crn.fromString(req);
        return ValidCrn.Effect.ACCEPT.equals(effect)
                ? isCrnNotInAllowed(requestCrn)
                : isCrnAmongDenied(requestCrn);
    }

    @Override
    protected String getInvalidCrnErrorMessage(String req) {
        return String.format("Invalid crn provided: %s", req);
    }

    @Override
    protected boolean crnInputIsInvalid(String req) {
        return !Crn.isCrn(req);
    }

    @Override
    protected boolean crnInputIsEmpty(String req) {
        return StringUtils.isEmpty(req);
    }

    private boolean isCrnAmongDenied(Crn requestCrn) {
        return Arrays.stream(getResourceDescriptors())
                .anyMatch(resourceDescriptor -> resourceDescriptor.checkIfCrnMatches(requestCrn));
    }

    private boolean isCrnNotInAllowed(Crn requestCrn) {
        return Arrays.stream(getResourceDescriptors())
                .noneMatch(resourceDescriptor -> resourceDescriptor.checkIfCrnMatches(requestCrn));
    }

}
