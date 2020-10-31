package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageDetails implements Serializable {

    private String imageName;

    private String os;

    private String osType;

    private String imageCatalogUrl;

    private String imageId;

    private String imageCatalogName;

    private Map<String, String> packageVersions;

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public void setImageCatalogUrl(String imageCatalogUrl) {
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public void setImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
    }

    public Map<String, String> getPackageVersions() {
        return packageVersions;
    }

    public void setPackageVersions(Map<String, String> packageVersions) {
        this.packageVersions = packageVersions;
    }

    @Override
    public String toString() {
        return "Image{"
                + "imageName='" + imageName + '\''
                + ", os='" + os + '\''
                + ", osType='" + osType + '\''
                + ", imageCatalogUrl='" + imageCatalogUrl + '\''
                + ", imageId='" + imageId + '\''
                + ", imageCatalogName='" + imageCatalogName + '\''
                + ", packageVersions=" + packageVersions + '}';
    }
}
