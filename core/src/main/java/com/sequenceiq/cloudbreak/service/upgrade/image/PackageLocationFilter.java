package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public interface PackageLocationFilter {

    List<String> URL_PATTERNS = List.of(
            "http[s]?://archive\\.cloudera\\.com.+",
            "http[s]?://archive\\.repo\\.cdp\\.clouderagovt\\.com.+",
            "http[s]?://archive\\.releng\\.gov-dev\\.cloudera\\.com.+",
            "http[s]?://stage\\.repo\\.cdp\\.clouderagovt\\.com.+"
    );

    boolean filterImage(Image image, ImageFilterParams imageFilterParams);

    default boolean isArchiveUrl(String url) {
        return URL_PATTERNS.stream().anyMatch(url::matches);
    }
}
