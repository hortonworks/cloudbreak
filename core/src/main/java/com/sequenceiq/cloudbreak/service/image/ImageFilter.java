package com.sequenceiq.cloudbreak.service.image;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.ImageCatalog;

public class ImageFilter {

    private final ImageCatalog imageCatalog;

    private final Set<String> platforms;

    private final String cbVersion;

    public ImageFilter(ImageCatalog imageCatalog, Set<String> platforms, String cbVersion) {
        this.imageCatalog = imageCatalog;
        this.platforms = platforms;
        this.cbVersion = cbVersion;
    }

    public ImageCatalog getImageCatalog() {
        return imageCatalog;
    }

    public Set<String> getPlatforms() {
        return platforms;
    }

    public String getCbVersion() {
        return cbVersion;
    }
}
