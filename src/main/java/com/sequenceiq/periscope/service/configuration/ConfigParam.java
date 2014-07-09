package com.sequenceiq.periscope.service.configuration;

public final class ConfigParam {

    public static final String MR_FRAMEWORK_NAME = "mapreduce.framework.name";
    public static final String FS_DEFAULT_NAME = "fs.defaultFS";
    public static final String YARN_RM_ADDRESS = "yarn.resourcemanager.address";
    public static final String YARN_RESOURCEMANAGER_SCHEDULER_ADDRESS = "yarn.resourcemanager.scheduler.address";
    public static final String YARN_SCHEDULER_ADDRESS = "yarn.resourcemanager.scheduler.address";

    private ConfigParam() {
        throw new IllegalStateException();
    }

}
