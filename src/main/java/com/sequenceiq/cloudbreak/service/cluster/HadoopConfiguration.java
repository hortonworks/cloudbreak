package com.sequenceiq.cloudbreak.service.cluster;

public enum HadoopConfiguration {

    YARN_SITE("yarn-site"),
    HDFS_SITE("hdfs-site");

    private final String key;

    private HadoopConfiguration(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
