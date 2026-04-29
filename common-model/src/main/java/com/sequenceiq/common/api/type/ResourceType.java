package com.sequenceiq.common.api.type;

import java.util.List;

public enum ResourceType {
    // AWS
    CLOUDFORMATION_STACK(CommonResourceType.TEMPLATE, true),
    AWS_RESERVED_IP(true),
    S3_ACCESS_ROLE_ARN(true),
    AWS_SUBNET(true),
    AWS_S3_ROLE(true),
    AWS_VPC(true),
    AWS_SNAPSHOT(true),
    AWS_ROOT_DISK_TAGGING(true),
    AWS_ENCRYPTED_VOLUME(true),
    AWS_ENCRYPTED_AMI(true),
    AWS_LAUNCHCONFIGURATION(false),
    AWS_VOLUMESET(true),
    AWS_EFS(true),
    AWS_INSTANCE(true),
    AWS_CLOUD_WATCH(true),
    RDS_INSTANCE(true),
    RDS_HOSTNAME(false),
    RDS_HOSTNAME_CANARY(CommonResourceType.CANARY, false),
    RDS_PORT(false),
    RDS_DB_SUBNET_GROUP(true),
    RDS_DB_PARAMETER_GROUP(true),
    ELASTIC_LOAD_BALANCER(true),
    ELASTIC_LOAD_BALANCER_LISTENER(true),
    ELASTIC_LOAD_BALANCER_TARGET_GROUP(true),
    AWS_SECURITY_GROUP(true),
    AWS_SSH_KEY(true),
    AWS_SECRETSMANAGER_SECRET(true),
    AWS_KMS_KEY(true),
    // AWS DISK TYPE FOR ROOT DISK
    AWS_ROOT_DISK(true),


    // GCP
    GCP_DISK(true),
    GCP_ATTACHED_DISK(true),
    GCP_ATTACHED_DISKSET(true),
    GCP_RESERVED_IP(true),
    GCP_NETWORK(false),
    GCP_SUBNET(false),
    GCP_FIREWALL_IN(false),
    GCP_FIREWALL_INTERNAL(false),
    GCP_INSTANCE(true),
    GCP_DATABASE(true),
    GCP_INSTANCE_GROUP(false),
    GCP_HEALTH_CHECK(false),
    GCP_BACKEND_SERVICE(false),
    GCP_FORWARDING_RULE(true),
    GCP_HEALTHCHECK_FIREWALL(false),

    //AZURE
    AZURE_INSTANCE(true),
    AZURE_NETWORK(true),
    AZURE_STORAGE(true),
    AZURE_SUBNET(false),
    AZURE_VOLUMESET(true),
    AZURE_DISK(true),
    AZURE_DISK_ENCRYPTION_SET(true),
    AZURE_MANAGED_IMAGE(true),
    AZURE_RESOURCE_GROUP(true),
    AZURE_DATABASE(true),
    AZURE_DATABASE_SECURITY_ALERT_POLICY(false),
    AZURE_PUBLIC_IP(true),
    AZURE_NETWORK_INTERFACE(true),
    AZURE_SECURITY_GROUP(true),
    AZURE_AVAILABILITY_SET(true),
    AZURE_PRIVATE_DNS_ZONE(true),
    AZURE_PRIVATE_ENDPOINT(true),
    AZURE_DNS_ZONE_GROUP(false),
    AZURE_DATABASE_CANARY(CommonResourceType.CANARY, false),
    AZURE_PRIVATE_ENDPOINT_CANARY(CommonResourceType.CANARY, false),
    AZURE_DNS_ZONE_GROUP_CANARY(CommonResourceType.CANARY, false),
    AZURE_VIRTUAL_NETWORK_LINK(true),
    AZURE_LOAD_BALANCER(true),
    AZURE_MANAGED_IDENTITY(true),
    AZURE_KEYVAULT_KEY(false),
    AZURE_NAT_GATEWAY(true),
    AZURE_FIREWALL(true),

    // OPENSTACK
    OPENSTACK_INSTANCE(false),
    OPENSTACK_ATTACHED_DISK(false),
    OPENSTACK_NETWORK(false),
    OPENSTACK_SUBNET(false),
    OPENSTACK_SECURITY_GROUP(false),
    OPENSTACK_PORT(false),
    OPENSTACK_FLOATING_IP(false),


    // ARM
    ARM_TEMPLATE(CommonResourceType.TEMPLATE, false),


    // YARN
    YARN_APPLICATION(CommonResourceType.TEMPLATE, false),
    YARN_LOAD_BALANCER(CommonResourceType.TEMPLATE, false),


    // MOCK
    MOCK_INSTANCE(false),
    MOCK_TEMPLATE(false),
    MOCK_DATABASE(false),
    MOCK_VOLUME(false);

    private static final List<ResourceType> INSTANCE_TYPES = List.of(GCP_INSTANCE, OPENSTACK_INSTANCE, MOCK_INSTANCE);

    private final CommonResourceType commonResourceType;

    private final boolean taggable;

    ResourceType() {
        this(CommonResourceType.RESOURCE, false);
    }

    ResourceType(boolean taggable) {
        this(CommonResourceType.RESOURCE, taggable);
    }

    ResourceType(CommonResourceType commonResourceType) {
        this(commonResourceType, false);
    }

    ResourceType(CommonResourceType commonResourceType, boolean taggable) {
        this.commonResourceType = commonResourceType;
        this.taggable = taggable;
    }

    public CommonResourceType getCommonResourceType() {
        return commonResourceType;
    }

    public boolean isTaggable() {
        return taggable;
    }

    public static boolean isInstanceResource(ResourceType resourceType) {
        return INSTANCE_TYPES.contains(resourceType);
    }

    public static boolean isCanaryResource(ResourceType resourceType) {
        return resourceType.getCommonResourceType() == CommonResourceType.CANARY;
    }
}