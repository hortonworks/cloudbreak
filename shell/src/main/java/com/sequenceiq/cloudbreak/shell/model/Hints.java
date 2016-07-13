package com.sequenceiq.cloudbreak.shell.model;

/**
 * Provides some guidance's to the user, what he/she can follow.
 */
public enum Hints {

    CREATE_CREDENTIAL("Create a new credential with the 'credential create' command"),
    CREATE_CREDENTIAL_WITH_TOPOLOGY("Create a new credential with the 'credential create' command and set the id of the newly created platform"),
    SELECT_CREDENTIAL("Create a new credential with the 'credential create' command or select an existing one with 'credential select'"),
    ADD_BLUEPRINT("Add a blueprint with the 'blueprint add' command"),
    SELECT_BLUEPRINT("Add a blueprint with the 'blueprint add' command or select an existing one with 'blueprint select'"),
    SELECT_NETWORK("Use 'network list' to list available networks, a network could be selected by 'network select' or created by 'network create' command"),
    CREATE_STACK("Create a stack based on a template with the 'stack create' command"),
    SELECT_STACK("Create a stack based on a template with the 'stack create' command or select an existing one with 'stack select'"),
    CREATE_CLUSTER("Create a cluster with the 'cluster create' command or configure the recipes with 'hostgroup configure' command"),
    CONFIGURE_INSTANCEGROUP("Configure instancegroups with the 'instancegroup configure' command"),
    CONFIGURE_HOSTGROUP("Configure hostgroups with the 'hostgroup configure' command"),
    NONE("No more hints for you.. :("),

    MARATHON_STACK("Create a marathon stack with 'marathon create' command select one of them with 'marathon select'"),
    CONFIGURE_MARATHON_HOSTGROUP("Configure hostgroups with 'marathon hostgroup configure'."),
    MARATHON_CLUSTER("Create marathon cluster with 'cluster create' command");


    private final String message;

    Hints(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
