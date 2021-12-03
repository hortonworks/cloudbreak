package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.Role;

public abstract class AbstractAwsSimulatePolicyValidator {

    SortedSet<String> getFailedActions(Role role, List<EvaluationResult> evaluationResults) {
        return evaluationResults.stream()
                .filter(this::isEvaluationFailed)
                .map(evaluationResult -> String.format("%s:%s:%s", role.getArn(),
                        evaluationResult.getEvalActionName(), evaluationResult.getEvalResourceName()))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    SortedSet<String> getWarnings(Role role, List<EvaluationResult> evaluationResults) {
        return evaluationResults.stream()
                .filter(this::isEvaluationWarning)
                .map(evaluationResult -> String.format("missing context values: %s",
                        String.join(",", new HashSet<>(evaluationResult.getMissingContextValues()))))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean isEvaluationFailed(EvaluationResult evaluationResult) {
        return evaluationResult.getEvalDecision().toLowerCase().contains("deny")
                && CollectionUtils.isEmpty(evaluationResult.getMissingContextValues());
    }

    private boolean isEvaluationWarning(EvaluationResult evaluationResult) {
        return evaluationResult.getEvalDecision().toLowerCase().contains("deny")
                && !CollectionUtils.isEmpty(evaluationResult.getMissingContextValues());
    }
}
