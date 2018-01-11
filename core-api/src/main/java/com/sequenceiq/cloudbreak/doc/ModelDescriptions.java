package com.sequenceiq.cloudbreak.doc;

public class ModelDescriptions {

    public static final String ID = "id of the resource";
    public static final String NAME = "name of the resource";
    public static final String DESCRIPTION = "description of the resource";
    public static final String PUBLIC_IN_ACCOUNT = "resource is visible in account";
    public static final String CLOUD_PLATFORM = "type of cloud provider";
    public static final String OWNER = "id of the resource owner that is provided by OAuth provider";
    public static final String ACCOUNT = "account id of the resource owner that is provided by OAuth provider";
    public static final String TOPOLOGY_ID = "id of the topology the resource belongs to";
    public static final String REQUESTS = "request object";
    public static final String RESPONSE = "response object";
    public static final String CREATED = "creation time of the resource in long";
    public static final String AMBARI_SERVER = "ambari server address";

    private ModelDescriptions() {
    }

    public static class BlueprintModelDescription {
        public static final String URL = "url source of an ambari blueprint, set this or the ambariBlueprint field";
        public static final String BLUEPRINT_NAME = "gathered from blueprintName field from the blueprint JSON";
        public static final String AMBARI_BLUEPRINT = "ambari blueprint JSON, set this or the url field";
        public static final String HOST_GROUP_COUNT = "number of host groups";
        public static final String STATUS = "status of the blueprint";
        public static final String INPUTS = "input parameters of the blueprint";
        public static final String BLUEPRINT_PROPERTIES = "properties to extend the blueprint with";
    }

    public static class CredentialModelDescription {
        public static final String PARAMETERS = "cloud specific parameters for credential";
    }

    public static class OrchestratorModelDescription {
        public static final String TYPE = "type of the orchestrator";
        public static final String PARAMETERS = "orchestrator specific parameters, like authentication details";
        public static final String ENDPOINT = "endpoint for the container orchestration api";
    }

    public static class TemplateModelDescription {
        public static final String VOLUME_COUNT = "number of volumes";
        public static final String VOLUME_SIZE = "size of volumes";
        public static final String VOLUME_TYPE = "type of the volumes";
        public static final String INSTANCE_TYPE = "type of the instance";
        public static final String CUSTOM_INSTANCE_TYPE = "custom instancetype definition";
        public static final String PARAMETERS = "cloud specific parameters for template";
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
        public static final String BLUEPRINT_NAME = "name that could indentify an existing blueprint";
        public static final String BLUEPRINT_ID = "id that could indentify an existing blueprint";
    }

