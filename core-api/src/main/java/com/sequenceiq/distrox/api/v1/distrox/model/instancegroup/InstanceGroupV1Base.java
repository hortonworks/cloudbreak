package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup;

import java.io.Serializable;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ScalabilityOption;
import com.sequenceiq.distrox.api.v1.distrox.model.CloudPlatformProvider;

import io.swagger.annotations.ApiModelProperty;

public class InstanceGroupV1Base implements Serializable, CloudPlatformProvider {

    @Min(value = 0, message = "The node count has to be greater or equals than 0")
    @Max(value = 100000, message = "The node count has to be less than 100000")
    @Digits(fraction = 0, integer = 10, message = "The node count has to be a number")
    @ApiModelProperty(value = InstanceGroupModelDescription.NODE_COUNT, required = true)
    private int nodeCount;

    @NotNull
    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String name;

    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_TYPE, allowableValues = "CORE,GATEWAY")
    private InstanceGroupType type = InstanceGroupType.CORE;

    @ApiModelProperty(InstanceGroupModelDescription.AZURE_PARAMETERS)
    private AzureInstanceGroupV1Parameters azure;

    @Valid
    @ApiModelProperty(InstanceGroupModelDescription.AWS_PARAMETERS)
    private AwsInstanceGroupV1Parameters aws;

    @ApiModelProperty
    private GcpInstanceGroupV1Parameters gcp;

    @ApiModelProperty
    private YarnInstanceGroupV1Parameters yarn;

    @ApiModelProperty(value = HostGroupModelDescription.RECOVERY_MODE, allowableValues = "MANUAL,AUTO")
    private RecoveryMode recoveryMode = RecoveryMode.MANUAL;

    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_MINIMUM_NODECOUNT)
    private Integer minimumNodeCount = 0;

    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_SCALABILITY_TYPE,
            allowableValues = "ALLOWED,FORBIDDEN,ONLY_UPSCALE,ONLY_DOWNSCALE")
    private ScalabilityOption scalabilityOption = ScalabilityOption.ALLOWED;

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

    public Integer getMinimumNodeCount() {
        return minimumNodeCount;
    }

    public void setMinimumNodeCount(Integer minimumNodeCount) {
        this.minimumNodeCount = minimumNodeCount;
    }

    public void setAzure(AzureInstanceGroupV1Parameters azure) {
        this.azure = azure;
    }

    public void setAws(AwsInstanceGroupV1Parameters aws) {
        this.aws = aws;
    }

    @Override
    public AzureInstanceGroupV1Parameters getAzure() {
        return azure;
    }

    @Override
    public AwsInstanceGroupV1Parameters getAws() {
        return aws;
    }

    @Override
    public GcpInstanceGroupV1Parameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpInstanceGroupV1Parameters gcp) {
        this.gcp = gcp;
    }

    @Override
    public YarnInstanceGroupV1Parameters getYarn() {
        return yarn;
    }

    public void setYarn(YarnInstanceGroupV1Parameters yarn) {
        this.yarn = yarn;
    }

    public RecoveryMode getRecoveryMode() {
        return recoveryMode;
    }

    public void setRecoveryMode(RecoveryMode recoveryMode) {
        this.recoveryMode = recoveryMode;
    }

    public ScalabilityOption getScalabilityOption() {
        return scalabilityOption;
    }

    public void setScalabilityOption(ScalabilityOption scalabilityOption) {
        this.scalabilityOption = scalabilityOption;
    }
}
