package com.sequenceiq.cloudbreak.doc;

public class OperationDescriptions {
    public static class BlueprintOpDescription {
        public static final String POST_PRIVATE = "create blueprint as private resource";
        public static final String POST_PUBLIC = "create blueprint as public resource";
        public static final String GET_PRIVATE = "retrieve private blueprints";
        public static final String GET_PUBLIC = "retrieve public and private (owned) blueprints";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private blueprint by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) blueprint by name";
        public static final String GET_BY_ID = "retrieve blueprint by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private blueprint by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private blueprint by name";
        public static final String DELETE_BY_ID = "delete blueprint by id";
        public static final String GET_BY_BLUEPRINT_NAME = "retrieve validation request by blueprint name";
        public static final String GET_BY_BLUEPRINT_ID = "retrieve validation request by blueprint id";
        public static final String LIST_BY_ORGANIZATION = "list blueprints for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get blueprint by name in organization";
        public static final String CREATE_IN_ORG = "create blueprint in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete blueprint by name in organization";
    }

    public static class TemplateOpDescription {
        public static final String POST_PRIVATE = "create template as private resource";
        public static final String POST_PUBLIC = "create template as public resource";
        public static final String GET_PRIVATE = "retrieve private templates";
        public static final String GET_PUBLIC = "retrieve public and private (owned) templates";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private template by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) template by name";
        public static final String GET_BY_ID = "retrieve template by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private template by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private template by name";
        public static final String DELETE_BY_ID = "delete template by id";
    }

    public static class ConstraintOpDescription {
        public static final String POST_PRIVATE = "create constraint template as private resource";
        public static final String POST_PUBLIC = "create constraint template as public resource";
        public static final String GET_PRIVATE = "retrieve private constraint templates";
        public static final String GET_PUBLIC = "retrieve public and private (owned) constraint templates";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private constraint template by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) constraint template by name";
        public static final String GET_BY_ID = "retrieve constraint template by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private constraint template by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private constraint template by name";
        public static final String DELETE_BY_ID = "delete constraint template by id";
    }

    public static class TopologyOpDesctiption {
        public static final String GET_BY_ID = "retrieve topology by id";
        public static final String GET_PUBLIC = "retrieve topoligies";
        public static final String POST_PUBLIC = "create topology as public resource";
        public static final String DELETE_BY_ID = "delete topology by id";
    }

    public static class CredentialOpDescription {
        public static final String POST_PRIVATE = "create credential as private resource";
        public static final String PUT_PRIVATE = "modify private credential resource";
        public static final String POST_PUBLIC = "create credential as public resource";
        public static final String PUT_PUBLIC = "modify public credential resource";
        public static final String PUT_IN_ORG = "modify public credential resource in organization";
        public static final String GET_PRIVATE = "retrieve private credentials";
        public static final String GET_PUBLIC = "retrieve public and private (owned) credentials";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private credential by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) credential by name";
        public static final String GET_BY_ID = "retrieve credential by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private credential by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private credential by name";
        public static final String INTERACTIVE_LOGIN = "interactive login";
        public static final String DELETE_BY_ID = "delete credential by id";
        public static final String GET_JKS_FILE = "retrieve azure JKS file by credential id";
        public static final String PUT_CERTIFICATE_BY_ID = "update azure credential by credential id";
        public static final String GET_SSH_FILE = "retrieve azure ssh key file for credential by credential id";
        public static final String GET_BY_CREDENTIAL_NAME = "retrieve credential request by credential name";
        public static final String LIST_BY_ORGANIZATION = "list credentials for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get credential by name in organization";
        public static final String CREATE_IN_ORG = "create credential in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete credential by name in organization";
    }

    public static class StackOpDescription {
        public static final String POST_PRIVATE = "create stack as private resource";
        public static final String POST_PUBLIC = "create stack as public resource";
        public static final String POST_PUBLIC_BLUEPRINT = "create stack as public resource for blueprint";
        public static final String GET_PRIVATE = "retrieve private stack";
        public static final String GET_PUBLIC = "retrieve public and private (owned) stacks";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private stack by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) stack by name";
        public static final String GET_BY_ID = "retrieve stack by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private stack by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private stack by name";
        public static final String DELETE_BY_ID = "delete stack by id";
        public static final String GET_STATUS_BY_ID = "retrieve stack status by stack id";
        public static final String GET_STATUS_BY_NAME = "retrieve stack status by stack name";
        public static final String PUT_BY_ID = "update stack by id";
        public static final String PUT_BY_NAME = "update stack by name";
        public static final String GET_METADATA = "retrieve stack metadata";
        public static final String GET_BY_AMBARI_ADDRESS = "retrieve stack by ambari address";
        public static final String GET_STACK_CERT = "retrieves the TLS certificate used by the gateway";
        public static final String VALIDATE = "validate stack";
        public static final String DELETE_INSTANCE_BY_ID = "delete instance resource from stack";
        public static final String GET_PLATFORM_VARIANTS = "retrieve available platform variants";
        public static final String GET_ALL = "retrieve all stacks";
        public static final String GET_BY_STACK_NAME = "retrieve stack request by stack name";
        public static final String RETRY_BY_ID = "retry stack and cluster provisioning of failed stack";
        public static final String LIST_BY_ORGANIZATION = "list stacks for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get stack by name in organization";
        public static final String CREATE_IN_ORG = "create stack in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete stack by name in organization";
        public static final String SYNC_BY_NAME_IN_ORG = "syncs the stack by name in organization";
        public static final String RETRY_BY_NAME_IN_ORG = "retries the stack by name in organization";
        public static final String STOP_BY_NAME_IN_ORG = "stops the stack by name in organization";
        public static final String START_BY_NAME_IN_ORG = "starts the stack by name in organization";
        public static final String SCALE_BY_NAME_IN_ORG = "scales the stack by name in organization";
        public static final String REPAIR_CLUSTER_IN_ORG = "repairs the stack by name in organization";
        public static final String DELETE_WITH_KERBEROS_IN_ORG = "deletes the stack (with kerberos cluster) "
                + "by name in organization";
        public static final String GET_STACK_REQUEST_IN_ORG = "gets StackRequest by name in organization";
        public static final String POST_STACK_FOR_BLUEPRINT_IN_ORG = "posts stack for blueprint in organization";
        public static final String DELETE_INSTANCE_BY_ID_IN_ORG = "deletes instance from the stack's cluster in organization";
        public static final String CHECK_IMAGE_IN_ORG = "checks image in stack by name in organization";
    }

    public static class ClusterOpDescription {
        public static final String POST_FOR_STACK = "create cluster for stack";
        public static final String GET_BY_STACK_ID = "retrieve cluster by stack id";
        public static final String GET_PRIVATE_BY_NAME = "retrieve cluster by stack name (private)";
        public static final String GET_PUBLIC_BY_NAME = "retrieve cluster by stack name (public)";
        public static final String DELETE_BY_STACK_ID = "delete cluster on a specific stack";
        public static final String PUT_BY_STACK_ID = "update cluster by stack id";
        public static final String PUT_BY_STACK_NAME = "update cluster by stack name";
        public static final String UPGRADE_AMBARI = "upgrade the Ambari version";
        public static final String GET_CLUSTER_PROPERTIES = "get cluster properties with blueprint outputs";
        public static final String FAILURE_REPORT = "failure report";
        public static final String REPAIR_CLUSTER = "repair the cluster";
    }

    public static class GatewayOpDescription {
        public static final String UPDATE_GATEWAY_TOPOLOGIES = "update topologies of a gateway";
    }

    public static class ClusterTemplateOpDescription {
        public static final String POST_PRIVATE = "create cluster template as private resource";
        public static final String POST_PUBLIC = "create cluster template as public resource";
        public static final String GET_PRIVATE = "retrieve private cluster templates";
        public static final String GET_PUBLIC = "retrieve public and private (owned) cluster template";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private cluster template by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) cluster template by name";
        public static final String GET_BY_ID = "retrieve cluster template by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private cluster template by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private cluster template by name";
        public static final String DELETE_BY_ID = "delete cluster template by id";
    }

    public static class RecipeOpDescription {
        public static final String POST_PRIVATE = "create recipe as private resource";
        public static final String POST_PUBLIC = "create recipe as public resource";
        public static final String GET_PRIVATE = "retrieve private recipes";
        public static final String GET_PUBLIC = "retrieve public and private (owned) recipes";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private recipe by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) recipe by name";
        public static final String GET_BY_ID = "retrieve recipe by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private recipe by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private recipe by name";
        public static final String DELETE_BY_ID = "delete recipe by id";
        public static final String GET_REQUEST_BY_NAME = "retrieve recipe request by recipe name";
        public static final String LIST_BY_ORGANIZATION = "list recipes for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get recipe by name in organization";
        public static final String CREATE_IN_ORG = "create recipe in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete recipe by name in organization";
    }

    public static class OrganizationOpDescription {
        public static final String POST = "create an organization";
        public static final String GET = "retrieve organizations";
        public static final String GET_BY_NAME = "retrieve an organization by name";
        public static final String GET_BY_ID = "retrieve an organization by id";
        public static final String DELETE_BY_ID = "delete an organization by id";
        public static final String DELETE_BY_NAME = "delete an organization by name";
        public static final String GET_REQUEST_BY_NAME = "retrieve organization request by organization name";
        public static final String CHANGE_USERS = "change users and their permissions in the organization";
        public static final String REMOVE_USERS = "removes users from the given organization by their userIds";
        public static final String ADD_USERS = "adds users to the given organization";
        public static final String UPDATE_USERS = "updates the users' permissions in the given organization";
    }

    public static class UsagesOpDescription {
        public static final String GET_ALL = "retrieve usages by filter parameters";
        public static final String GET_PUBLIC = "retrieve public and private (owned) usages by filter parameters";
        public static final String GET_PRIVATE = "retrieve private usages by filter parameters";
        public static final String GET_FLEX_DAILY = "retrieve Flex related daily usages";
        public static final String GET_FLEX_LATEST = "retrieve Flex related latest usages, usages for the given day";
    }

    public static class EventOpDescription {
        public static final String GET_BY_TIMESTAMP = "retrieve events by timestamp (long)";
        public static final String GET_BY_ID = "retrieve events by stackid (long)";
        public static final String GET_BY_NAME = "retrieve events by name";
        public static final String GET_EVENTS_BY_NAME = "retrieve events by name";
        public static final String GET_EVENTS_ZIP_BY_NAME = "retrieve events in zip by name";
    }

    public static class NetworkOpDescription {
        public static final String POST_PRIVATE = "create network as private resource";
        public static final String POST_PUBLIC = "create network as public resource";
        public static final String GET_PRIVATE = "retrieve private networks";
        public static final String GET_PUBLIC = "retrieve public and private (owned) networks";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private network by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) network by name";
        public static final String GET_BY_ID = "retrieve network by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private network by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private network by name";
        public static final String DELETE_BY_ID = "delete network by id";
    }

    public static class UserOpDescription {
        public static final String USER_DETAILS_EVICT = "remove user from cache (by username)";
        public static final String CURRENT_USER_DETAILS_EVICT = "remove current user from cache";
        public static final String USER_GET_PROFILE = "user related profile";
        public static final String USER_PUT_PROFILE = "modify user related profile";
        public static final String GET_TENANT_USERS = "retrieve all users in the tenant";
        public static final String USER_GET_PROFILE_IN_ORG = "user related profile in organization";
        public static final String USER_PUT_PROFILE_IN_ORG = "modify user related profile in organization";
    }

    public static class SecurityGroupOpDescription {
        public static final String POST_PRIVATE = "create security group as private resource";
        public static final String POST_PUBLIC = "create security group as public resource";
        public static final String GET_PRIVATE = "retrieve private security groups";
        public static final String GET_PUBLIC = "retrieve public and private (owned) security groups";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private security group by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) security group by name";
        public static final String GET_BY_ID = "retrieve security group by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private security group by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private security group by name";
        public static final String DELETE_BY_ID = "delete security group by id";
    }

    public static class AccountPreferencesDescription {
        public static final String GET_PRIVATE = "retrieve account preferences for admin user";
        public static final String IS_PLATFORM_SELECTION_DISABLED = "is platform selection disabled";
        public static final String PLATFORM_ENABLEMENT = "is platform selection enabled";
        public static final String PUT_PRIVATE = "update account preferences of admin user";
        public static final String POST_PRIVATE = "post account preferences of admin user";
        public static final String VALIDATE = "validate account preferences of all stacks";
    }

    public static class LdapConfigOpDescription {
        public static final String POST_PRIVATE = "create LDAP config as private resource";
        public static final String POST_PUBLIC = "create LDAP config as public resource";
        public static final String GET_PRIVATE = "retrieve private LDAP configs";
        public static final String GET_PUBLIC = "retrieve public and private (owned) LDAP configs";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private LDAP config by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) LDAP config by name";
        public static final String GET_BY_ID = "retrieve LDAP config by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private LDAP config by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private LDAP config by name";
        public static final String DELETE_BY_ID = "delete LDAP config by id";
        public static final String POST_CONNECTION_TEST = "test that the connection could be established of an existing or new LDAP config";
        public static final String GET_REQUEST = "get request";
        public static final String LIST_BY_ORGANIZATION = "list LDAP configs for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get LDAP config by name in organization";
        public static final String CREATE_IN_ORG = "create LDAP config in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete LDAP config by name in organization";
    }

    public static class UtilityOpDescription {
        public static final String TEST_DATABASE = "tests a database connection parameters";
        public static final String CREATE_DATABASE = "create a database for the service in the RDS if the connection could be created";
        public static final String STACK_MATRIX = "returns default ambari details for distinct HDP and HDF";
        public static final String KNOX_SERVICES = "returns supported knox services";
        public static final String CLOUD_STORAGE_MATRIX = "returns supported cloud storage for stack version";
        public static final String CUSTOM_PARAMETERS = "returns custom parameters";
        public static final String FILE_SYSTEM_PARAMETERS = "returns filesystem parameters";
        public static final String CHECK_CLIENT_VERSION = "checks the client version";
    }

    public static class RdsConfigOpDescription {
        public static final String POST_PRIVATE = "create RDS configuration as private resource";
        public static final String POST_PUBLIC = "create RDS configuration as public resource";
        public static final String GET_PRIVATE = "retrieve private RDS configurations";
        public static final String GET_PUBLIC = "retrieve public and private (owned) RDS configurations";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private RDS configuration by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) RDS configuration by name";
        public static final String GET_BY_ID = "retrieve RDS configuration by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private RDS configuration by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private RDS configuration by name";
        public static final String DELETE_BY_ID = "delete RDS configuration by id";
        public static final String POST_CONNECTION_TEST = "test RDS connectivity";
        public static final String GET_REQUEST = "get request";
        public static final String LIST_BY_ORGANIZATION = "list RDS configs for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get RDS config by name in organization";
        public static final String CREATE_IN_ORG = "create RDS config in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete RDS config by name in organization";
        public static final String GET_REQUEST_IN_ORG = "get request in organization";
    }

    public static class ProxyConfigOpDescription {
        public static final String POST_PRIVATE = "create proxy configuration as private resource";
        public static final String POST_PUBLIC = "create proxy configuration as public resource";
        public static final String GET_PRIVATE = "retrieve private proxy configurations";
        public static final String GET_PUBLIC = "retrieve public and private (owned) proxy configurations";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private proxy configuration by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) proxy configuration by name";
        public static final String GET_BY_ID = "retrieve proxy configuration by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private proxy configuration by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private proxy configuration by name";
        public static final String DELETE_BY_ID = "delete proxy configuration by id";
        public static final String LIST_BY_ORGANIZATION = "list proxy configurations for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get proxy configuration by name in organization";
        public static final String CREATE_IN_ORG = "create proxy configuration in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete proxy configuration by name in organization";
    }

    public static class ManagementPackOpDescription {
        public static final String POST_PRIVATE = "create management pack as private resource";
        public static final String POST_PUBLIC = "create management pack as public resource";
        public static final String GET_PRIVATE = "retrieve private management packs";
        public static final String GET_PUBLIC = "retrieve public and private (owned) management packs";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private management pack by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) management pack by name";
        public static final String GET_BY_ID = "retrieve management pack by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private management pack by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private management pack by name";
        public static final String DELETE_BY_ID = "delete management pack by id";
        public static final String LIST_BY_ORGANIZATION = "list management packs for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get management pack by name in organization";
        public static final String CREATE_IN_ORG = "create management pack in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete management pack by name in organization";
    }

    public static class ConnectorOpDescription {
        public static final String GET_PLATFORMS = "retrive available platforms";
        public static final String GET_PLATFORM_VARIANTS = "retrive available platform variants";
        public static final String GET_PLATFORM_VARIANT_BY_TYPE = "retrive a platform variant by type";
        public static final String GET_DISK_TYPES = "retrive available disk types";
        public static final String GET_DISK_TYPE_BY_TYPE = "retrive disks by type";
        public static final String GET_ORCHESTRATOR_TYPES = "retrive available orchestrator types";
        public static final String GET_ORCHESTRATORS_BY_TYPES = "retrive orchestrators by type";
        public static final String GET_VM_TYPES = "retrive available vm types";
        public static final String GET_VM_TYPE_BY_TYPE = "retrive vm types by type";
        public static final String GET_REGIONS = "retrive available regions";
        public static final String GET_REGION_R_BY_TYPE = "retrive regions by type";
        public static final String GET_REGION_AV_BY_TYPE = "retrive availability zones by type";
        public static final String GET_IMAGES = "retrive available images";
        public static final String GET_RECOMMENDATION = "creates a recommendation that advises cloud resources for the given blueprint";
        public static final String GET_TAG_SPECIFICATIONS = "retrive tag specifications";
        public static final String GET_SPECIALS = "retrive special properties";
        public static final String GET_IMAGE_R_BY_TYPE = "retrive images by type";
        public static final String GET_NETWORKS = "retrive network properties";
        public static final String GET_SECURITYGROUPS = "retrive securitygroups properties";
        public static final String GET_SSHKEYS = "retrive sshkeys properties";
        public static final String GET_VMTYPES_BY_CREDENTIAL = "retrive vmtype properties by credential";
        public static final String GET_GATEWAYS = "retrive gateways with properties";
        public static final String GET_IPPOOLS = "retrive ip pools with properties";
        public static final String GET_ACCESSCONFIGS = "retrive access configs with properties";
        public static final String GET_ENCRYPTIONKEYS = "retrive encryption keys with properties";
    }

    public static class SettingsOpDescription {
        public static final String GET_ALL_SETTINGS = "retrive all available settings";
        public static final String GET_RECIPE_SETTINGS = "retrive available recipe settings";
        public static final String GET_DATABASE_SETTINGS = "retrive available Ambari database settings";
    }

    public static class SubscriptionOpDescription {
        public static final String SUBSCRIBE = "retrive subscribe identifier";
    }

    public static class SmartSenseSubOpDescription {
        public static final String POST_PRIVATE = "create SmartSense subscription as private resource";
        public static final String POST_PUBLIC = "create SmartSense subscription as public resource";
        public static final String GET_PRIVATE = "retrieve private SmartSense subscriptions";
        public static final String GET_PUBLIC = "retrieve public and private (owned) SmartSense subscriptions";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private SmartSense subscription by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) SmartSense subscription by name";
        public static final String GET = "retrieve default SmartSense subscription";
        public static final String GET_BY_ID = "retrieve SmartSense subscription by id";
        public static final String DELETE_BY_ID = "delete SmartSense subscription by id";
        public static final String LIST_BY_ORGANIZATION = "list SmartSense subscriptions for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get SmartSense subscription by name in organization";
        public static final String GET_DEFAULT_IN_ORG = "get default SmartSense subscription by name in organization";
        public static final String CREATE_IN_ORG = "create SmartSense subscription in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete SmartSense subscription by name in organization";
    }

    public static class FlexSubOpDescription {
        public static final String POST_PRIVATE = "create Flex subscription as private resource";
        public static final String POST_PUBLIC = "create Flex subscription as public resource";
        public static final String GET_PRIVATE = "retrieve private Flex subscriptions";
        public static final String GET_PUBLIC = "retrieve public and private (owned) Flex subscriptions";
        public static final String GET_PRIVATE_BY_NAME = "retrieve a private Flex subscription by name";
        public static final String GET_PUBLIC_BY_NAME = "retrieve a public or private (owned) Flex subscription by name";
        public static final String GET_BY_ID = "retrieve Flex subscription by id";
        public static final String DELETE_PRIVATE_BY_NAME = "delete private Flex subscription by name";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private Flex subscription by name";
        public static final String DELETE_BY_ID = "delete Flex subscription by id";
        public static final String SET_DEFAULT_IN_ACCOUNT = "sets the account default flag on the Flex subscription";
        public static final String SET_DEFAULT_IN_ORG = "sets the organization default flag on the Flex subscription";
        public static final String SET_USED_FOR_CONTROLLER_IN_ACCOUNT = "sets the account 'used for controller' flag on the Flex subscription";
        public static final String SET_USED_FOR_CONTROLLER_IN_ORG = "sets the organization 'used for controller' flag on the Flex subscription";
        public static final String LIST_BY_ORGANIZATION = "list Flex subscriptions for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get Flex subscription by name in organization";
        public static final String CREATE_IN_ORG = "create Flex subscription in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete Flex subscription by name in organization";
    }

    public static class ImageCatalogOpDescription {
        public static final String GET_PUBLICS_IMAGE_CATALOGS = "list available custom image catalogs as public resources";
        public static final String GET_PUBLIC_IMAGE_CATALOG_BY_NAME = "get custom image catalog by name";
        public static final String GET_PUBLIC_IMAGE_CATALOG_BY_ID = "get custom image catalog by id";
        public static final String GET_IMAGES_BY_PROVIDER = "determines available images for the Cloudbreak version "
                + "by the given provider and default image catalog url";
        public static final String GET_IMAGES_BY_PROVIDER_AND_CUSTOM_IMAGE_CATALOG = "determines available images for the Cloudbreak version "
                + "by the given provider and given image catalog url";
        public static final String POST_PUBLIC = "create Image Catalog as public resources";
        public static final String POST_PRIVATE = "create Image Catalog as private resources";
        public static final String DELETE_PUBLIC_BY_NAME = "delete public (owned) or private Image Catalog by id";
        public static final String PUT_PUBLIC_BY_NAME = "update public (owned) or private Image Catalog by id";
        public static final String GET_BY_IMAGE_CATALOG_NAME = "retrieve imagecatalog request by imagecatalog name";
        public static final String GET_IMAGES_BY_STACK_NAME_AND_CUSTOM_IMAGE_CATALOG = "determines available images for the given stack"
                + "from the given imagecatalog name";
        public static final String GET_IMAGES_BY_STACK_NAME = "determines available images for the given stack"
                + "from the default image catalog";
        public static final String LIST_BY_ORGANIZATION = "list image catalogs for the given organization";
        public static final String GET_BY_NAME_IN_ORG = "get image catalog by name in organization";
        public static final String CREATE_IN_ORG = "create image catalog in organization";
        public static final String DELETE_BY_NAME_IN_ORG = "delete image catalog by name in organization";
    }

    public static class SecurityRuleOpDescription {
        public static final String GET_DEFAULT_SECURITY_RULES = "get default security rules";
    }

    public static class RepositoryConfigsValidationOpDescription {
        public static final String POST_REPOSITORY_CONFIGS_VALIDATION = "validate repository configs fields, check their availability";
    }

    public static class AuditOpDescription {
        public static final String LIST_IN_ORG = "list audit events for the given organization";
        public static final String GET_BY_ORG = "Get audit event in organization";
        public static final String LIST_IN_ORG_ZIP = "list audit events for the given organization in zip file";
    }

    public static class KnoxServicesOpDescription {
        public static final String LIST_IN_ORG_FOR_BLUEPRINT = "list supported exposable services for the specified blueprint";
    }
}
