package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.UsageModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CloudbreakUsage")
public class CloudbreakUsageJson implements JsonEntity {

    @ApiModelProperty(StackModelDescription.USERNAME)
    private String username;

    @ApiModelProperty(UsageModelDescription.DAY)
    private String day;

    @ApiModelProperty(UsageModelDescription.PROVIDER)
    private String provider;

    @ApiModelProperty(StackModelDescription.REGION)
    private String region;

    @ApiModelProperty(StackModelDescription.AVAILABILITY_ZONE)
    private String availabilityZone;

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

    @ApiModelProperty(UsageModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @ApiModelProperty(UsageModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @ApiModelProperty(UsageModelDescription.DURATION)
    private String duration;

    @ApiModelProperty(UsageModelDescription.INSTANCE_NUMBER)
    private Integer instanceNum;

    @ApiModelProperty(UsageModelDescription.PEAK)
    private Integer peak;

    @ApiModelProperty(UsageModelDescription.FLEX_ID)
    private String flexId;

    @ApiModelProperty(UsageModelDescription.STACK_UUID)
    private String stackUuid;

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

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Integer getInstanceNum() {
        return instanceNum;
    }

    public void setInstanceNum(Integer instanceNum) {
        this.instanceNum = instanceNum;
    }

    public Integer getPeak() {
        return peak;
    }

    public void setPeak(Integer peak) {
        this.peak = peak;
    }

    public String getFlexId() {
        return flexId;
    }

    public void setFlexId(String flexId) {
        this.flexId = flexId;
    }

    public String getStackUuid() {
        return stackUuid;
    }

    public void setStackUuid(String stackUuid) {
        this.stackUuid = stackUuid;
    }
}