    public static class StackModelDescription {
        public static final String STACK_ID = "id of the stack";
        public static final String IMAGE = "image of the stack";
        public static final String STACK_NAME = "name of the stack";
        public static final String REGION = "region of the stack";
        public static final String AVAILABILITY_ZONE = "availability zone of the stack";
        public static final String CREDENTIAL_ID = "credential resource id for the stack";
        public static final String CREDENTIAL_NAME = "credential resource name for the stack";
        public static final String USERNAME = "ambari username";
        public static final String PASSWORD = "ambari password";
        public static final String ENABLE_SECURITY = "enable Kerberos security";
        public static final String KERBEROS_MASTER_KEY = "kerberos master key";
        public static final String KERBEROS_ADMIN = "kerberos admin user";
        public static final String KERBEROS_PASSWORD = "kerberos admin password";
        public static final String KERBEROS_KDC_URL = "kerberos KDC server URL";
        public static final String KERBEROS_ADMIN_URL = "kerberos admin server URL";
        public static final String KERBEROS_PRINCIPAL = "kerberos principal";
        public static final String PARAMETERS = "additional cloud specific parameters for stack";
        public static final String FAILURE_ACTION = "action on failure";
        public static final String FAILURE_POLICY = "failure policy in case of failures";
        public static final String STACK_STATUS = "status of the stack";
        public static final String STATUS_REASON = "status message of the stack";
        public static final String STATUS_REQUEST = "status of the scale request";
        public static final String AMBARI_IP = "public ambari ip of the stack";
        public static final String AMBARI_URL = "public ambari url";
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
        public static final String INSTANCE_GROUPS = "collection of instance groupst";
        public static final String IMAGE_SETTINGS = "settings for custom images";
        public static final String IMAGE_CATALOG = "custom image catalog URL";
        public static final String IMAGE_ID = "virtual machine image id from ImageCatalog, machines of the cluster will be started from this image";
        public static final String INSTANCE_GROUP_ADJUSTMENT = "instance group adjustment";
        public static final String TAGS = "stack related tags";
        public static final String APPLICATION_TAGS = "stack related application tags";
        public static final String DEFAULT_TAGS = "stack related default tags";
        public static final String USERDEFINED_TAGS = "stack related userdefined tags";
        public static final String CREDENTIAL = "stack related credential";
        public static final String AUTHENTICATION = "stack related authentication";
        public static final String FILESYSTEM = "cluster related filesystem";
        public static final String NETWORK = "stack related network";
        public static final String FLEX_ID = "id of the related flex subscription";
        public static final String SOURCE_CREDENTIAL = "source credential object for cloning";
        public static final String CLUSTER_REQUEST = "cluster request object on stack";
        public static final String FLEX_SUBSCRIPTION = "the related flex subscription";
        public static final String NODE_COUNT = "node count of the stack";
        public static final String HARDWARE_INFO_RESPONSE = "hardware information where pairing hostmetadata with instancemetadata";
        public static final String USAGES = "usage information for a specific stack";
        public static final String EVENTS = "related events for a cloudbreak stack";
        public static final String CUSTOM_DOMAIN = "custom domain name for the nodes in the stack";
        public static final String CUSTOM_HOSTNAME = "custom hostname for nodes in the stack";
        public static final String CLUSTER_NAME_AS_SUBDOMAIN = "using the cluster name to create subdomain";
        public static final String HOSTGROUP_NAME_AS_HOSTNAME = "using the hostgroup names to create hostnames";
        public static final String CUSTOM_DOMAIN_SETTINGS = "settings related to custom domain names";
        public static final String GENERAL_SETTINGS = "general configuration parameters for a cluster (e.g. 'name', 'credentialname')";
        public static final String PLACEMENT_SETTINGS = "placement configuration parameters for a cluster (e.g. 'region', 'availabilityZone')";
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
        public static final String VALIDATE_BLUEPRINT = "validate blueprint";
        public static final String HOURS = "duration - how long the cluster is running in hours";
        public static final String MINUTES = "duration - how long the cluster is running in minutes (minus hours)";
        public static final String EMAIL_TO = "send email to the requested address";
        public static final String EMAIL_NEEDED = "send email about the result of the cluster installation";
        public static final String SERVICE_ENDPOINT_MAP = "most important services in the cluster";
        public static final String CONFIG_STRATEGY = "config recommendation strategy";
        public static final String BLUEPRINT_INPUTS = "blueprint inputs in the cluster";
        public static final String BLUEPRINT_CUSTOM_PROPERTIES = "blueprint custom properties";
        public static final String CUSTOM_CONTAINERS = "custom containers";
        public static final String CUSTOM_QUEUE = "custom queue for yarn orchestrator";
        public static final String EXECUTOR_TYPE = "executor type of cluster";
        public static final String CONNECTED_CLUSTER = "cluster can connect to a datalake you can define the parameters here";
        public static final String CLUSTER_ATTRIBUTES = "Additional information for ambari cluster";
        public static final String RDSCONFIG_IDS = "RDS configuration ids for the cluster";
        public static final String RDSCONFIGS = "RDS configurations for the cluster";
        public static final String LDAP_CONFIG_ID = "LDAP config id for the cluster";
        public static final String LDAP_CONFIG_NAME = "LDAP config name for the cluster";
        public static final String LDAP_CONFIG = "LDAP config for the cluster";
        public static final String HOSTGROUP_ADJUSTMENT = "host group adjustment";
        public static final String ADJUSTMENT_TYPE = "type of  adjustment";
        public static final String STATUS_REQUEST = "request status";
        public static final String USERNAME_PASSWORD = "user details";
        public static final String AMBARI_REQUEST = "ambari specific requests";
        public static final String HOSTGROUPS = "collection of hostgroups";
        public static final String AMBARI_STACK_DETAILS = "details of the Ambari stack";
        public static final String AMBARI_REPO_DETAILS = "details of the Ambari package repository";
        public static final String AMBARI_DATABASE_DETAILS = "details of the external Ambari database";
        public static final String AMBARI_DATABASE_ERROR = "result of Ambari database test";
        public static final String RDS_CONFIGS = "details of the external database for Hadoop components";
        public static final String RDS_CONNECTION_RESULT = "result of RDS connection test";
        public static final String LDAP_CONNECTION_RESULT = "result of Ldap connection test";
        public static final String CREATION_FINISHED = "Epoch time of cluster creation finish";
        public static final String AMBARI_SECURITY_MASTER_KEY = "a master key for encrypting the passwords in Ambari";
    }

