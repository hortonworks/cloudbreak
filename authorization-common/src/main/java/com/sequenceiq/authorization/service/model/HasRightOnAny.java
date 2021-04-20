package com.sequenceiq.authorization.service.model;

import static com.sequenceiq.authorization.utils.AuthorizationMessageUtils.formatIdentifiersForErrorMessage;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public class HasRightOnAny implements AuthorizationRule {

    private static final String FAILURE_MESSAGE_FIX = "Doesn't have '%s' right on any of the ";

    private static final String FAILURE_MESSAGE_REPEATABLE_TEMPLATE = "%s(s) %s";

    private final AuthorizationResourceAction right;

    private final Set<String> crns;

    public HasRightOnAny(AuthorizationResourceAction right, Collection<String> crns) {
        this.right = right;
        this.crns = new LinkedHashSet<>(crns);
    }

    public AuthorizationResourceAction getRight() {
        return right;
    }

    public Set<String> getCrns() {
        return crns;
    }

    @Override
    public void convert(BiConsumer<AuthorizationResourceAction, String> collector) {
        for (String crn : crns) {
            collector.accept(right, crn);
        }
    }

    @Override
    public String getAsFailureMessage(Function<AuthorizationResourceAction, String> rightMapper, Function<String, Optional<String>> nameMapper) {
        Map<String, List<String>> byResourceType = crns.stream()
                .collect(groupingBy(this::getResourceType, LinkedHashMap::new, toList()));
        return String.format(FAILURE_MESSAGE_FIX, rightMapper.apply(right)) + byResourceType.entrySet()
                .stream()
                .map(entry -> String.format(FAILURE_MESSAGE_REPEATABLE_TEMPLATE, entry.getKey(),
                        formatIdentifiersForErrorMessage(entry.getValue(), nameMapper)))
                .collect(Collectors.joining(" or on ")) + '.';
    }

    @Override
    public String toString(Function<AuthorizationResourceAction, String> rightMapper) {
        return String.format("(has-right-on-any \"%s\" %s)", rightMapper.apply(right),
                crns.stream().map(crn -> '"' + crn + '"').collect(Collectors.joining(", ", "[", "]")));
    }

    @Override
    public Optional<AuthorizationRule> evaluateAndGetFailed(Iterator<Boolean> results) {
        int failedCrns = 0;
        for (String crn : crns) {
            if (!results.next()) {
                failedCrns++;
            }
        }
        if (failedCrns == crns.size()) {
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
        HasRightOnAny that = (HasRightOnAny) o;
        return right == that.right &&
                Objects.equals(crns, that.crns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(right, crns);
    }

    @Override
    public String toString() {
        return toString(AuthorizationResourceAction::name);
    }
}
