package com.sequenceiq.freeipa.service.freeipa.trust.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TaskResults(
        @JsonProperty("taskResults") List<TaskResult> taskResults,
        @JsonProperty("taskResultMap") Map<TaskResultType, List<TaskResult>> taskResultMap) {
    @JsonCreator
    public TaskResults {
    }

    public TaskResults() {
        this(new ArrayList<>(), new HashMap<>());
    }

    public TaskResults addTaskResult(TaskResult taskResult) {
        taskResults.add(taskResult);
        taskResultMap.computeIfAbsent(taskResult.type(), type -> new ArrayList<>()).add(taskResult);
        return this;
    }

    public boolean hasTaskResultType(TaskResultType taskResultType) {
        return taskResultMap.containsKey(taskResultType);
    }

    public boolean hasErrors() {
        return hasTaskResultType(TaskResultType.ERROR);
    }

    public List<TaskResult> getTaskResultsByType(TaskResultType taskResultType) {
        return taskResultMap.getOrDefault(taskResultType, List.of());
    }

    public List<TaskResult> getErrors() {
        return getTaskResultsByType(TaskResultType.ERROR);
    }

    public List<TaskResult> getWarnings() {
        return getTaskResultsByType(TaskResultType.WARNING);
    }

    public List<TaskResult> getSuccessfulTasks() {
        return Stream.concat(getTaskResultsByType(TaskResultType.INFO).stream(), getTaskResultsByType(TaskResultType.WARNING).stream()).toList();
    }
}
