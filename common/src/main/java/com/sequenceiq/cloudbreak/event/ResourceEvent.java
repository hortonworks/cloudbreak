package com.sequenceiq.cloudbreak.event;

public enum ResourceEvent {
    SDX_CLUSTER_CREATED("resource.sdx.created"),
    SDX_CLUSTER_PROVISION_STARTED("resource.sdx.provisionstarted"),
    SDX_CLUSTER_PROVISION_FINISHED("resource.sdx.provisionfinished"),
    SDX_CLUSTER_DELETED("resource.sdx.deleted"),
    SDX_CLUSTER_DELETION_STARTED("resource.sdx.deletionstarted"),
    SDX_CLUSTER_DELETION_FINISHED("resource.sdx.deletionfinished"),
    SDX_CLUSTER_DELETION_FAILED("resource.sdx.deletionfailed"),
    SDX_CLUSTER_CREATION_FAILED("resource.sdx.failed"),
    SDX_RDS_DELETION_STARTED("resource.sdx.rdsdeletionstarted"),
    SDX_RDS_DELETION_FAILED("resource.sdx.rdsdeletionfailed"),
    SDX_RDS_DELETION_FINISHED("resource.sdx.rdsdeletionfinished"),
    SDX_RDS_CREATION_STARTED("resource.sdx.rdscreationstarted"),
    SDX_RDS_CREATION_FAILED("resource.sdx.rdscreationfailed"),
    SDX_RDS_CREATION_FINISHED("resource.sdx.rdscreationfinished"),
    SDX_WAITING_FOR_ENVIRONMENT("resource.sdx.envwait"),
    SDX_ENVIRONMENT_FINISHED("resource.sdx.envfinished"),
    SDX_REPAIR_STARTED("resource.sdx.repair.startred"),
    SDX_REPAIR_FINISHED("resource.sdx.repair.finished"),
    SDX_REPAIR_FAILED("resource.sdx.repair.failed"),
    WORKSPACE_CREATED("resource.workspace.created"),
    WORKSPACE_DELETED("resource.workspace.deleted"),
    BLUEPRINT_CREATED("resource.blueprint.created"),
    BLUEPRINT_DELETED("resource.blueprint.deleted"),
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
    TEST_CONNECTION_FAILED("resource.connection.failed"),

    ENVIRONMENT_NETWORK_CREATION_STARTED("environment.network.creation.started"),
    ENVIRONMENT_NETWORK_CREATION_FAILED("environment.network.creation.failed"),
    ENVIRONMENT_FREEIPA_CREATION_STARTED("environment.freeipa.creation.started"),
    ENVIRONMENT_FREEIPA_CREATION_FAILED("environment.freeipa.creation.failed"),
    ENVIRONMENT_CREATION_FINISHED("environment.creation.finished"),
    ENVIRONMENT_CREATION_FAILED("environment.creation.failed"),

    ENVIRONMENT_NETWORK_DELETION_STARTED("environment.network.deletion.started"),
    ENVIRONMENT_CLUSTER_DEFINITION_DELETE_STARTED("environment.clusterdefinition.deletion.started"),
    ENVIRONMENT_DATABASE_DELETION_STARTED("environment.database.deletion.started"),
    ENVIRONMENT_FREEIPA_DELETION_STARTED("environment.freeipa.deletion.started"),
    ENVIRONMENT_IDBROKER_MAPPINGS_DELETION_STARTED("environment.idbroker.mappings.deletion.started"),
    ENVIRONMENT_DELETION_FINISHED("environment.deletion.finished"),
    ENVIRONMENT_DELETION_FAILED("environment.deletion.failed"),

    CREDENTIAL_AZURE_INTERACTIVE_CREATED("credential.azure.interactive.created"),
    CREDENTIAL_AZURE_INTERACTIVE_STATUS("credential.azure.interactive.status"),
    CREDENTIAL_AZURE_INTERACTIVE_FAILED("credential.azure.interactive.failed");

    private final String message;

    ResourceEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
