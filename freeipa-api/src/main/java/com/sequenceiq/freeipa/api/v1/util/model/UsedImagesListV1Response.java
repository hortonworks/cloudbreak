package com.sequenceiq.freeipa.api.v1.util.model;

import java.util.LinkedList;
import java.util.List;

public class UsedImagesListV1Response {

    private List<UsedImageStacksV1Response> usedImages;

    public UsedImagesListV1Response() {
        this.usedImages = new LinkedList<>();
    }

    public List<UsedImageStacksV1Response> getUsedImages() {
        return usedImages;
    }

    public void addImage(String imageId) {
        final UsedImageV1Response imageView = new UsedImageV1Response(imageId);
        this.usedImages.stream()
                .filter(usedImage -> usedImage.getImage().equals(imageView))
                .findFirst()
                .ifPresentOrElse(UsedImageStacksV1Response::addUsage, () -> this.usedImages.add(new UsedImageStacksV1Response(imageView)));
    }

    public void setUsedImages(List<UsedImageStacksV1Response> usedImages) {
        this.usedImages = usedImages;
    }

    @Override
    public String toString() {
        return "UsedImagesListV1Response{" +
                "usedImages=" + usedImages +
                '}';
    }
}
