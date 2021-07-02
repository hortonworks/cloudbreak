package com.sequenceiq.common.api.type;

import java.util.List;

public enum ResourceType {
    // AWS
    CLOUDFORMATION_STACK(CommonResourceType.TEMPLATE),
    AWS_RESERVED_IP,
    S3_ACCESS_ROLE_ARN,
    AWS_SUBNET,
    AWS_S3_ROLE,
    AWS_VPC,
    AWS_SNAPSHOT,
    AWS_ENCRYPTED_VOLUME,
    AWS_ENCRYPTED_AMI,
    AWS_LAUNCHCONFIGURATION,
    AWS_VOLUMESET,
    AWS_EFS,
    AWS_INSTANCE,
    RDS_INSTANCE,
    RDS_HOSTNAME,
    RDS_PORT,
    RDS_DB_SUBNET_GROUP,
    RDS_DB_PARAMETER_GROUP,
    ELASTIC_LOAD_BALANCER,
    ELASTIC_LOAD_BALANCER_LISTENER,
    ELASTIC_LOAD_BALANCER_TARGET_GROUP,
    AWS_SECURITY_GROUP,

    // OPENSTACK
    HEAT_STACK(CommonResourceType.TEMPLATE),
    OPENSTACK_ATTACHED_DISK,
    OPENSTACK_NETWORK,
    OPENSTACK_SUBNET,
    OPENSTACK_ROUTER,
    OPENSTACK_SECURITY_GROUP,
    OPENSTACK_PORT,
    OPENSTACK_INSTANCE,
    OPENSTACK_FLOATING_IP,

    // GCP
    GCP_DISK,
    GCP_ATTACHED_DISK,
    GCP_ATTACHED_DISKSET,
    GCP_RESERVED_IP,
    GCP_NETWORK,
    GCP_SUBNET,
    GCP_FIREWALL_IN,
    GCP_FIREWALL_INTERNAL,
    GCP_INSTANCE,
    GCP_DATABASE,

    //AZURE
    AZURE_INSTANCE,
    AZURE_NETWORK,
    AZURE_STORAGE,
    AZURE_SUBNET,
    AZURE_VOLUMESET,
    AZURE_DISK,
    AZURE_DISK_ENCRYPTION_SET,
    AZURE_MANAGED_IMAGE,
    AZURE_RESOURCE_GROUP,
    AZURE_DATABASE,
    AZURE_DATABASE_SECURITY_ALERT_POLICY,
    AZURE_PUBLIC_IP,
    AZURE_NETWORK_INTERFACE,
    AZURE_SECURITY_GROUP,
    AZURE_AVAILABILITY_SET,
    AZURE_PRIVATE_ENDPOINT,
    AZURE_PRIVATE_DNS_ZONE,
    AZURE_DNS_ZONE_GROUP,
    AZURE_VIRTUAL_NETWORK_LINK,
    AZURE_LOAD_BALANCER,

    // ARM
    ARM_TEMPLATE(CommonResourceType.TEMPLATE),

    // YARN
    YARN_APPLICATION(CommonResourceType.TEMPLATE),
    YARN_LOAD_BALANCER(CommonResourceType.TEMPLATE),

    // MOCK
    MOCK_INSTANCE,
    MOCK_TEMPLATE,
    MOCK_VOLUME;

    private static final List<ResourceType> INSTANCE_TYPES = List.of(GCP_INSTANCE, OPENSTACK_INSTANCE, MOCK_INSTANCE);

    private final CommonResourceType commonResourceType;

    ResourceType() {
        this(CommonResourceType.RESOURCE);
    }

    ResourceType(CommonResourceType commonResourceType) {
        this.commonResourceType = commonResourceType;
    }

    public CommonResourceType getCommonResourceType() {
        return commonResourceType;
    }

    public static boolean isInstanceResource(ResourceType resourceType) {
        return INSTANCE_TYPES.contains(resourceType);
    }
}
