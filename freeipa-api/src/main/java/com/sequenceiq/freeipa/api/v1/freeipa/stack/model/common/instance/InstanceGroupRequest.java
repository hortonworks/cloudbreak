package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
    @NotNull
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
}
