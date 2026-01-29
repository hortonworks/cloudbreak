package com.sequenceiq.cloudbreak.constant;

import java.util.regex.Pattern;

public class AzureConstants {
    public static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    public static final String NETWORK_ID = "networkId";

    public static final String NO_PUBLIC_IP = "noPublicIp";

    public static final String DATABASE_PRIVATE_DNS_ZONE_ID = "databasePrivateDsZoneId";

    public static final String AKS_PRIVATE_DNS_ZONE_ID = "aksPrivateDnsZoneId";

    public static final String NO_OUTBOUND_LOAD_BALANCER = "noOutboundLoadBalancer";

    public static final String USE_PUBLIC_DNS_FOR_PRIVATE_AKS = "usePublicDnsForPrivateAks";

    public static final String LUN_DEVICE_PATH_PREFIX = "/dev/disk/azure/scsi[1-9]/lun";

    public static final Pattern LUN_DEVICE_REGEX_PATTERN = Pattern.compile("/dev/disk/azure/scsi\\[1-9]/lun(\\d+)$");

    public static final String LUN_DEVICE_NAME_TEMPLATE = LUN_DEVICE_PATH_PREFIX + "%d";

    private AzureConstants() {
    }
}
