package com.sequenceiq.cloudbreak.validation;

import static com.sequenceiq.cloudbreak.validation.ValidCrn.Effect.ACCEPT;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class CrnCollectionValidator extends AbstractCrnValidator<Collection<String>> {

    @Override
    protected String getErrorMessageIfServiceOrResourceTypeInvalid(Collection<String> req, Set<Pair> serviceAndResourceTypePairs, ValidCrn.Effect effect) {
        return String.format("Crns provided: [%s] have invalid resource type or service type. " +
                        "%s service type / resource type pairs: %s",
                Joiner.on(",").join(getUnacceptedCrns(req, effect)),
                effect.getName(),
                Joiner.on(",").join(serviceAndResourceTypePairs));
    }

    @Override
    protected boolean crnInputHasInvalidServiceOrResourceType(Collection<String> req, ValidCrn.Effect effect) {
        return !getUnacceptedCrns(req, effect).isEmpty();
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

    private Set<String> getUnacceptedCrns(Collection<String> req, ValidCrn.Effect effect) {
        return ACCEPT.equals(effect)
                ? isMissingFromAllowedCrns(req)
                : isPresentAmongDeniedCrns(req);
    }

    private Set<String> isPresentAmongDeniedCrns(Collection<String> req) {
        return req.stream()
                .filter(crnString -> Arrays.stream(getResourceDescriptors())
                        .anyMatch(resourceDescriptor -> resourceDescriptor.checkIfCrnMatches(Crn.fromString(crnString))))
                .collect(Collectors.toSet());
    }

    private Set<String> isMissingFromAllowedCrns(Collection<String> req) {
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
