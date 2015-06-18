package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.UsageModelDescription;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.StackModelDescription;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("CloudbreakUsage")
public class CloudbreakUsageJson implements JsonEntity {
    @ApiModelProperty(ModelDescriptions.OWNER)
    private String owner;

    @ApiModelProperty(StackModelDescription.USERNAME)
    private String username;

    @ApiModelProperty(ModelDescriptions.ACCOUNT)
    private String account;

    @ApiModelProperty(UsageModelDescription.DAY)
    private String day;

    @ApiModelProperty(UsageModelDescription.PROVIDER)
    private String provider;

    @ApiModelProperty(StackModelDescription.REGION)
    private String region;

    @ApiModelProperty(UsageModelDescription.INSTANCE_HOURS)
    private Long instanceHours;

    @ApiModelProperty(StackModelDescription.STACK_ID)
    private Long stackId;

    @ApiModelProperty(StackModelDescription.STACK_NAME)
    private String stackName;

    @ApiModelProperty(UsageModelDescription.COSTS)
    private Double costs;

    @ApiModelProperty(UsageModelDescription.INSTANCE_TYPE)
    private String instanceType;

    @ApiModelProperty(UsageModelDescription.INSTANCE_GROUP)
    private String instanceGroup;

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

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Long getInstanceHours() {
        return instanceHours;
    }

    public void setInstanceHours(Long instanceHours) {
        this.instanceHours = instanceHours;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public Double getCosts() {
        return costs;
    }

    public void setCosts(Double costs) {
        this.costs = costs;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }
}
