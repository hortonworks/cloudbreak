package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.BlueprintModelDescription;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.EventModelDescription;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.domain.Status;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("CloudbreakEvent")
public class CloudbreakEventsJson implements JsonEntity {

    @ApiModelProperty(EventModelDescription.TYPE)
    private String eventType;
    @ApiModelProperty(EventModelDescription.TIMESTAMP)
    private long eventTimestamp;
    @ApiModelProperty(EventModelDescription.MESSAGE)
    private String eventMessage;
    @ApiModelProperty(ModelDescriptions.OWNER)
    private String owner;
    @ApiModelProperty(ModelDescriptions.ACCOUNT)
    private String account;
    @ApiModelProperty(ModelDescriptions.CLOUD_PLATFORM)
    private String cloud;
    @ApiModelProperty(StackModelDescription.REGION)
    private String region;
    @ApiModelProperty(StackModelDescription.BLUEPRINT_ID)
    private long blueprintId;
    @ApiModelProperty(BlueprintModelDescription.BLUEPRINT_NAME)
    private String blueprintName;
    @ApiModelProperty(StackModelDescription.STACK_ID)
    private Long stackId;
    @ApiModelProperty(StackModelDescription.STACK_NAME)
    private String stackName;
    @ApiModelProperty(StackModelDescription.STATUS)
    private Status stackStatus;
    @ApiModelProperty(InstanceGroupModelDescription.NODE_COUNT)
    private Integer nodeCount;
    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    private String instanceGroup;

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(long eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getEventMessage() {
        return eventMessage;
    }

    public void setEventMessage(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public Status getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(Status stackStatus) {
        this.stackStatus = stackStatus;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }
}
