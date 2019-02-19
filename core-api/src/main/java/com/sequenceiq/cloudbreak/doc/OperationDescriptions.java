package com.sequenceiq.cloudbreak.doc;

public class OperationDescriptions {
    public static class ClusterDefinitionOpDescription {
        public static final String GET_BY_NAME = "retrieve validation request by cluster definition name";
        public static final String LIST_BY_WORKSPACE = "list cluster definitions for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get cluster definition by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create cluster definition in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete cluster definition by name in workspace";
    }

    public static class CredentialOpDescription {
        public static final String PUT_IN_WORKSPACE = "modify public credential resource in workspace";
        public static final String INTERACTIVE_LOGIN = "interactive login";
        public static final String INIT_CODE_GRANT_FLOW = "start a credential creation with Oauth2 Authorization Code Grant flow";
        public static final String INIT_CODE_GRANT_FLOW_ON_EXISTING = "Reinitialize Oauth2 Authorization Code Grant flow on an existing credential";
        public static final String AUTHORIZE_CODE_GRANT_FLOW = "Authorize Oauth2 Authorization Code Grant flow";
        public static final String LIST_BY_WORKSPACE = "list credentials for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get credential by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create credential in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete credential by name in workspace";
        public static final String GET_PREREQUISTIES_BY_CLOUD_PROVIDER = "get credential prerequisites for cloud platform";
    }

    public static class StackOpDescription {
        public static final String POST_PRIVATE = "create stack as private resource";
        public static final String POST_PUBLIC = "create stack as public resource";
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
        public static final String GET_BY_AMBARI_ADDRESS = "retrieve stack by ambari address";
        public static final String GET_STACK_CERT = "retrieves the TLS certificate used by the gateway";
        public static final String VALIDATE = "validate stack";
        public static final String DELETE_INSTANCE_BY_ID = "delete instance resource from stack";
        public static final String GET_ALL = "retrieve all stacks";
        public static final String GET_BY_STACK_NAME = "retrieve stack request by stack name";
        public static final String RETRY_BY_ID = "retry stack and cluster provisioning of failed stack";
        public static final String LIST_BY_WORKSPACE = "list stacks for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get stack by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create stack in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete stack by name in workspace";
        public static final String SYNC_BY_NAME_IN_WORKSPACE = "syncs the stack by name in workspace";
        public static final String RETRY_BY_NAME_IN_WORKSPACE = "retries the stack by name in workspace";
        public static final String STOP_BY_NAME_IN_WORKSPACE = "stops the stack by name in workspace";
        public static final String START_BY_NAME_IN_WORKSPACE = "starts the stack by name in workspace";
        public static final String SCALE_BY_NAME_IN_WORKSPACE = "scales the stack by name in workspace";
        public static final String REPAIR_CLUSTER_IN_WORKSPACE = "repairs the stack by name in workspace";
        public static final String DELETE_WITH_KERBEROS_IN_WORKSPACE = "deletes the stack (with kerberos cluster) by name in workspace";
        public static final String GET_STACK_REQUEST_IN_WORKSPACE = "gets StackRequest by name in workspace";
        public static final String POST_STACK_FOR_CLUSTER_DEFINITION_IN_WORKSPACE = "posts stack for cluster definition in workspace";
        public static final String DELETE_INSTANCE_BY_ID_IN_WORKSPACE = "deletes instance from the stack's cluster in workspace";
        public static final String CHECK_IMAGE_IN_WORKSPACE = "checks image in stack by name in workspace";
    }

    public static class ClusterOpDescription {
        public static final String POST_FOR_STACK = "create cluster for stack";
        public static final String GET_BY_STACK_ID = "retrieve cluster by stack id";
        public static final String GET_PRIVATE_BY_NAME = "retrieve cluster by stack name (private)";
        public static final String GET_PUBLIC_BY_NAME = "retrieve cluster by stack name (public)";
        public static final String DELETE_BY_STACK_ID = "delete cluster on a specific stack";
        public static final String PUT_BY_STACK_ID = "update cluster by stack id";
        public static final String UPGRADE_AMBARI = "upgrade the Ambari version";
        public static final String SET_MAINTENANCE_MODE = "set maintenance mode for the cluster";
        public static final String GET_CLUSTER_PROPERTIES = "get cluster properties with cluster definition outputs";
        public static final String FAILURE_REPORT = "failure report";
        public static final String REPAIR_CLUSTER = "repair the cluster";
    }

    public static class GatewayOpDescription {
        public static final String UPDATE_GATEWAY_TOPOLOGIES = "update topologies of a gateway";
    }

