package com.sequenceiq.cloudbreak.cloud.openstack.common;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class OpenStackConstants {
    public static final Platform OPENSTACK_PLATFORM = Platform.platform("OPENSTACK");

    public static final String TENANT_ID = "tenantId";
    public static final String NETWORK_ID = "networkId";
    public static final String SUBNET_ID = "subnetId";
    public static final String ROUTER_ID = "routerId";
    public static final String SECURITYGROUP_ID = "securityGroupId";
    public static final String VOLUME_MOUNT_POINT = "volumeMountPoint";
    public static final String INSTANCE_ID = "instanceId";
    public static final String PORT_ID = "portId";
    public static final String SERVER = "server";
    public static final String FLOATING_IP_IDS = "floatingIpIds";
    public static final String PUBLIC_NET_ID = "publicNetId";

    private OpenStackConstants() {
    }

    public enum OpenStackVariant {
        HEAT("HEAT"),
        NATIVE("NATIVE");

        private Variant variant;

        OpenStackVariant(String variant) {
            this.variant = Variant.variant(variant);
        }

        public Variant variant() {
            return this.variant;
        }
    }
}
