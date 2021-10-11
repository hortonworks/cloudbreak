package com.sequenceiq.authorization.service.model;

import static com.sequenceiq.authorization.utils.AuthorizationMessageUtils.formatIdentifiersForErrorMessage;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
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

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public class HasRightOnAll implements AuthorizationRule {

    private static final String FAILURE_MESSAGE_FIX = "Doesn't have '%s' right on ";

    private static final String FAILURE_MESSAGE_REPEATABLE_TEMPLATE = "%s(s) %s";

    private final AuthorizationResourceAction right;

    private final Set<String> crns;

    public HasRightOnAll(AuthorizationResourceAction right, Collection<String> crns) {
        this.right = right;
        this.crns = new LinkedHashSet<>(crns);
    }

    public static Optional<AuthorizationRule> from(AuthorizationResourceAction right, Collection<String> crns) {
        if (CollectionUtils.isEmpty(crns)) {
            return Optional.empty();
        } else if (crns.size() == 1) {
            return Optional.of(new HasRight(right, crns.iterator().next()));
        } else {
            return Optional.of(new HasRightOnAll(right, crns));
        }
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
                .collect(Collectors.joining(" and on ")) + '.';
    }

    @Override
    public String toString(Function<AuthorizationResourceAction, String> rightMapper) {
        return String.format("(has-right-on-all \"%s\" %s)", rightMapper.apply(right),
                crns.stream().map(crn -> '"' + crn + '"').collect(Collectors.joining(", ", "[", "]")));
    }

    @Override
    public Optional<AuthorizationRule> evaluateAndGetFailed(Iterator<Boolean> results) {
        List<String> failedCrns = new ArrayList<>();
        for (String crn : crns) {
            if (!results.next()) {
                failedCrns.add(crn);
            }
        }
        if (failedCrns.isEmpty()) {
            return Optional.empty();
        } else if (failedCrns.size() == 1) {
            return Optional.of(new HasRight(right, failedCrns.get(0)));
        } else {
            return Optional.of(new HasRightOnAll(right, failedCrns));
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
        HasRightOnAll that = (HasRightOnAll) o;
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
