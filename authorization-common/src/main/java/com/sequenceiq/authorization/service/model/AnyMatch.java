package com.sequenceiq.authorization.service.model;

import static java.util.stream.Collectors.joining;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public class AnyMatch implements AuthorizationRule {

    private final List<AuthorizationRule> authorizations;

    public AnyMatch(List<AuthorizationRule> authorizations) {
        this.authorizations = authorizations;
    }

    public static Optional<AuthorizationRule> from(List<AuthorizationRule> values) {
        if (values.isEmpty()) {
            return Optional.empty();
        } else if (values.size() == 1) {
            return Optional.of(values.get(0));
        } else {
            return Optional.of(new AnyMatch(values));
        }
    }

    public List<AuthorizationRule> getAuthorizations() {
        return authorizations;
    }

    @Override
    public void convert(BiConsumer<AuthorizationResourceAction, String> collector) {
        for (AuthorizationRule authorization : authorizations) {
            authorization.convert(collector);
        }
    }

    @Override
    public String getAsFailureMessage(Function<AuthorizationResourceAction, String> rightMapper) {
        return "You need to have at least one of the following resource rights. " + authorizations
                .stream()
                .map(a -> a.getAsFailureMessage(rightMapper))
                .collect(Collectors.joining(" "));
    }

    @Override
    public String toString(Function<AuthorizationResourceAction, String> rightMapper) {
        return String.format("(any-match %s)", authorizations.stream().map(a -> a.toString(rightMapper)).collect(joining(", ")));
    }

    @Override
    public Optional<AuthorizationRule> evaluateAndGetFailed(Iterator<Boolean> results) {
        int failedConditions = 0;
        for (AuthorizationRule authorization : authorizations) {
            Optional<AuthorizationRule> failed = authorization.evaluateAndGetFailed(results);
            if (failed.isPresent()) {
                failedConditions++;
            }
        }
        if (failedConditions == authorizations.size()) {
            return Optional.of(this);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnyMatch anyMatch = (AnyMatch) o;
        return Objects.equals(authorizations, anyMatch.authorizations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorizations);
    }

    @Override
    public String toString() {
        return toString(AuthorizationResourceAction::name);
    }
}
