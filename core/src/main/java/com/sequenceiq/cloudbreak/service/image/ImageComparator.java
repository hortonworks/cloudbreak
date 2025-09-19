package com.sequenceiq.cloudbreak.service.image;

import java.io.Serializable;
import java.util.Comparator;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class ImageComparator implements Comparator<Image>, Serializable {

    @Inject
    private ImageOsService imageOsService;

    @Override
    public int compare(Image image1, Image image2) {
        return Comparator.comparing(Image::getOs, osComparator())
                .thenComparing(imageDateComparator())
                .compare(image1, image2);
    }

    private Comparator<String> osComparator() {
        return Comparator.comparing(imageOsService.getPreferredOs()::equalsIgnoreCase);
    }

    private Comparator<Image> imageDateComparator() {
        return (image1, image2) -> image1.getCreated() == null || image2.getCreated() == null
                ? image1.getDate().compareTo(image2.getDate())
                : image1.getCreated().compareTo(image2.getCreated());
    }
}