    public static class ClusterTemplateOpDescription {
        public static final String LIST_BY_WORKSPACE = "list cluster templates for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get cluster template by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create cluster template in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete cluster template by name in workspace";
    }

    public static class RecipeOpDescription {
        public static final String GET_REQUEST_BY_NAME = "retrieve recipe request by recipe name";
        public static final String LIST_BY_WORKSPACE = "list recipes for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get recipe by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create recipe in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete recipe by name in workspace";
    }

    public static class WorkspaceOpDescription {
        public static final String POST = "create an workspace";
        public static final String GET = "retrieve workspaces";
        public static final String GET_BY_NAME = "retrieve an workspace by name";
        public static final String DELETE_BY_NAME = "delete an workspace by name";
        public static final String CHANGE_USERS = "change users and their permissions in the workspace";
        public static final String REMOVE_USERS = "removes users from the given workspace by their userIds";
        public static final String ADD_USERS = "adds users to the given workspace";
        public static final String UPDATE_USERS = "updates the users' permissions in the given workspace";
    }

    public static class EventOpDescription {
        public static final String GET_BY_TIMESTAMP = "retrieve events by timestamp (long)";
        public static final String GET_BY_NAME = "retrieve events by name";
        public static final String GET_EVENTS_BY_NAME = "retrieve events by name";
        public static final String GET_EVENTS_ZIP_BY_NAME = "retrieve events in zip by name";
    }

    public static class UserOpDescription {
        public static final String GET_TENANT_USERS = "retrieve all users in the tenant";
    }

    public static class UserProfileOpDescription {
        public static final String CURRENT_USER_DETAILS_EVICT = "remove current user from cache";
        public static final String GET_USER_PROFILE = "user related profile";
    }

    public static class AccountPreferencesDescription {
        public static final String GET = "retrieve account preferences for admin user";
    }

    public static class LdapConfigOpDescription {
        public static final String POST_CONNECTION_TEST = "test that the connection could be established of an existing or new LDAP config";
        public static final String GET_REQUEST = "get request";
        public static final String LIST_BY_WORKSPACE = "list LDAP configs for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get LDAP config by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create LDAP config in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete LDAP config by name in workspace";
        public static final String ATTACH_TO_ENVIRONMENTS = "attach ldap resource to environemnts";
        public static final String DETACH_FROM_ENVIRONMENTS = "detach ldap resource from environemnts";
    }

    public static class FileSystemOpDescription {
        public static final String FILE_SYSTEM_PARAMETERS = "returns filesystem parameters";
    }

    public static class UtilityOpDescription {
        public static final String STACK_MATRIX = "returns default ambari details for distinct HDP and HDF";
        public static final String KNOX_SERVICES = "returns supported knox services";
        public static final String CLOUD_STORAGE_MATRIX = "returns supported cloud storage for stack version";
        public static final String CUSTOM_PARAMETERS = "returns custom parameters";
        public static final String DATALAKE_PREREQUISITES = "returns datalake prerequisites";
        public static final String CHECK_CLIENT_VERSION = "checks the client version";
        public static final String NOTIFICATION_TEST = "Trigger a new notification to the notification system could be validated from the begins";
    }

    public static class DatabaseOpDescription {
        public static final String POST_CONNECTION_TEST = "test RDS connectivity";
        public static final String LIST_BY_WORKSPACE = "list RDS configs for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get RDS config by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create RDS config in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete RDS config by name in workspace";
        public static final String GET_REQUEST_IN_WORKSPACE = "get request in workspace";
        public static final String ATTACH_TO_ENVIRONMENTS = "attach RDS resource to environemnts";
        public static final String DETACH_FROM_ENVIRONMENTS = "detach RDS resource from environemnts";
    }

    public static class ProxyConfigOpDescription {
        public static final String LIST_BY_WORKSPACE = "list proxy configurations for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get proxy configuration by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create proxy configuration in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete proxy configuration by name in workspace";
        public static final String ATTACH_TO_ENVIRONMENTS = "attach proxy resource to environemnts";
        public static final String DETACH_FROM_ENVIRONMENTS = "detach proxy resource from environemnts";
        public static final String GET_REQUEST_BY_NAME = "get request by name";
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
        public static final String LIST_BY_WORKSPACE = "list management packs for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get management pack by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create management pack in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete management pack by name in workspace";
    }

    public static class KubernetesConfigOpDescription {
        public static final String LIST_BY_WORKSPACE = "list Kubernetes configs for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get Kubernetes config by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create Kubernetes config in workspace";
        public static final String PUT_IN_WORKSPACE = "modify Kubernetes config in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete Kubernetes config by name in workspace";
        public static final String ATTACH_TO_ENVIRONMENTS = "attach Kubernetes resource to environemnts";
        public static final String DETACH_FROM_ENVIRONMENTS = "detach Kubernetes resource from environemnts";
    }

