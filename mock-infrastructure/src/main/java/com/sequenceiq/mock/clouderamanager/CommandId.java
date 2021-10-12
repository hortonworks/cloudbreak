package com.sequenceiq.mock.clouderamanager;

import java.math.BigDecimal;

public class CommandId {
    public static final BigDecimal CLUSTER_START = new BigDecimal(1);

    public static final BigDecimal CLUSTER_STOP = new BigDecimal(2);

    public static final BigDecimal CLUSTER_REFRESH = new BigDecimal(3);

    public static final BigDecimal CLUSTER_RESTART = new BigDecimal(4);

    public static final BigDecimal MGMT_START = new BigDecimal(5);

    public static final BigDecimal MGMT_STOP = new BigDecimal(6);

    public static final BigDecimal MGMT_RESTART = new BigDecimal(7);

    public static final BigDecimal DELETE_CRED = new BigDecimal(8);

    public static final BigDecimal HOST_DECOMMISSION = new BigDecimal(9);

    public static final BigDecimal REFRESH_PARCEL = new BigDecimal(10);

    public static final BigDecimal DEPLOY_CLIENT_CONFIG = new BigDecimal(11);

    public static final BigDecimal START_PARCEL_DOWNLOAD = new BigDecimal(12);

    public static final BigDecimal START_DISTRIBUTION_PARCEL = new BigDecimal(13);

    public static final BigDecimal ACTIVATE_PARCEL = new BigDecimal(14);

    public static final BigDecimal UPGRADE_CDH_COMMAND = new BigDecimal(15);

    private CommandId() {
    }
}