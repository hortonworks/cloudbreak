package com.sequenceiq.freeipa.api.model.instance;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AwsInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AzureInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.GcpInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.MockInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.OpenStackInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.YarnInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class InstanceGroupV4Base extends ProviderParametersBase implements JsonEntity {

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

    @ApiModelProperty(InstanceGroupModelDescription.AZURE_PARAMETERS)
    private AzureInstanceGroupV4Parameters azure;

    @ApiModelProperty(InstanceGroupModelDescription.GCP_PARAMETERS)
    private GcpInstanceGroupV4Parameters gcp;

    @ApiModelProperty(InstanceGroupModelDescription.AWS_PARAMETERS)
    private AwsInstanceGroupV4Parameters aws;

    @ApiModelProperty(InstanceGroupModelDescription.OPENSTACK_PARAMETERS)
    private OpenStackInstanceGroupV4Parameters openstack;

    @ApiModelProperty(hidden = true)
    private YarnInstanceGroupV4Parameters yarn;

    @ApiModelProperty(hidden = true)
    private MockInstanceGroupV4Parameters mock;

    @ApiModelProperty(value = HostGroupModelDescription.RECOVERY_MODE, allowableValues = "MANUAL,AUTO")
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

    public OpenStackInstanceGroupV4Parameters createOpenstack() {
        if (openstack == null) {
            openstack = new OpenStackInstanceGroupV4Parameters();
        }
        return openstack;
    }

    public void setOpenstack(OpenStackInstanceGroupV4Parameters openstack) {
        this.openstack = openstack;
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

    public OpenStackInstanceGroupV4Parameters getOpenstack() {
        return openstack;
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
}
