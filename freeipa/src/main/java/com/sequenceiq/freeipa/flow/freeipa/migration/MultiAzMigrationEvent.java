package com.sequenceiq.freeipa.flow.freeipa.migration;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class MultiAzMigrationEvent extends StackEvent {

    private final String operationId;

    private final Variant sourceVariant;

    private final Variant targetVariant;

    @SuppressWarnings("IllegalType")
    private final HashSet<String> instanceIds;

    private final String primaryGwInstanceId;

    @JsonCreator
    public MultiAzMigrationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("sourceVariant") Variant sourceVariant,
            @JsonProperty("targetVariant") Variant targetVariant,
            @JsonProperty("instanceIds") HashSet<String> instanceIds,
            @JsonProperty("primaryGwInstanceId") String primaryGwInstanceId) {
        super(selector, stackId);
        this.operationId = operationId;
        this.sourceVariant = sourceVariant;
        this.targetVariant = targetVariant;
        this.instanceIds = Objects.requireNonNullElse(instanceIds, new HashSet<>());
        this.primaryGwInstanceId = primaryGwInstanceId;
    }

    public String getOperationId() {
        return operationId;
    }

    public Variant getSourceVariant() {
        return sourceVariant;
    }

    public Variant getTargetVariant() {
        return targetVariant;
    }

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

    public String getPrimaryGwInstanceId() {
        return primaryGwInstanceId;
    }

    @JsonIgnore
    public boolean variantMigrationNeeded() {
        return Objects.nonNull(sourceVariant)
                && Objects.nonNull(targetVariant)
                && !Objects.equals(sourceVariant.getValue(), targetVariant.getValue());
    }

    @JsonIgnore
    public boolean shouldRecreatePrimaryGw() {
        return variantMigrationNeeded();
    }

    @JsonIgnore
    public Set<String> getNonPrimaryGwInstanceIdsToRecreate() {
        return instanceIds.stream()
                .filter(instanceId -> !instanceId.equals(primaryGwInstanceId))
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "MultiAzMigrationEvent{" +
                "operationId='" + operationId + '\'' +
                ", sourceVariant=" + sourceVariant +
                ", targetVariant=" + targetVariant +
                ", instanceIds=" + instanceIds +
                ", primaryGwInstanceId='" + primaryGwInstanceId + '\'' +
                "} " + super.toString();
    }
}
