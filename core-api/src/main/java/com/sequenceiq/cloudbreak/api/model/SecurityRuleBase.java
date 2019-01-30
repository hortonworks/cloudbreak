package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityRuleModelDescription;
import com.sequenceiq.cloudbreak.validation.SubnetType;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SecurityRuleBase implements JsonEntity {

    @ApiModelProperty(value = SecurityRuleModelDescription.SUBNET, required = true)
    @ValidSubnet(SubnetType.CUSTOM)
    private String subnet;

    @ApiModelProperty(value = SecurityRuleModelDescription.PORTS, required = true)
    @Pattern(regexp = "^[1-9][0-9]{0,4}(-[1-9][0-9]{0,4}){0,1}(,[1-9][0-9]{0,4}(-[1-9][0-9]{0,4}){0,1})*$",
            message = "The ports field should contain a comma separated list of port numbers, for example: 8080,9090,5555")
    private String ports;

    @ApiModelProperty(value = SecurityRuleModelDescription.PROTOCOL, required = true)
    private String protocol;

    @ApiModelProperty(SecurityRuleModelDescription.MODIFIABLE)
    private Boolean modifiable;

    protected SecurityRuleBase() {
    }

    protected SecurityRuleBase(String subnet) {
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

    public Boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(Boolean modifiable) {
        this.modifiable = modifiable;
    }
}
