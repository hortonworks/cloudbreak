package com.sequenceiq.freeipa.dto;

import com.sequenceiq.freeipa.api.model.image.Image;

public class ImageWrapper {

    private Image image;

    private String catalogUrl;

    private String catalonName;

    public ImageWrapper(Image image, String catalogUrl, String catalonName) {
        this.image = image;
        this.catalogUrl = catalogUrl;
        this.catalonName = catalonName;
    }

    public Image getImage() {
        return image;
    }

    public String getCatalogUrl() {
        return catalogUrl;
    }

    public String getCatalonName() {
        return catalonName;
    }
}
