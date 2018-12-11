package com.sequenceiq.cloudbreak.validation.bean;

public interface ImageCatalogProvider {
    Object getImageCatalogV2(String catalogUrl) throws Exception;

    void evictImageCatalogCache(String catalogUrl);
}