    public static class GatewayModelDescription {
        public static final String ENABLE_KNOX_GATEWAY = "enable Knox gateway security";
        public static final String KNOX_PATH = "Knox gateway path";
        public static final String KNOX_GATEWAY_TYPE = "Knox gateway type";
        public static final String KNOX_SSO_TYPE = "Knox SSO type";
        public static final String KNOX_TOPOLOGY_NAME = "Knox topology name";
        public static final String EXPOSED_KNOX_SERVICES = "exposed Knox services";
        public static final String KNOX_SSO_PROVIDER = "SSO provider cluster name";
        public static final String KNOX_SSO_CERT = "SSO Provider certificate";
    }

    public static class ClusterTemplateModelDescription {
        public static final String NAME = "name of the cluster template";
        public static final String TEMPLATE = "stringified template JSON";
        public static final String TYPE = "type of the cluster template";
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
        public static final String AVAILABILITY_ZONES = "availability zones";
        public static final String DEFAULT_REGIOS = "default regions";
    }

    public static class AmbariRepoDetailsDescription {
        public static final String VERSION = "version of the Ambari";
        public static final String AMBARI_BASE_URL = "url of the Ambari repository";
        public static final String AMBARI_REPO_GPG_KEY = "gpg key of the Ambari repository";
    }

    public static class AmbariDatabaseDetailsDescription {
        public static final String VENDOR = "vendor of the Ambari database";
        public static final String NAME = "name of the Ambari database";
        public static final String HOST = "host of the Ambari database";
        public static final String PORT = "port of the Ambari database";
        public static final String USER_NAME = "user name for the Ambari database";
        public static final String PASSWORD = "password for the Ambari database";
    }

    public static class AmbariStackDetailsDescription {
        public static final String STACK = "name of the stack, like HDP";
        public static final String VERSION = "version of the stack";
        public static final String OS = "operating system for the stack, like redhat6";
        public static final String STACK_REPO_ID = "id of the stack repository";
        public static final String UTILS_REPO_ID = "id of the stack utils repository";
        public static final String STACK_BASE_URL = "url of the stack repository";
        public static final String UTILS_BASE_URL = "url of the stack utils repository";
        public static final String VERIFY = "whether to verify or not the repo url";
        public static final String REPOSITORY_VERSION = "version of the repository for VDF file creation in Ambari";
        public static final String VDF_URL = "local path on the Ambari server or URL that point to the desired VDF file";
        public static final String MPACK_URL = "url the MPACK that needs to be installed before HDF installation";
    }

    public static class RDSConfig {
        public static final String CONNECTION_URL = "JDBC connection URL in the form of jdbc:<db-type>://<address>:<port>/<db>";
        public static final String DB_TYPE = "Type of the external database (allowed values: MYSQL, POSTGRES)";
        public static final String USERNAME = "Username to use for the jdbc connection";
        public static final String PASSWORD = "Password to use for the jdbc connection";
        public static final String NAME = "Name of the RDS configuration resource";
        public static final String HDPVERSION = "HDP version for the RDS configuration";
        public static final String VALIDATED = "If true, then the RDS configuration will be validated";
        public static final String RDSTYPE = "Type of rds (HIVE or RANGER)";
        public static final String RDS_PROPERTIES = "custom properties for rds connection";
        public static final String RDS_REQUEST = "rds config request";
        public static final String RDS_REQUEST_CLUSTER_NAME = "requested cluster name";
    }

