package com.sequenceiq.cloudbreak.service.image;

import java.io.Serializable;
import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class ImageComparator implements Comparator<Image>, Serializable {

    @Override
    public int compare(Image image1, Image image2) {
        return image1.getCreated() == null || image2.getCreated() == null
                ? image1.getDate().compareTo(image2.getDate())
                : image1.getCreated().compareTo(image2.getCreated());
    }
}