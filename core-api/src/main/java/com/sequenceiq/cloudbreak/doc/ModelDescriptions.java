package com.sequenceiq.cloudbreak.doc;

public class ModelDescriptions {

    public static final String ID = "id of the resource";
    public static final String NAME = "name of the resource";
    public static final String DESCRIPTION = "description of the resource";
    public static final String PUBLIC_IN_ACCOUNT = "resource is visible in account";
    public static final String ATTRIBUTES = "provider specific attributes of the credential";
    public static final String CLOUD_PLATFORM = "type of cloud provider";
    public static final String TOPOLOGY_ID = "id of the topology the resource belongs to";
    public static final String RESPONSE = "response object";
    public static final String FIXINPUTS = "fixinputs object";
    public static final String DATALAKEINPUTS = "datalakeinputs object";
    public static final String CREATED = "creation time of the resource in long";
    public static final String AMBARI_SERVER = "ambari server address";
    public static final String WORKSPACE_OF_THE_RESOURCE = "workspace of the resource";
    public static final String USER_ID = "User ID in the new authorization model";
    public static final String WORKSPACE_ID = "Workspace ID of the resource";
    public static final String PREINSTALLED = "Denotes that the image is prewarmed or base image.";
    public static final String ENVIRONMENTS = "Environments of the resource";
    public static final String GOV_CLOUD_FLAG = "Flag indicating if the credential type is AWS Gov Cloud";
    public static final String TENANT_NAME = "name of the current tenant";

    private ModelDescriptions() {
    }

    public static class BlueprintModelDescription {
        public static final String URL = "url source of a blueprint, set this or the blueprint field";
        public static final String BLUEPRINT_NAME = "gathered from blueprintName field from the blueprint";
        public static final String BLUEPRINT = "blueprint, set this or the url field";
        public static final String HOST_GROUP_COUNT = "number of host groups";
        public static final String STATUS = "status of the blueprint";
        public static final String INPUTS = "input parameters of the blueprint";
        public static final String BLUEPRINT_PROPERTIES = "properties to extend the blueprint with";
        public static final String TAGS = "user defined tags for blueprint";
        public static final String STACK_TYPE = "The type of the stack: for example HDP or HDF";
        public static final String STACK_VERSION = "The version of the stack";
    }

    public static class CredentialModelDescription {
        public static final String AWS_PARAMETERS = "custom parameters for AWS credential";
        public static final String GCP_PARAMETERS = "custom parameters for GCP credential";
        public static final String AZURE_PARAMETERS = "custom parameters for Azure credential";
        public static final String OPENSTACK_PARAMETERS = "custom parameters for Openstack credential";
        public static final String CUMULUS_YARN_PARAMETERS = "custom parameters for Cumulus Yarn credential";
        public static final String YARN_PARAMETERS = "custom parameters for Yarn credential";
        public static final String ACCOUNT_IDENTIFIER = "provider specific identifier of the account/subscription/project that is used by Cloudbreak";
        public static final String AWS_CREDENTIAL_PREREQUISITES = "AWS specific credential prerequisites.";
        public static final String AZURE_CREDENTIAL_PREREQUISITES = "Azure specific credential prerequisites.";
        public static final String GCP_CREDENTIAL_PREREQUISITES = "GCP specific credential prerequisites.";
        public static final String AWS_EXTERNAL_ID = "AWS specific identifier for role based credential creation - "
                + "External id for 'Another AWS account' typed roles.";
        public static final String AWS_POLICY_JSON = "AWS specific JSON file that is base64 encoded and "
                + "describes the necessary AWS policies for cloud resource provisioning.";
        public static final String AZURE_APP_CREATION_COMMAND = "Azure CLI command to create Azure AD Application as prerequisite for credential creation."
                + "The field is base64 encoded.";
        public static final String CODE_GRANT_FLOW_LOGIN_URL = "Login URL for code grant flow";
        public static final String GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND = "GCP specific 'gcloud' CLI based commands to "
                + "create prerequisites for Cloudbreak credential creation. The field is base64 encoded.";
    }

    public static class OrchestratorModelDescription {
        public static final String TYPE = "type of the orchestrator";
        public static final String PARAMETERS = "orchestrator specific parameters, like authentication details";
        public static final String ENDPOINT = "endpoint for the container orchestration api";
    }

    public static class TemplateModelDescription {
        public static final String VOLUME_COUNT = "number of volumes";
        public static final String VOLUME_SIZE = "size of volume";
        public static final String ROOT_VOLUME_SIZE = "size of the root volume";
        public static final String VOLUME_TYPE = "type of the volumes";
        public static final String INSTANCE_TYPE = "type of the instance";
        public static final String CUSTOM_INSTANCE_TYPE = "custom instancetype definition";
        public static final String PARAMETERS = "cloud specific parameters for template";
        public static final String AWS_PARAMETERS = "aws specific parameters for template";
        public static final String GCP_PARAMETERS = "gcp specific parameters for template";
        public static final String OPENSTACK_PARAMETERS = "openstack specific parameters for template";
        public static final String YARN_PARAMETERS = "yarn specific parameters for template";
        public static final String AZURE_PARAMETERS = "azure specific parameters for template";
        public static final String AWS_SPOT_PRICE = "spot price for aws";
        public static final String AZURE_PRIVATE_ID = "private id for azure";
        public static final String ENCRYPTION = "encryption for vm";
        public static final String ENCRYPTION_TYPE = "encryption type for vm (DEFAULT|CUSTOM|NONE)";
        public static final String GCP_ENCRYPTION_TYPE = "encryption type for vm (DEFAULT|CUSTOM)";
        public static final String ENCRYPTION_KEY = "encryption key for vm";
        public static final String ENCRYPTED = "should encrypt the vm";
        public static final String ENCRYPTION_METHOD = "encryption method for the key (RAW|RSA)";
        public static final String SECRET_PARAMETERS = "cloud specific secret parameters for template";
    }

