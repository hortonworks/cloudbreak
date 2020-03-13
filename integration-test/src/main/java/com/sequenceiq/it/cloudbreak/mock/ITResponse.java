package com.sequenceiq.it.cloudbreak.mock;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import spark.Route;

public abstract class ITResponse implements Route {

    public static final String AMBARI_API_ROOT = "/api/v1";

    public static final String MOCK_ROOT = "/spi";

    public static final String SALT_API_ROOT = "/saltapi";

    public static final String SALT_BOOT_ROOT = "/saltboot";

    public static final String FREEIPA_ROOT = "/ipa";

    public static final String IMAGE_CATALOG = "/imagecatalog";

    public static final String FREEIPA_IMAGE_CATALOG = "/freeipaimagecatalog";

    public static final String IMAGE_CATALOG_PREWARMED = "/imagecatalogwithprewarmed";

    public static final String IMAGE_CATALOG_UPGRADE = "/imagecatalogupgrade";

    private static final Logger LOGGER = LoggerFactory.getLogger(ITResponse.class);

    private static final String MOCKRESPONSE = "/mockresponse/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected static String responseFromJsonFile(String path) {
        try (InputStream inputStream = ITResponse.class.getResourceAsStream(MOCKRESPONSE + path)) {
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            LOGGER.error("can't read file from path", e);
            return "";
        }
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
