package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests;


import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.base.UIPropertiesV4Base;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class UserProfileV4Request implements JsonEntity {

    private String credentialName;

    private Long credentialId;

    private String imageCatalogName;

    private Set<UIPropertiesV4Base> uiProperties;

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

    public Set<UIPropertiesV4Base> getUiProperties() {
        return uiProperties;
    }

    public void setUiProperties(Set<UIPropertiesV4Base> uiProperties) {
        this.uiProperties = uiProperties;
    }
}