    public static class ConstraintTemplateModelDescription {
        public static final String CPU = "number of CPU cores needed for the Ambari node";
        public static final String MEMORY = "memory needed for the Ambari container (GB)";
        public static final String DISK = "disk size needed for an Ambari node (GB)";
        public static final String ORCHESTRATOR_TYPE = "type of orchestrator";
    }

    public static class ImageModelDescription {
        public static final String IMAGE_NAME = "name of the image";
        public static final String IMAGE_CATALOG_URL = "url of the image catalog";
        public static final String IMAGE_ID = "id of the image";
        public static final String IMAGE_CATALOG_NAME = "name of the image catalog";

    }

    public static class StructuredEventResponseDescription {
        public static final String TYPE = "Type of the Audit event. REST|FLOW|NOTIFICATION";
        public static final String OPERATION = "Basic details about the Audit event";
        public static final String EVENT_JSON = "Raw StructuredEvent content";
        public static final String STATUS = "Summary status of the Audit event";
        public static final String DURATION = "Timespan of the event in miliseconds";
    }

    public static class CloudbreakDetailsModelDescription {
        public static final String VERSION = "version of the Cloudbreak that provisioned the stack";
    }

    public static class PlatformResourceRequestModelDescription {
        public static final String CREDENTIAL_ID = "credential resource id for the request";
        public static final String CREDENTIAL_NAME = "credential resource name for the request";
        public static final String REGION = "Related region";
        public static final String FILTER = "filter for resources";
        public static final String AVAILABILITY_ZONE = "related availability zone";
    }

    public static class RecommendationRequestModelDescription {
        public static final String BLUEPRINT_NAME = "name that could indentify an existing cluster defintion";
        public static final String BLUEPRINT_ID = "id that could indentify an existing cluster defintion";
    }

