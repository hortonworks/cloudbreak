package com.sequenceiq.cloudbreak.common.type;

public enum ResourceEvent {
    WORKSPACE_CREATED("resource.workspace.created"),
    WORKSPACE_DELETED("resource.workspace.deleted"),
    BLUEPRINT_CREATED("resource.clusterdefinition.created"),
    BLUEPRINT_DELETED("resource.clusterdefinition.deleted"),
    CREDENTIAL_CREATED("resource.credential.created"),
    CREDENTIAL_MODIFIED("resource.credential.modified"),
    CREDENTIAL_DELETED("resource.credential.deleted"),
    CLUSTER_TEMPLATE_CREATED("resource.clustertemplate.created"),
    CLUSTER_TEMPLATE_DELETED("resource.clustertemplate.deleted"),
    LDAP_CREATED("resource.ldap.created"),
    LDAP_DELETED("resource.ldap.deleted"),
    MANAGEMENT_PACK_CREATED("resource.mpack.created"),
    MANAGEMENT_PACK_DELETED("resource.mpack.deleted"),
    KUBERNETES_CONFIG_CREATED("resource.kubernetesconfig.created"),
    KUBERNETES_CONFIG_MODIFIED("resource.kubernetesconfig.modified"),
    KUBERNETES_CONFIG_DELETED("resource.kubernetesconfig.deleted"),
    IMAGE_CATALOG_CREATED("resource.imagecatalog.created"),
    IMAGE_CATALOG_DELETED("resource.imagecatalog.deleted"),
    NETWORK_CREATED("resource.network.created"),
    NETWORK_DELETED("resource.network.deleted"),
    RECIPE_CREATED("resource.recipe.created"),
    RECIPE_DELETED("resource.recipe.deleted"),
    RDS_CONFIG_CREATED("resource.rdsconfig.created"),
    RDS_CONFIG_DELETED("resource.rdsconfig.deleted"),
    PROXY_CONFIG_CREATED("resource.proxyconfig.created"),
    PROXY_CONFIG_DELETED("resource.proxyconfig.deleted"),
    SECURITY_GROUP_CREATED("resource.securitygroup.created"),
    SECURITY_GROUP_DELETED("resource.securitygroup.deleted"),
    TEMPLATE_CREATED("resource.template.created"),
    TEMPLATE_DELETED("resource.template.deleted"),
    TOPOLOGY_CREATED("resource.topology.created"),
    FILESYSTEM_CREATED("resource.esystem.created"),
    FILESYSTEM_DELETED("resource.filesystem.deleted"),
    TOPOLOGY_DELETED("resource.topology.deleted"),
    MAINTENANCE_MODE_ENABLED("resource.maintenancemode.enabled"),
    MAINTENANCE_MODE_DISABLED("resource.maintenancemode.disabled"),
    TEST_CONNECTION_SUCCESS("resource.connection.success"),
    TEST_CONNECTION_FAILED("resource.connection.failed"),;

    private final String message;

    ResourceEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
