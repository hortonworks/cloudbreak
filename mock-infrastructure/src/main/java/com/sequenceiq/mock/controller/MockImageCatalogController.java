package com.sequenceiq.mock.controller;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.service.ImageCatalogMockService;

@RestController
public class MockImageCatalogController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockImageCatalogController.class);

    @Inject
    private ImageCatalogMockService imageCatalogMockService;

    @GetMapping("/mock-image-catalog")
    @ResponseBody
    public String auth(@RequestParam("catalog-name") String name,
            @RequestParam("cb-version") String cbVersion,
            @RequestParam("runtime") String runtime,
            @RequestParam(name = "cm", required = false) String cm,
            @RequestParam(name = "default-image-uuid", required = false) String defaultImageUuid,
            @RequestParam(name = "non-default-image-uuid", required = false) String nonDefaultImageUuid,
            @RequestParam("mock-server-address") String mockServerAddress) {
        LOGGER.info("Request to generate image catalog with name: {}, cb version: {}, runtime: {}, cm: {}, default image uuid: {}, " +
                "non default image uuid: {}, mock server address: {}", name, cbVersion, runtime, cm, defaultImageUuid, nonDefaultImageUuid, mockServerAddress);
        String generatedImageCatalog = imageCatalogMockService.getImageCatalogByName(name, cbVersion, runtime, cm, defaultImageUuid,
                nonDefaultImageUuid, mockServerAddress);
        LOGGER.info("Generated image catalog: {}", generatedImageCatalog);
        return generatedImageCatalog;
    }
}
