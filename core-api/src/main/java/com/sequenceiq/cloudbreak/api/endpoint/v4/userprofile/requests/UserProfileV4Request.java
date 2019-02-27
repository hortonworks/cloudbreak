package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests;


import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class UserProfileV4Request implements JsonEntity {

    private String credentialName;

    private Long credentialId;

    private String imageCatalogName;

    private ShowTerminatedClustersPreferencesV4Request showTerminatedClustersPreferencesV4Request;

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public void setImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
    }

    public ShowTerminatedClustersPreferencesV4Request getShowTerminatedClustersPreferencesV4Request() {
        return showTerminatedClustersPreferencesV4Request;
    }

    public void setShowTerminatedClustersPreferencesV4Request(ShowTerminatedClustersPreferencesV4Request showTerminatedClustersPreferencesV4Request) {
        this.showTerminatedClustersPreferencesV4Request = showTerminatedClustersPreferencesV4Request;
    }
}
