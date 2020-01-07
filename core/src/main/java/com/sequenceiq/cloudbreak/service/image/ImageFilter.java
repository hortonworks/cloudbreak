package com.sequenceiq.cloudbreak.service.image;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.ImageCatalog;

public class ImageFilter {

    private final ImageCatalog imageCatalog;

    private final Set<String> platforms;

    private final String cbVersion;

    private boolean baseImageEnabled;

    private Set<String> operatingSystems;

    private String clusterVersion;

    public ImageFilter(ImageCatalog imageCatalog, Set<String> platforms, String cbVersion) {
        this.imageCatalog = imageCatalog;
        this.platforms = platforms;
        this.cbVersion = cbVersion;
    }

    public ImageFilter(ImageCatalog imageCatalog, Set<String> platforms, String cbVersion, boolean baseImageEnabled, Set<String> operatingSystems,
            String clusterVersion) {
        this.imageCatalog = imageCatalog;
        this.platforms = platforms;
        this.cbVersion = cbVersion;
        this.baseImageEnabled = baseImageEnabled;
        this.operatingSystems = operatingSystems;
        this.clusterVersion = clusterVersion;
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

    public boolean isBaseImageEnabled() {
        return baseImageEnabled;
    }

    public Set<String> getOperatingSystems() {
        return operatingSystems;
    }

    public String getClusterVersion() {
        return clusterVersion;
    }
}
