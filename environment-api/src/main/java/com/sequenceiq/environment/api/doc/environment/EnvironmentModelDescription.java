package com.sequenceiq.environment.api.doc.environment;

public class EnvironmentModelDescription {

    public static final String SUBNET_IDS = "Subnet ids of the specified networks";
    public static final String SUBNET_METAS = "Subnet metadata of the specified networks";
    public static final String EXISTING_NETWORK = "The existing network is created by the user, otherwise created by the Cloudbreak.";
    public static final String PREFERED_SUBNET_ID = "The subnet in which resource should be deployed if not specified by user";
    public static final String PRIVATE_SUBNET_CREATION = "A flag to enable or disable the private subnet creation.";
    public static final String AWS_SPECIFIC_PARAMETERS = "Subnet ids of the specified networks";
    public static final String AZURE_SPECIFIC_PARAMETERS = "Subnet ids of the specified networks";
    public static final String YARN_SPECIFIC_PARAMETERS = "Yarn parameters";
    public static final String MOCK_PARAMETERS = "Mock parameters";
    public static final String AWS_VPC_ID = "Subnet ids of the specified networks";
    public static final String AZURE_RESOURCE_GROUP_NAME = "Subnet ids of the specified networks";
    public static final String AZURE_NETWORK_ID = "Subnet ids of the specified networks";
    public static final String AZURE_NO_PUBLIC_IP = "Subnet ids of the specified networks";
    public static final String CREATE_FREEIPA = "Create freeipa in environment";

    public static final String CREDENTIAL_NAME_REQUEST = "Name of the credential of the environment. If the name is given, "
            + "the detailed credential is ignored in the request.";
    public static final String CREDENTIAL_REQUEST = "If credentialName is not specified, the credential is used to create the new credential for "
            + "the environment.";
    public static final String INTERACTIVE_LOGIN_CREDENTIAL_VERIFICATION_URL = "The url provided by Azure where the user have to use the given user code "
            + "to sign in";
    public static final String INTERACTIVE_LOGIN_CREDENTIAL_USER_CODE = "The user code what has to be used for the sign-in process on the Azure portal";
    public static final String REGIONS = "Regions of the environment.";
    public static final String REGION_DISPLAYNAMES = "regions with displayNames";
    public static final String LOCATION = "Location of the environment.";
    public static final String LONGITUDE = "Location longitude of the environment.";
    public static final String LATITUDE = "Location latitude of the environment.";
    public static final String LOCATION_DISPLAY_NAME = "Display name of the location of the environment.";
    public static final String NETWORK = "Network related specifics of the environment.";
    public static final String TELEMETRY = "Telemetry related specifics of the environment.";

    public static final String CREDENTIAL_RESPONSE = "Credential of the environment.";
    public static final String CLOUD_PLATFORM = "Cloud platform of the environment.";
    public static final String STATUS = "Status of the environment.";

    public static final String AUTHENTICATION = "SSH key for accessing cluster node instances.";
    public static final String FREE_IPA = "Properties for FreeIpa which can be attached to the given environment";
    public static final String PUBLIC_KEY = "SSH Public key string.";
    public static final String PUBLIC_KEY_ID = "Public key ID registered at the cloud provider.";
    public static final String LOGIN_USER_NAME = "User name created on the nodes for SSH access";

    public static final String SECURITY_ACCESS = "Security control for FreeIPA and Datalake deployment.";
    public static final String KNOX_SECURITY_GROUP = "Security group where Knox-enabled hosts are placed.";
    public static final String DEFAULT_SECURITY_GROUP = "Security group where all other hosts are placed.";
    public static final String SECURITY_CIDR = "CIDR range which is allowed for inbound traffic. Either IPv4 or IPv6 is allowed.";

    public static final String TUNNEL = "Configuration that the connection going directly or with cluster proxy or with ccm and cluster proxy.";

    public static final String LOG_CLOUD_STORAGE = "Cloud storage configuration for this environment. Service logs will be stored in the defined location.";

    public static final String IDBROKER_MAPPING_SOURCE = "IDBroker mapping source.";
    public static final String ADMIN_GROUP_NAME = "Name of the admin group to be used for all the services.";

    public static final String AWS_PARAMETERS = "AWS Specific parameters.";
    public static final String TAGS = "Tags for environments.";
    public static final String S3_GUARD = "S3Guard parameters.";
    public static final String S3_GUARD_DYNAMO_TABLE_NAME = "S3Guard Dynamo table name.";

    public static final String PARENT_ENVIRONMENT_CRN = "Parent environment global identifier";

    private EnvironmentModelDescription() {
    }
}
