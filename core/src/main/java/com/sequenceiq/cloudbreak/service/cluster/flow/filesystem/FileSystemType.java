package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem;

import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.dash.DashFileSystemConfiguration;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.gcs.GcsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.wasbintegrated.WasbIntegratedFileSystemConfiguration;

public enum FileSystemType {
    DASH(DashFileSystemConfiguration.class),
    WASB_INTEGRATED(WasbIntegratedFileSystemConfiguration.class),
    GCS(GcsFileSystemConfiguration.class);

    private Class clazz;

    FileSystemType(Class clazz) {
        this.clazz = clazz;
    }

    public Class getClazz() {
        return clazz;
    }
}
