package com.sequenceiq.cloudbreak.job.disk.model;

import java.util.List;

public class InstanceResourceDto {

    private String instanceId;

    private String fstab;

    private List<VolumeDto> volumes;

    public InstanceResourceDto() {
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getFstab() {
        return fstab;
    }

    public void setFstab(String fstab) {
        this.fstab = fstab;
    }

    public List<VolumeDto> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeDto> volumes) {
        this.volumes = volumes;
    }

    public record VolumeDto(
            String volumeId,
            String deviceName,
            String mountPoint,
            int size,
            String volumeType,
            String uuid,
            String serial,
            String hctl,
            String fsType
    ) {
        @Override
        public String toString() {
            return "Volume{" +
                    "id='" + volumeId + '\'' +
                    ", device='" + deviceName + '\'' +
                    ", size=" + size +
                    ", mountPoint='" + mountPoint + '\'' +
                    ", fsType='" + fsType + '\'' +
                    '}';
        }
    }
}
