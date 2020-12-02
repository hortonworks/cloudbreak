package com.sequenceiq.cloudbreak.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class CrnCollectionValidator extends AbstractCrnValidator<Collection<String>> {

    @Override
    protected String getErrorMessageIfServiceOrResourceTypeInvalid(Collection<String> req, Set<Pair> serviceAndResourceTypePairs) {
        return String.format("Crns provided: [%s] have invalid resource type or service type. " +
                        "Accepted service type / resource type pairs: %s",
                Joiner.on(",").join(getUnacceptedCrns(req)),
                Joiner.on(",").join(serviceAndResourceTypePairs));
    }

    @Override
    protected boolean crnInputHasInvalidServiceOrResourceType(Collection<String> req) {
        return !getUnacceptedCrns(req).isEmpty();
    }

    @Override
    protected String getInvalidCrnErrorMessage(Collection<String> req) {
        return String.format("Invalid crns provided: %s", Joiner.on(",").join(getInvalidCrns(req)));
    }

    @Override
    protected boolean crnInputIsInvalid(Collection<String> req) {
        return !getInvalidCrns(req).isEmpty();
    }

    @Override
    protected boolean crnInputIsEmpty(Collection<String> req) {
        return req == null || req.isEmpty();
    }

    private Set<String> getUnacceptedCrns(Collection<String> req) {
        return req.stream()
                .filter(crnString -> Arrays.stream(getResourceDescriptors())
                        .noneMatch(resourceDescriptor -> resourceDescriptor.checkIfCrnMatches(Crn.fromString(crnString))))
                .collect(Collectors.toSet());
    }

    private Set<String> getInvalidCrns(Collection<String> req) {
        return req.stream()
                .filter(crnString -> !Crn.isCrn(crnString))
                .collect(Collectors.toSet());
    }

}
