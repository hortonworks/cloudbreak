package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ImageSettingsV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ImageSettingsResponse extends ImageSettingsBase {

    @Schema(description = FreeIpaModelDescriptions.ImageSettingsModelDescription.LDAP_AGENT_VERSION)
    private String ldapAgentVersion;

    @Schema(description = FreeIpaModelDescriptions.ImageSettingsModelDescription.IMDS_VERSION)
    private String imdsVersion;

    @Schema(description = FreeIpaModelDescriptions.ImageSettingsModelDescription.SOURCE_IMAGE)
    private String sourceImage;

    public String getLdapAgentVersion() {
        return ldapAgentVersion;
    }

    public void setLdapAgentVersion(String ldapAgentVersion) {
        this.ldapAgentVersion = ldapAgentVersion;
    }

    public String getSourceImage() {
        return sourceImage;
    }

    public void setSourceImage(String sourceImage) {
        this.sourceImage = sourceImage;
    }

    public String getImdsVersion() {
        return imdsVersion;
    }

    public void setImdsVersion(String imdsVersion) {
        this.imdsVersion = imdsVersion;
    }

    @Override
    public String toString() {
        return "ImageSettingsResponse{" +
                "ldapAgentVersion='" + ldapAgentVersion + '\'' +
                ", sourceImage='" + sourceImage + '\'' +
                ", imdsVersion='" + imdsVersion + '\'' +
                "} " + super.toString();
    }
}
