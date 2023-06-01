package com.sequenceiq.cloudbreak.common.orchestration;

public class NodeVolumes {
    private final int dataBaseVolumeIndex;

    private final String dataVolumes;

    private final String dataVolumesWithDataLoss;

    private final String serialIds;

    private final String serialIdsWithDataLoss;

    private final String fstab;

    private final String uuids;

    public NodeVolumes(int databaseVolumeIndex, String dataVolumes, String dataVolumesWithDataLoss, String serialIds, String serialIdsWithDataLoss,
        String fstab, String uuids) {
        this.dataBaseVolumeIndex = databaseVolumeIndex;
        this.dataVolumes = dataVolumes;
        this.dataVolumesWithDataLoss = dataVolumesWithDataLoss;
        this.serialIds = serialIds;
        this.serialIdsWithDataLoss = serialIdsWithDataLoss;
        this.fstab = fstab;
        this.uuids = uuids;
    }

    public int getDatabaseVolumeIndex() {
        return dataBaseVolumeIndex;
    }

    public String getDataVolumes() {
        return dataVolumes;
    }

    public String getSerialIds() {
        return serialIds;
    }

    public String getFstab() {
        return fstab;
    }

    public String getUuids() {
        return uuids;
    }

    public String getDataVolumesWithDataLoss() {
        return dataVolumesWithDataLoss;
    }

    public String getSerialIdsWithDataLoss() {
        return serialIdsWithDataLoss;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NodeVolumes{");
        sb.append("dataBaseVolumeIndex=").append(dataBaseVolumeIndex);
        sb.append(", dataVolumes='").append(dataVolumes).append('\'');
        sb.append(", dataVolumesWithDataLoss='").append(dataVolumesWithDataLoss).append('\'');
        sb.append(", serialIds='").append(serialIds).append('\'');
        sb.append(", serialIdsWithDataLoss='").append(serialIdsWithDataLoss).append('\'');
        sb.append(", fstab='").append(fstab).append('\'');
        sb.append(", uuids='").append(uuids).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
