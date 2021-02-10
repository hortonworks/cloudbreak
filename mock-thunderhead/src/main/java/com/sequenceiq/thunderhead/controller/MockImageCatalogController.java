package com.sequenceiq.thunderhead.controller;

import javax.inject.Inject;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.thunderhead.service.ImageCatalogMockService;

@RestController
public class MockImageCatalogController {

    @Inject
    private ImageCatalogMockService imageCatalogMockService;

    @GetMapping("/thunderhead/mock-image-catalog")
    @ResponseBody
    public String auth(@RequestParam("catalog-name") String name,
            @RequestParam("cb-version") String cbVersion,
            @RequestParam("runtime") String runtime,
            @RequestParam(name = "cm", required = false) String cm,
            @RequestParam(name = "default-image-uuid", required = false) String defaultImageUuid,
            @RequestParam(name = "non-default-image-uuid", required = false) String nonDefaultImageUuid) {
        return imageCatalogMockService.getImageCatalogByName(name, cbVersion, runtime, cm, defaultImageUuid, nonDefaultImageUuid);
    }
}
