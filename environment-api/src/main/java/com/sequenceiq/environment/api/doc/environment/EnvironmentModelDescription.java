package com.sequenceiq.environment.api.doc.environment;

public class EnvironmentModelDescription {

    public static final String SUBNET_IDS = "Subnet ids of the specified networks";
    public static final String SUBNET_METAS = "Subnet metadata of the specified networks";
    public static final String AWS_SPECIFIC_PARAMETERS = "Subnet ids of the specified networks";
    public static final String AZURE_SPECIFIC_PARAMETERS = "Subnet ids of the specified networks";
    public static final String AWS_VPC_ID = "Subnet ids of the specified networks";
    public static final String AZURE_RESOURCE_GROUP_NAME = "Subnet ids of the specified networks";
    public static final String AZURE_NETWORK_ID = "Subnet ids of the specified networks";
    public static final String AZURE_NO_PUBLIC_IP = "Subnet ids of the specified networks";
    public static final String AZURE_NO_FIREWALL_RULES = "Subnet ids of the specified networks";
    public static final String CREATE_FREEIPA = "Create freeipa in environment";

    public static final String CREDENTIAL_NAME_REQUEST = "Name of the credential of the environment. If the name is given, "
            + "the detailed credential is ignored in the request.";
    public static final String CREDENTIAL_REQUEST = "If credentialName is not specified, the credential is used to create the new credential for "
            + "the environment.";
    public static final String INTERACTIVE_LOGIN_CREDENTIAL_VERIFICATION_URL = "The url provided by Azure where the user have to use the given user code "
            + "to sign in";
    public static final String INTERACTIVE_LOGIN_CREDENTIAL_USER_CODE = "The user code what has to be used for the sign-in process on the Azure portal";
    public static final String PROXY_CONFIGS_REQUEST = "Name of the proxy configurations to be attached to the environment.";
    public static final String REGIONS = "Regions of the environment.";
    public static final String REGION_DISPLAYNAMES = "regions with displayNames";
    public static final String LOCATION = "Location of the environment.";
    public static final String LONGITUDE = "Location longitude of the environment.";
    public static final String LATITUDE = "Location latitude of the environment.";
    public static final String LOCATION_DISPLAY_NAME = "Display name of the location of the environment.";
    public static final String NETWORK = "Network related specifics of the environment.";
    public static final String TELEMETRY = "Telemetry related specifics of the environment.";
    public static final String TELEMETRY_LOGGING = "Logging related specifics of the environment.";
    public static final String TELEMETRY_WORKLOAD_ANALYTICS = "Workload analytics related specifics of the environment.";
    public static final String TELEMETRY_LOGGING_OUTPUT_TYPE = "Logging output type of the environment";
    public static final String TELEMETRY_LOGGING_ATTRIBUTES = "Logging component specific attributes of the environment";
    public static final String TELEMETRY_FEATURE_ENABLED = "Enable telemetry component feature of the environment";
    public static final String TELEMETRY_WA_DATABUS_ENDPOINT = "Databus endpoint for workload analytics (environment)";
    public static final String TELEMETRY_WA_ATTRIBUTES = "Workload analytics attributes of the environment";

    public static final String CREDENTIAL_RESPONSE = "Credential of the environment.";
    public static final String PROXY_CONFIGS_RESPONSE = "Proxy configurations in the environment.";
    public static final String CLOUD_PLATFORM = "Cloud platform of the environment.";
    public static final String STATUS = "Status of the environment.";

    public static final String AUTHENTICATION = "SSH key for accessing cluster node instances.";
    public static final String FREE_IPA = "Properties for FreeIpa which can be attached to the given environment";
    public static final String PUBLIC_KEY = "SSH Public key string.";
    public static final String PUBLIC_KEY_ID = "Public key ID registered at the cloud provider.";
    public static final String LOGIN_USER_NAME = "User name created on the nodes for SSH access";

    private EnvironmentModelDescription() {
    }
}
