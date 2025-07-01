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
    AWS_ROOT_DISK_TAGGING,
    AWS_ENCRYPTED_VOLUME,
    AWS_ENCRYPTED_AMI,
    AWS_LAUNCHCONFIGURATION,
    AWS_VOLUMESET,
    AWS_EFS,
    AWS_INSTANCE,
    AWS_CLOUD_WATCH,
    RDS_INSTANCE,
    RDS_HOSTNAME,
    RDS_HOSTNAME_CANARY(CommonResourceType.CANARY),
    RDS_PORT,
    RDS_DB_SUBNET_GROUP,
    RDS_DB_PARAMETER_GROUP,
    ELASTIC_LOAD_BALANCER,
    ELASTIC_LOAD_BALANCER_LISTENER,
    ELASTIC_LOAD_BALANCER_TARGET_GROUP,
    AWS_SECURITY_GROUP,
    AWS_SSH_KEY,
    AWS_SECRETSMANAGER_SECRET,
    AWS_KMS_KEY,
    // AWS DISK TYPE FOR ROOT DISK
    AWS_ROOT_DISK,

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
    GCP_INSTANCE_GROUP,
    GCP_HEALTH_CHECK,
    GCP_BACKEND_SERVICE,
    GCP_FORWARDING_RULE,
    GCP_HEALTHCHECK_FIREWALL,

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
    AZURE_PRIVATE_DNS_ZONE,
    AZURE_PRIVATE_ENDPOINT,
    AZURE_DNS_ZONE_GROUP,
    AZURE_DATABASE_CANARY(CommonResourceType.CANARY),
    AZURE_PRIVATE_ENDPOINT_CANARY(CommonResourceType.CANARY),
    AZURE_DNS_ZONE_GROUP_CANARY(CommonResourceType.CANARY),
    AZURE_VIRTUAL_NETWORK_LINK,
    AZURE_LOAD_BALANCER,
    AZURE_MANAGED_IDENTITY,
    AZURE_KEYVAULT_KEY,
    AZURE_NAT_GATEWAY,
    AZURE_FIREWALL,

    // ARM
    ARM_TEMPLATE(CommonResourceType.TEMPLATE),

    // YARN
    YARN_APPLICATION(CommonResourceType.TEMPLATE),
    YARN_LOAD_BALANCER(CommonResourceType.TEMPLATE),

    // MOCK
    MOCK_INSTANCE,
    MOCK_TEMPLATE,
    MOCK_DATABASE,
    MOCK_VOLUME;

    private static final List<ResourceType> INSTANCE_TYPES = List.of(GCP_INSTANCE, MOCK_INSTANCE);

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

    public static boolean isCanaryResource(ResourceType resourceType) {
        return resourceType.getCommonResourceType() == CommonResourceType.CANARY;
    }
}