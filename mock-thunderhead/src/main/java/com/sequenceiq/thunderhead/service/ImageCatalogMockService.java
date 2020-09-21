package com.sequenceiq.thunderhead.service;


import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class ImageCatalogMockService {

    public String getImageCatalogByName(String name, String cbVersion, String runtimeVersion) {
        String catalog = FileReaderUtils.readFileFromClasspathQuietly(String.format("mock-image-catalogs/%s.json", name));
        return catalog.replace("CB_VERSION", cbVersion)
                .replace("CDH_RUNTIME", runtimeVersion);
    }

}
