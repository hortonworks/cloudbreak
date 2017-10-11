package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.RecoveryMode;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("instanceGroupsV2")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceGroupV2Request implements JsonEntity {

    @Min(value = 0, message = "The node count has to be greater or equals than 0")
    @Max(value = 100000, message = "The node count has to be less than 100000")
    @Digits(fraction = 0, integer = 10, message = "The node count has to be a number")
    @ApiModelProperty(value = InstanceGroupModelDescription.NODE_COUNT, required = true)
    private int nodeCount;

    @NotNull
    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String group;

    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_GROUP_TYPE)
    private InstanceGroupType type = InstanceGroupType.CORE;

    @ApiModelProperty(InstanceGroupModelDescription.PARAMETERS)
    private Map<String, Object> parameters = new HashMap<>();

    @ApiModelProperty(InstanceGroupModelDescription.TEMPLATE)
    private TemplateV2Request template;

    @ApiModelProperty(InstanceGroupModelDescription.SECURITYGROUP)
    private SecurityGroupV2Request securityGroup;

    @ApiModelProperty(ModelDescriptions.HostGroupModelDescription.RECIPE_NAMES)
    private Set<String> recipeNames = new HashSet<>();

    @ApiModelProperty(ModelDescriptions.HostGroupModelDescription.RECOVERY_MODE)
    private RecoveryMode recoveryMode = RecoveryMode.MANUAL;

    public TemplateV2Request getTemplate() {
        return template;
    }

    public void setTemplate(TemplateV2Request template) {
        this.template = template;
    }

    public SecurityGroupV2Request getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupV2Request securityGroup) {
        this.securityGroup = securityGroup;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public InstanceGroupType getType() {
        return type;
    }

    public void setType(InstanceGroupType type) {
        this.type = type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Set<String> getRecipeNames() {
        return recipeNames;
    }

    public void setRecipeNames(Set<String> recipeNames) {
        this.recipeNames = recipeNames;
    }

    public RecoveryMode getRecoveryMode() {
        return recoveryMode;
    }

    public void setRecoveryMode(RecoveryMode recoveryMode) {
        this.recoveryMode = recoveryMode;
    }
}
