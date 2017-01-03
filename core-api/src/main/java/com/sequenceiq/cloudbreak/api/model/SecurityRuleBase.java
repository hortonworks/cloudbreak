package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SecurityRuleBase implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.SUBNET, required = true)
    @Pattern(regexp =
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/([0-9]|[1-2][0-9]|3[0-2]))$",
            message = "The subnet field should contain a valid CIDR definition.")
    private String subnet;

    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.PORTS, required = true)
    @Pattern(regexp = "^[1-9][0-9]{0,4}(-[1-9][0-9]{0,4}){0,1}(,[1-9][0-9]{0,4}(-[1-9][0-9]{0,4}){0,1})*$",
            message = "The ports field should contain a comma separated list of port numbers, for example: 8080,9090,5555")
    private String ports;

    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.PROTOCOL, required = true)
    private String protocol;

    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.MODIFIABLE)
    private boolean modifiable;

    public SecurityRuleBase() {
    }

    public SecurityRuleBase(String subnet) {
        this.subnet = subnet;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getPorts() {
        return ports;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }
}
