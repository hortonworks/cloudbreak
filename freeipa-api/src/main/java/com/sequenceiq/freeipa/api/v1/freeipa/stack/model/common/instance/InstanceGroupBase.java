package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.InstanceGroupModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class InstanceGroupBase {
    @Min(value = 0, message = "The node count has to be greater or equals than 0")
    @Max(value = 100000, message = "The node count has to be less than 100000")
    @Digits(fraction = 0, integer = 10, message = "The node count has to be a number")
    @Schema(description = InstanceGroupModelDescription.NODE_COUNT, requiredMode = Schema.RequiredMode.REQUIRED)
    private int nodeCount;

    @NotNull
    @Schema(description = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = InstanceGroupModelDescription.INSTANCE_GROUP_TYPE, allowableValues = "MASTER,SLAVE")
    private InstanceGroupType type = InstanceGroupType.MASTER;

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InstanceGroupType getType() {
        return type;
    }

    public void setType(InstanceGroupType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "InstanceGroupBase{" +
                "nodeCount=" + nodeCount +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
