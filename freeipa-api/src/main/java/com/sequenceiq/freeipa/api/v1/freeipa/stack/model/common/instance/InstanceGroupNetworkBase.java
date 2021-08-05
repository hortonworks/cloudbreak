package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.io.Serializable;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.InstanceGroupAwsNetworkParameters;

import io.swagger.annotations.ApiModelProperty;

public class InstanceGroupNetworkBase implements Serializable {

    @ApiModelProperty(FreeIpaModelDescriptions.InstanceGroupModelDescription.AWS_PARAMETERS)
    private InstanceGroupAwsNetworkParameters aws;

    public InstanceGroupAwsNetworkParameters getAws() {
        return aws;
    }

    public void setAws(InstanceGroupAwsNetworkParameters aws) {
        this.aws = aws;
    }

    @Override
    public String toString() {
        return "InstanceGroupNetworkBase{" +
                "aws=" + aws +
                '}';
    }
}
