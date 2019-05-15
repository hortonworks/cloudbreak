package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("InstanceGroupV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceGroupResponse extends InstanceGroupBase {
    @NotNull
    @ApiModelProperty(InstanceGroupModelDescription.TEMPLATE)
    private InstanceTemplateResponse instanceTemplate;

    @Valid
    @ApiModelProperty(InstanceGroupModelDescription.SECURITYGROUP)
    private SecurityGroupResponse securityGroup;

    public InstanceTemplateResponse getInstanceTemplate() {
        return instanceTemplate;
    }

    public void setInstanceTemplate(InstanceTemplateResponse instanceTemplate) {
        this.instanceTemplate = instanceTemplate;
    }

    public SecurityGroupResponse getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupResponse securityGroup) {
        this.securityGroup = securityGroup;
    }
}
