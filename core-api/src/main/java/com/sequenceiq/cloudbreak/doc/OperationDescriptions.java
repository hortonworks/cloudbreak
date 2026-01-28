package com.sequenceiq.cloudbreak.doc;

public class OperationDescriptions {
    public static class BlueprintOpDescription {
        public static final String GET_BY_NAME = "retrieve validation request by blueprint name";
        public static final String LIST_BY_WORKSPACE = "list blueprints for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get blueprint by name in workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE_INTERNAL = "get blueprint by name in workspace internal";
        public static final String GET_BY_CRN_IN_WORKSPACE = "get blueprint by crn";
        public static final String CREATE_IN_WORKSPACE = "create blueprint in workspace";
        public static final String CREATE_IN_WORKSPACE_INTERNAL = "create blueprint in workspace internal";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete blueprint by name in workspace";
        public static final String DELETE_BY_CRN_IN_WORKSPACE = "delete blueprint by crn";
        public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "delete multiple blueprints by name in workspace";
        public static final String CLI_COMMAND = "produce cli command input";
    }

    public static class StackOpDescription {
        public static final String GET_BY_CRN = "retrieve stack by crn";
        public static final String GET_DEPENDENT_HOSTGROUPS_BY_CRN = "retrieve dependent hostgroups for autoscale policy by crn";
        public static final String GET_STATUS_BY_NAME = "retrieve stack status by stack name";
        public static final String PUT_BY_ID = "update stack by id";
        public static final String PUT_START_INSTANCES_BY_ID = "update stack to start instances by id";
        public static final String PUT_BY_NAME = "update stack by name";
        public static final String GET_STACK_CERT = "retrieves the TLS certificate used by the gateway";
        public static final String GET_ALL = "retrieve all stacks";
        public static final String LIST_BY_WORKSPACE = "list stacks for the given workspace and environment name";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get stack by name in workspace";
        public static final String GET_BY_NAME_WITH_RESOURCES_IN_WORKSPACE = "get stack by name with resources in workspace";
        public static final String GET_BY_CRN_WITH_RESOURCES_IN_WORKSPACE = "get stack by crn with resources in workspace";
        public static final String GET_BY_CRN_IN_WORKSPACE = "get stack by crn in workspace";
        public static final String GET_AUTOSCALE_BY_NAME = "get autoscale stack by name in workspace";
        public static final String GET_INTERNAL_AUTOSCALE_BY_NAME = "get internal autoscale stack by name in workspace";
        public static final String GET_AUTOSCALE_BY_CRN = "get autoscale stack by crn in workspace";
        public static final String CREATE_IN_WORKSPACE = "create stack in workspace";
        public static final String CREATE_IN_WORKSPACE_INTERNAL = "create stack in workspace, internal only";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete stack by name in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE_INTERNAL = "delete stack by name in workspace, internal only";
        public static final String UPDATE_BY_NAME_IN_WORKSPACE = "update stack by name in workspace";
        public static final String UPDATE_LOAD_BALANCER_PEM_DNS_IN_WORKSPACE = "update load balancer PEM dns in workspace";
        public static final String UPDATE_LOAD_BALANCER_IPA_DNS_IN_WORKSPACE = "update load balancer free IPA dns in workspace";
        public static final String SYNC_BY_NAME_IN_WORKSPACE = "syncs the stack by name in workspace";
        public static final String SYNC_CM_BY_NAME_IN_WORKSPACE = "syncs from CM the parcel and CM versions by name in workspace";
        public static final String RETRY_BY_NAME_IN_WORKSPACE = "retries the stack by name in workspace";
        public static final String STOP_BY_NAME_IN_WORKSPACE = "stops the stack by name in workspace";
        public static final String STOP_BY_NAME_IN_WORKSPACE_INTERNAL = "stops the stack by name in workspace, internal only";
        public static final String RESTART_SERVICES_BY_NAME_IN_WORKSPACE_INTERNAL = "restarts services in the cluster, internal only";
        public static final String START_BY_NAME_IN_WORKSPACE = "starts the stack by name in workspace";
        public static final String START_BY_NAME_IN_WORKSPACE_INTERNAL = "starts the stack by name in workspace, internal only";
        public static final String ROTATE_SALT_PASSWORD_BY_CRN_IN_WORKSPACE_INTERNAL = "rotates the SaltStack user password of stack by crn in workspace, " +
                "internal only";
        public static final String SALT_PASSWORD_STATUS = "checks if salt password rotation is needed";
        public static final String SCALE_BY_NAME_IN_WORKSPACE = "scales the stack by name in workspace";
        public static final String REPAIR_CLUSTER_IN_WORKSPACE = "repairs the stack by name in workspace";
        public static final String REPAIR_CLUSTER_IN_WORKSPACE_INTERNAL = "repairs the stack by name in workspace, internal only";
        public static final String UPGRADE_CLUSTER_IN_WORKSPACE = "upgrades the stack by name in workspace";
        public static final String UPGRADE_OS_IN_WORKSPACE = "Upgrades the cluster OS by name and upgrades sets internal";
        public static final String UPGRADE_CLUSTER_IN_WORKSPACE_INTERNAL = "upgrades the stack by name in workspace, internal only";
        public static final String RECOVER_CLUSTER_IN_WORKSPACE_INTERNAL = "recovers the stack by name in workspace, internal only";
        public static final String REFRESH_RECIPES_IN_WORKSPACE = "refresh recipes on the cluster by name in workspace";
        public static final String REFRESH_RECIPES_IN_WORKSPACE_INTERNAL = "refresh recipes on the cluster by name in workspace, internal only";
        public static final String ATTACH_RECIPE_IN_WORKSPACE = "attach recipe to the cluster by name in workspace";
        public static final String ATTACH_RECIPE_IN_WORKSPACE_INTERNAL = "attach recipe to the cluster by name in workspace, internal only";
        public static final String DETACH_RECIPE_IN_WORKSPACE = "detach recipe from the cluster by name in workspace";
        public static final String DETACH_RECIPE_IN_WORKSPACE_INTERNAL = "detach recipe from the cluster by name in workspace, internal only";
        public static final String REFRESH_RECIPES_BY_NAME = "refresh recipes on the cluster by stack name";
        public static final String REFRESH_RECIPES_BY_CRN = "refresh recipes on the cluster by stack crn";
        public static final String ATTACH_RECIPE_BY_CRN = "attach recipe to the cluster by stack crn";
        public static final String ATTACH_RECIPE_BY_NAME = "attach recipe to the cluster by stack crn";
        public static final String DETACH_RECIPE_BY_CRN = "detach recipe from the cluster by stack crn";
        public static final String DETACH_RECIPE_BY_NAME = "detach recipe from the cluster by stack crn";
        public static final String CHECK_FOR_UPGRADE_CLUSTER_IN_WORKSPACE = "check for upgrades for the stack by name in workspace";
        public static final String DELETE_WITH_KERBEROS_IN_WORKSPACE = "deletes the stack (with kerberos cluster) by name in workspace";
        public static final String GET_STACK_REQUEST_IN_WORKSPACE = "gets StackRequest by name in workspace";
        public static final String POST_STACK_FOR_BLUEPRINT_IN_WORKSPACE = "posts stack for blueprint in workspace";
        public static final String DELETE_INSTANCE_BY_ID_IN_WORKSPACE = "deletes instance from the stack's cluster in workspace";
        public static final String DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE = "deletes multiple instances from the stack's cluster in workspace";
        public static final String RESTART_MULTIPLE_INSTANCES = "Restart multiple instances from the stack's cluster";
        public static final String STOP_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE = "stop multiple instances from the stack's cluster in workspace";
        public static final String CHECK_IMAGE_IN_WORKSPACE = "checks image in stack by name in workspace";
        public static final String CHECK_IMAGE_IN_WORKSPACE_INTERNAL = "checks image in stack by name in workspace, internal only";
        public static final String GENERATE_HOSTS_INVENTORY = "Generate hosts inventory";
        public static final String CHECK_STACK_UPGRADE = "Checks for upgrade options by name";
        public static final String STACK_UPGRADE = "Upgrades a cluster to the latest CM or CDH version";
        public static final String STACK_UPGRADE_INTERNAL = "Upgrades a cluster to the latest CM or CDH version, internal only";
        public static final String CHECK_RDS_UPGRADE_INTERNAL =
                "Checks that upgrade of the external database of a cluster to a given version is possible, internal only";
        public static final String RDS_UPGRADE_INTERNAL = "Upgrades the external database of a cluster to a given version, internal only";
        public static final String LIST_RETRYABLE_FLOWS = "List retryable failed flows";
        public static final String DATABASE_BACKUP = "Performs a backup of the database to a provided location";
        public static final String DATABASE_BACKUP_INTERNAL = "Performs a backup of the database to a provided location, internal only";
        public static final String DATABASE_RESTORE = "Performs a restore of the database from a provided location";
        public static final String DATABASE_RESTORE_INTERNAL = "Performs a restore of the database from a provided location, internal only";
        public static final String ROTATE_CERTIFICATES = "Rotates the certificates of the cluster";
        public static final String RENEW_CERTIFICATE = "Trigger a certificate renewal on the desired cluster which is identified via stack's name";
        public static final String UPDATE_LOAD_BALANCERS = "Updates an existing cluster with load balancers, including adding the Endpoint Gateway " +
                "if it's enabled.";
        public static final String CHANGE_IMAGE_CATALOG = "Changes image catalog of the cluster";
        public static final String RANGER_RAZ_ENABLED = "Determines if Ranger Raz is present in the cluster.";
        public static final String GENERATE_IMAGE_CATALOG = "Generates an image catalog that only contains the currently used image for creating instances";
        public static final String RE_REGISTER_CLUSTER_PROXY_CONFIG = "Re-registers the cluster proxy config for the cluster if needed.";
        public static final String GET_USED_SUBNETS_BY_ENVIRONMENT_CRN = "List the used subnets by the given Environment resource CRN";
        public static final String GET_DELETED_STACKS_SINCE_TS = "retrieve deleted stacks after given timestamp";
        public static final String GET_CLUSTER_PROXY_CONFIGURATION = "Get Cluster Proxy configuration";
        public static final String GET_LIMITS_CONFIGURATION = "Get limits configuration";
        public static final String GET_RECOMMENDATION = "Get recommendation";
        public static final String AUTHORIZE_FOR_AUTOSCALE = "Authorize for Autoscale";
        public static final String IMD_UPDATE = "update instance metadata for stack's instances";
        public static final String REFRESH_ENTITLEMENTS_PARAMS = "Refresh configurations based on entitlements";
    }

