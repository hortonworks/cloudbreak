package com.sequenceiq.cloudbreak.service.image;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;

public class ImageTestUtil {

    public static final String PLATFORM = "AZURE";

    private ImageTestUtil() {
    }

    public static StatedImage getImageFromCatalog(boolean prewarmed, String uuid, String stackVersion, String imageCatalogUrl, String imageCatalogName) {
        Image image = getImage(prewarmed, uuid, stackVersion);
        return StatedImage.statedImage(image, imageCatalogUrl, imageCatalogName);
    }

    public static StatedImage getImageFromCatalog(boolean prewarmed, String uuid, String stackVersion) {
        Image image = getImage(prewarmed, uuid, stackVersion);
        return StatedImage.statedImage(image, "url", "name");
    }

    public static Image getImage(boolean prewarmed, String uuid, String stackVersion) {
        Map<String, String> packageVersions = Collections.singletonMap("package", "version");

        Map<String, String> regionImageIdMap = new HashMap<>();
        regionImageIdMap.put("region", uuid);
        Map<String, String> stackDetailsMap = new HashMap<>();
        stackDetailsMap.put("redhat7", "http://foo/parcels");
        stackDetailsMap.put("repoid", String.format("CDH-%s", stackVersion));
        stackDetailsMap.put("repository-version", String.format("%s-1.cdh%s.p0.2457278", stackVersion, stackVersion));
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put(PLATFORM, regionImageIdMap);

        ImageStackDetails stackDetails = null;
        if (prewarmed) {
            StackRepoDetails repoDetails = new StackRepoDetails(stackDetailsMap, Collections.emptyMap());
            stackDetails = new ImageStackDetails(stackVersion, repoDetails, "1");
        }
        return new Image("imageDate", System.currentTimeMillis(), "imageDesc", "centos7", uuid, stackVersion, Collections.emptyMap(),
                imageSetsByProvider, stackDetails, "centos", packageVersions,
                Collections.emptyList(), Collections.emptyList(), "1", true, "myBaseUrl", "sourceId");
    }
}