    public static class StackModelDescription {
        public static final String TENANANT = "name of the tenant";
        public static final String WORKSPACE_ID = "id of the workspace";
        public static final String USER_ID = "id of the user";
        public static final String STACK_ID = "id of the stack";
        public static final String CUSTOM = "AmbariKerberosDescriptor parameters as a json";
        public static final String ENTRIES = "Entries parameters as a json";
        public static final String IMAGE = "image of the stack";
        public static final String STACK_NAME = "name of the stack";
        public static final String REGION = "region of the stack";
        public static final String AVAILABILITY_ZONE = "availability zone of the stack";
        public static final String CREDENTIAL_ID = "credential resource id for the stack";
        public static final String CREDENTIAL_NAME = "credential resource name for the stack";
        public static final String USERNAME = "ambari username";
        public static final String PASSWORD = "ambari password";
        public static final String EXTENDED_BLUEPRINT_TEXT = "extended blueprint text";
        public static final String ENABLE_SECURITY = "enable Kerberos security";
        public static final String KERBEROS_MASTER_KEY = "kerberos master key";
        public static final String KERBEROS_ADMIN = "kerberos admin user";
        public static final String KERBEROS_PASSWORD = "kerberos admin password";
        public static final String KERBEROS_URL = "kerberos KDC server URL";
        public static final String KERBEROS_ADMIN_URL = "kerberos admin server URL";
        public static final String KERBEROS_PRINCIPAL = "kerberos principal";
        public static final String KERBEROS_REALM = "kerberos realm";
        public static final String KERBEROS_CONTAINER_DN = "kerberos containerDn";
        public static final String KERBEROS_LDAP_URL = "URL of the connected ldap";
        public static final String KERBEROS_CONFIG_NAME = "the name of the kerberos configuration";
        public static final String KERBEROS_TCP_ALLOW = "kerberos configuration name";
        public static final String DESCRIPTOR = "Ambari kerberos descriptor";
        public static final String KRB_5_CONF = "Ambari kerberos krb5.conf template";
        public static final String KERBEROS_DOMAIN = "cluster instances will set this as the domain part of their hostname";
        public static final String KERBEROS_NAMESERVERS = "comma separated list of nameservers' IP address which will be used by cluster instances";
        public static final String PARAMETERS = "additional cloud specific parameters for stack";
        public static final String INPUTS = "dynamic properties";
        public static final String FAILURE_ACTION = "action on failure";
        public static final String FAILURE_POLICY = "failure policy in case of failures";
        public static final String STACK_STATUS = "status of the stack";
        public static final String STATUS_REASON = "status message of the stack";
        public static final String STATUS_REQUEST = "status of the scale request";
        public static final String SERVER_IP = "public ambari ip of the stack";
        public static final String SERVER_URL = "public ambari url";
        public static final String DP_AMBARI_USERNAME = "ambari username for Dataplane";
        public static final String DP_AMBARI_PASSWORD = "ambari password for Dataplane";
        public static final String NETWORK_ID = "network resource id for the stack";
        public static final String CERTIFICATE = "server certificate used by the gateway";
        public static final String CLIENT_KEY = "client key used by the gateway";
        public static final String CLIENT_CERT = "client certificate used by the gateway";
        public static final String CLUSTER_STATUS = "status of the cluster";
        public static final String PLATFORM_VARIANT = "cloud provider api variant";
        public static final String ORCHESTRATOR = "the details of the container orchestrator api to use";
        public static final String STACK_TEMPLATE = "freemarker template for the stack";
        public static final String CREATED = "creation time of the stack in long";
        public static final String GATEWAY_PORT = "port of the gateway secured proxy";
        public static final String AMBARI_VERSION = "specific version of ambari";
        public static final String HDP_VERSION = "specific version of HDP";
        public static final String CLOUDBREAK_DETAILS = "details of the Cloudbreak that provisioned the stack";
        public static final String S3_ACCESS_ROLE_ARN = "S3 access role arn";
        public static final String VPC_ID = "cluster vpc id";
        public static final String SUBNET_ID = "cluster subnet id";
        public static final String FILE_SYSTEM = "external file system configuration";
        public static final String CLOUD_STORAGE = "external cloud storage configuration";
        public static final String INSTANCE_GROUPS = "collection of instance groupst";
        public static final String IMAGE_SETTINGS = "settings for custom images";
        public static final String IMAGE_CATALOG = "custom image catalog URL";
        public static final String IMAGE_ID = "virtual machine image id from ImageCatalog, machines of the cluster will be started from this image";
        public static final String OS_TYPE = "os type of the image, this property is only considered when no specific image id is provided";
        public static final String INSTANCE_GROUP_ADJUSTMENT = "instance group adjustment";
        public static final String TAGS = "stack related tags";
        public static final String APPLICATION_TAGS = "stack related application tags";
        public static final String DEFAULT_TAGS = "stack related default tags";
        public static final String USERDEFINED_TAGS = "stack related userdefined tags";
        public static final String CREDENTIAL = "stack related credential";
        public static final String AUTHENTICATION = "stack related authentication";
        public static final String FILESYSTEM = "cluster related filesystem";
        public static final String NETWORK = "stack related network";
        public static final String SOURCE_CREDENTIAL = "source credential object for cloning";
        public static final String CLUSTER = "cluster object on stack";
        public static final String CLUSTER_REQUEST = "cluster request object on stack";
        public static final String USER = "the related user";
        public static final String NODE_COUNT = "node count of the stack";
        public static final String HARDWARE_INFO_RESPONSE = "hardware information where pairing hostmetadata with instancemetadata";
        public static final String EVENTS = "related events for a cloudbreak stack";
        public static final String CUSTOM_DOMAIN = "custom domain name for the nodes in the stack";
        public static final String CUSTOM_HOSTNAME = "custom hostname for nodes in the stack";
        public static final String CLUSTER_NAME_AS_SUBDOMAIN = "using the cluster name to create subdomain";
        public static final String HOSTGROUP_NAME_AS_HOSTNAME = "using the hostgroup names to create hostnames";
        public static final String CUSTOM_DOMAIN_SETTINGS = "settings related to custom domain names";
        public static final String GENERAL_SETTINGS = "general configuration parameters for a cluster (e.g. 'name', 'credentialname')";
        public static final String PLACEMENT_SETTINGS = "placement configuration parameters for a cluster (e.g. 'region', 'availabilityZone')";
        public static final String SHARED_SERVICE_REQUEST = "Shared service request";
        public static final String ENVIRONMENT = "environment which the stack is assigned to";
        public static final String KERBEROS_KDC_VERIFY_KDC_TRUST = "Allows to select either a trusting SSL connection or a "
                + "validating (non-trusting) SSL connection to KDC";
        public static final String TERMINATED = "termination completion time of stack in long";
        public static final String AWS_PARAMETERS = "aws specific parameters for stack";
        public static final String AZURE_PARAMETERS = "azure specific parameters for stack";
        public static final String GCP_PARAMETERS = "gcp specific parameters for stack";
        public static final String OPENSTACK_PARAMETERS = "openstack specific parameters for stack";
    }

