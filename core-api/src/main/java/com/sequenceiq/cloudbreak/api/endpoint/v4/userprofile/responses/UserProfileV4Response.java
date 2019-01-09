package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4ShortResponse;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserProfileV4Response {

    private CredentialV4Response credential;

    private ImageCatalogV4ShortResponse imageCatalog;

    private String username;

    private String userId;

    private String tenant;

    public CredentialV4Response getCredential() {
        return credential;
    }

    public void setCredential(CredentialV4Response credential) {
        this.credential = credential;
    }

    public ImageCatalogV4ShortResponse getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(ImageCatalogV4ShortResponse imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
