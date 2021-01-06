package com.sequenceiq.cloudbreak.cm.config;

public enum MgmtServices {
    EVENTSERVER("cloudera-scm-eventserver", "eventserver_index_dir"),
    HOSTMONITOR("cloudera-host-monitor", "firehose_storage_dir"),
    REPORTSMANAGER("cloudera-scm-headlamp", "headlamp_scratch_dir"),
    SERVICEMONITOR("cloudera-service-monitor", "firehose_storage_dir");

    private String directory;

    private String configName;

    MgmtServices(String directory, String configName) {
        this.directory = directory;
        this.configName = configName;
    }

    public String getDirectory() {
        return directory;
    }

    public String getConfigName() {
        return configName;
    }
}
