package com.sequenceiq.environment.credential.attributes.cumulus;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CumulusYarnCredentialAttributes implements Serializable {

    private String ambariPassword;

    private String ambariUrl;

    private String ambariUser;

    public String getAmbariPassword() {
        return ambariPassword;
    }

    public void setAmbariPassword(String ambariPassword) {
        this.ambariPassword = ambariPassword;
    }

    public String getAmbariUrl() {
        return ambariUrl;
    }

    public void setAmbariUrl(String ambariUrl) {
        this.ambariUrl = ambariUrl;
    }

    public String getAmbariUser() {
        return ambariUser;
    }

    public void setAmbariUser(String ambariUser) {
        this.ambariUser = ambariUser;
    }
}
