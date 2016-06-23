package com.sequenceiq.cloudbreak.doc

object ModelDescriptions {

    val ID = "id of the resource"
    val NAME = "name of the resource"
    val DESCRIPTION = "description of the resource"
    val PUBLIC_IN_ACCOUNT = "resource is visible in account"
    val CLOUD_PLATFORM = "type of cloud provider"
    val OWNER = "id of the resource owner that is provided by OAuth provider"
    val ACCOUNT = "account id of the resource owner that is provided by OAuth provider"
    val TOPOLOGY_ID = "id of the topology the resource belongs to"

    object BlueprintModelDescription {
        val URL = "url source of an ambari blueprint, set this or the ambariBlueprint field"
        val BLUEPRINT_NAME = "gathered from blueprintName field from the blueprint JSON"
        val AMBARI_BLUEPRINT = "ambari blueprint JSON, set this or the url field"
        val HOST_GROUP_COUNT = "number of host groups"
    }

    object CredentialModelDescription {
        val PUBLIC_KEY = "public key for accessing instances"
        val LOGIN_USERNAME = "authentication name for machines"
        val PARAMETERS = "cloud specific parameters for credential"
    }

    object OrchestratorModelDescription {
        val TYPE = "type of the orchestrator (Swarm or Marathon)"
        val PARAMETERS = "orchestrator specific parameters, like authentication details"
        val ENDPOINT = "endpoint for the container orchestration api"
    }

    object TemplateModelDescription {
        val VOLUME_COUNT = "number of volumes"
        val VOLUME_SIZE = "size of volumes"
        val VOLUME_TYPE = "type of the volumes"
        val INSTANCE_TYPE = "type of the instance"
        val PARAMETERS = "cloud specific parameters for template"
    }

    object ConstraintTemplateModelDescription {
        val CPU = "number of CPU cores needed for the Ambari node"
        val MEMORY = "memory needed for the Ambari container (GB)"
        val DISK = "disk size needed for an Ambari node (GB)"
        val ORCHESTRATOR_TYPE = "type of orchestrator"
    }

    object ImageModelDescription {
        val IMAGE_NAME = "name of the image"
        val HDPVERSION = "hdp version of image"
    }

    object StackModelDescription {
        val STACK_ID = "id of the stack"
        val IMAGE = "image of the stack"
        val STACK_NAME = "name of the stack"
        val REGION = "region of the stack"
        val AVAILABILITY_ZONE = "availability zone of the stack"
        val CREDENTIAL_ID = "credential resource id for the stack"
        val USERNAME = "ambari username"
        val PASSWORD = "ambari password"
        val PARAMETERS = "additional cloud specific parameters for stack"
        val SECURITY_GROUP_ID = "security group resource id for the stack"
        val FAILURE_ACTION = "action on failure"
        val FAILURE_POLICY = "failure policy in case of failures"
        val STACK_STATUS = "status of the stack"
        val STATUS_REASON = "status message of the stack"
        val AMBARI_IP = "public ambari ip of the stack"
        val BLUEPRINT_ID = "id of the referenced blueprint"
        val NETWORK_ID = "network resource id for the stack"
        val CERTIFICATE = "certificate used by the gateway"
        val CLUSTER_STATUS = "status of the cluster"
        val PLATFORM_VARIANT = "cloud provider api variant"
        val ORCHESTRATOR = "the details of the container orchestrator api to use"
        val RELOCATE_DOCKER = "relocate the docker service in startup time"
        val CREATED = "creation time of the stack in long"
        val GATEWAY_PORT = "port of the gateway secured proxy"
        val AMBARI_VERSION = "specific version of ambari"
        val HDP_VERSION = "specific version of HDP"
    }

    object ClusterModelDescription {
        val STATUS = "status of the cluster"
        val STATUS_REASON = "status message of the cluster"
        val CLUSTER_NAME = "name of the cluster"
        val CLUSTER_ID = "id of the cluster"
        val BLUEPRINT_ID = "blueprint id for the cluster"
        val HOURS = "duration - how long the cluster is running in hours"
        val MINUTES = "duration - how long the cluster is running in minutes (minus hours)"
        val EMAIL_NEEDED = "send email about the result of the cluster installation"
        val SERVICE_ENDPOINT_MAP = "most important services in the cluster"
        val CONFIG_STRATEGY = "config recommendation strategy"
        val LDAP_REQUIRED = "flag for default LDAP support"
        val SSSDCONFIG_ID = "SSSD config id for the cluster"
        val ENABLE_SHIPYARD = "shipyard service enabled in the cluster"
    }

    object AmbariStackDetailsDescription {
        val STACK = "name of the stack, like HDP"
        val VERSION = "version of the stack"
        val OS = "operating system for the stack, like redhat6"
        val STACK_REPO_ID = "id of the stack repository"
        val UTILS_REPO_ID = "id of the stack utils repository"
        val STACK_BASE_URL = "url of the stack repository"
        val UTILS_BASE_URL = "url of the stack utils repository"
        val VERIFY = "whether to verify or not the repo url"
    }

