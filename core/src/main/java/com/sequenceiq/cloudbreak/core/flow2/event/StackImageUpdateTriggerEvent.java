package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

import reactor.rx.Promise;

public class StackImageUpdateTriggerEvent extends StackEvent {

    private final String newImageId;

    private String imageCatalogName;

    private String imageCatalogUrl;

    public StackImageUpdateTriggerEvent(String selector, Long stackId, String newImageId) {
        super(selector, stackId);
        this.newImageId = newImageId;
    }

    public StackImageUpdateTriggerEvent(String selector, Long stackId, String newImageId, String imageCatalogName, String imageCatalogUrl) {
        super(selector, stackId);
        this.newImageId = newImageId;
        this.imageCatalogName = imageCatalogName;
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public StackImageUpdateTriggerEvent(String selector, ImageChangeDto imageChangeDto) {
        super(selector, imageChangeDto.getStackId());
        newImageId = imageChangeDto.getImageId();
        imageCatalogName = imageChangeDto.getImageCatalogName();
        imageCatalogUrl = imageChangeDto.getImageCatalogUrl();
    }

    public StackImageUpdateTriggerEvent(String selector, Long stackId, Promise<AcceptResult> accepted, String newImageId) {
        super(selector, stackId, accepted);
        this.newImageId = newImageId;
    }

    public String getNewImageId() {
        return newImageId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(StackImageUpdateTriggerEvent.class, other,
                event -> Objects.equals(newImageId, event.newImageId)
                        && Objects.equals(imageCatalogName, event.imageCatalogName)
                        && Objects.equals(imageCatalogUrl, event.imageCatalogUrl));
    }
}
