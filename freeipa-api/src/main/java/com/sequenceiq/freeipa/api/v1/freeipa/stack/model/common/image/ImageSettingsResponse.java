package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ImageSettingsV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ImageSettingsResponse extends ImageSettingsBase {

    @ApiModelProperty(FreeIpaModelDescriptions.ImageSettingsModelDescription.LDAP_AGENT_VERSION)
    private String ldapAgentVersion;

    public String getLdapAgentVersion() {
        return ldapAgentVersion;
    }

    public void setLdapAgentVersion(String ldapAgentVersion) {
        this.ldapAgentVersion = ldapAgentVersion;
    }

    @Override
    public String toString() {
        return "ImageSettingsResponse{" +
                "ldapAgentVersion='" + ldapAgentVersion + '\'' +
                "} " + super.toString();
    }
}
