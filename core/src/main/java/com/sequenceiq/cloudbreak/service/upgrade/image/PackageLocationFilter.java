package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.regex.Pattern;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public interface PackageLocationFilter {
    Pattern URL_PATTERN = Pattern.compile("http[s]?://archive\\.cloudera\\.com.+");

    boolean filterImage(Image image, Image currentImage);
}
