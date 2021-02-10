package com.sequenceiq.freeipa.api.v1.util.model;

public class UsedImageStacksV1Response {

    private UsedImageV1Response image;

    private int numberOfStacks;

    public UsedImageStacksV1Response(UsedImageV1Response image) {
        this.image = image;
        this.numberOfStacks = 1;
    }

    public UsedImageStacksV1Response() {
    }

    public UsedImageV1Response getImage() {
        return image;
    }

    public int getNumberOfStacks() {
        return numberOfStacks;
    }

    public void addUsage() {
        this.numberOfStacks++;
    }

    public void setImage(UsedImageV1Response image) {
        this.image = image;
    }

    public void setNumberOfStacks(int numberOfStacks) {
        this.numberOfStacks = numberOfStacks;
    }

    @Override
    public String toString() {
        return "UsedImageStacksV1Response{" +
                "image=" + image +
                ", numberOfStacks=" + numberOfStacks +
                '}';
    }
}
