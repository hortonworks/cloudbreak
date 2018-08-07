package com.sequenceiq.cloudbreak.api.model.users;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileRequest implements JsonEntity {

    private String credentialName;

    private Long credentialId;

    private String imageCatalogName;

    private Map<String, Object> uiProperties = new HashMap<>();

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

    public Map<String, Object> getUiProperties() {
        return uiProperties;
    }

    public void setUiProperties(Map<String, Object> uiProperties) {
        this.uiProperties = uiProperties;
    }
}