    public static class ClusterModelDescription {
        public static final String STATUS = "status of the cluster";
        public static final String RESULT_DBS = "name of the created dbs";
        public static final String STATUS_REASON = "status message of the cluster";
        public static final String CLUSTER_NAME = "name of the cluster";
        public static final String CLUSTER_ID = "id of the cluster";
        public static final String BLUEPRINT_ID = "blueprint id for the cluster";
        public static final String BLUEPRINT_NAME = "blueprint name for the cluster";
        public static final String BLUEPRINT = "blueprint for the cluster";
        public static final String SHARED_SERVICE_REQUEST = "Shared service request";
        public static final String VALIDATE_BLUEPRINT = "blueprint validation";
        public static final String VALIDATE_REPOSITORIES = "ambari and stack repository validation";
        public static final String SHARED_SERVICE = "shared service for a specific stack";
        public static final String FILESYSTEM = "filesystem for a specific stack";
        public static final String HOURS = "duration - how long the cluster is running in hours";
        public static final String MINUTES = "duration - how long the cluster is running in minutes (minus hours)";
        public static final String CLUSTER_EXPOSED_SERVICES = "cluster exposed services for topologies";
        public static final String CONFIG_STRATEGY = "config recommendation strategy, default value is 'ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES'";
        public static final String BLUEPRINT_INPUTS = "blueprint inputs in the cluster";
        public static final String BLUEPRINT_CUSTOM_PROPERTIES = "blueprint custom properties";
        public static final String CUSTOM_CONTAINERS = "custom containers";
        public static final String CUSTOM_QUEUE = "custom queue for yarn orchestrator";
        public static final String EXECUTOR_TYPE = "executor type of cluster";
        public static final String CONNECTED_CLUSTER = "cluster can connect to a datalake you can define the parameters here";
        public static final String SOURCE_CLUSTER_ID = "source cluster id";
        public static final String SOURCE_CLUSTER_NAME = "source cluster name";
        public static final String CLUSTER_ATTRIBUTES = "Additional information for ambari cluster";
        public static final String RDSCONFIG_IDS = "RDS configuration ids for the cluster";
        public static final String RDSCONFIG_NAMES = "RDS configuration names for the cluster";
        public static final String DATABASES = "Database configurations for the cluster";
        public static final String LDAP_CONFIG_ID = "LDAP config id for the cluster";
        public static final String LDAP_CONFIG_NAME = "LDAP config name for the cluster";
        public static final String LDAP_CONFIG = "LDAP config for the cluster";
        public static final String HOSTGROUP_ADJUSTMENT = "host group adjustment";
        public static final String ADJUSTMENT_TYPE = "type of  adjustment";
        public static final String STATUS_REQUEST = "request status";
        public static final String USERNAME_PASSWORD = "user details";
        public static final String AMBARI_REQUEST = "ambari specific requests";
        public static final String CM_REQUEST = "cloudera manager specific requests";
        public static final String HOSTGROUPS = "collection of hostgroups";
        public static final String AMBARI_STACK_DETAILS = "details of the Ambari stack";
        public static final String AMBARI_REPO_DETAILS = "details of the Ambari package repository";
        public static final String CM_REPO_DETAILS = "details of the Cloudera Manager package repository";
        public static final String CM_PRODUCT_DETAILS = "list of Cloudera Manager product detials";
        public static final String AMBARI_DATABASE_ERROR = "result of Ambari database test";
        public static final String RDS_CONFIGS = "details of the external database for Hadoop components";
        public static final String LDAP_CONNECTION_RESULT = "result of Ldap connection test";
        public static final String CREATION_FINISHED = "Epoch time of cluster creation finish";
        public static final String AMBARI_SECURITY_MASTER_KEY = "a master key for encrypting the passwords in Ambari";
        public static final String UPTIME = "duration - how long the cluster is running in milliseconds";
        public static final String PROXY_NAME = "proxy configuration name for the cluster";
        public static final String PROXY_CONFIG_ID = "proxy configuration id for the cluster";
        public static final String LOCATIONS = "cloud storage locations";
        public static final String STATUS_MAINTENANCE_MODE = "maintenance mode status";
        public static final String SECURE = "tells wether the cluster is secured or not";
        public static final String KERBEROSCONFIG_NAME = "Kerberos config name for the cluster";
        public static final String AMBARI_URL = "Ambari url";
        public static final String AWS_PARAMETERS = "aws specific parameters for cloud storage";
        public static final String AZURE_PARAMETERS = "azure specific parameters for cloud storage";
        public static final String GCP_PARAMETERS = "gcp specific parameters for cloud storage";
        public static final String OPENSTACK_PARAMETERS = "openstack specific parameters for cloud storage";
    }

    public static class GatewayModelDescription {
        public static final String ENABLE_KNOX_GATEWAY = "[DEPRECATED] 'enableGateway' is no longer needed to determine if "
                + "gateway needs to be launched or not.\n"
                + "Presence of gateway definition in request is suffucicient. This value is only used in legacy requests, \n"
                + "when 'topologyName' or 'exposedServices' is defined in the root of Gateway, instead of using topologies.\n"
                + "When it is a legacy request and 'enableGateway' is set to 'false', gateway will not be saved and created.";
        public static final String KNOX_PATH = "Knox gateway path";
        public static final String KNOX_GATEWAY_TYPE = "Knox gateway type";
        public static final String KNOX_SSO_TYPE = "Knox SSO type";
        public static final String DEPRECATED_KNOX_TOPOLOGY_NAME = "[DEPRECATED] Use the 'topologyName' inside the 'topologies' part of the request.\n"
                + "If 'topologyName' is specified, other deprecated properties ('exposedServices' and 'enableGateway') will be used as well, "
                + "and 'topologies' will be ignored.";
        public static final String KNOX_TOPOLOGY_NAME = "Knox topology name";
        public static final String DEPRECATED_EXPOSED_KNOX_SERVICES = "[DEPRECATED] Use the 'exposedServices' inside the 'topologies' part of the request.\n"
                + "If 'exposedServices' is specified, other deprecated properties ('topologyName' and 'enableGateway') will be used as well, "
                + "and 'topologies' will be ignored.";
        public static final String EXPOSED_KNOX_SERVICES = "exposed Knox services - those services that should be accessible through Knox gateway.";
        public static final String KNOX_SSO_PROVIDER = "SSO provider cluster name";
        public static final String KNOX_SSO_CERT = "SSO Provider certificate";
        public static final String GATEWAY_TOPOLOGIES = "Topology definitions of the gateway.";
    }

    public static class ClusterTemplateModelDescription {
        public static final String NAME = "name of the cluster template";
        public static final String TEMPLATE = "stringified template JSON";
        public static final String TYPE = "type of the cluster template. The default is OTHER";
        public static final String CLOUD_PLATFORM = "cloudplatform which this template is compatible with";
        public static final String DATALAKE_REQUIRED = "datalake required which this template is compatible with. The default is OPTIONAL";
    }

    public static class ClusterTemplateViewModelDescription {
        public static final String TYPE = "type of the cluster template";
        public static final String CLOUD_PLATFORM = "cloudplatform which this template is compatible with";
        public static final String DATALAKE_REQUIRED = "datalake required which this template is compatible with";
        public static final String NODE_COUNT = "node count of the cluster template";
        public static final String STACK_TYPE = "stack type of cluster template";
        public static final String STACK_VERSION = "stack version of cluster template";
    }

