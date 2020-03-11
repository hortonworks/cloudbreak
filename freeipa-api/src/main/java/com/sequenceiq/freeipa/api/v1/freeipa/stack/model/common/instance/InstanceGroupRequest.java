package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("InstanceGroupV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceGroupRequest extends InstanceGroupBase {
    @ApiModelProperty(InstanceGroupModelDescription.TEMPLATE)
    private InstanceTemplateRequest instanceTemplate;

    @Valid
    @ApiModelProperty(InstanceGroupModelDescription.SECURITYGROUP)
    private SecurityGroupRequest securityGroup;

    public InstanceTemplateRequest getInstanceTemplate() {
        return instanceTemplate;
    }

    public void setInstanceTemplateRequest(InstanceTemplateRequest instanceTemplate) {
        this.instanceTemplate = instanceTemplate;
    }

    public SecurityGroupRequest getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupRequest securityGroup) {
        this.securityGroup = securityGroup;
    }

    @Override
    public String toString() {
        return super.toString() + "InstanceGroupRequest{"
                + "instanceGroupName=" + getName()
                + ", nodeCount=" + getNodeCount()
                + ", type=" + getType()
                + ", instanceTemplate=" + instanceTemplate
                + ", securityGroup=" + securityGroup
                + '}';
    }
}