    public static class FileSystem {
        public static final String NAME = "name of the filesystem";
        public static final String TYPE = "type of the filesystem";
        public static final String DEFAULT = "true if fs.defaultFS should point to this filesystem";
        public static final String PROPERTIES = "configuration of the filesystem access as key-value pairs";
    }

    public static class RecipeModelDescription {
        public static final String CONTENT = "content of recipe";
        public static final String TYPE = "type of recipe";
        public static final String RECIPE_URI = "recipe uri";
    }

    public static class InstanceGroupModelDescription {
        public static final String INSTANCE_GROUP_NAME = "name of the instance group";
        public static final String INSTANCE_GROUP_TYPE = "type of the instance group";
        public static final String TEMPLATE = "instancegroup related template";
        public static final String SECURITYGROUP = "instancegroup related securitygroup";
        public static final String NODE_COUNT = "number of nodes";
        public static final String TEMPLATE_ID = "referenced template id";
        public static final String INSTANCE_TYPE = "type of the instance";
        public static final String STATUS = "status of the instance";
        public static final String SECURITY_GROUP_ID = "security group resource id for the instance group";
        public static final String METADATA = "metadata of instances";
        public static final String PARAMETERS = "cloud specific parameters for instance group";
    }

    public static class InstanceGroupAdjustmentModelDescription {
        public static final String SCALING_ADJUSTMENT = "scaling adjustment of the instance groups";
        public static final String WITH_CLUSTER_EVENT = "on stack update, update cluster too";
    }

    public static class HostGroupModelDescription {
        public static final String RECIPE_IDS = "referenced recipe ids";
        public static final String RECIPE_NAMES = "referenced recipe names";
        public static final String RECIPES = "referenced recipes";
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

    public static class UsageModelDescription {
        public static final String PROVIDER = "cloud provider of the stack";
        public static final String COSTS = "computed costs of instance usage";
        public static final String DAY = "the day the usage of resources happened";
        public static final String INSTANCE_HOURS = "hours since the instance is running";
        public static final String INSTANCE_TYPE = "type of instance";
        public static final String INSTANCE_GROUP = "group name of instance";
        public static final String BLUEPRINT_ID = "id of the blueprint";
        public static final String BLUEPRINT_NAME = "name of the blueprint";
        public static final String DURATION = "time since the instances are running in millisec";
        public static final String INSTANCE_NUMBER = "number of instances running";
        public static final String PEAK = "maximum number of instances running";
        public static final String FLEX_ID = "flex subscription id";
        public static final String STACK_UUID = "unique id of the cluster";
    }

    public static class EventModelDescription {
        public static final String TYPE = "type of the event";
        public static final String TIMESTAMP = "timestamp of the event";
        public static final String MESSAGE = "message of the event";
    }

    public static class NetworkModelDescription {
        public static final String PARAMETERS = "provider specific parameters of the specified network";
        public static final String SUBNET_CIDR = "the subnet definition of the network in CIDR format";
    }

    public static class SecurityGroupModelDescription {
        public static final String SECURITY_RULES = "list of security rules that relates to the security group";
        public static final String SECURITY_GROUP_ID = "Exisiting security group id";
    }

    public static class SecurityRuleModelDescription {
        public static final String SUBNET = "definition of allowed subnet in CIDR format";
        public static final String PORTS = "comma separated list of accessible ports";
        public static final String PROTOCOL = "protocol of the rule";
        public static final String MODIFIABLE = "flag for making the rule modifiable";
    }

    public static class AccountPreferencesModelDescription {
        public static final String MAX_NO_CLUSTERS = "max number of clusters in the account (0 when unlimited)";
        public static final String MAX_NO_NODES_PER_CLUSTER = "max number of vms in a cluster of account (0 when unlimited)";
        public static final String MAX_NO_CLUSTERS_PER_USER = "max number of clusters for user within the account (0 when unlimited)";
        public static final String ALLOWED_INSTANCE_TYPES = "allowed instance types in the account (empty list for no restriction)";
        public static final String CLUSTER_TIME_TO_LIVE = "lifecycle of the cluster in hours (0 for immortal clusters)";
        public static final String ACCOUNT_TIME_TO_LIVE = "lifecycle of the account and its clusters in hours (0 for immortal account)";
        public static final String PLATFORMS = "list of the cloudplatforms visible on the UI";
        public static final String DEFAULT_TAGS = "default tags for the resources created";
    }

