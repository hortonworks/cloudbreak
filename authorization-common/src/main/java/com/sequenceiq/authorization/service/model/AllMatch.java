package com.sequenceiq.authorization.service.model;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public class AllMatch implements AuthorizationRule {

    private final List<AuthorizationRule> authorizations;

    public AllMatch(List<AuthorizationRule> authorizations) {
        this.authorizations = authorizations;
    }

    public static Optional<AuthorizationRule> from(List<Optional<AuthorizationRule>> values) {
        List<AuthorizationRule> result = values.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
        return fromList(result);
    }

    public static Optional<AuthorizationRule> fromList(List<AuthorizationRule> values) {
        if (values.isEmpty()) {
            return Optional.empty();
        } else if (values.size() == 1) {
            return Optional.of(values.get(0));
        } else {
            return Optional.of(new AllMatch(values));
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
        return "Not authorized for the following reasons. " + authorizations
                .stream()
                .map(a -> a.getAsFailureMessage(rightMapper))
                .collect(Collectors.joining(" "));
    }

    @Override
    public String toString(Function<AuthorizationResourceAction, String> rightMapper) {
        return String.format("(all-match %s)", authorizations.stream().map(a -> a.toString(rightMapper)).collect(joining(", ")));
    }

    @Override
    public Optional<AuthorizationRule> evaluateAndGetFailed(Iterator<Boolean> results) {
        return from(authorizations
                .stream()
                .map(authorization -> authorization.evaluateAndGetFailed(results))
                .collect(toList()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AllMatch allMatch = (AllMatch) o;
        return Objects.equals(authorizations, allMatch.authorizations);
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
