package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.LinkedList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.Image;

public class UsedImagesListV4Response {

    private List<UsedImageStacksV4Response> usedImages;

    public UsedImagesListV4Response() {
        this.usedImages = new LinkedList<>();
    }

    public List<UsedImageStacksV4Response> getUsedImages() {
        return usedImages;
    }

    public void setUsedImages(List<UsedImageStacksV4Response> usedImages) {
        this.usedImages = usedImages;
    }

    public void addImage(Image image) {
        final UsedImageV4Response imageView = new UsedImageV4Response(image);
        this.usedImages.stream()
                .filter(usedImage -> usedImage.getImage().equals(imageView))
                .findFirst()
                .ifPresentOrElse(UsedImageStacksV4Response::addUsage, () -> this.usedImages.add(new UsedImageStacksV4Response(imageView)));
    }

    @Override
    public String toString() {
        return "UsedImagesListV4Response{" +
                "usedImages=" + usedImages +
                '}';
    }
}
