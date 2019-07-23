package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.ImageSettingsModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class ImageSettingsBase {
    @ApiModelProperty(ImageSettingsModelDescription.IMAGE_CATALOG)
    private String catalog;

    @ApiModelProperty(ImageSettingsModelDescription.IMAGE_ID)
    private String id;

    @ApiModelProperty(ImageSettingsModelDescription.OS_TYPE)
    private String os;

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    @Override
    public String toString() {
        return "ImageSettingsBase{"
                + "catalog='" + catalog + '\''
                + ", id='" + id + '\''
                + ", os='" + os + '\''
                + '}';
    }
}
