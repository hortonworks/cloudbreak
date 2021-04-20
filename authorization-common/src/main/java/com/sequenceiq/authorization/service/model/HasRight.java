package com.sequenceiq.authorization.service.model;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public class HasRight implements AuthorizationRule {

    private static final String FAILURE_MESSAGE_TEMPLATE = "Doesn't have '%s' right on %s [%s].";

    private final AuthorizationResourceAction right;

    private final String crn;

    public HasRight(AuthorizationResourceAction right, String crn) {
        this.right = right;
        this.crn = crn;
    }

    public AuthorizationResourceAction getRight() {
        return right;
    }

    public String getCrn() {
        return crn;
    }

    @Override
    public void convert(BiConsumer<AuthorizationResourceAction, String> collector) {
        collector.accept(right, crn);
    }

    @Override
    public String getAsFailureMessage(Function<AuthorizationResourceAction, String> rightMapper,
            Function<String, Optional<String>> nameMapper) {
        String resourceNameFormatted = nameMapper.apply(crn)
                .map(name -> String.format("name: %s", name)).orElse("");
        String resourceCrnFormatted = String.format("crn: %s", crn);
        String identifiers = Stream.of(resourceNameFormatted, resourceCrnFormatted)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(", "));
        return String.format(FAILURE_MESSAGE_TEMPLATE, rightMapper.apply(right), getResourceType(crn), identifiers);
    }

    @Override
    public Optional<AuthorizationRule> evaluateAndGetFailed(Iterator<Boolean> results) {
        if (results.next()) {
            return Optional.empty();
        } else {
            return Optional.of(this);
        }
    }

    @Override
    public String toString(Function<AuthorizationResourceAction, String> rightMapper) {
        return String.format("(has-right \"%s\" \"%s\")", rightMapper.apply(right), crn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HasRight hasRight = (HasRight) o;
        return right == hasRight.right &&
                Objects.equals(crn, hasRight.crn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(right, crn);
    }

    @Override
    public String toString() {
        return toString(AuthorizationResourceAction::name);
    }
}
