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

    private Integer volumeSize;

    private String volumeType;

    private String discoveryFQDN;

    @JsonCreator
    public VolumeSetAttributes(@JsonProperty("availabilityZone") String availabilityZone, @JsonProperty("deleteOnTermination") Boolean deleteOnTermination,
            @JsonProperty("fstab") String fstab, @JsonProperty("uuids") String uuids, @JsonProperty("volumes") List<Volume> volumes,
            @JsonProperty("volumeSize") Integer volumeSize, @JsonProperty("volumeType") String volumeType,
            @JsonProperty("discoveryFQDN") String discoveryFQDN) {
        this.availabilityZone = availabilityZone;
        this.deleteOnTermination = deleteOnTermination;
        this.fstab = fstab;
        this.uuids = uuids;
        this.volumes = volumes;
        this.volumeSize = volumeSize;
        this.volumeType = volumeType;
        this.discoveryFQDN = discoveryFQDN;
    }

    public VolumeSetAttributes(String availabilityZone, Boolean deleteOnTermination, String fstab, List<Volume> volumes,
            Integer volumeSize, String volumeType) {
        this.availabilityZone = availabilityZone;
        this.deleteOnTermination = deleteOnTermination;
        this.fstab = fstab;
        this.volumes = volumes;
        this.volumeSize = volumeSize;
        this.volumeType = volumeType;
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

    public void setDiscoveryFQDN(String discoveryFQDN) {
        this.discoveryFQDN = discoveryFQDN;
    }

    public String getDiscoveryFQDN() {
        return discoveryFQDN;
    }

    @Override
    public String toString() {
        return "VolumeSetAttributes{" +
                "availabilityZone='" + availabilityZone + '\'' +
                ", deleteOnTermination=" + deleteOnTermination +
                ", fstab='" + fstab + '\'' +
                ", volumes=" + volumes +
                ", uuids='" + uuids + '\'' +
                ", volumeSize=" + volumeSize +
                ", volumeType='" + volumeType + '\'' +
                ", discoveryFQDN='" + discoveryFQDN + '\'' +
                '}';
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Volume {

        private String id;

        private String device;

        private Integer size;

        private String type;

        private CloudVolumeUsageType cloudVolumeUsageType;

        private CloudVolumeStatus cloudVolumeStatus;

        public Volume(@JsonProperty("id") String id,
                @JsonProperty("device") String device,
                @JsonProperty("size") Integer size,
                @JsonProperty("type") String type,
                @JsonProperty("usageType") CloudVolumeUsageType cloudVolumeUsageType) {
            this.id = id;
            this.device = device;
            this.type = type;
            this.size = size;
            this.cloudVolumeUsageType = cloudVolumeUsageType;
            this.cloudVolumeStatus = null;
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

        public CloudVolumeUsageType getCloudVolumeUsageType() {
            return cloudVolumeUsageType == null ? CloudVolumeUsageType.GENERAL : cloudVolumeUsageType;
        }

        public void setCloudVolumeUsageType(CloudVolumeUsageType cloudVolumeUsageType) {
            this.cloudVolumeUsageType = cloudVolumeUsageType;
        }

        public CloudVolumeStatus getCloudVolumeStatus() {
            return cloudVolumeStatus;
        }

        public void setCloudVolumeStatus(CloudVolumeStatus cloudVolumeStatus) {
            this.cloudVolumeStatus = cloudVolumeStatus;
        }

        @Override
        public String toString() {
            return "Volume{" +
                    "id='" + id + '\'' +
                    ", device='" + device + '\'' +
                    ", size=" + size +
                    ", type='" + type + '\'' +
                    ", cloudVolumeUsageType=" + cloudVolumeUsageType +
                    '}';
        }

    }

    public static class Builder {

        private String availabilityZone;

        private Boolean deleteOnTermination;

        private String fstab;

        private String uuids;

        private List<Volume> volumes;

        private Integer volumeSize;

        private String volumeType;

        private String discoveryFQDN;

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

        public Builder withUuids(String uuids) {
            this.uuids = uuids;
            return this;
        }

        public Builder withVolumes(List<Volume> volumes) {
            this.volumes = volumes;
            return this;
        }

        public Builder withVolumeSize(Integer volumeSize) {
            this.volumeSize = volumeSize;
            return this;
        }

        public Builder withVolumeType(String volumeType) {
            this.volumeType = volumeType;
            return this;
        }

        public Builder withDiscoveryFQDN(String discoveryFQDN) {
            this.discoveryFQDN = discoveryFQDN;
            return this;
        }

        public VolumeSetAttributes build() {
            return new VolumeSetAttributes(availabilityZone, deleteOnTermination, fstab, uuids, volumes, volumeSize, volumeType, discoveryFQDN);
        }
    }
}
