package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.filter;

import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ImageCatalogGetImagesV4Filter {

    @QueryParam("stackName")
    private String stackName;

    @QueryParam("platform")
    private String platform;

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
