package com.sequenceiq.mock.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class ImageCatalogMockService {

    private static final String DEFAULT_IMAGE_UUID = "f6e778fc-7f17-4535-9021-515351df3691";

    private static final String NON_DEFAULT_IMAGE_UUID = "1a6eadd2-5B95-4EC9-B300-13dc43208b64";

    private static final String PATCH_VERSION_POSTFIX = ".600";

    public String getImageCatalogByName(String name, String cbVersion, String runtimeVersion, String cmVersion, String defaultImageUuid,
            String nonDefaultImageUuid, String mockServerAddress) {
        String catalog = FileReaderUtils.readFileFromClasspathQuietly(String.format("mock-image-catalogs/%s.json", name));
        String nextRuntimeVersion = getNextRuntimeVersion(runtimeVersion);
        return catalog.replace("CB_VERSION", cbVersion)
                .replace("CDH_RUNTIME_NEXT", nextRuntimeVersion)
                .replace("CDH_RUNTIME", runtimeVersion)
                .replace("CDH_RELEASE_NEXT", nextRuntimeVersion + PATCH_VERSION_POSTFIX)
                .replace("CDH_RELEASE", runtimeVersion + PATCH_VERSION_POSTFIX)
                .replace("CM_VERSION_NEXT", Objects.requireNonNullElse(cmVersion, nextRuntimeVersion))
                .replace("CM_VERSION", Objects.requireNonNullElse(cmVersion, runtimeVersion))
                .replace("NON_DEFAULT_IMAGE_UUID", Objects.requireNonNullElse(nonDefaultImageUuid, NON_DEFAULT_IMAGE_UUID))
                .replace("DEFAULT_IMAGE_UUID", Objects.requireNonNullElse(defaultImageUuid, DEFAULT_IMAGE_UUID))
                .replace("MOCK_SERVER_ADDRESS", mockServerAddress);
    }

    public String getNextRuntimeVersion(String runtime) {
        if ("7.2.18".equals(runtime)) {
            return "7.3.1";
        }
        String[] splitted = runtime.split("\\.");
        int last = Integer.parseInt(splitted[splitted.length - 1]);
        List<String> elements = new ArrayList<>(Arrays.asList(splitted).subList(0, splitted.length - 1));
        elements.add(String.valueOf(last + 1));
        return String.join(".", elements);
    }
}