    public static class LdapConfigModelDescription {
        public static final String SERVER_HOST = "public host or IP address of LDAP server";
        public static final String SERVER_PORT = "port of LDAP server (typically: 389 or 636 for LDAPS)";
        public static final String PROTOCOL = "determines the protocol (LDAP or LDAP over SSL)";
        public static final String BIND_DN = "bind distinguished name for connection test and group search (e.g. cn=admin,dc=example,dc=org)";
        public static final String BIND_PASSWORD = "password for the provided bind DN";
        public static final String USER_SEARCH_BASE = "template for user search for authentication (e.g. dc=hadoop,dc=apache,dc=org)";
        public static final String GROUP_SEARCH_BASE = "template for group search for authorization (e.g. dc=hadoop,dc=apache,dc=org)";
        public static final String USER_NAME_ATTRIBUTE = "attribute name for simplified search filter (e.g. sAMAccountName in case of AD, UID or cn for LDAP).";
        public static final String DOMAIN = "domain in LDAP server (e.g. ad.seq.com).";
        public static final String DIRECTORY_TYPE = "directory type of server LDAP or AD";
        public static final String USER_OBJECT_CLASS = "User Object Class (defaults to person)";
        public static final String GROUP_OBJECT_CLASS = "Group Object Class (defaults to groupOfNames)";
        public static final String GROUP_ID_ATTRIBUTE = "Group Id Attribute (defaults to cn)";
        public static final String GROUP_MEMBER_ATTRIBUTE = "Group Member Attribute (defaults to member)";
        public static final String ADMIN_GROUP = "LDAP group for administrators";
    }

    public static class RDSConfigModelDescription {
        public static final String CLUSTER_NAMES = "list of clusters which use config";
    }

    public static class TopologyModelDescription {
        public static final String NODES = "topology mapping";
    }

    public static class SubscriptionModelDescription {
        public static final String ENDPOINT = "url of the endpoint";
    }

    public static class FailureReport {
        public static final String FAILED_NODES = "List of failed nodes";
    }

    public static class RepairClusterRequest {
        public static final String HOSTGROUPS = "List of hostgroups where the failed nodes will be repaired";
        public static final String REMOVE_ONLY = "If true, the failed nodes will only be removed, otherwise the failed nodes will be removed and "
                + "new nodes will be started.";
    }

    public static class SmartSenseSubscriptionModelDescription {
        public static final String SUBSCRIPTION_ID = "Identifier of SmartSense subscription.";
        public static final String AUTOGENERATED = "Flag of aut generated SmartSense subscription.";
    }

    public static class FlexSubscriptionModelDescription {
        public static final String FLEX_SUBSCRIPTION_ID = "Identifier of Flex subscription.";
        public static final String SMARTSENSE_SUBSCRIPTION_ID = "Identifier of SmartSense subscription Cloudbreak domain object json representation.";
        public static final String SMARTSENSE_SUBSCRIPTION = "The associated SmartSense subscription Cloudbreak domain object json representation.";
        public static final String IS_DEFAULT = "true if the flex subscription is the default one";
        public static final String USED_FOR_CONTROLLER = "true if the flex subscription was used for the controller";
    }

    public static class StackAuthenticationBase {
        public static final String PUBLIC_KEY = "public key for accessing instances";
        public static final String LOGIN_USERNAME = "authentication name for machines";
        public static final String PUBLIC_KEY_ID = "public key id for accessing instances";
    }

    public static class ImageCatalogDescription {
        public static final String IMAGE_CATALOG_URL = "custom image catalog's URL";
        public static final String DEFAULT = "true if image catalog is the default one";
    }

    public static class SecurityRulesModelDescription {
        public static final String CORE = "security rules for core type";
        public static final String GATEWAY = "security rules for gateway type";
    }
}
