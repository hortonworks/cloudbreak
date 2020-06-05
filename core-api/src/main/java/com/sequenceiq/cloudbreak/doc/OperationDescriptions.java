package com.sequenceiq.cloudbreak.doc;

public class OperationDescriptions {
    public static class BlueprintOpDescription {
        public static final String GET_BY_NAME = "retrieve validation request by blueprint name";
        public static final String LIST_BY_WORKSPACE = "list blueprints for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get blueprint by name in workspace";
        public static final String GET_BY_CRN_IN_WORKSPACE = "get blueprint by crn";
        public static final String CREATE_IN_WORKSPACE = "create blueprint in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete blueprint by name in workspace";
        public static final String DELETE_BY_CRN_IN_WORKSPACE = "delete blueprint by crn";
        public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "delete multiple blueprints by name in workspace";
        public static final String CLI_COMMAND = "produce cli command input";
    }

    public static class StackOpDescription {
        public static final String GET_BY_CRN = "retrieve stack by crn";
        public static final String GET_STATUS_BY_NAME = "retrieve stack status by stack name";
        public static final String PUT_BY_ID = "update stack by id";
        public static final String PUT_BY_NAME = "update stack by name";
        public static final String GET_BY_AMBARI_ADDRESS = "retrieve stack by ambari address";
        public static final String GET_STACK_CERT = "retrieves the TLS certificate used by the gateway";
        public static final String GET_ALL = "retrieve all stacks";
        public static final String LIST_BY_WORKSPACE = "list stacks for the given workspace and environment name";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get stack by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create stack in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete stack by name in workspace";
        public static final String SYNC_BY_NAME_IN_WORKSPACE = "syncs the stack by name in workspace";
        public static final String RETRY_BY_NAME_IN_WORKSPACE = "retries the stack by name in workspace";
        public static final String STOP_BY_NAME_IN_WORKSPACE = "stops the stack by name in workspace";
        public static final String START_BY_NAME_IN_WORKSPACE = "starts the stack by name in workspace";
        public static final String SCALE_BY_NAME_IN_WORKSPACE = "scales the stack by name in workspace";
        public static final String REPAIR_CLUSTER_IN_WORKSPACE = "repairs the stack by name in workspace";
        public static final String UPGRADE_CLUSTER_IN_WORKSPACE = "upgrades the stack by name in workspace";
        public static final String CHECK_FOR_UPGRADE_CLUSTER_IN_WORKSPACE = "check for upgrades for the stack by name in workspace";
        public static final String DELETE_WITH_KERBEROS_IN_WORKSPACE = "deletes the stack (with kerberos cluster) by name in workspace";
        public static final String GET_STACK_REQUEST_IN_WORKSPACE = "gets StackRequest by name in workspace";
        public static final String POST_STACK_FOR_BLUEPRINT_IN_WORKSPACE = "posts stack for blueprint in workspace";
        public static final String DELETE_INSTANCE_BY_ID_IN_WORKSPACE = "deletes instance from the stack's cluster in workspace";
        public static final String DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE = "deletes multiple instances from the stack's cluster in workspace";
        public static final String CHECK_IMAGE_IN_WORKSPACE = "checks image in stack by name in workspace";
        public static final String GENERATE_HOSTS_INVENTORY = "Generate hosts inventory";
        public static final String CHECK_STACK_UPGRADE = "Checks for upgrade options by name";
        public static final String STACK_UPGRADE = "Upgrades a cluster to the latest CM or CDH version";
        public static final String LIST_RETRYABLE_FLOWS = "List retryable failed flows";
    }

    public static class ClusterOpDescription {
        public static final String PUT_BY_STACK_ID = "update cluster by stack id";
        public static final String SET_MAINTENANCE_MODE_BY_NAME = "set maintenance mode for the cluster by name";
        public static final String SET_MAINTENANCE_MODE_BY_CRN = "set maintenance mode for the cluster by crn";
        public static final String UPDATE_SALT = "Update salt states on cluster";
    }

