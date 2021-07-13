package com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsBase;

public class ImageInfoResponse extends ImageSettingsBase {

    private String imageName;

    private String date;

    private String catalogName;

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    @Override
    public String toString() {
        return "ImageInfoResponse{" +
                "imageName='" + imageName + '\'' +
                ", date='" + date + '\'' +
                ", catalogName='" + catalogName + '\'' +
                "} " + super.toString();
    }
}