    object RDSConfig {
        val CONNECTION_URL = "JDBC connection URL in the form of jdbc:<db-type>://<address>:<port>/<db>"
        val DB_TYPE = "Type of the external database (allowed values: MYSQL, POSTGRES)"
        val USERNAME = "Username to use for the jdbc connection"
        val PASSWORD = "Password to use for the jdbc connection"
    }

    object FileSystem {
        val NAME = "name of the filesystem"
        val TYPE = "type of the filesystem"
        val DEFAULT = "true if fs.defaultFS should point to this filesystem"
        val PROPERTIES = "configuration of the filesystem access as key-value pairs"
    }

    object RecipeModelDescription {
        val TIMEOUT = "recipe timeout in minutes"
        val PLUGINS = "list of consul plugins with execution types"
        val PROPERTIES = "additional plugin properties"
    }

    object SssdConfigModelDescription {
        val PROVIDER_TYPE = "provider type"
        val URL = "comma-separated list of URIs of the LDAP servers"
        val SCHEMA = "schema of the database"
        val BASE_SEARCH = "search base of the database"
        val TLS_REQUCERT = "TLS behavior of the connection"
        val AD_SERVER = "comma-separated list of IP addresses or hostnames of the AD servers"
        val KERBEROS_SERVER = "comma-separated list of IP addresses or hostnames of the Kerberos servers"
        val KERBEROS_REALM = "name of the Kerberos realm"
        val CONFIGURATION = "custom configuration"
    }

    object InstanceGroupModelDescription {
        val INSTANCE_GROUP_NAME = "name of the instance group"
        val INSTANCE_GROUP_TYPE = "type of the instance group"
        val NODE_COUNT = "number of nodes"
        val TEMPLATE_ID = "referenced template id"
        val STATUS = "status of the instance"
    }

    object InstanceGroupAdjustmentModelDescription {
        val SCALING_ADJUSTMENT = "scaling adjustment of the instance groups"
        val WITH_CLUSTER_EVENT = "on stack update, update cluster too"
    }

    object HostGroupModelDescription {
        val RECIPE_IDS = "referenced recipe ids"
        val HOST_GROUP_NAME = "name of the host group"
        val CONSTRAINT = "instance group or resource constraint for a hostgroup"
        val INSTANCE_GROUP = "name of an instance group where the hostgroup will be deployed"
        val CONSTRAINT_NAME = "name of a constraint template that defines the resource constraints for the hostgroup"
    }

    object UserNamePasswordModelDescription {
        val NEW_USER_NAME = "new user name in ambari"
        val OLD_PASSWORD = "old password in ambari"
        val NEW_PASSWORD = "new password in ambari"
    }

    object HostMetadataModelDescription {
        val STATE = "state of the host"
    }

    object HostGroupAdjustmentModelDescription {
        val SCALING_ADJUSTMENT = "scaling adjustment of the host groups"
        val WITH_STACK_UPDATE = "on cluster update, update stack too"
    }

    object InstanceMetaDataModelDescription {
        val PRIVATE_IP = "private ip of the insctance"
        val PUBLIC_IP = "public ip of the instance"
        val INSTANCE_ID = "id of the instance"
        val AMBARI_SERVER = "ambari server address"
        val DISCOVERY_FQDN = "the fully qualified domain name of the node in the service discovery cluster"
    }

    object FailurePolicyModelDescription {
        val THRESHOLD = "threshold of failure policy"
    }

    object UsageModelDescription {
        val PROVIDER = "cloud provider of the stack"
        val COSTS = "computed costs of instance usage"
        val DAY = "days since the instance is running"
        val INSTANCE_HOURS = "hours since the instance is running"
        val INSTANCE_TYPE = "type of instance"
        val INSTANCE_GROUP = "group name of instance"
    }

    object EventModelDescription {
        val TYPE = "type of the event"
        val TIMESTAMP = "timestamp of the event"
        val MESSAGE = "message of the event"
    }

    object NetworkModelDescription {
        val PARAMETERS = "provider specific parameters of the specified network"
        val SUBNET_CIDR = "the subnet definition of the network in CIDR format"
    }

    object SecurityGroupModelDescription {
        val SECURITY_RULES = "list of security rules that relates to the security group"
    }

    object SecurityRuleModelDescription {
        val SUBNET = "definition of allowed subnet in CIDR format"
        val PORTS = "comma separated list of accessible ports"
        val PROTOCOL = "protocol of the rule"
        val MODIFIABLE = "flag for making the rule modifiable"
    }

    object AccountPreferencesModelDescription {
        val MAX_NO_CLUSTERS = "max number of clusters in the account (0 when unlimited)"
        val MAX_NO_NODES_PER_CLUSTER = "max number of vms in a cluster of account (0 when unlimited)"
        val MAX_NO_CLUSTERS_PER_USER = "max number of clusters for user within the account (0 when unlimited)"
        val ALLOWED_INSTANCE_TYPES = "allowed instance types in the account (empty list for no restriction)"
        val CLUSTER_TIME_TO_LIVE = "lifecycle of the cluster in hours (0 for immortal clusters)"
        val ACCOUNT_TIME_TO_LIVE = "lifecycle of the account and its clusters in hours (0 for immortal account)"
        val PLATFORMS = "list of the cloudplatforms visible on the UI"
    }
}