    public static class ClusterOpDescription {
        public static final String PUT_BY_STACK_ID = "update cluster by stack id";
        public static final String SET_MAINTENANCE_MODE_BY_NAME = "set maintenance mode for the cluster by name";
        public static final String SET_MAINTENANCE_MODE_BY_CRN = "set maintenance mode for the cluster by crn";
        public static final String UPDATE_SALT = "Update salt states on cluster";
        public static final String UPDATE_PILLAR_CONFIG = "Update salt pillar configuration on cluster";
    }

    public static class ClusterTemplateOpDescription {
        public static final String LIST_BY_WORKSPACE = "list cluster templates for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get cluster template by name in workspace";
        public static final String GET_BY_CRN_IN_WORKSPACE = "get cluster template by crn in workspace";
        public static final String CREATE_IN_WORKSPACE = "create cluster template in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete cluster template by name in workspace";
        public static final String DELETE_BY_CRN_IN_WORKSPACE = "delete cluster template by crn in workspace";
        public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "delete multiple cluster templates by name in workspace";
        public static final String LIST_BY_ENV = "list cluster templates by environment crn";
    }

    public static class RecipeOpDescription {
        public static final String GET_REQUEST_BY_NAME = "retrieve recipe request by recipe name";
        public static final String GET_REQUESTS_BY_NAMES = "retrieve recipes request by recipe names";
        public static final String LIST_BY_WORKSPACE = "list recipes for the given workspace";
        public static final String LIST_BY_WORKSPACE_INTERNAL = "list recipes for the given workspace internal";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get recipe by name in workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE_INTERNAL = "get recipe by name in workspace internal";
        public static final String GET_BY_CRN_IN_WORKSPACE = "get recipe by crn in workspace";
        public static final String CREATE_IN_WORKSPACE = "create recipe in workspace";
        public static final String CREATE_IN_WORKSPACE_INTERNAL = "create recipe in workspace internal";
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
        public static final String BULK_GET = "retrieve events for multiple stack";
        public static final String GET_BY_TIMESTAMP = "retrieve events by timestamp (long)";
        public static final String GET_BY_NAME = "retrieve events by name";
        public static final String GET_BY_CRN = "retrieve events by crn";
        public static final String GET_EVENTS_BY_NAME = "retrieve events by name";
        public static final String GET_EVENTS_BY_CRN = "retrieve events by crn";
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
        public static final String FILE_SYSTEM_PARAMETERS_INTERNAL = "returns filesystem parameters using internal actor";
    }

