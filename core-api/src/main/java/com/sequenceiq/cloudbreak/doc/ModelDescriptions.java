package com.sequenceiq.cloudbreak.doc;

public class ModelDescriptions {

    public static final String ID = "id of the resource";
    public static final String NAME = "name of the resource";
    public static final String DESCRIPTION = "description of the resource";
    public static final String ATTRIBUTES = "provider specific attributes of the credential";
    public static final String CLOUD_PLATFORM = "type of cloud provider";
    public static final String ARCHITECTURE = "CPU architecture allowedValues: { X86_64, ARM64 }";
    public static final String CREATED = "creation time of the resource in long";
    public static final String AMBARI_SERVER = "ambari server address";
    public static final String WORKSPACE_OF_THE_RESOURCE = "workspace of the resource";
    public static final String CREATOR = "the creator of the resource - Deprecated: data owner of any user in UMS, " +
            "creator should not be stored and used anywhere, since user of creator can leave the given company and can become invalid, " +
            "usage of it can be error prone";
    public static final String CRN = "the unique crn of the resource";
    public static final String CRNS = "the unique crns of the resource";
    public static final String USER_ID = "User ID in the new authorization model";
    public static final String WORKSPACE_ID = "Workspace ID of the resource";
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
        public static final String TAGS = "user defined tags for blueprint";
        public static final String UPGRADEABLE = "marks if combination of the blueprint version and services enables upgrade";
        public static final String STACK_TYPE = "The type of the stack: for example HDP or HDF";
        public static final String STACK_VERSION = "The version of the stack";
    }

    public static class TemplateModelDescription {
        public static final String VOLUME_COUNT = "number of volumes";
        public static final String VOLUME_SIZE = "size of volume";
        public static final String ROOT_VOLUME_SIZE = "size of the root volume";
        public static final String VOLUME_TYPE = "type of the volumes";
        public static final String DATABASE_VOLUME_SIZE = "size of the database volume";
        public static final String DATABASE_VOLUME_TYPE = "type of the database volume";
        public static final String INSTANCE_TYPE = "type of the instance";
        public static final String AWS_PARAMETERS = "aws specific parameters for template";
        public static final String GCP_PARAMETERS = "gcp specific parameters for template";
        public static final String OPENSTACK_PARAMETERS_DEPRECATED = "openstack specific parameters for template [DEPRECATED]";
        public static final String YARN_PARAMETERS = "yarn specific parameters for template";
        public static final String AZURE_PARAMETERS = "azure specific parameters for template";
        public static final String AWS_SPOT_PARAMETERS = "aws specific spot instance parameters for template";
        public static final String AWS_PLACEMENT_GROUP = "aws specific placement group parameter for vm";
        public static final String AWS_PLACEMENT_GROUP_STRATEGY = "aws placement group strategy for vm";
        public static final String SPOT_PERCENTAGE = "percentage of spot instances launched in instance group";
        public static final String SPOT_MAX_PRICE = "max price per hour of spot instances launched in instance group";
        public static final String AZURE_PRIVATE_ID = "private id for azure";
        public static final String ENCRYPTION = "encryption for vm";
        public static final String ENCRYPTION_TYPE = "encryption type for vm (DEFAULT|CUSTOM|NONE)";
        public static final String ENCRYPTION_KEY = "encryption key for vm";
        public static final String ENCRYPTION_METHOD = "encryption method for the key (RAW|RSA)";
        public static final String DISK_ENCRYPTION_SET_ID = "disk encryption set Resource ID";
        public static final String ENCRYPTION_AT_HOST_ENABLED = "boolean determining if VM should be encrypted at host";
    }

    public static class ImageModelDescription {
        public static final String IMAGE_NAME = "name of the image";
        public static final String IMAGE_CATALOG_URL = "url of the image catalog";
        public static final String IMAGE_ID = "id of the image";
        public static final String IMAGE_CATALOG_NAME = "name of the image catalog";
        public static final String IMAGE_TYPE = "type of the image - datalake, datahub or freeipa";
        public static final String SOURCE_IMAGE_ID = "id of the source image serving as the base of the customized image";
        public static final String PARCEL_BASE_URL = "custom base url for parcels in case of a customized image";
        public static final String VMS_TO_REGIONS = "VM image references mapped to cloud regions";
        public static final String PARCEL_NAME = "display name of the parcel";
        public static final String PARCEL_VERSION = "version of the parcel";
        public static final String PARCEL_BUILD_NUMBER = "build number of the parcel";
    }

    public static class CustomImageDescription {
        public static final String IMAGE_ID = "id of the image";
        public static final String IMAGE_TYPE = "type of the image - datalake, datahub or freeipa";
        public static final String SOURCE_IMAGE_ID = "id of the source image serving as the base of the customized image";
        public static final String BASE_PARCEL_URL = "base url of of parcels in the image";
        public static final String VM_IMAGES = "image references of the custom image in different regions";
    }

    public static class VmImageDescription {
        public static final String REGION = "vm image region";
        public static final String IMAGE_REFERENCE = "vm image reference";
    }

    public static class CloudbreakDetailsModelDescription {
        public static final String VERSION = "version of the Cloudbreak that provisioned the stack";
    }

    public static class StackModelDescription {
        public static final String TENANANT = "name of the tenant";
        public static final String CRN = "the unique crn of the resource";
        public static final String TYPE = "the typeof the resource";
        public static final String WORKSPACE_ID = "id of the workspace";
        public static final String USER_ID = "id of the user";
        public static final String USER_CRN = "crn of the user";
        public static final String STACK_ID = "id of the stack";
        public static final String CUSTOM = "AmbariKerberosDescriptor parameters as a json";
        public static final String ENTRIES = "Entries parameters as a json";
        public static final String IMAGE = "image of the stack";
        public static final String STACK_NAME = "name of the stack";
        public static final String REGION = "region of the stack";
        public static final String AVAILABILITY_ZONE = "availability zone of the stack";
        public static final String CREDENTIAL_NAME = "credential resource name for the stack";
        public static final String CUSTOM_CONFIGURATIONS_NAME = "custom configurations name for the stack";
        public static final String CUSTOM_CONFIGURATIONS_CRN = "custom configurations crn for the stack";
        public static final String USERNAME = "ambari username";
        public static final String PASSWORD = "ambari password";
        public static final String INPUTS = "dynamic properties";
        public static final String STACK_STATUS = "status of the stack";
        public static final String STATUS_REASON = "status message of the stack";
        public static final String STATUS_REQUEST = "status of the scale request";
        public static final String SERVER_IP = "public ambari ip of the stack";
        public static final String SERVER_FQDN = "FQDN of the gateway node for the stack";
        public static final String SERVER_URL = "public ambari url";
        public static final String CM_MANAGEMENT_USERNAME = "CM username for shared usage";
        public static final String CM_MANAGEMENT_PASSWORD = "CM password for shared usage";
        public static final String NETWORK_ID = "network resource id for the stack";
        public static final String CERTIFICATE = "server certificate used by the gateway";
        public static final String CLIENT_KEY = "client key used by the gateway";
        public static final String CLIENT_CERT = "client certificate used by the gateway";
        public static final String CLUSTER_STATUS = "status of the cluster";
        public static final String CREATED = "creation time of the stack in long";
        public static final String GATEWAY_PORT = "port of the gateway secured proxy";
        public static final String HDP_VERSION = "specific version of HDP";
        public static final String CLOUDBREAK_DETAILS = "details of the Cloudbreak that provisioned the stack";
        public static final String SALT_CB_VERSION = "cloudbreak version stored by the salt state component of the cluster";
        public static final String SUBNET_ID = "cluster subnet id";
        public static final String CLOUD_STORAGE = "external cloud storage configuration";
        public static final String INSTANCE_GROUPS = "collection of instance groups";
        public static final String IMAGE_SETTINGS = "settings for custom images";
        public static final String IMAGE_CATALOG = "custom image catalog URL";
        public static final String IMAGE_ID = "virtual machine image id from ImageCatalog, machines of the cluster will be started from this image";
        public static final String IMAGE_OS = "os of the image, this property is only considered when no specific image id is provided";
        public static final String INSTANCE_GROUP_ADJUSTMENT = "instance group adjustment";
        public static final String TAGS = "stack related tags";
        public static final String APPLICATION_TAGS = "stack related application tags";
        public static final String DEFAULT_TAGS = "stack related default tags";
        public static final String USERDEFINED_TAGS = "stack related userdefined tags";
        public static final String TELEMETRY = "stack related telemetry settings";
        public static final String CREDENTIAL = "stack related credential";
        public static final String AUTHENTICATION = "stack related authentication";
        public static final String NETWORK = "stack related network";
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
        public static final String PLACEMENT_SETTINGS = "placement configuration parameters for a cluster (e.g. 'region', 'availabilityZone')";
        public static final String ENVIRONMENT_CRN = "CRN of the environment which the stack is assigned to";
        public static final String STACK_VERSION = "Runtime version of the cluster";
        public static final String RESOURCE_CRN = "CRN of the cluster if there is corresponding cluster in another service (for example in SDX)";
        public static final String TERMINATED = "termination completion time of stack in long";
        public static final String AWS_PARAMETERS = "aws specific parameters for stack";
        public static final String AZURE_PARAMETERS = "azure specific parameters for stack";
        public static final String GCP_PARAMETERS = "gcp specific parameters for stack";
        public static final String YARN_PARAMETERS = "yarn specific parameters for stack";
        public static final String OPENSTACK_PARAMETERS_DEPRECATED = "openstack specific parameters for stack [DEPRECATED]";
        public static final String CLOUD_PLATFORM = "Cloudplatform of the stack";
        public static final String VARIANT = "Variant of the stack";
        public static final String TUNNEL = "Configuration that the connection going directly or with cluster proxy or with ccm and cluster proxy.";
        public static final String FLOW_ID = "Flow identifier for the current stack creation. Only returned during the stack create request/response.";
        public static final String EXTERNAL_DATABASE = "External database parameters for the stack.";
        public static final String ENABLE_LOAD_BALANCER = "Enable load balancer.";
        public static final String LOAD_BALANCER = "Any load balancers routing traffic both to and inside of the cluster.";
        public static final String LOAD_BALANCER_FQDN = "The registered FQDN of the load balancer.";
        public static final String LOAD_BALANCER_CLOUD_DNS = "The AWS generated DNS name for the load balancer.";
        public static final String LOAD_BALANCER_IP = "The frontend ip address for the load balancer.";
        public static final String LOAD_BALANCER_TARGETS = "The list of target instances the load balancer routes traffic to.";
        public static final String LOAD_BALANCER_TYPE = "Whether the load balancer is internet-facing (public), or only accessible over private endpoints.";
        public static final String LOAD_BALANCER_AWS = "The AWS resource id for the load balancer.";
        public static final String LOAD_BALANCER_SKU = "The SKU to be used for the Azure load balancer.";
        public static final String TARGET_GROUP_PORT = "The port where the load balancer receives traffic and forward it to the associated targets.";
        public static final String TARGET_GROUP_INSTANCES = "Ids for the target instances receiving traffic from the load balancer on the defined port.";
        public static final String TARGET_GROUP_AWS = "The AWS listener and target group resource ids.";
        public static final String AWS_LB_ARN = "The ARN of the AWS load balancer.";
        public static final String AWS_LISTENER_ARN = "The ARN of the AWS listener for the specified port.";
        public static final String AWS_TARGETGROUP_ARN = "The ARN of the AWS target group for the specified port.";
        public static final String LOAD_BALANCER_AZURE = "The Azure load balancer cloud resource information.";
        public static final String TARGET_GROUP_AZURE = "The Azure target availability set information.";
        public static final String AZURE_LB_NAME = "The Azure Load Balancer name";
        public static final String AZURE_LB_SKU = "The Azure Load Balancer SKU";
        public static final String AZURE_LB_AVAILABILITY_SET = "The availability set that contains the instances recieving traffic from the load balancer " +
                "on the specificed port.";
        public static final String LOAD_BALANCER_GCP = "The GCP load balancer information";
        public static final String GCP_LOAD_BALANCER_NAME = "The name in GCP of the load balancer forwarding rule";
        public static final String GCP_LB_INSTANCE_GROUP = "The instances listed belong to any group receiving traffic from the loadbalancer for this port";
        public static final String GCP_LB_BACKEND_SERVICE = "The backend service that assigns this instance group to the load balancer";
        public static final String JAVA_VERSION = "Java version to be forced on virtual machines";

        public static final String SUPPORTED_IMDS_VERSION = "IMDS version supported by the given stack.";
        public static final String MULTIPLE_AVAILABILITY_ZONES = "Indicates the enablement of the multiple availability zones functionality on the stack level";
        public static final String ATTACHED_RESOURCES = "List of resources attached to the stack";
        public static final String SECURITY = "Security related objects";
        public static final String PROVIDER_SYNC_STATES = "Contains information about provider issues. No issues found if empty or 'VALID'.";
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
        public static final String SHARED_SERVICE = "shared service for a specific stack";
        public static final String FILESYSTEM = "filesystem for a specific stack";
        public static final String HOURS = "duration - how long the cluster is running in hours";
        public static final String MINUTES = "duration - how long the cluster is running in minutes (minus hours)";
        public static final String CLUSTER_EXPOSED_SERVICES = "cluster exposed services for topologies";
        public static final String CUSTOM_CONTAINERS = "custom containers";
        public static final String CUSTOM_QUEUE = "custom queue for yarn orchestrator";
        public static final String EXECUTOR_TYPE = "executor type of cluster";
        public static final String CLUSTER_ATTRIBUTES = "Additional information for ambari cluster";
        public static final String RDSCONFIG_NAMES = "RDS configuration names for the cluster";
        public static final String REDBEAMS_DB_SERVER_CRN = "Contains valid Crn for a redbeams database server";
        public static final String DATABASES = "Database configurations for the cluster";
        public static final String HOSTGROUP_ADJUSTMENT = "host group adjustment";
        public static final String STATUS_REQUEST = "request status";
        public static final String USERNAME_PASSWORD = "user details";
        public static final String CM_REQUEST = "cloudera manager specific requests";
        public static final String HOSTGROUPS = "collection of hostgroups";
        public static final String CM_REPO_DETAILS = "details of the Cloudera Manager package repository";
        public static final String CM_PRODUCT_DETAILS = "list of Cloudera Manager product detials";
        public static final String CM_ENABLE_AUTOTLS = "Enable autotls on clusters generated by Cloudera Manager";
        public static final String CREATION_FINISHED = "Epoch time of cluster creation finish";
        public static final String UPTIME = "duration - how long the cluster is running in milliseconds";
        public static final String PROXY_NAME = "proxy configuration name for the cluster";
        public static final String PROXY_CRN = "proxy CRN for the cluster";
        public static final String LOCATIONS = "cloud storage locations";
        public static final String STATUS_MAINTENANCE_MODE = "maintenance mode status";
        public static final String AWS_PARAMETERS = "aws specific parameters for cloud storage";
        public static final String AZURE_PARAMETERS = "azure specific parameters for cloud storage";
        public static final String GCP_PARAMETERS = "gcp specific parameters for cloud storage";
        public static final String VARIANT = "Cluster manager variant";
        public static final String ENABLE_RANGER_RAZ = "Enables Ranger Raz for the cluster on S3, ADLSv2 and GCS.";

        public static final String ENABLE_RANGER_RMS = "Enables Ranger Rms for the cluster on S3";
        public static final String CERTIFICATE_ROTATION_TYPE = "the type of the certificate rotation";
        public static final String CERT_EXPIRATION = "Indicates the certificate status on the cluster";
        public static final String CERT_EXPIRATION_DETAILS = "Provide certificate expiration details, especially days left until expiration";
        public static final String ROTATE_SALT_PASSWORD_REASON = "Indicates why the salt password rotation was started";

        public static final String FLAG_SKIP_SALT_UPDATE = "Flag to indicate if salt update should be skipped or not.";
        public static final String DATABASE_SSL_ENABLED = "Whether database SSL is enabled on this cluster or not.";
    }

    public static class GatewayModelDescription {
        public static final String KNOX_PATH = "Knox gateway path";
        public static final String KNOX_GATEWAY_TYPE = "Knox gateway type";
        public static final String KNOX_SSO_TYPE = "Knox SSO type";
        public static final String KNOX_TOPOLOGY_NAME = "Knox topology name";
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
        public static final String DISK_TYPES = "disk types";
        public static final String DEFAULT_DISKS = "default disks";
        public static final String DISK_MAPPINGS = "disk mappings";
        public static final String DISK_DISPLAYNAMES = "disk displayNames";
        public static final String TAG_SPECIFICATIONS = "tag specifications";
        public static final String VIRTUAL_MACHNES = "virtual machines";
        public static final String DEFAULT_VIRTUAL_MACHINES = "default virtual machines";
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
        public static final String DISPLAY_NAME = "display name of the Cloudera manager product - contains the name and the version";
        public static final String VERSION = "version of the Cloudera manager product";
        public static final String PARCEL = "parcel url of the Cloudera manager product";
        public static final String CSD = "CSD (service descriptor) urls for the Cloudera manager product";
    }

    public static class StackRepositoryDescription {
        public static final String STACK = "name of the stack, like HDP";
        public static final String VERSION = "version of the stack";
        public static final String OS = "operating system for the stack, like redhat6";
        public static final String STACK_BASE_URL = "url of the stack repository";
        public static final String UTILS_BASE_URL = "url of the stack utils repository";
        public static final String REPOSITORY_VERSION = "version of the repository for VDF file creation in Ambari";
        public static final String VDF_URL = "local path on the Ambari server or URL that point to the desired VDF file";
        public static final String MPACK_URL = "url the MPACK that needs to be installed before HDF installation";
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
        public static final String AZURE_DATABASE_REQUEST = "Azure Database request.";
        public static final String AZURE_DATABASE_TYPE = "The type of the azure database: single server / flexible server";
        public static final String FLEXIBLE_SERVER_DELEGATED_SUBNET = "flexible server delegated subnet id";
        public static final String DISABLE_DB_SSL_ENFORCEMENT = "Option to enforce disabling database SSL encryption for clusters.";
        public static final String USER_OPERATION = "Operation regarding user (creation or deletion).";
    }

    public static class DatabaseServerModelDescription {
        public static final String NAME = "Name of the database server";
        public static final String CRN = "CRN of the database server";
        public static final String DESCRIPTION = "Description of the database server";
        public static final String HOST = "Host of the database server";
        public static final String PORT = "Port of the database server";
        public static final String CONNECTION_DRIVER = "Name of the JDBC connection driver (for example: 'org.postgresql.Driver')";
        public static final String DATABASE_VENDOR = "Name of the database vendor (MYSQL, POSTGRES, ...)";
        public static final String DATABASE_VENDOR_DISPLAY_NAME = "Display name of the database vendor (MySQL, PostgreSQL, ...)";
        public static final String USERNAME = "Username of the administrative user of the database server";
        public static final String PASSWORD = "Password of the administrative user of the database server";
        public static final String ENVIRONMENT_CRN = "CRN of the environment of the database server";
        public static final String CLUSTER_CRN = "CRN of the cluster of the database server";
        public static final String MAJOR_VERSION = "Major version of the database server engine";
        public static final String CREATION_DATE = "Creation date / time of the database server, in epoch milliseconds";
        public static final String RESOURCE_STATUS = "Ownership status of the database server";
        public static final String STATUS = "Status of the database server stack";
        public static final String STATUS_REASON = "Additional status information about the database server stack";
        public static final String SSL_CERTIFICATES = "Set of SSL certificates for the actual database server";
        public static final String SSL_CERTIFICATE_TYPE = "SSL certificate type";
        public static final String SSL_MODE = "SSL enforcement mode for the actual database server";
        public static final String SSL_CONFIG = "SSL config of the database server";
        public static final String SSL_CERTIFICATE_STATUS = "Current status of the set of relevant SSL certificates for the database server";
        public static final String SSL_CERTIFICATE_EXPIRATION_DATE_AS_LONG = "Expiration date of the the SSL cert as long.";
        public static final String SSL_CERTIFICATE_EXPIRATION_DATE_AS_STRING = "Expiration date of the the SSL cert as date string.";
        public static final String SSL_CERTIFICATE_ACTIVE_VERSION = "Version number of the SSL certificate currently active for the database server";
        public static final String SSL_CERTIFICATE_HIGHEST_AVAILABLE_VERSION =
                "Highest version number of the SSL certificate available for the database server; does not necessarily equal the active version";
        public static final String SSL_CERTIFICATE_ACTIVE_CLOUD_PROVIDER_IDENTIFIER =
                "Cloud provider specific identifier of the SSL certificate currently active for the database server";
    }

    public static class FileSystem {
        public static final String NAME = "name of the filesystem";
        public static final String TYPE = "type of the filesystem";
    }

    public static class RecipeModelDescription {
        public static final String DESCRIPTION = "A recipe is a script that runs on all nodes of a selected node "
                + "group at a specific time. You can use recipes for tasks such as installing additional software "
                + "or performing advanced cluster configuration. For example, you can use a recipe to put a JAR file "
                + "on the Hadoop classpath.";
        public static final String CONTENT = "content of recipe";
        public static final String TYPE = "type of recipe";
        public static final String WORKSPACE_ID = "id of the workspace";
        public static final String WORKSPACE_NAME = "name of the workspace";
    }

    public static class InstanceGroupModelDescription {
        public static final String INSTANCE_GROUP_NAME = "name of the instance group";
        public static final String MULTI_INSTANCE = "multiple instance which should be deleted";
        public static final String INSTANCE_GROUP_TYPE = "type of the instance group, default value is CORE";
        public static final String INSTANCE_GROUP_SCALABILITY_TYPE = "type of the instance group scalability, default value is ALLOWED";
        public static final String INSTANCE_GROUP_MINIMUM_NODECOUNT = "minimum nodecount in an instancegroup";
        public static final String TEMPLATE = "instancegroup related template";
        public static final String NETWORK = "instancegroup related network";
        public static final String AVAILABILITY_ZONES = "instancegroup related availability zones";
        public static final String SECURITYGROUP = "instancegroup related securitygroup";
        public static final String NODE_COUNT = "number of nodes";
        public static final String INSTANCE_TYPE = "type of the instance";
        public static final String STATUS = "status of the instance";
        public static final String METADATA = "metadata of instances";
        public static final String AWS_PARAMETERS = "aws specific parameters for instance group";
        public static final String AZURE_PARAMETERS = "azure specific parameters for instance group";
        public static final String GCP_PARAMETERS = "gcp specific parameters for instance group";
        public static final String OPENSTACK_PARAMETERS_DEPRECATED = "openstack specific parameters for instance group [DEPRECATED]";
    }

    public static class InstanceGroupAdjustmentModelDescription {
        public static final String SCALING_ADJUSTMENT = "scaling adjustment of the instance groups";
        public static final String WITH_CLUSTER_EVENT = "on stack update, update cluster too";
        public static final String ADJUSTMENT_TYPE = "scaling adjustment type";
        public static final String THRESHOLD = "scaling threshold";
        public static final String FORCE = "Force remove instance";
    }

    public static class InstanceGroupNetworkScaleModelDescription {
        public static final String NETWORK_SCALE_REQUEST = "request to support scaling operations in multi-availability zone environments";
        public static final String PREFERRED_SUBNET_IDS = "list of subnet ids to provision the virtual machines in desired availability zones";

        public static final String PREFERRED_AZS = "list of azs to provision the virtual machines in desired availability zones";

    }

    public static class HostGroupModelDescription {
        public static final String RECIPE_IDS = "referenced recipe ids";
        public static final String RECIPE_NAMES = "referenced recipe names";
        public static final String NETWORK = "referenced network";
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
        public static final String STATUS_REASON = "reason of the state";
    }

    public static class HostGroupAdjustmentModelDescription {
        public static final String SCALING_ADJUSTMENT = "scaling adjustment of the host groups";
        public static final String WITH_STACK_UPDATE = "on cluster update, update stack too";
        public static final String VALIDATE_NODE_COUNT = "validate node count during downscale";
        public static final String FORCED = "Force remove host";
    }

    public static class InstanceMetaDataModelDescription {
        public static final String PRIVATE_IP = "private IP of the instance";
        public static final String PUBLIC_IP = "public IP of the instance";
        public static final String INSTANCE_ID = "ID of the instance";
        public static final String DISCOVERY_FQDN = "the fully qualified domain name of the node in the service discovery cluster";
        public static final String SUBNET_ID = "ID of the subnet the instance is deployed in";
        public static final String AVAILABILITY_ZONE = "name of the availability zone the instance is deployed in";
        public static final String RACK_ID = "ID of the virtual network rack the instance is deployed in";
        public static final String PROVIDER_INSTANCE_TYPE = "The actual instance type of the instance";
    }

    public static class EventModelDescription {
        public static final String TYPE = "type of the event";
        public static final String TIMESTAMP = "timestamp of the event";
        public static final String MESSAGE = "message of the event";
        public static final String NOTIFICATION_TYPE = "Type of the notification to be identifiable by the UI";
    }

    public static class NetworkModelDescription {
        public static final String AWS_PARAMETERS = "provider specific parameters of the specified network";
        public static final String GCP_PARAMETERS = "provider specific parameters of the specified network";
        public static final String AZURE_PARAMETERS = "provider specific parameters of the specified network";
        public static final String MOCK_PARAMETERS = "mock network parameters";
        public static final String YARN_PARAMETERS = "provider specific parameters of the specified network";
        public static final String OPENSTACK_PARAMETERS_DEPRECATED = "provider specific parameters of the specified network";
        public static final String SUBNET_CIDR = "the subnet definition of the network in CIDR format";
    }

    public static class SecurityGroupModelDescription {
        public static final String SECURITY_RULES = "list of security rules that relates to the security group";
        public static final String SECURITY_GROUP_IDS = "Exisiting security group ids";
    }

    public static class SecurityRuleModelDescription {
        public static final String SUBNET = "definition of allowed subnet in CIDR format";
        public static final String PORTS = "list of accessible ports";
        public static final String PROTOCOL = "protocol of the rule";
        public static final String MODIFIABLE = "flag for making the rule modifiable";
    }

    public static class RDSConfigModelDescription {
        public static final String CLUSTER_NAMES = "list of clusters which use config";
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

    public static class RepairClusterRequest {
        public static final String HOSTGROUPS = "List of hostgroups where the failed nodes will be repaired";
        public static final String RESTART_SERVICES = "If true, the services will be restarted after repair";
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

    public static class CustomImageCatalogDescription {
        public static final String NAME = "custom image catalog name";
        public static final String DESCRIPTION = "custom image catalog description";
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
        public static final String SECURE = "Connect with SSL connection";
    }

    public static class UpgradeModelDescription {
        public static final String IMAGE_ID = "UUID of the image to upgrade";
        public static final String RUNTIME = "Cloudera Runtime version";
        public static final String LOCK_COMPONENTS = "Upgrades to image with the same version of stack and clustermanager, if available";
        public static final String DRY_RUN = "Checks the eligibility of an image to upgrade";
        public static final String SHOW_AVAILABLE_IMAGES = "Returns the list of images that are eligible for the upgrade";
        public static final String KEEP_VARIANT = "Value which indicates that the internal cloud platform variant should not be changed during the operation.";
        public static final String SKIP_DATAHUB_VALIDATION = "With this option, the Data Lake upgrade can be performed with running Data Hub clusters. " +
                "The usage of this option can cause problems on the running Data Hub clusters during the Data Lake upgrade.";
    }

    public static class CmSyncRequest {
        public static final String IMAGE_IDS = "List of image UUIDs to find CM and parcel information. " +
                "If left empty, all images are used from the image catalog.";
    }

    public static class UpgradeCcmDescription {
        public static final String FLOW_ID = "Flow identifier for the current Upgrade CCM operation.";
        public static final String CCM_UPGRADE_RESPONSE_TYPE = "Information about the Upgrade CCM operation acceptance.";
        public static final String CCM_UPGRADE_ERROR_REASON = "Reason of the error if Upgrade CCM could not be started.";
    }

    public static class UpgradeOutboundTypeDescription {
        public static final String FLOW_ID = "Flow identifier for the current Upgrade default outbound operation.";
        public static final String CURRENT_OUTBOUND_TYPES = "Information about actual outbound types, per stack.";
        public static final String OUTBOUND_UPGRADE_VALIDATION_MESSAGE = "Information about the stacks that require outbound type upgrade.";
        public static final String OUTBOUND_UPGRADE_RESPONSE_TYPE = "Information about the Upgrade default outbound operation acceptance.";
        public static final String OUTBOUND_UPGRADE_ERROR_REASON = "Reason of the error for Upgrade default outbound operation";
    }

    public static class InstanceMetadataUpdateRequest {
        public static final String IMD_UPDATE_TYPE = "Type of instance metadata update.";
    }

    public static class RotateRdsCertificateDescription {
        public static final String FLOW_ID = "Flow identifier for the current Rotate RDS Certificate operation.";
        public static final String ROTATE_RDS_CERTIFICATE_RESPONSE_TYPE = "Information about the Rotate RDS Certificate operation acceptance.";
        public static final String ROTATE_RDS_CERTIFICATE_ERROR_REASON = "Reason of the error if Rotate RDS Certificate could not be started.";
    }

    public static class MigrateRdsDescription {
        public static final String FLOW_ID = "Flow identifier for the Migrate Database to SSL operation.";
        public static final String MIGRATE_DATABASE_TO_SSL_RESPONSE_TYPE = "Information about the Migrate Database to SSL operation acceptance.";
        public static final String MIGRATE_DATABASE_TO_SSL_ERROR_REASON = "Reason of the error if Migrate Database to SSL could not be started.";
    }

}