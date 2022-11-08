package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public interface ImageFilter {

    List<Image> filterImages(List<Image> imageList);
}
