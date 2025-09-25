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
    public static final String PREFERED_SUBNET_IDS = "The subnets in which resource should be deployed if not specified by user. " +
            "It's a null in case of environment list";
    public static final String NETWORKCIDRS = "The network cidrs for the configured vpc";
    public static final String PRIVATE_SUBNET_CREATION = "A flag to enable or disable the private subnet creation.";
    public static final String SERVICE_ENDPOINT_CREATION = "A flag to enable or disable the service endpoint creation.";
    public static final String PUBLIC_ENDPOINT_ACCESS_GATEWAY = "A flag to enable the Public Endpoint Access Gateway, which provides public UI/API " +
            "access to private data lakes and data hubs.";
    public static final String ENDPOINT_ACCESS_GATEWAY_SUBNET_IDS = "Subnet ids for the Public Endpoint Access Gateway. If provided, these " +
            "are the subnets that will be used to create a public Knox endpoint for out-of-network UI/API access. If not provided, public subnets will be " +
            "selected from the subnet list provided for environment creation. (Optional)";
    public static final String ENDPOINT_ACCESS_GATEWAY_SUBNET_METAS = "Subnet metadata for the Public Endpoint Access Gateway. If provided, these " +
            "are the subnets that will be used to create a public Knox endpoint for out-of-network UI/API access. If not provided, public subnets will be " +
            "selected from the subnet list provided for environment creation. (Optional)";
    public static final String OUTBOUND_INTERNET_TRAFFIC = "A flag to enable or disable the outbound internet traffic from the instances.";
    public static final String AWS_SPECIFIC_PARAMETERS = "AWS-specific properties of the network";
    public static final String AZURE_SPECIFIC_PARAMETERS = "Azure-specific properties of the network";
    public static final String GCP_SPECIFIC_PARAMETERS = "GCP-specific properties of the network";
    public static final String YARN_SPECIFIC_PARAMETERS = "Yarn-specific properties of the network";
    public static final String MOCK_PARAMETERS = "Mock-specific properties of the network";
    public static final String AWS_VPC_ID = "AWS VPC ID of the specified networks";
    public static final String GCP_NETWORK_ID = "Gcp network id";
    public static final String GCP_SHARED_PROJECT_ID = "Gcp shared project id";
    public static final String GCP_NO_PUBLIC_IP = "Gcp no public ip";
    public static final String GCP_NO_FIREWALL_RULES = "Gcp no firewall rules";
    public static final String GCP_AVAILABILITY_ZONES = "Gcp availability zones";

    public static final String AZURE_AVAILABILITY_ZONES = "List of Azure Availability Zones";
    public static final String AZURE_DELEGATED_FLEXIBLE_SERVER_SUBNET_IDS = "List of Azure Delegated Subnet IDs for flexible database server";
    public static final String AZURE_USE_PUBLIC_DNS_FOR_PRIVATE_AKS = "Instruct Azure to use public DNS for all DNS records in a private cluster";

    public static final String AZURE_RESOURCE_GROUP_NAME = "Azure Resource Group Name of the specified network";
    public static final String AZURE_PRIVATE_DNS_ZONE_ID = "Full resource ID of an existing Azure private DNS zone";

    public static final String AZURE_AKS_PRIVATE_DNS_ZONE_ID = "Full resource ID of an existing Azure private DNS zone for a private AKS cluster";
    public static final String AZURE_NETWORK_ID = "Azure Network ID of the specified network";
    public static final String AZURE_NO_PUBLIC_IP = "Azure Network is private if this flag is true";

    public static final String CREATE_FREEIPA = "Create freeipa in environment";

    public static final String FREEIPA = "The FreeIPA paramaters";
    public static final String FREEIPA_INSTANCE_COUNT_BY_GROUP = "The number of FreeIPA instances to create per group when creating FreeIPA in environment";
    public static final String FREEIPA_INSTANCE_TYPE = "Override default FreeIPA instance type";
    public static final String MULTIAZ_FREEIPA = "The FreeIPA multi-AZ enabled or not";

    public static final String FREEIPA_RECIPES = "The recipe names that are applied for the FreeIPA";
    public static final String YARN_QUEUE = "Queue for Yarn deployments";
    public static final String YARN_LIFETIME = "Lifetime for Yarn deployments in seconds";

    public static final String POLICY_VALIDATION_ERROR_SERVICE = "The policy validation service name";
    public static final String POLICY_VALIDATION_ERROR_MESSAGE = "The policy validation message of service";
    public static final String POLICY_VALIDATION_ERROR_CODE = "The policy validation code";
    public static final String REMOTE_ENVIRONMENT_CRN = "The Remote environment crn";

    public static final String CREDENTIAL_NAME_REQUEST = "Name of the credential of the environment. If the name is given, "
            + "the detailed credential is ignored in the request.";
    public static final String CREDENTIAL_REQUEST = "If credentialName is not specified, the credential is used to create the new credential for "
            + "the environment.";
    public static final String CREDENTIAL_CRN = "Credential CRN";
    public static final String INTERACTIVE_LOGIN_CREDENTIAL_VERIFICATION_URL = "The url provided by Azure where the user has to use the given user code "
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
    public static final String BACKUP = "Backup related specifics of the environment.";

    public static final String CREDENTIAL_RESPONSE = "Credential of the environment.";
    public static final String CLOUD_PLATFORM = "Cloud platform of the environment.";
    public static final String STATUS = "Status of the environment.";

    public static final String AUTHENTICATION = "SSH key for accessing cluster node instances.";
    public static final String FREE_IPA = "Properties for FreeIpa which can be attached to the given environment";
    public static final String CLOUD_PROVIDER_VARIANT = "Variant of cloud provider";
    public static final String EXTERNALIZED_COMPUTE_CLUSTER = "Properties for Externalized compute cluster";
    public static final String PUBLIC_KEY = "SSH Public key string.";
    public static final String PUBLIC_KEY_ID = "Public key ID registered at the cloud provider.";
    public static final String LOGIN_USER_NAME = "User name created on the nodes for SSH access";

    public static final String SECURITY_ACCESS = "Security control for FreeIPA and Datalake deployment.";
    public static final String KNOX_SECURITY_GROUP = "Security group where Knox-enabled hosts are placed. Comma separated list.";
    public static final String DEFAULT_SECURITY_GROUP = "Security group where all other hosts are placed. Comma separated list.";
    public static final String KNOX_SECURITY_GROUPS = "Security groups where Knox-enabled hosts are placed. Comma separated list.";
    public static final String DEFAULT_SECURITY_GROUPS = "Security groups where all other hosts are placed. Comma separated list.";
    public static final String SECURITY_CIDR = "CIDR range which is allowed for inbound traffic. Either IPv4 or IPv6 is allowed.";
    public static final String LOADBALANCER_CREATION = "Flag that marks the request to create without loadbalancers";

    public static final String SELINUX = "SELinux enabled on the image.";

    public static final String TUNNEL = "Configuration that the connection going directly or with cluster proxy or with CCM and cluster proxy.";
    public static final String OVERRIDE_TUNNEL = "Flag that marks that the request was intended to set the tunnel version by hand and it will not be " +
            "overwritten by Cloudbreak";

    public static final String LOG_CLOUD_STORAGE = "Cloud storage configuration for this environment. Service logs will be stored in the defined location.";

    public static final String ENVIRONMENT_CRN = "CRN of the environment of the database server";
    public static final String SSL_CERTIFICATE_STATUS =
            "Current status of the set of relevant SSL certificates for the database server";
    public static final String DATABASE_SERVER_CERTIFICATE_REQUEST = "Request containing information about a database server SSL certificate";
    public static final String DATABASE_SERVER_CERTIFICATE_RESPONSE = "Response containing information about a database server SSL certificate";
    public static final String DATABASE_SERVER_CERTIFICATE_RESPONSES = "A set of multiple database server SSL certificate responses";

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

    public static final String RESOURCE_GROUP_PARAMETERS = "Azure resource group parameters.";
    public static final String EXISTING_RESOURCE_GROUP_NAME = "Name of an existing Azure resource group.";
    public static final String RESOURCE_GROUP_USAGE = "Resource group usage: single resource group for all resources where possible "
            + "or use multiple resource groups.";

    public static final String ENCRYPTION_KEY_URL = "URL of the Customer Managed Key to encrypt Azure resources";
    public static final String ENCRYPTION_KEY_RESOURCE_GROUP_NAME = "Name of the Azure resource group of the Customer Managed Key to encrypt Azure resources";

    public static final String ENCRYPTION_USER_MANAGED_IDENTITY = "User managed identity for encryption";

    public static final String DISK_ENCRYPTION_SET_ID = "Resource Id of the disk encryption set used to encrypt Azure disks.";
    public static final String RESOURCE_ENCRYPTION_PARAMETERS = "Azure resource encryption parameters.";

    public static final String AZURE_HOST_ENCRYPTION_PARAMETERS = "Azure enable host encryption parameters.";

    public static final String PARENT_ENVIRONMENT_CRN = "Parent environment global identifier";
    public static final String PARENT_ENVIRONMENT_NAME = "Parent environment name";
    public static final String PARENT_ENVIRONMENT_CLOUD_PLATFORM = "Parent environment cloud platform";

    public static final String GCP_RESOURCE_ENCRYPTION_PARAMETERS = "Parameter for GCP resource encryption.";
    public static final String ENCRYPTION_KEY = "Key Resource of the Customer Managed Encryption Key to encrypt GCP resources";

    public static final String FREEIPA_AWS_PARAMETERS = "Aws specific FreeIpa parameters";
    public static final String FREEIPA_RECIPE_LIST = "FreeIpa recipe list";
    public static final String FREEIPA_AZURE_PARAMETERS = "Azure specific FreeIpa parameters";
    public static final String FREEIPA_GCP_PARAMETERS = "Gcp specific FreeIpa parameters";
    public static final String FREEIPA_AWS_SPOT_PARAMETERS = "Aws spot instance related parameters.";
    public static final String FREEIPA_AWS_SPOT_PERCENTAGE = "Percentage of spot instances launched in FreeIpa instance group";
    public static final String FREEIPA_AWS_SPOT_MAX_PRICE = "Max price per hour of spot instances launched in FreeIpa instance group";
    public static final String FREEIPA_IMAGE = "Image parameters for FreeIpa instance creation.";
    public static final String FREEIPA_SECURITY = "Security parameters for FreeIpa instance creation.";
    public static final String FREEIPA_IMAGE_CATALOG = "Image catalog for FreeIpa instance creation.";
    public static final String FREEIPA_IMAGE_ID = "Image ID for FreeIpa instance creation.";
    public static final String FREEIPA_IMAGE_OS_TYPE = "OS type to be chosen from the image catalog";
    public static final String FREEIPA_ARCHITECTURE = "CPU architecture of the freeipa instance";

    public static final String CREATE_EXTERNALIZED_COMPUTE_CLUSTER = "Create externalized compute cluster for environment";
    public static final String EXTERNALIZED_COMPUTE_PRIVATE_CLUSTER = "Externalized compute cluster private flag";
    public static final String EXTERNALIZED_COMPUTE_KUBE_API_AUTHORIZED_IP_RANGES = "Externalized compute cluster Kubernetes API authorized IP ranges";
    public static final String EXTERNALIZED_COMPUTE_OUTBOUND_TYPE = "Externalized compute cluster outbound type";
    public static final String EXTERNALIZED_COMPUTE_WORKER_NODE_SUBNET_IDS = "Externalized compute cluster worker node subnet ids";
    public static final String EXTERNALIZED_COMPUTE_AZURE_PARAMS = "Externalized compute cluster azure-specific parameters";

    public static final String PROXYCONFIG_NAME = "Name of the proxyconfig of the environment.";
    public static final String PROXYCONFIG_RESPONSE = "ProxyConfig attached to the environment.";

    public static final String ENVIRONMENT_SERVICE_VERSION = "The version of the Cloudbreak build used to create the environment.";
    public static final String ENVIRONMENT_DELETION_TYPE = "Deletion type of environment.";
    public static final String ENVIRONMENT_DOMAIN_NAME = "The domain name that's has been generated for the environment.";

    public static final String AWS_DISK_ENCRYPTION_PARAMETERS = "AWS Disk encryption parameters";

    public static final String NO_OUTBOUND_LOAD_BALANCER = "Flag that marks the request to not create an outbound load balancer";

    public static final String DATA_SERVICES = "Data Services parameters of the environment";

    public static final String HYBRID_ENVIRONMENT = "Hybrid environment related requests";

    public static final String FREEIPA_NODE_COUNT = "FreeIPA node count of the environment";

    public static final String ENVIRONMENT_ENABLE_SECRET_ENCRYPTION = "True if the secret encryption feature is enabled for the environment.";

    public static final String ENVIRONMENT_ENABLE_COMPUTE_CLUSTER = "True if externalized compute cluster is enabled for the environment.";

    public static final String ENVIRONMENT_TYPE = "Type of the environment";

    public static final String FREEIPA_LOADBALANCER = "FreeIpa load balancer type to be created. " +
            "Possible values: NONE, INTERNAL_NLB. Default is INTERNAL_NLB.";

    public static final String REMOTE_ENV_CRN = "CRN of remote Environment for hybrid environment";

    public static final String ENCRYPTION_PROFILE = "Encryption profile name for TLS and ciphers configuration";

    public static final String ENCRYPTION_PROFILE_NAME = "Name of encryption profile to be used. " +
            "If not specified, the default encryption profile will be used.";

    private EnvironmentModelDescription() {
    }
}
