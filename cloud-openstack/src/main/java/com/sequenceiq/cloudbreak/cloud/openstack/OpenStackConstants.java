package com.sequenceiq.cloudbreak.cloud.openstack;

public class OpenStackConstants {
    public static final String OPENSTACK = "OPENSTACK";

    public static final String TENANT_ID = "tenantId";
    public static final String NETWORK_ID = "networkId";
    public static final String SUBNET_ID = "subnetId";
    public static final String ROUTER_ID = "routerId";
    public static final String SECURITYGROUP_ID = "securityGroupId";
    public static final String VOLUME_ID = "volumeId";
    public static final String VOLUME_MOUNT_POINT = "volumeMountPoint";
    public static final String INSTANCE_ID = "instanceId";
    public static final String PORT_ID = "portId";
    public static final String SERVER = "server";
    public static final String FLOATING_IP_IDS = "floatingIpIds";

    private OpenStackConstants() {
    }

    public enum Variant {
        HEAT,
        NATIVE
    }
}
