package com.sequenceiq.cloudbreak.service.image.catalog.model;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImageCatalogPlatform)) {
            return false;
        }
        ImageCatalogPlatform that = (ImageCatalogPlatform) o;
        return Objects.equals(platform, that.platform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform);
    }
}
