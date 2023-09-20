package com.sequenceiq.common.model;

import java.util.Locale;
import java.util.Objects;

public class ImageCatalogPlatform {

    private final String platform;

    public ImageCatalogPlatform(String platform) {
        this.platform = platform.toLowerCase(Locale.ROOT);
    }

    public String name() {
        return platform;
    }

    public String nameToUpperCase() {
        return platform.toUpperCase(Locale.ROOT);
    }

    public String nameToLowerCase() {
        return platform.toLowerCase(Locale.ROOT);
    }

    public static ImageCatalogPlatform imageCatalogPlatform(String platform) {
        return new ImageCatalogPlatform(platform.toUpperCase(Locale.ROOT));
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

    @Override
    public String toString() {
        return "ImageCatalogPlatform{" +
                "platform='" + platform + '\'' +
                '}';
    }
}
