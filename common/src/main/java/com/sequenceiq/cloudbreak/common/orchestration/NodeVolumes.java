package com.sequenceiq.cloudbreak.common.orchestration;

public class NodeVolumes {
    private final int dataBaseVolumeIndex;

    private final String dataVolumes;

    private final String serialIds;

    private final String fstab;

    private final String uuids;

    public NodeVolumes(int databaseVolumeIndex, String dataVolumes, String serialIds, String fstab, String uuids) {
        this.dataBaseVolumeIndex = databaseVolumeIndex;
        this.dataVolumes = dataVolumes;
        this.serialIds = serialIds;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NodeVolumes{");
        sb.append("dataBaseVolumeIndex=").append(dataBaseVolumeIndex);
        sb.append(", dataVolumes='").append(dataVolumes).append('\'');
        sb.append(", serialIds='").append(serialIds).append('\'');
        sb.append(", fstab='").append(fstab).append('\'');
        sb.append(", uuids='").append(uuids).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
