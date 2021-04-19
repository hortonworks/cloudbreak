package com.sequenceiq.freeipa.service.image;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class ImageProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageProviderFactory.class);

    @Inject
    private FreeIpaImageProvider freeIpaImageProvider;

    @Inject
    private CoreImageProvider coreImageProvider;

    public ImageProvider getImageProvider(String catalog) {
        if (Strings.isNullOrEmpty(catalog) || catalog.startsWith("http") || catalog.endsWith(".json")) {
            LOGGER.info("Use image provider with FreeIpaSvc downstream to lookup image by id.");
            return freeIpaImageProvider;
        } else {
            LOGGER.info("Use image provider with CoreSvc downstream to lookup image by id.");
            return coreImageProvider;
        }
    }
}
