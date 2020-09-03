package com.sequenceiq.environment.api.doc.environment;

public class EnvironmentModelDescription {

    public static final String SUBNET_IDS = "Subnet ids of the specified networks";
    public static final String SUBNET_METAS = "Subnet metadata of the specified networks";
    public static final String CB_SUBNETS = "Subnet metadata of CB subnets, union of the DWX and public subnets";
    public static final String DWX_SUBNETS = "Subnet metadata of DWX subnets";
    public static final String MLX_SUBNETS = "Subnet metadata of MLX subnets (Deprecated)";
    public static final String LIFTIE_SUBNETS = "Subnet metadata of liftie subnets (Deprecated)";
    public static final String EXISTING_NETWORK = "The existing network is created by the user, otherwise created by the Cloudbreak.";
    public static final String PREFERED_SUBNET_ID = "The subnet in which resource should be deployed if not specified by user. " +
            "It's a null in case of environment list";
    public static final String NETWORKCIDRS = "The network cidrs for the configured vpc";
    public static final String PRIVATE_SUBNET_CREATION = "A flag to enable or disable the private subnet creation.";
    public static final String SERVICE_ENDPOINT_CREATION = "A flag to enable or disable the service endpoint creation.";
    public static final String OUTBOUND_INTERNET_TRAFFIC = "A flag to enable or disable the outbound internet traffic from the instances.";
    public static final String AWS_SPECIFIC_PARAMETERS = "Subnet ids of the specified networks";
    public static final String AZURE_SPECIFIC_PARAMETERS = "Subnet ids of the specified networks";
    public static final String YARN_SPECIFIC_PARAMETERS = "Yarn parameters";
    public static final String MOCK_PARAMETERS = "Mock parameters";
    public static final String AWS_VPC_ID = "Subnet ids of the specified networks";
    public static final String GCP_NETWORK_ID = "Gcp network id";
    public static final String GCP_SHARED_PROJECT_ID = "Gcp shared project id";
    public static final String GCP_NO_PUBLIC_IP = "Gcp no public ip";
    public static final String GCP_NO_FIREWALL_RULES = "Gcp no firewall rules";

    public static final String AZURE_RESOURCE_GROUP_NAME = "Subnet ids of the specified networks";
    public static final String AZURE_NETWORK_ID = "Subnet ids of the specified networks";
    public static final String AZURE_NO_PUBLIC_IP = "Subnet ids of the specified networks";
    public static final String OPENSTACK_NETWORK_ID = "Openstack network id";
    public static final String OPENSTACK_ROUTER_ID = "Openstack router id";
    public static final String OPENSTACK_PUBLIC_NET_ID = "Openstack public net id";
    public static final String OPENSTACK_NETWORKING_OPTION_ID = "Openstack networking option id";

    public static final String AZURE_NO_FIREWALL_RULES = "Subnet ids of the specified networks";
    public static final String CREATE_FREEIPA = "Create freeipa in environment";
    public static final String FREEIPA = "The FreeIPA paramaters";
    public static final String FREEIPA_INSTANCE_COUNT_BY_GROUP = "The number of FreeIPA instances to create per group when creating freeipa in environment";
    public static final String YARN_QUEUE = "Queue for Yarn deployments";
    public static final String YARN_LIFETIME = "Lifetime for Yarn deployments in seconds";

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

    public static final String CLOUD_STORAGE_VALIDATION = "Cloud storage validation enabled or not.";

    public static final String ADMIN_GROUP_NAME = "Name of the admin group to be used for all the services.";

    public static final String AWS_PARAMETERS = "AWS Specific parameters.";
    public static final String TAGS = "Tags for environments.";
    public static final String S3_GUARD = "S3Guard parameters.";
    public static final String S3_GUARD_DYNAMO_TABLE_NAME = "S3Guard Dynamo table name.";
    public static final String QUEUE = "Queue for yarn and cumulus.";
    public static final String AZURE_PARAMETERS = "AZURE Specific parameters.";
    public static final String GCP_PARAMETERS = "GCP Specific parameters.";
    public static final String YARN_PARAMETERS = "YARN Specific parameters.";
    public static final String OPENSTACK_PARAMETERS = "OPENSTACK Specific parameters.";

    public static final String RESOURCE_GROUP_PARAMETERS = "Azure resource group parameters.";
    public static final String EXISTING_RESOURCE_GROUP_NAME = "Name of an existing azure resource group.";
    public static final String RESOURCE_GROUP_NAME = "Name of the azure resource group.";
    public static final String RESOURCE_GROUP_USAGE = "Resource group usage: single resource group for all resources where possible "
            + "or use multiple resource groups.";

    public static final String PARENT_ENVIRONMENT_CRN = "Parent environment global identifier";
    public static final String PARENT_ENVIRONMENT_NAME = "Parent environment name";
    public static final String PARENT_ENVIRONMENT_CLOUD_PLATFORM = "Parent environment cloud platform";

    public static final String FREEIPA_AWS_PARAMETERS = "Aws specific FreeIpa parameters";
    public static final String FREEIPA_AZURE_PARAMETERS = "Azure specific FreeIpa parameters";
    public static final String FREEIPA_OPENSTACK_PARAMETERS = "Openstack specific FreeIpa parameters";
    public static final String FREEIPA_GCP_PARAMETERS = "Gcp specific FreeIpa parameters";
    public static final String FREEIPA_AWS_SPOT_PARAMETERS = "Aws spot instance related parameters.";
    public static final String FREEIPA_AWS_SPOT_PERCENTAGE = "Percentage of spot instances launched in FreeIpa instance group";
    public static final String FREEIPA_AWS_SPOT_MAX_PRICE = "Max price per hour of spot instances launched in FreeIpa instance group";

    public static final String PROXYCONFIG_NAME = "Name of the proxyconfig of the environment.";
    public static final String PROXYCONFIG_RESPONSE = "ProxyConfig attached to the environment.";

    private EnvironmentModelDescription() {
    }
}
