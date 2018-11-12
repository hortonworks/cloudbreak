package com.sequenceiq.cloudbreak.common.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VolumeSetResourceAttributes {

    private List<Volume> volumes;

    private String availabilityZone;

    private Integer volumeSize;

    private String volumeType;

    private Boolean deleteOnTermination;

    private String fstab;

    @JsonCreator
    public VolumeSetResourceAttributes(@JsonProperty("availabilityZone") String availabilityZone, @JsonProperty("volumeSize") Integer volumeSize,
            @JsonProperty("volumeType") String volumeType, @JsonProperty("deleteOnTermination") Boolean deleteOnTermination,
            @JsonProperty("fstab") String fstab, @JsonProperty("volumes") List<Volume> volumes) {
        this.availabilityZone = availabilityZone;
        this.volumeSize = volumeSize;
        this.volumeType = volumeType;
        this.deleteOnTermination = deleteOnTermination;
        this.fstab = fstab;
        this.volumes = volumes;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<Volume> volumes) {
        this.volumes = volumes;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Integer getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(Integer volumeSize) {
        this.volumeSize = volumeSize;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public Boolean getDeleteOnTermination() {
        return deleteOnTermination;
    }

    public void setDeleteOnTermination(Boolean deleteOnTermination) {
        this.deleteOnTermination = deleteOnTermination;
    }

    public String getFstab() {
        return fstab;
    }

    public void setFstab(String fstab) {
        this.fstab = fstab;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Volume {

        private String id;

        private String mounthPath;

        private String uuid;

        private String fstab;

        private String device;

        public Volume(@JsonProperty("id") String id, @JsonProperty("mounthPath") String mounthPath, @JsonProperty("uuid") String uuid,
                @JsonProperty("device") String device) {
            this.id = id;
            this.mounthPath = mounthPath;
            this.uuid = uuid;
            this.device = device;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMounthPath() {
            return mounthPath;
        }

        public void setMounthPath(String mountPath) {
            this.mounthPath = mountPath;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getFstab() {
            return fstab;
        }

        public void setFstab(String fstab) {
            this.fstab = fstab;
        }

        public String getDevice() {
            return device;
        }

        public void setDevice(String device) {
            this.device = device;
        }
    }
}
