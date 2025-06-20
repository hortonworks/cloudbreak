package com.sequenceiq.environment.api.doc.environment;

public class EnvironmentOpDescription {
    public static final String CREATE = "Create an environment.";
    public static final String GET_BY_NAME = "Get an environment by name.";
    public static final String GET_BY_CRN = "Get an environment by CRN.";
    public static final String GET_XP_BY_NAME = "Get the experience(s) that connects to an environment that has the following name.";
    public static final String GET_XP_BY_CRN = "Get the experience(s) that connects to an environment that has the following CRN.";
    public static final String LIST = "List all environments.";
    public static final String INTERNAL_LIST = "List all environments by account ID using the internal actor.";
    public static final String INTERNAL_CONSUMPTION_LIST = "List all environments for consumption.";
    public static final String UPGRADE_CCM_AVAILABLE =
            "Determine if Upgrade CCM is available in the environment, i.e. is there any component which is not upgraded.";
    public static final String UPGRADE_DEFAULT_OUTBOUND_AVAILABLE =
            "Validates if 'upgrade default outbound' operation is available for the environment, i.e. is there any component " +
                    "for which outbound is not upgraded.";
    public static final String DELETE_BY_NAME = "Delete an environment by name. Only possible if no cluster is running in the environment.";
    public static final String DELETE_BY_CRN = "Delete an environment by CRN. Only possible if no cluster is running in the environment.";
    public static final String DELETE_MULTIPLE_BY_NAME = "Delete multiple environment by names. Only possible if no cluster is running in the environments.";
    public static final String DELETE_MULTIPLE_BY_CRN = "Delete multiple environment by CRNs. Only possible if no cluster is running in the environments.";
    public static final String CHANGE_CREDENTIAL_BY_NAME = "Changes the credential of the environment and the clusters in the environment of a given name.";
    public static final String CHANGE_CREDENTIAL_BY_CRN = "Changes the credential of the environment and the clusters in the environment of a given CRN.";
    public static final String CHANGE_TELEMETRY_FEATURES_BY_NAME = "Changes telemetry features(s) of the environment in the environment of a given name.";
    public static final String CHANGE_TELEMETRY_FEATURES_BY_CRN = "Changes telemetry features(s) of the environment in the environment of a given CRN.";
    public static final String POLICY_VALIDATION_BY_CRN = "Validate policy json by environment crn.";
    public static final String EDIT_BY_NAME = "Edit an environment by name. Location, regions and description can be changed.";
    public static final String EDIT_BY_CRN = "Edit an environment by CRN. Location, regions and description can be changed.";
    public static final String START_BY_NAME = "Start an environment by name. The freeipa, datalake and datahubs will be started in this order";
    public static final String START_BY_CRN = "Start an environment by CRN. The freeipa, datalake and datahubs will be started in this order";
    public static final String STOP_BY_NAME = "Stop an environment by name. The datahubs, datalake and freeipa will be stopped in this order";
    public static final String STOP_BY_CRN = "Stop an environment by CRN. The datahubs, datalake and freeipa will be stopped in this order";
    public static final String VERIFY_CREDENTIAL_BY_CRN = "Verifies the credential used by the given environment.";
    public static final String CLI_COMMAND = "produce cli command input for environment creation";
    public static final String GET_CRN_BY_NAME = "Get the crn of an environment by name.";
    public static final String GET_NAME_BY_CRN = "Get the name of an environment by crn.";
    public static final String UPDATE_CONFIG_BY_CRN = "Update the configuration for all stacks in the Environment by the Environment CRN";
    public static final String UPDATE_LOAD_BALANCERS_BY_ENV_NAME = "Updates all cluster in an environment with load balancers, including adding the " +
        "Endpoint Access Gateway if it's enabled. Environment is specified by name.";
    public static final String UPDATE_LOAD_BALANCERS_BY_ENV_CRN = "Updates all cluster in an environment with load balancers, including adding the " +
        "Endpoint Access Gateway if it's enabled. Environment is specified by CRN.";
    public static final String CREATE_DEFAULT_EXTERNALIZED_COMPUTE_CLUSTER = "Creates default externalized compute cluster for the environment";
    public static final String REINITIALIZE_DEFAULT_EXTERNALIZED_COMPUTE_CLUSTER = "Reinitialize default externalized compute cluster for the environment";
    public static final String GET_LAST_FLOW_PROGRESS = "Get last flow operation progress details for resource by resource crn";
    public static final String LIST_FLOW_PROGRESS = "List recent flow operations progress details for resource by resource crn";
    public static final String GET_OPERATION = "Get flow operation progress details for resource by resource crn";
    public static final String GET_OPERATION_STATUS = "Get flow operation status for resource by resource crn and operation id";
    public static final String UPDATE_AZURE_ENCRYPTION_RESOURCES_BY_NAME = "Updates the Customer managed key of the Azure environment of a given name.";
    public static final String UPDATE_AZURE_ENCRYPTION_RESOURCES_BY_CRN = "Updates the Customer managed key of the Azure environment of a given CRN.";
    public static final String UPGRADE_CCM = "Initiates the CCM tunnel type upgrade to the latest available version";
    public static final String VERTICAL_SCALING = "Initiates the vertical scaling on FreeIPA";
    public static final String ENABLE_SELINUX = "Enable SeLinux on FreeIPA";
    public static final String LIST_SUPPORTED_OS = "LIst supported operating systems by environment.";
    public static final String VALIDATE_CLOUD_STORAGE = "Validates cloud storage";
    public static final String LIST_CERTIFICATE_STATUS =
            "Lists all database servers certificate status that are known, either because they were registered or because this service "
                    + "created them.";

    private EnvironmentOpDescription() {
    }
}