    public static class UtilityOpDescription {
        public static final String STACK_MATRIX = "returns default ambari details for distinct HDP and HDF";
        public static final String KNOX_SERVICES = "returns supported knox services";
        public static final String CLOUD_STORAGE_MATRIX = "returns supported cloud storage for stack version";
        public static final String CUSTOM_PARAMETERS = "returns custom parameters";
        public static final String NOTIFICATION_TEST = "Trigger a new notification to the notification system could be validated from the begins";
        public static final String RENEW_CERTIFICATE = "Trigger a certificate renewal on the desired cluster which is identified via stack's name";
        public static final String USED_IMAGES = "List the images that are in use";
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
        public static final String GET_RECOMMENDATION_BY_DATAHUB_CRN = "creates a recommendation that advises cloud resources for the given blueprint" +
                " based on the given datahub crn.";
        public static final String GET_RECOMMENDATION_BY_CRED_CRN = "creates a recommendation that advises cloud resources for the given" +
                " blueprint based on the given credential crn.";

        public static final String GET_RECOMMENDATION_BY_ENV_CRN = "creates a recommendation that advises cloud resources for the given" +
                " blueprint based on the given environment crn.";
    }

    public static class ImageCatalogOpDescription {
        public static final String PUT_BY_NAME = "update Image Catalog by id";
        public static final String GET_BY_IMAGE_CATALOG_NAME = "retrieve imagecatalog request by imagecatalog name";
        public static final String GET_IMAGES_BY_NAME = "determines available images for the given stack or platform"
                + "from the given imagecatalog name";
        public static final String GET_IMAGE_BY_ID = "determines the image for the image UUID using a default imagecatalog name";
        public static final String GET_IMAGE_BY_NAME_AND_ID = "determines the image for the image UUID"
                + "from the given imagecatalog name";
        public static final String GET_IMAGES = "determines available images for the given stack or platform"
                + "from the default image catalog";
        public static final String LIST_BY_WORKSPACE = "list image catalogs for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get image catalog by name in workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE_INTERNAL = "get image catalog by name in workspace internal";
        public static final String GET_BY_CRN_IN_WORKSPACE = "get image catalog by crn in workspace";
        public static final String CREATE_IN_WORKSPACE = "create image catalog in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete image catalog by name in workspace";
        public static final String DELETE_BY_CRN_IN_WORKSPACE = "delete image catalog by crn in workspace";
        public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "delete multiple image catalogs by name in workspace";
        public static final String GET_IMAGE_FROM_DEFAULT = "returns an image from the default catalog based on the provided filter";
        public static final String GET_IMAGE_FROM_DEFAULT_BY_ID = "returns an image from the default catalog by id";
        public static final String GET_DEFAULT_IMAGE_CATALOG_RUNTIME_VERSIONS = "get runtime versions from the default catalog";
        public static final String VALIDATE_RECOMMENDED_IMAGE_WITH_PROVIDER = "validate recommended image with provider";
    }

