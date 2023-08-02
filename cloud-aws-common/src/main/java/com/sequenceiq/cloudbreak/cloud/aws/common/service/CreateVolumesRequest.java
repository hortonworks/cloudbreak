package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;

import software.amazon.awssdk.services.ec2.model.TagSpecification;

public class CreateVolumesRequest {

    private List<CloudResource> cloudResources;

    private int attachedVolumesCount;

    private int volToAddPerInstance;

    private VolumeSetAttributes.Volume volumeRequest;

    private TagSpecification tagSpecification;

    private String volumeEncryptionKey;

    private boolean encryptedVolume;

    private String availabilityZone;

    @JsonCreator
    public CreateVolumesRequest(
            @JsonProperty("cloudResources") List<CloudResource> cloudResources,
            @JsonProperty("attachedVolumesCount") int attachedVolumesCount,
            @JsonProperty("volToAddPerInstance") int volToAddPerInstance,
            @JsonProperty("volumeRequest") VolumeSetAttributes.Volume volumeRequest,
            @JsonProperty("tagSpecification") TagSpecification tagSpecification,
            @JsonProperty("volumeEncryptionKey") String volumeEncryptionKey,
            @JsonProperty("encryptedVolume") boolean encryptedVolume
        ) {
        this.cloudResources = cloudResources;
        this.attachedVolumesCount = attachedVolumesCount;
        this.volToAddPerInstance = volToAddPerInstance;
        this.volumeRequest = volumeRequest;
        this.tagSpecification = tagSpecification;
        this.volumeEncryptionKey = volumeEncryptionKey;
        this.encryptedVolume = encryptedVolume;
        this.availabilityZone = "";
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public void setCloudResources(List<CloudResource> cloudResources) {
        this.cloudResources = cloudResources;
    }

    public int getAttachedVolumesCount() {
        return attachedVolumesCount;
    }

    public void setAttachedVolumesCount(int attachedVolumesCount) {
        this.attachedVolumesCount = attachedVolumesCount;
    }

    public int getVolToAddPerInstance() {
        return volToAddPerInstance;
    }

    public void setVolToAddPerInstance(int volToAddPerInstance) {
        this.volToAddPerInstance = volToAddPerInstance;
    }

    public VolumeSetAttributes.Volume getVolumeRequest() {
        return volumeRequest;
    }

    public void setVolumeRequest(VolumeSetAttributes.Volume volumeRequest) {
        this.volumeRequest = volumeRequest;
    }

    public TagSpecification getTagSpecification() {
        return tagSpecification;
    }

    public void setTagSpecification(TagSpecification tagSpecification) {
        this.tagSpecification = tagSpecification;
    }

    public String getVolumeEncryptionKey() {
        return volumeEncryptionKey;
    }

    public void setVolumeEncryptionKey(String volumeEncryptionKey) {
        this.volumeEncryptionKey = volumeEncryptionKey;
    }

    public boolean isEncryptedVolume() {
        return encryptedVolume;
    }

    public void setEncryptedVolume(boolean encryptedVolume) {
        this.encryptedVolume = encryptedVolume;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CreateVolumesRequest.class.getSimpleName() + "[", "]")
                .add("cloudResources=" + cloudResources)
                .add("attachedVolumesCount=" + attachedVolumesCount)
                .add("volToAddPerInstance=" + volToAddPerInstance)
                .add("volumeRequest=" + volumeRequest)
                .add("isEncryptedVolume=" + encryptedVolume)
                .add("availabilityZone=" + availabilityZone)
                .toString();
    }
}
