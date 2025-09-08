package com.sequenceiq.freeipa.service.freeipa.trust.operation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;

@Component
public class TaskResultConverter {

    private static final String MESSAGE = "MESSAGE";

    private static final String TASK_RESULT_TYPE = "TYPE";

    public FailureDetails convertFailedTaskResult(TaskResult taskResult, String environmentCrn) {
        FailureDetails failureDetails = new FailureDetails();
        failureDetails.setEnvironment(environmentCrn);
        failureDetails.setMessage(taskResult.message());
        failureDetails.getAdditionalDetails().putAll(taskResult.additionalParams());
        return failureDetails;
    }

    public FailureDetails convertFailedTaskResult(String taskResult, String environmentCrn) {
        FailureDetails failureDetails = new FailureDetails();
        failureDetails.setEnvironment(environmentCrn);
        failureDetails.setMessage(taskResult);
        return failureDetails;
    }

    public SuccessDetails convertSuccessfulTaskResult(TaskResult taskResult, String environmentCrn) {
        SuccessDetails successDetails = new SuccessDetails();
        successDetails.setEnvironment(environmentCrn);
        successDetails.getAdditionalDetails().put(MESSAGE, List.of(taskResult.message()));
        successDetails.getAdditionalDetails().put(TASK_RESULT_TYPE, List.of(taskResult.type().name()));
        return successDetails;
    }

    public SuccessDetails convertSuccessfulTaskResult(String message, String environmentCrn) {
        return convertSuccessfulTaskResult(message, TaskResultType.INFO, environmentCrn);
    }

    public SuccessDetails convertSuccessfulTaskResult(String message, TaskResultType taskResultType, String environmentCrn) {
        SuccessDetails successDetails = new SuccessDetails();
        successDetails.setEnvironment(environmentCrn);
        successDetails.getAdditionalDetails().put(MESSAGE, List.of(message));
        successDetails.getAdditionalDetails().put(TASK_RESULT_TYPE, List.of(taskResultType.name()));
        return successDetails;
    }

    public List<SuccessDetails> convertSuccessfulTasks(List<TaskResult> tasks, String environmentCrn) {
        return tasks.stream()
                .map(task -> convertSuccessfulTaskResult(task, environmentCrn))
                .toList();
    }

    public List<FailureDetails> convertFailedTasks(List<TaskResult> tasks, String environmentCrn) {
        return tasks.stream()
                .map(task -> convertFailedTaskResult(task, environmentCrn))
                .toList();
    }
}