    public static class ConnectorModelDescription {
        public static final String PLATFORM_VARIANTS = "platform variants";
        public static final String DEFAULT_VARIANTS = "default variants";
        public static final String DISK_TYPES = "disk types";
        public static final String DEFAULT_DISKS = "default disks";
        public static final String DISK_MAPPINGS = "disk mappings";
        public static final String DISK_DISPLAYNAMES = "disk displayNames";
        public static final String IMAGES = "default images";
        public static final String IMAGES_REGEX = "images regex";
        public static final String TAG_SPECIFICATIONS = "tag specifications";
        public static final String SPECIAL_PARAMETERS = "custom parameters";
        public static final String PLATFORM_SPECIFIC_SPECIAL_PARAMETERS = "platform specific custom parameters";
        public static final String ORCHESTRATORS = "orchestrators";
        public static final String DEFAULT_ORCHESTRATORS = "default orchestrators";
        public static final String VIRTUAL_MACHNES = "virtual machines";
        public static final String DEFAULT_VIRTUAL_MACHINES = "default virtual machines";
        public static final String DEFAULT_VIRTUAL_MACHINES_PER_ZONES = "default virtual machines per zones";
        public static final String VIRTUAL_MACHINES_PER_ZONES = "virtual machines per zones";
        public static final String REGIONS = "regions";
        public static final String REGION_DISPLAYNAMES = "regions with displayNames";
        public static final String REGION_LOCATIONS = "regions with location data";
        public static final String AVAILABILITY_ZONES = "availability zones";
        public static final String DEFAULT_REGIOS = "default regions";
    }

    public static class ClusterManagerRepositoryDescription {
        public static final String VERSION = "version of the cluster manager";
        public static final String BASE_URL = "url of the cluster manager repository";
        public static final String REPO_GPG_KEY = "gpg key of the cluster manager repository";
    }

    public static class ClouderaManagerProductDescription {
        public static final String NAME = "name of the Cloudera manager product";
        public static final String VERSION = "version of the Cloudera manager product";
        public static final String PARCEL = "parcel url of the Cloudera manager product";
    }

    public static class AmbariDatabaseDetailsDescription {
        public static final String VENDOR = "vendor of the Ambari database";
        public static final String NAME = "name of the Ambari database";
        public static final String HOST = "host of the Ambari database";
        public static final String PORT = "port of the Ambari database";
        public static final String USER_NAME = "user name for the Ambari database";
        public static final String PASSWORD = "password for the Ambari database";
    }

    public static class StackRepositoryDescription {
        public static final String STACK = "name of the stack, like HDP";
        public static final String VERSION = "version of the stack";
        public static final String OS = "operating system for the stack, like redhat6";
        public static final String OS_TYPE = "operating system type for the stack, like centos6";
        public static final String STACK_REPO_ID = "id of the stack repository";
        public static final String UTILS_REPO_ID = "id of the stack utils repository";
        public static final String STACK_BASE_URL = "url of the stack repository";
        public static final String UTILS_BASE_URL = "url of the stack utils repository";
        public static final String ENABLE_GPL_REPO = "enable gpl repository";
        public static final String VERIFY = "whether to verify or not the repo url";
        public static final String REPOSITORY_VERSION = "version of the repository for VDF file creation in Ambari";
        public static final String VDF_URL = "local path on the Ambari server or URL that point to the desired VDF file";
        public static final String MPACK_URL = "url the MPACK that needs to be installed before HDF installation";
        public static final String MPACKS = "Management packs which are needed for the HDP / HDF clusters";
        public static final String STACK_REPO_DETAILS = "details of the Stack repository";
    }

    public static class Database {
        public static final String CONNECTION_URL = "JDBC connection URL in the form of jdbc:<db-type>://<address>:<port>/<db>";
        public static final String CONNECTION_DRIVER_NAME = "Name of the JDBC connection driver (for example: 'org.postgresql.Driver')";
        public static final String DB_ENGINE = "Name of the external database engine (MYSQL, POSTGRES...)";
        public static final String DB_ENGINE_DISPLAYNAME = "Display name of the external database engine (Mysql, PostgreSQL...)";
        public static final String VERSION = "Version of the Database";
        public static final String USERNAME = "Username to use for the jdbc connection";
        public static final String PASSWORD = "Password to use for the jdbc connection";
        public static final String ORACLE = "Oracle specific properties";
        public static final String NAME = "Name of the RDS configuration resource";
        public static final String STACK_VERSION = "(HDP, HDF)Stack version for the RDS configuration";
        public static final String RDSTYPE = "Type of RDS, aka the service name that will use the RDS like HIVE, DRUID, SUPERSET, RANGER, etc.";
        public static final String CONNECTOR_JAR_URL = "URL that points to the jar of the connection driver(connector)";
        public static final String DATABASE_REQUEST_CLUSTER_NAME = "requested cluster name";
        public static final String DATABASE_CONNECTION_TEST_RESULT = "result of RDS connection test";
        public static final String DATABASE_REQUEST = "unsaved RDS config to be tested by connectivity";
    }

    public static class FileSystem {
        public static final String NAME = "name of the filesystem";
        public static final String TYPE = "type of the filesystem";
        public static final String DEFAULT = "true if fs.defaultFS should point to this filesystem";
        public static final String PROPERTIES = "configuration of the filesystem access as key-value pairs";
        public static final String LOCATIONS = "configuration of the filesystem location";
    }

