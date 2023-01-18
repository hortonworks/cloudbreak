package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AwsInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AzureInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.GcpInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.MockInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.OpenStackInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.YarnInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ScalabilityOption;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceGroupV4Base extends ProviderParametersBase implements JsonEntity {

    @Min(value = 0, message = "The node count has to be greater or equals than 0")
    @Max(value = 400, message = "The node count has to be less than 400")
    @Digits(fraction = 0, integer = 10, message = "The node count has to be a number")
    @Schema(description = InstanceGroupModelDescription.NODE_COUNT, required = true)
    private int nodeCount;

    @NotNull
    @Schema(description = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String name;

    @Schema(description = InstanceGroupModelDescription.INSTANCE_GROUP_TYPE)
    private InstanceGroupType type = InstanceGroupType.CORE;

    @Schema(description = InstanceGroupModelDescription.INSTANCE_GROUP_SCALABILITY_TYPE)
    private ScalabilityOption scalabilityOption = ScalabilityOption.ALLOWED;

    @Schema(description = InstanceGroupModelDescription.INSTANCE_GROUP_MINIMUM_NODECOUNT)
    private Integer minimumNodeCount = 0;

    @Schema(description = InstanceGroupModelDescription.AZURE_PARAMETERS)
    private AzureInstanceGroupV4Parameters azure;

    @Schema(description = InstanceGroupModelDescription.GCP_PARAMETERS)
    private GcpInstanceGroupV4Parameters gcp;

    @Valid
    @Schema(description = InstanceGroupModelDescription.AWS_PARAMETERS)
    private AwsInstanceGroupV4Parameters aws;

    @Schema(description = InstanceGroupModelDescription.OPENSTACK_PARAMETERS_DEPRECATED)
    @Deprecated
    private OpenStackInstanceGroupV4Parameters openstack;

    @Schema(hidden = true)
    private YarnInstanceGroupV4Parameters yarn;

    @Schema
    private MockInstanceGroupV4Parameters mock;

    @Schema(description = HostGroupModelDescription.RECOVERY_MODE)
    private RecoveryMode recoveryMode = RecoveryMode.MANUAL;

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

    public AzureInstanceGroupV4Parameters createAzure() {
        if (azure == null) {
            azure = new AzureInstanceGroupV4Parameters();
        }
        return azure;
    }

    public void setAzure(AzureInstanceGroupV4Parameters azure) {
        this.azure = azure;
    }

    public GcpInstanceGroupV4Parameters createGcp() {
        if (gcp == null) {
            gcp = new GcpInstanceGroupV4Parameters();
        }
        return gcp;
    }

    public void setGcp(GcpInstanceGroupV4Parameters gcp) {
        this.gcp = gcp;
    }

    public AwsInstanceGroupV4Parameters createAws() {
        if (aws == null) {
            aws = new AwsInstanceGroupV4Parameters();
        }
        return aws;
    }

    public void setAws(AwsInstanceGroupV4Parameters aws) {
        this.aws = aws;
    }

    @Override
    public YarnInstanceGroupV4Parameters createYarn() {
        if (yarn == null) {
            yarn = new YarnInstanceGroupV4Parameters();
        }
        return yarn;
    }

    public void setYarn(YarnInstanceGroupV4Parameters yarn) {
        this.yarn = yarn;
    }

    @Override
    public MockInstanceGroupV4Parameters createMock() {
        if (mock == null) {
            mock = new MockInstanceGroupV4Parameters();
        }
        return mock;
    }

    public void setMock(MockInstanceGroupV4Parameters mock) {
        this.mock = mock;
    }

    public AzureInstanceGroupV4Parameters getAzure() {
        return azure;
    }

    public GcpInstanceGroupV4Parameters getGcp() {
        return gcp;
    }

    public AwsInstanceGroupV4Parameters getAws() {
        return aws;
    }

    public YarnInstanceGroupV4Parameters getYarn() {
        return yarn;
    }

    public MockInstanceGroupV4Parameters getMock() {
        return mock;
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
