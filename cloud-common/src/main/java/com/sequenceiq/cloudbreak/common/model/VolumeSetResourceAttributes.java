package com.sequenceiq.cloudbreak.common.model;

import java.util.List;

public class VolumeSetResourceAttributes {

    private List<Volume> volumes;

    private String availabilityZone;

    private String volumeSize;

    private String volumeType;

    private Boolean deleteOnTermination = Boolean.FALSE;

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

    public String getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(String volumeSize) {
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

    public static class Volume {

        private String id;

        private String mountPath;

        private String uuid;

        private String fstab;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMountPath() {
            return mountPath;
        }

        public void setMountPath(String mountPath) {
            this.mountPath = mountPath;
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
    }
}
