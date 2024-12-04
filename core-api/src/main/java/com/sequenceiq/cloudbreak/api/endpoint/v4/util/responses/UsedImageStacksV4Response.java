package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import io.swagger.v3.oas.annotations.media.Schema;

public class UsedImageStacksV4Response {

    private UsedImageV4Response image;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int numberOfStacks;

    public UsedImageStacksV4Response(UsedImageV4Response image) {
        this.image = image;
        this.numberOfStacks = 1;
    }

    public UsedImageStacksV4Response() {
    }

    public UsedImageV4Response getImage() {
        return image;
    }

    public int getNumberOfStacks() {
        return numberOfStacks;
    }

    public void addUsage() {
        this.numberOfStacks++;
    }

    public void setImage(UsedImageV4Response image) {
        this.image = image;
    }

    public void setNumberOfStacks(int numberOfStacks) {
        this.numberOfStacks = numberOfStacks;
    }

    @Override
    public String toString() {
        return "UsedImageStacksV4Response{" +
                "image=" + image +
                ", numberOfStacks=" + numberOfStacks +
                '}';
    }
}
