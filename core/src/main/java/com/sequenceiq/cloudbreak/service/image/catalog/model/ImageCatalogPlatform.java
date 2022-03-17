package com.sequenceiq.cloudbreak.service.image.catalog.model;

public class ImageCatalogPlatform {

    private final String platform;

    public ImageCatalogPlatform(String platform) {
        this.platform = platform.toLowerCase();
    }

    public String name() {
        return platform;
    }

    public String nameToUpperCase() {
        return platform.toUpperCase();
    }

    public String nameToLowerCase() {
        return platform.toLowerCase();
    }

    public static ImageCatalogPlatform imageCatalogPlatform(String platform) {
        return new ImageCatalogPlatform(platform.toUpperCase());
    }
}
