package com.sequenceiq.cloudbreak.api.model.users;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogShortResponse;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserProfileResponse {

    private CredentialResponse credential;

    private ImageCatalogShortResponse imageCatalog;

    private String username;

    private Map<String, Object> uiProperties = new HashMap<>();

    public CredentialResponse getCredential() {
        return credential;
    }

    public void setCredential(CredentialResponse credential) {
        this.credential = credential;
    }

    public Map<String, Object> getUiProperties() {
        return uiProperties;
    }

    public void setUiProperties(Map<String, Object> uiProperties) {
        this.uiProperties = uiProperties;
    }

    public ImageCatalogShortResponse getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(ImageCatalogShortResponse imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