    public static class ConnectorOpDescription {
        public static final String GET_DISK_TYPES = "retrive available disk types";
        public static final String GET_REGION_R_BY_TYPE = "retrive regions by type";
        public static final String GET_RECOMMENDATION = "creates a recommendation that advises cloud resources for the given cluster definition";
        public static final String GET_TAG_SPECIFICATIONS = "retrive tag specifications";
        public static final String GET_NETWORKS = "retrive network properties";
        public static final String GET_SECURITYGROUPS = "retrive securitygroups properties";
        public static final String GET_SSHKEYS = "retrive sshkeys properties";
        public static final String GET_VMTYPES_BY_CREDENTIAL = "retrive vmtype properties by credential";
        public static final String GET_GATEWAYS = "retrive gateways with properties";
        public static final String GET_IPPOOLS = "retrive ip pools with properties";
        public static final String GET_ACCESSCONFIGS = "retrive access configs with properties";
        public static final String GET_ENCRYPTIONKEYS = "retrive encryption keys with properties";
    }

    public static class SubscriptionOpDescription {
        public static final String SUBSCRIBE = "retrive subscribe identifier";
    }

    public static class SmartSenseSubOpDescription {
        public static final String GET = "retrieve default SmartSense subscription";
        public static final String LIST_BY_WORKSPACE = "list SmartSense subscriptions for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get SmartSense subscription by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create SmartSense subscription in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete SmartSense subscription by name in workspace";
    }

    public static class FlexSubOpDescription {
        public static final String SET_DEFAULT_IN_WORKSPACE = "sets the workspace default flag on the Flex subscription";
        public static final String SET_USED_FOR_CONTROLLER_IN_WORKSPACE = "sets the workspace 'used for controller' flag on the Flex subscription";
        public static final String LIST_BY_WORKSPACE = "list Flex subscriptions for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get Flex subscription by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create Flex subscription in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete Flex subscription by name in workspace";
    }

    public static class ImageCatalogOpDescription {
        public static final String PUT_BY_NAME = "update Image Catalog by id";
        public static final String GET_BY_IMAGE_CATALOG_NAME = "retrieve imagecatalog request by imagecatalog name";
        public static final String GET_IMAGES_BY_NAME = "determines available images for the given stack or platform"
                + "from the given imagecatalog name";
        public static final String GET_IMAGES = "determines available images for the given stack or platform"
                + "from the default image catalog";
        public static final String LIST_BY_WORKSPACE = "list image catalogs for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get image catalog by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create image catalog in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete image catalog by name in workspace";
    }

    public static class SecurityRuleOpDescription {
        public static final String GET_DEFAULT_SECURITY_RULES = "get default security rules";
    }

    public static class RepositoryConfigsValidationOpDescription {
        public static final String POST_REPOSITORY_CONFIGS_VALIDATION = "validate repository configs fields, check their availability";
    }

    public static class AuditOpDescription {
        public static final String LIST_IN_WORKSPACE = "List audit events for the given workspace";
        public static final String GET_BY_WORKSPACE = "Get audit event in workspace by id";
        public static final String LIST_IN_WORKSPACE_ZIP = "List audit events for the given workspace in zip file";
    }

    public static class EnvironmentOpDescription {

        public static final String CREATE = "Create an environment.";
        public static final String GET = "Get an environment.";
        public static final String ATTACH_RESOURCES = "Attach resources to an environment.";
        public static final String DETACH_RESOURCES = "Detach resources from an environment.";
        public static final String LIST = "List all environments in the workspace.";
        public static final String DELETE = "Delete an environment. Only possible if no cluster is running in the environment.";
        public static final String CHANGE_CREDENTIAL = "Changes the credential of the environment and the clusters in the environment.";
        public static final String REGISTER_EXTERNAL_DATALAKE = "Register external datalake";
        public static final String EDIT = "Edit and environment. Location, regions and description can be changed.";
    }

    public static class KerberosOpDescription {
        public static final String LIST_BY_WORKSPACE = "list kerberos configs for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get kerberos config by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create kerberos config in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete kerberos config by name in workspace";
        public static final String ATTACH_TO_ENVIRONMENTS = "attach kerberos config to environemnts";
        public static final String DETACH_FROM_ENVIRONMENTS = "detach kerberos config from environemnts";
        public static final String GET_REQUEST = "get request by name";
    }
}