    public static class CustomImageCatalogOpDescription {
        public static final String LIST = "list image catalogs";
        public static final String GET_BY_NAME = "get custom image catalog by name";
        public static final String CREATE = "create custom image catalog";
        public static final String DELETE_BY_NAME = "delete custom image catalog by name";
        public static final String GET_BY_NAME_IN_CATALOG = "get custom image by name in custom image catalog";
        public static final String CREATE_IN_CATALOG = "create a new image in an already existing custom image catalog";
        public static final String UPDATE_IN_CATALOG = "update an existing image in a custom image catalog";
        public static final String DELETE_FROM_CATALOG = "delete an existing image from a custom image catalog";
    }

    public static class SecurityRuleOpDescription {
        public static final String GET_DEFAULT_SECURITY_RULES = "get default security rules";
    }

    public static class RepositoryConfigsValidationOpDescription {
        public static final String POST_REPOSITORY_CONFIGS_VALIDATION = "validate repository configs fields, check their availability";
    }

    public static class AuditOpDescription {
        public static final String LIST_IN_WORKSPACE = "List audit events for the given workspace. Please use the API filter by resource crn, " +
                "because the resource type and id combination is deprecated.";
        public static final String GET_BY_WORKSPACE = "Get audit event in workspace by id";
        public static final String LIST_IN_WORKSPACE_ZIP = "List audit events for the given workspace in zip file. Please use the API filter " +
                "by resource crn, because the resource type and id combination is deprecated.";
    }

    public static class InfoOpDescription {
        public static final String INFO = "get info";
    }

    public static class CostOpDescription {
        public static final String LIST = "List costs based on cluster crns";

        public static final String LIST_BY_ENV = "List costs based on cluster crns and environment crns";
    }

    public static class CO2OpDescription {
        public static final String LIST = "List CO2 cost based on cluster CRNs";
        public static final String LIST_BY_ENV = "List CO2 costs based on cluster CRNs and environment CRNs";
    }

    public static class EncryptionOpDescription {
        public static final String GET_ENCRYPTION_KEYS = "Get encryption keys of the stack";
    }
}
