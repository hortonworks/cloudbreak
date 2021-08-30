package com.sequenceiq.mock.clouderamanager;

public class CommandId {
    public static final Integer CLUSTER_START = Integer.valueOf(1);

    public static final Integer CLUSTER_STOP = Integer.valueOf(2);

    public static final Integer CLUSTER_REFRESH = Integer.valueOf(3);

    public static final Integer CLUSTER_RESTART = Integer.valueOf(4);

    public static final Integer MGMT_START = Integer.valueOf(5);

    public static final Integer MGMT_STOP = Integer.valueOf(6);

    public static final Integer MGMT_RESTART = Integer.valueOf(7);

    public static final Integer DELETE_CRED = Integer.valueOf(8);

    public static final Integer HOST_DECOMMISSION = Integer.valueOf(9);

    public static final Integer REFRESH_PARCEL = Integer.valueOf(10);

    public static final Integer DEPLOY_CLIENT_CONFIG = Integer.valueOf(11);

    public static final Integer START_PARCEL_DOWNLOAD = Integer.valueOf(12);

    public static final Integer START_DISTRIBUTION_PARCEL = Integer.valueOf(13);

    public static final Integer ACTIVATE_PARCEL = Integer.valueOf(14);

    public static final Integer DEACTIVATE_PARCEL = Integer.valueOf(15);

    public static final Integer UNDISTRIBUTE_PARCEL = Integer.valueOf(16);

    public static final Integer REMOVE_PARCEL = Integer.valueOf(17);

    public static final Integer UPGRADE_CDH_COMMAND = Integer.valueOf(18);

    public static final Integer REMOVE_HOSTS = Integer.valueOf(19);

    private CommandId() {
    }
}