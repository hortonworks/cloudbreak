package com.sequenceiq.cloudbreak.service.image;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakImageCatalog;

@Service
public class ImageCatalogProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    public CloudbreakImageCatalog getImageCatalog(String customImageCatalog) {
        if (defaultCatalogUrl == null && customImageCatalog == null) {
            LOGGER.warn("No image catalog was defined!");
            return null;
        }
        String catalogUrl = defaultCatalogUrl;
        if (customImageCatalog != null) {
            catalogUrl = customImageCatalog;
        }
        LOGGER.info("Used Image Catalog: {}", catalogUrl);
        try {
            if (catalogUrl.startsWith("http")) {
                Client client = RestClientUtil.get();
                WebTarget target = client.target(catalogUrl);
                return (CloudbreakImageCatalog) target.request().get().getEntity();
            } else {
                LOGGER.warn("Image catalog URL is not valid: {}", catalogUrl);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get image catalog", e);
        }
        return null;
    }

    public String getDefaultCatalogUrl() {
        return defaultCatalogUrl;
    }
}