    public static class RecipeModelDescription {
        public static final String DESCRIPTION = "A recipe is a script that runs on all nodes of a selected node "
                + "group at a specific time. You can use recipes for tasks such as installing additional software "
                + "or performing advanced cluster configuration. For example, you can use a recipe to put a JAR file "
                + "on the Hadoop classpath.";
        public static final String CONTENT = "content of recipe";
        public static final String TYPE = "type of recipe [PRE_AMBARI_START,PRE_TERMINATION,POST_AMBARI_START,POST_CLUSTER_INSTALL]. "
                + "The default is PRE_AMBARI_START";
        public static final String WORKSPACE_ID = "id of the workspace";
        public static final String WORKSPACE_NAME = "name of the workspace";
    }

    public static class InstanceGroupModelDescription {
        public static final String INSTANCE_GROUP_NAME = "name of the instance group";
        public static final String INSTANCE_GROUP_TYPE = "type of the instance group, default value is CORE";
        public static final String TEMPLATE = "instancegroup related template";
        public static final String SECURITYGROUP = "instancegroup related securitygroup";
        public static final String NODE_COUNT = "number of nodes";
        public static final String TEMPLATE_ID = "referenced template id";
        public static final String INSTANCE_TYPE = "type of the instance";
        public static final String STATUS = "status of the instance";
        public static final String SECURITY_GROUP_ID = "security group resource id for the instance group";
        public static final String METADATA = "metadata of instances";
        public static final String PARAMETERS = "cloud specific parameters for instance group";
        public static final String AWS_PARAMETERS = "aws specific parameters for instance group";
        public static final String AZURE_PARAMETERS = "azure specific parameters for instance group";
        public static final String GCP_PARAMETERS = "gcp specific parameters for instance group";
        public static final String OPENSTACK_PARAMETERS = "openstack specific parameters for instance group";
    }

    public static class InstanceGroupAdjustmentModelDescription {
        public static final String SCALING_ADJUSTMENT = "scaling adjustment of the instance groups";
        public static final String WITH_CLUSTER_EVENT = "on stack update, update cluster too";
    }

    public static class MpackDetailsDescription {
        public static final String MPACK_NAME = "name of the ambari management pack";
        public static final String MPACK_URL = "url of the ambari management pack";
        public static final String PURGE = "if true, management pack will be installed with '--purge' flag";
        public static final String PURGE_LIST = "if provided, management pack will be installed with '--purgeList' option with this values";
        public static final String FORCE = "if true, management pack will be installed with '--force' flag";
        public static final String IGNORE_WARNING = "if true, then we dont validate the url";
        public static final String STACK_DEFAULT = "if true, the management pack is mandatory for the stack";
        public static final String PREINSTALLED = "if true, the management pack is already installed on the instances";
    }

    public static class KubernetesConfig {
        public static final String NAME = "Name of the Kubernetes configuration resource";
        public static final String CONFIG = "Kubernetes configuration";
    }

    public static class HostGroupModelDescription {
        public static final String RECIPE_IDS = "referenced recipe ids";
        public static final String RECIPE_NAMES = "referenced recipe names";
        public static final String RECIPES = "referenced recipes";
        public static final String EXTENDED_RECIPES = "referenced extended recipes";
        public static final String HOST_GROUP_NAME = "name of the host group";
        public static final String CONSTRAINT = "instance group or resource constraint for a hostgroup";
        public static final String INSTANCE_GROUP = "name of an instance group where the hostgroup will be deployed";
        public static final String CONSTRAINT_NAME = "name of a constraint template that defines the resource constraints for the hostgroup";
        public static final String HOST_COUNT = "number of hosts in the hostgroup";
        public static final String METADATA = "metadata of hosts";
        public static final String RECOVERY_MODE = "recovery mode of the hostgroup's nodes";
    }

    public static class UserNamePasswordModelDescription {
        public static final String NEW_USER_NAME = "new user name in ambari";
        public static final String OLD_PASSWORD = "old password in ambari";
        public static final String NEW_PASSWORD = "new password in ambari";
    }

    public static class HostMetadataModelDescription {
        public static final String STATE = "state of the host";
    }

    public static class HostGroupAdjustmentModelDescription {
        public static final String SCALING_ADJUSTMENT = "scaling adjustment of the host groups";
        public static final String WITH_STACK_UPDATE = "on cluster update, update stack too";
        public static final String VALIDATE_NODE_COUNT = "validate node count during downscale";
    }

    public static class InstanceMetaDataModelDescription {
        public static final String PRIVATE_IP = "private ip of the insctance";
        public static final String PUBLIC_IP = "public ip of the instance";
        public static final String INSTANCE_ID = "id of the instance";
        public static final String DISCOVERY_FQDN = "the fully qualified domain name of the node in the service discovery cluster";
    }

    public static class FailurePolicyModelDescription {
        public static final String THRESHOLD = "threshold of failure policy";
    }

    public static class EventModelDescription {
        public static final String TYPE = "type of the event";
        public static final String TIMESTAMP = "timestamp of the event";
        public static final String MESSAGE = "message of the event";
        public static final String NOTIFICATION_TYPE = "Type of the notification to be identifiable by the UI";
    }

    public static class NetworkModelDescription {
        public static final String PARAMETERS = "provider specific parameters of the specified network";
        public static final String AWS_PARAMETERS = "provider specific parameters of the specified network";
        public static final String GCP_PARAMETERS = "provider specific parameters of the specified network";
        public static final String AZURE_PARAMETERS = "provider specific parameters of the specified network";
        public static final String OPEN_STACK_PARAMETERS = "provider specific parameters of the specified network";
        public static final String SUBNET_CIDR = "the subnet definition of the network in CIDR format";
    }

