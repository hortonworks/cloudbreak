package com.sequenceiq.cloudbreak.common.type;

public enum ResourceEvent {
    BLUEPRINT_CREATED("resource.blueprint.created"),
    BLUEPRINT_DELETED("resource.blueprint.deleted"),
    CREDENTIAL_CREATED("resource.credential.created"),
    CREDENTIAL_DELETED("resource.credential.deleted"),
    CLUSTER_TEMPLATE_CREATED("resource.clustertemplate.created"),
    CLUSTER_TEMPLATE_DELETED("resource.clustertemplate.deleted"),
    LDAP_CREATED("resource.ldap.created"),
    LDAP_DELETED("resource.ldap.deleted"),
    NETWORK_CREATED("resource.network.created"),
    NETWORK_DELETED("resource.network.deleted"),
    RECIPE_CREATED("resource.recipe.created"),
    RECIPE_DELETED("resource.recipe.deleted"),
    RDS_CONFIG_CREATED("resource.rdsconfig.created"),
    RDS_CONFIG_DELETED("resource.rdsconfig.deleted"),
    SECURITY_GROUP_CREATED("resource.securitygroup.created"),
    SECURITY_GROUP_DELETED("resource.securitygroup.deleted"),
    TEMPLATE_CREATED("resource.template.created"),
    TEMPLATE_DELETED("resource.template.deleted"),
    TOPOLOGY_CREATED("resource.topology.created"),
    TOPOLOGY_DELETED("resource.topology.deleted");

    private final String message;

    ResourceEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
