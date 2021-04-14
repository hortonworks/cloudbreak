package com.sequenceiq.freeipa.dto;

import com.sequenceiq.freeipa.api.model.image.Image;

public class ImageWrapper {

    private Image image;

    private String catalogUrl;

    private String catalogName;

    public ImageWrapper(Image image, String catalogUrl, String catalogName) {
        this.image = image;
        this.catalogUrl = catalogUrl;
        this.catalogName = catalogName;
    }

    public Image getImage() {
        return image;
    }

    public String getCatalogUrl() {
        return catalogUrl;
    }

    public String getCatalogName() {
        return catalogName;
    }
}