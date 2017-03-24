package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CloudbreakFlexUsage")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudbreakFlexUsageJson  implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.OWNER)
    private String owner;

    @ApiModelProperty(ModelDescriptions.ACCOUNT)
    private String account;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.USERNAME)
    private String username;

    @ApiModelProperty(ModelDescriptions.UsageModelDescription.DAY)
    private String day;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.REGION)
    private String region;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.STACK_NAME)
    private String stackName;

    @ApiModelProperty(ModelDescriptions.UsageModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @ApiModelProperty(ModelDescriptions.UsageModelDescription.INSTANCE_NUMBER)
    private Integer instanceNum;

    @ApiModelProperty(ModelDescriptions.UsageModelDescription.PEAK)
    private Integer peak;

    @ApiModelProperty(ModelDescriptions.UsageModelDescription.FLEX_ID)
    private String flexId;

    @ApiModelProperty(ModelDescriptions.UsageModelDescription.SMARTSENSE_ID)
    private String smartSenseId;

    @ApiModelProperty(ModelDescriptions.UsageModelDescription.STACK_UUID)
    private String stackUuid;

    @ApiModelProperty(ModelDescriptions.UsageModelDescription.PARENT_UUID)
    private String parentUuid;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
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

    public String getSmartSenseId() {
        return smartSenseId;
    }

    public void setSmartSenseId(String smartSenseId) {
        this.smartSenseId = smartSenseId;
    }

    public String getStackUuid() {
        return stackUuid;
    }

    public void setStackUuid(String stackUuid) {
        this.stackUuid = stackUuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }
}
