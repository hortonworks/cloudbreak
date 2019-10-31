package com.sequenceiq.environment.api.doc.environment;

public class EnvironmentOpDescription {
    public static final String CREATE = "Create an environment.";
    public static final String GET_BY_NAME = "Get an environment by name.";
    public static final String GET_BY_CRN = "Get an environment by CRN.";
    public static final String ATTACH_RESOURCES = "Attach resources to an environment.";
    public static final String DETACH_RESOURCES = "Detach resources from an environment.";
    public static final String LIST = "List all environments.";
    public static final String DELETE_BY_NAME = "Delete an environment by name. Only possible if no cluster is running in the environment.";
    public static final String DELETE_BY_CRN = "Delete an environment by CRN. Only possible if no cluster is running in the environment.";
    public static final String DELETE_MULTIPLE_BY_NAME = "Delete multiple environment by namns. Only possible if no cluster is running in the environments.";
    public static final String DELETE_MULTIPLE_BY_CRN = "Delete multiple environment by CRNs. Only possible if no cluster is running in the environments.";
    public static final String CHANGE_CREDENTIAL_BY_NAME = "Changes the credential of the environment and the clusters in the environment of a given name.";
    public static final String CHANGE_CREDENTIAL_BY_CRN = "Changes the credential of the environment and the clusters in the environment of a given CRN.";
    public static final String CHANGE_NETWORK = "Changes the network of the environment.";
    public static final String REGISTER_EXTERNAL_DATALAKE = "Register external datalake.";
    public static final String EDIT_BY_NAME = "Edit an environment by name. Location, regions and description can be changed.";
    public static final String EDIT_BY_CRN = "Edit an environment by CRN. Location, regions and description can be changed.";
    public static final String DATALAKE_PREREQUISITES = "Returns datalake prerequisites.";
    public static final String START_BY_NAME = "Start an environment by name. The freeipa, datalake and datahubs will be started in this order";
    public static final String START_BY_CRN = "Start an environment by CRN. The freeipa, datalake and datahubs will be started in this order";
    public static final String STOP_BY_NAME = "Stop an environment by name. The datahubs, datalake and freeipa will be stopped in this order";
    public static final String STOP_BY_CRN = "Stop an environment by CRN. The datahubs, datalake and freeipa will be stopped in this order";
    public static final String CLI_COMMAND = "produce cli command input for environment creation";

    private EnvironmentOpDescription() {
    }
}
