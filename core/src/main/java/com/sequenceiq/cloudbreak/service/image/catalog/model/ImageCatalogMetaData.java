package com.sequenceiq.cloudbreak.service.image.catalog.model;

import java.util.List;

public class ImageCatalogMetaData {

    private List<String> runtimeVersions;

    public ImageCatalogMetaData(List<String> runtimeVersions) {
        this.runtimeVersions = runtimeVersions;
    }

    public List<String> getRuntimeVersions() {
        return runtimeVersions;
    }

    @Override
    public String toString() {
        return "ImageCatalogMetaData{" +
                "runtimeVersions=" + runtimeVersions +
                '}';
    }
}