    public static class SecurityGroupModelDescription {
        public static final String SECURITY_RULES = "list of security rules that relates to the security group";
        public static final String SECURITY_GROUP_ID = "Exisiting security group id";
        public static final String SECURITY_GROUP_IDS = "Exisiting security group ids";
    }

    public static class SecurityRuleModelDescription {
        public static final String SUBNET = "definition of allowed subnet in CIDR format";
        public static final String PORTS = "list of accessible ports";
        public static final String PROTOCOL = "protocol of the rule";
        public static final String MODIFIABLE = "flag for making the rule modifiable";
    }
    public static class LdapConfigModelDescription {
        public static final String REQUEST = "LDAP config request";
        public static final String RESPONSE = "LDAP config response";
        public static final String SERVER_HOST = "public host or IP address of LDAP server";
        public static final String SERVER_PORT = "port of LDAP server (typically: 389 or 636 for LDAPS)";
        public static final String PROTOCOL = "determines the protocol (LDAP or LDAP over SSL)";
        public static final String BIND_DN = "bind distinguished name for connection test and group search (e.g. cn=admin,dc=example,dc=org)";
        public static final String BIND_PASSWORD = "password for the provided bind DN";
        public static final String USER_SEARCH_BASE = "template for user search for authentication (e.g. dc=hadoop,dc=apache,dc=org)";
        public static final String USER_DN_PATTERN = "template for pattern based user search for authentication (e.g. cn={0},dc=hadoop,dc=apache,dc=org)";
        public static final String GROUP_SEARCH_BASE = "template for group search for authorization (e.g. dc=hadoop,dc=apache,dc=org)";
        public static final String USER_NAME_ATTRIBUTE = "attribute name for simplified search filter (e.g. sAMAccountName in case of AD, UID or cn for LDAP).";
        public static final String DOMAIN = "domain in LDAP server (e.g. ad.seq.com).";
        public static final String DIRECTORY_TYPE = "directory type of server LDAP or ACTIVE_DIRECTORY and the default is ACTIVE_DIRECTORY ";
        public static final String USER_OBJECT_CLASS = "User Object Class (defaults to person)";
        public static final String GROUP_OBJECT_CLASS = "Group Object Class (defaults to groupOfNames)";
        public static final String GROUP_ID_ATTRIBUTE = "Group Id Attribute (defaults to cn)";
        public static final String GROUP_MEMBER_ATTRIBUTE = "Group Member Attribute (defaults to member)";
        public static final String ADMIN_GROUP = "LDAP group for administrators";
        public static final String VALIDATION_REQUEST = "Request that contains the minimal set of fields to test LDAP connectivity";
        public static final String CERTIFICATE = "Self-signed certificate of LDAPS server";
    }

    public static class RDSConfigModelDescription {
        public static final String REQUEST = "RDS config request";
        public static final String RESPONSE = "RDS config response";
        public static final String CLUSTER_NAMES = "list of clusters which use config";
    }

    public static class KerberosConfigModelDescription {
        public static final String REQUEST = "Kerberos config request";
        public static final String RESPONSE = "Kerberos config response";
    }

    public static class TopologyModelDescription {
        public static final String NODES = "topology mapping";
    }

    public static class SubscriptionModelDescription {
        public static final String ENDPOINT = "url of the endpoint";
    }

    public static class SupportedDatabaseModelDescription {
        public static final String DATABASENAME = "Name of the database";
        public static final String DISPLAYNAME = "Display name of the database";
        public static final String JDBCPREFIX = "Jdbc prefix of the database";
        public static final String NAME = "Name of the service";
        public static final String SERVICE_DISPLAYNAME = "Display name of the service";
        public static final String DATABASES = "Supported database list";
        public static final String VERSIONS = "Supported version types currently only for Oracle";
    }

    public static class FailureReport {
        public static final String FAILED_NODES = "List of failed nodes";
    }

    public static class RepairClusterRequest {
        public static final String HOSTGROUPS = "List of hostgroups where the failed nodes will be repaired";
        public static final String REMOVE_ONLY = "If true, the failed nodes will only be removed, otherwise the failed nodes will be removed and "
                + "new nodes will be started.";
        public static final String NODES = "Object consisting of deleteVolumes flag and a list of node IDs which will "
                + "be repaired specifically. The existing disk volumes on the nodes will be re-created if the deleteVolumes flag is true.";
    }

    public static class RepairClusterNodeRequest {
        public static final String DELETE_VOLUMES = "If true, delete volumes, otherwise reattaches them to a newly created node instance.";
        public static final String IDS = "Node ID list of nodes that need replacement.";
    }

    public static class HardwareModelDescription {
        public static final String METADATA = "Metadata of instances.";
    }

    public static class StackAuthenticationBase {
        public static final String PUBLIC_KEY = "public key for accessing instances";
        public static final String LOGIN_USERNAME = "authentication name for machines";
        public static final String PUBLIC_KEY_ID = "public key id for accessing instances";
    }

    public static class ImageCatalogDescription {
        public static final String IMAGE_CATALOG_URL = "custom image catalog's URL";
        public static final String DEFAULT = "true if image catalog is the default one";
        public static final String IMAGE_RESPONSES = "image response in imagecatalog";
    }

    public static class SecurityRulesModelDescription {
        public static final String CORE = "security rules for core type";
        public static final String GATEWAY = "security rules for gateway type";
    }

