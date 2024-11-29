package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InstanceGroupV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceGroupResponse extends InstanceGroupBase {
    @NotNull
    @Schema(description = InstanceGroupModelDescription.TEMPLATE, requiredMode = Schema.RequiredMode.REQUIRED)
    private InstanceTemplateResponse instanceTemplate;

    @Valid
    @Schema(description = InstanceGroupModelDescription.SECURITYGROUP, requiredMode = Schema.RequiredMode.REQUIRED)
    private SecurityGroupResponse securityGroup;

    @NotNull
    @Schema(description = InstanceGroupModelDescription.NETWORK, requiredMode = Schema.RequiredMode.REQUIRED)
    private InstanceGroupNetworkResponse network;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<InstanceMetaDataResponse> metaData = new HashSet<>();

    @Schema(description = InstanceGroupModelDescription.AVAILABILITY_ZONES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> availabilityZones = new HashSet<>();

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

    public Set<InstanceMetaDataResponse> getMetaData() {
        return metaData;
    }

    public void setMetaData(Set<InstanceMetaDataResponse> metaData) {
        this.metaData = metaData;
    }

    public InstanceGroupNetworkResponse getNetwork() {
        return network;
    }

    public void setNetwork(InstanceGroupNetworkResponse network) {
        this.network = network;
    }

    public Set<String> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Set<String> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    @Override
    public String toString() {
        return "InstanceGroupResponse{" +
                "InstanceGroupBase=" + super.toString() +
                ", instanceTemplate=" + instanceTemplate +
                ", securityGroup=" + securityGroup +
                ", network=" + network +
                ", metaData=" + metaData +
                ", availabilityZones=" + availabilityZones +
                '}';
    }
}
