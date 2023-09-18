package com.sequenceiq.freeipa.dto;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;

public class ImageWrapper {

    private final Image image;

    private final String catalogUrl;

    private final String catalogName;

    private ImageWrapper(Image image, String catalogUrl, String catalogName) {
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

    public static ImageWrapper ofFreeipaImage(Image image, String catalogUrl) {
        return new ImageWrapper(image, catalogUrl, null);
    }

    public static ImageWrapper ofCoreImage(Image image, String catalogName) {
        return new ImageWrapper(image, null, catalogName);
    }

    @Override
    public String toString() {
        return "ImageWrapper{" +
                "image=" + image +
                ", catalogUrl='" + catalogUrl + '\'' +
                ", catalogName='" + catalogName + '\'' +
                '}';
    }
}