    public static class RepositoryConfigValidationDescription {
        public static final String FIELDS = "Indicates the request's value with the same key is valid and reachable by Cloudbreak or not";
    }

    public static class UtilDescription {
        public static final String STORAGE_NAME = "Storage name of the path";
        public static final String ACCOUNT_NAME = "Account name of the path";
        public static final String FILESYTEM_TYPE = "Type of filesystem";
        public static final String ATTACHED_CLUSTER = "Attached cluster";
    }

    public static class ProxyConfigModelDescription {
        public static final String DESCRIPTION = "Cloudbreak allows you to save your existing proxy configuration "
                + "information as an external source so that you can provide the proxy information to multiple "
                + "clusters that you create with Cloudbreak";
        public static final String SERVER_HOST = "host or IP address of proxy server";
        public static final String SERVER_PORT = "port of proxy server (typically: 3128 or 8080)";
        public static final String PROTOCOL = "determines the protocol (http or https)";
        public static final String NAME = "Name of the proxy configuration resource";
        public static final String USERNAME = "Username to use for basic authentication";
        public static final String PASSWORD = "Password to use for basic authentication";
    }

    public static class EnvironmentRequestModelDescription {
        public static final String CREDENTIAL_NAME = "Name of the credential of the environment. If the name is given, "
                + "the detailed credential is ignored in the request.";
        public static final String CREDENTIAL = "If credentialName is not specified, the credential is used to create the new credential for the environment.";
        public static final String INTERACTIVE_LOGIN_CREDENTIAL_VERIFICATION_URL = "The url provided by Azure where the user have to use the given user code "
                + "to sign in";
        public static final String INTERACTIVE_LOGIN_CREDENTIAL_USER_CODE = "The user code what has to be used for the sign-in process on the Azure portal";
        public static final String CREDENTIAL_DESCRIPTION = "Credential request related data";
        public static final String PROXY_CONFIGS = "Name of the proxy configurations to be attached to the environment.";
        public static final String RDS_CONFIGS = "Name of the RDS configurations to be attached to the environment.";
        public static final String KUBERNETES_CONFIGS = "Name of the Kubernetes configurations to be attached to the environment.";
        public static final String LDAP_CONFIGS = "Name of the LDAP configurations to be attached to the environment.";
        public static final String REGIONS = "Regions of the environment.";
        public static final String LOCATION = "Location of the environment.";
        public static final String LONGITUDE = "Location longitude of the environment.";
        public static final String LATITUDE = "Location latitude of the environment.";
        public static final String KERBEROS_CONFIGS = "Name of Kerberos configs to be attached to the environment.";
        public static final String LOCATION_DISPLAY_NAME = "Display name of the location of the environment.";
        public static final String NETWORK = "Network related specifics of the environment.";
    }

    public static class EnvironmentResponseModelDescription {
        public static final String CREDENTIAL_NAME = "Name of the credential of the environment.";
        public static final String CREDENTIAL = "Credential of the environment.";
        public static final String PROXY_CONFIGS = "Proxy configurations in the environment.";
        public static final String RDS_CONFIGS = "RDS configurations in the environment.";
        public static final String KUBERNETES_CONFIGS = "Kubernetes configurations in the environment.";
        public static final String LDAP_CONFIGS = "LDAP configurations in the environment.";
        public static final String REGIONS = "Regions of the environment.";
        public static final String CLOUD_PLATFORM = "Cloud platform of the environment.";
        public static final String LOCATION = "Location of the environment.";
        public static final String WORKLOAD_CLUSTERS = "Workload clusters created in the environment.";
        public static final String WORKLOAD_CLUSTER_NAMES = "Names of the workload clusters created in the environment.";
        public static final String DATALAKE_CLUSTERS = "Datalake clusters created in the environment.";
        public static final String DATALAKE_CLUSTER_NAMES = "Names of the datalake clusters created in the environment.";
        public static final String DATALAKE_RESOURCES_NAMES = "Datalake cluster resources registered to the environment.";
        public static final String DATALAKE_RESOURCES = "Datalake cluster resources registered to the environment.";
        public static final String KERBEROS_CONFIGS = "Kerberos configs in the environment.";
        public static final String NETWORK = "Network related specifics of the environment.";
    }

    public static class SecretResponseModelDescription {
        public static final String ENGINE_PATH = "Engine path of the secret.";
        public static final String SECRET_PATH = "Path of the secret.";
    }

    public static class DatalakeResourcesDescription {
        public static final String SERVICE_DESCRIPTORS = "Descriptors of the datalake services";
        public static final String SERVICE_NAME = "Name of the datalake service";
        public static final String BLUEPRINT_PARAMS = "Bluepirnt parameters from the datalake services";
        public static final String COMPONENT_HOSTS = "Component hosts of the datalake services";
    }

    public static class EnvironmentNetworkDescription {
        public static final String SUBNET_IDS = "Subnet ids of the specified networks";
        public static final String AWS_SPECIFIC_PARAMETERS = "Subnet ids of the specified networks";
        public static final String AZURE_SPECIFIC_PARAMETERS = "Subnet ids of the specified networks";
        public static final String AWS_VPC_ID = "Subnet ids of the specified networks";
        public static final String AZURE_RESOURCE_GROUP_NAME = "Subnet ids of the specified networks";
        public static final String AZURE_NETWORK_ID = "Subnet ids of the specified networks";
        public static final String AZURE_NO_PUBLIC_IP = "Subnet ids of the specified networks";
        public static final String AZURE_NO_FIREWALL_RULES = "Subnet ids of the specified networks";
    }
}
