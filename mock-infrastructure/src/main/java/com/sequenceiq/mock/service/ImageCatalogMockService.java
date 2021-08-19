package com.sequenceiq.mock.service;


import java.util.Objects;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class ImageCatalogMockService {

    private static final String DEFAULT_IMAGE_UUID = "f6e778fc-7f17-4535-9021-515351df3691";

    private static final String NON_DEFAULT_IMAGE_UUID = "1a6eadd2-5B95-4EC9-B300-13dc43208b64";

    public String getImageCatalogByName(String name, String cbVersion, String runtimeVersion, String cmVersion, String defaultImageUuid,
            String nonDefaultImageUuid) {
        String catalog = FileReaderUtils.readFileFromClasspathQuietly(String.format("mock-image-catalogs/%s.json", name));
        return catalog.replace("CB_VERSION", cbVersion)
                .replace("CDH_RUNTIME", runtimeVersion)
                .replace("CM_VERSION", Objects.requireNonNullElse(cmVersion, runtimeVersion))
                .replace("DEFAULT_IMAGE_UUID", Objects.requireNonNullElse(defaultImageUuid, DEFAULT_IMAGE_UUID))
                .replace("NON_DEFAULT_IMAGE_UUID", Objects.requireNonNullElse(nonDefaultImageUuid, NON_DEFAULT_IMAGE_UUID));
    }

}
