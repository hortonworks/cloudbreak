package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VolumeSetAttributes {

    private String availabilityZone;

    private Boolean deleteOnTermination;

    private String fstab;

    private List<Volume> volumes;

    private String uuids;

    @JsonCreator
    public VolumeSetAttributes(@JsonProperty("availabilityZone") String availabilityZone, @JsonProperty("deleteOnTermination") Boolean deleteOnTermination,
            @JsonProperty("fstab") String fstab, @JsonProperty("volumes") List<Volume> volumes) {
        this.availabilityZone = availabilityZone;
        this.deleteOnTermination = deleteOnTermination;
        this.fstab = fstab;
        this.volumes = volumes;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
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

    public List<Volume> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<Volume> volumes) {
        this.volumes = volumes;
    }

    public void setUuids(String uuids) {
        this.uuids = uuids;
    }

    public String getUuids() {
        return uuids;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Volume {

        private String id;

        private String device;

        private Integer size;

        private String type;

        public Volume(@JsonProperty("id") String id,
                @JsonProperty("device") String device,
                @JsonProperty("size") Integer size,
                @JsonProperty("type") String type) {
            this.id = id;
            this.device = device;
            this.type = type;
            this.size = size;

        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setDevice(String device) {
            this.device = device;
        }

        public String getDevice() {
            return device;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class Builder {

        private String availabilityZone;

        private Boolean deleteOnTermination;

        private String fstab;

        private List<Volume> volumes;

        public Builder withAvailabilityZone(String availabilityZone) {
            this.availabilityZone = availabilityZone;
            return this;
        }

        public Builder withDeleteOnTermination(Boolean deleteOnTermination) {
            this.deleteOnTermination = deleteOnTermination;
            return this;
        }

        public Builder withFstab(String fstab) {
            this.fstab = fstab;
            return this;
        }

        public Builder withVolumes(List<Volume> volumes) {
            this.volumes = volumes;
            return this;
        }

        public VolumeSetAttributes build() {
            return new VolumeSetAttributes(availabilityZone, deleteOnTermination, fstab, volumes);
        }
    }
}
