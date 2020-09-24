package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.InstanceGroupModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class InstanceGroupBase {
    @Min(value = 0, message = "The node count has to be greater or equals than 0")
    @Max(value = 100000, message = "The node count has to be less than 100000")
    @Digits(fraction = 0, integer = 10, message = "The node count has to be a number")
    @ApiModelProperty(value = InstanceGroupModelDescription.NODE_COUNT, required = true)
    private int nodeCount;

    @NotNull
    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String name;

    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_TYPE, allowableValues = "MASTER,SLAVE")
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