    public static class ClusterTemplateOpDescription {
        public static final String LIST_BY_WORKSPACE = "list cluster templates for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get cluster template by name in workspace";
        public static final String GET_BY_CRN_IN_WORKSPACE = "get cluster template by crn in workspace";
        public static final String CREATE_IN_WORKSPACE = "create cluster template in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete cluster template by name in workspace";
        public static final String DELETE_BY_CRN_IN_WORKSPACE = "delete cluster template by crn in workspace";
        public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "delete multiple cluster templates by name in workspace";
    }

    public static class RecipeOpDescription {
        public static final String GET_REQUEST_BY_NAME = "retrieve recipe request by recipe name";
        public static final String LIST_BY_WORKSPACE = "list recipes for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get recipe by name in workspace";
        public static final String GET_BY_CRN_IN_WORKSPACE = "get recipe by crn in workspace";
        public static final String CREATE_IN_WORKSPACE = "create recipe in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete recipe by name in workspace";
        public static final String DELETE_BY_CRN_IN_WORKSPACE = "delete recipe by crn in workspace";
        public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "delete multiple recipes by name in workspace";
        public static final String CLI_COMMAND = "produce cli command input";
    }

    public static class WorkspaceOpDescription {
        public static final String POST = "create an workspace";
        public static final String GET = "retrieve workspaces";
        public static final String GET_BY_NAME = "retrieve an workspace by name";
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
        public static final String GET_TERMINATED_CLUSTERS_PREFERENCES = "get user preference to show or hide terminated clusters";
        public static final String PUT_TERMINATED_CLUSTERS_PREFERENCES = "set user preference to show or hide terminated clusters";
        public static final String DELETE_TERMINATED_INSTANCES_PREFERENCES = "delete user preference to show or hide terminated clusters";
    }

    public static class AccountPreferencesDescription {
        public static final String GET = "retrieve account preferences for admin user";
    }

    public static class FileSystemOpDescription {
        public static final String FILE_SYSTEM_PARAMETERS = "returns filesystem parameters";
    }

    public static class UtilityOpDescription {
        public static final String STACK_MATRIX = "returns default ambari details for distinct HDP and HDF";
        public static final String KNOX_SERVICES = "returns supported knox services";
        public static final String CLOUD_STORAGE_MATRIX = "returns supported cloud storage for stack version";
        public static final String CUSTOM_PARAMETERS = "returns custom parameters";
        public static final String NOTIFICATION_TEST = "Trigger a new notification to the notification system could be validated from the begins";
        public static final String CHECK_RIGHT = "Checking rights from UI";
        public static final String RENEW_CERTIFICATE = "Trigger a certificate renewal on the desired cluster which is identified via stack's name";
    }

    public static class DatabaseOpDescription {
        public static final String LIST = "list RDS configs";
        public static final String GET_BY_NAME = "get RDS config by name";
        public static final String CREATE = "create RDS config";
        public static final String DELETE_BY_NAME = "delete RDS config by name";
        public static final String DELETE_MULTIPLE_BY_NAME = "delete multiple RDS configs by name";
        public static final String GET_REQUEST = "get request";
    }

    public static class ConnectorOpDescription {
        public static final String GET_SERVICE_VERSIONS_BY_BLUEPRINT_NAME = "retrive services and versions";
        public static final String GET_RECOMMENDATION = "creates a recommendation that advises cloud resources for the given blueprint";
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
        public static final String GET_BY_CRN_IN_WORKSPACE = "get image catalog by crn in workspace";
        public static final String CREATE_IN_WORKSPACE = "create image catalog in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete image catalog by name in workspace";
        public static final String DELETE_BY_CRN_IN_WORKSPACE = "delete image catalog by crn in workspace";
        public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "delete multiple image catalogs by name in workspace";
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

    public static class InfoOpDescription {
        public static final String INFO = "get info";
    }
}
