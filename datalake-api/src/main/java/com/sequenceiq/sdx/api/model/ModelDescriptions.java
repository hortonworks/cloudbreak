package com.sequenceiq.sdx.api.model;

public class ModelDescriptions {

    public static final String RECOVERY_TYPE = "Type of the recovery operation, automatic restore is performed in case of 'RECOVER_WITH_DATA'.";

    public static final String RECOVERY_FORCE = "Boolean flag indicating whether to force the recovery or not.";

    public static final String BACKUP_ID = "The ID of the database backup.";

    public static final String BACKUP_LOCATION = "The location where the database backup will be stored.";

    public static final String BACKUP_TEMP_LOCATION = "The location where the database backup will be stored temporarily.";

    public static final String BACKUP_TIMEOUT_IN_MINUTES = "The maximum duration allowed for a backup in minutes.";

    public static final String RESTORE_TEMP_LOCATION = "The location where the database restore will be stored temporarily.";

    public static final String RESTORE_TIMEOUT_IN_MINUTES = "The maximum duration allowed for a restore in minutes.";

    public static final String OPERATION_TYPE = "The type of disaster recovery operation.";

    public static final String CLOSE_CONNECTIONS = "The conditional parameter for whether connections to the database will be closed during backup or not.";

    public static final String SKIP_DATABASE_NAMES = "The names of the Databases not to be backed up, or blank for all";

    public static final String ENVIRONMENT_CRN = "Environment crn.";

    public static final String ENVIRONMENT_NAME = "The name of the environment.";

    public static final String CLUSTER_SHAPE = "The shape of the cluster such as Micro Duty, Light Duty, Medium Duty...";

    public static final String DATA_ACCESS_ROLE = "The role used for cloud storage access.";

    public static final String RANGER_AUDIT_ROLE = "The role used by Ranger for cloud storage access.";

    public static final String CREDENTIAL_CRN = "The crn of the credential used for the cloud provider.";

    public static final String CLOUD_STORAGE_DETAILS = "Details about the cloud storage type and location.";

    public static final String CLOUD_STORAGE_BASE_LOCATION = "Cloud storage base location.";

    public static final String CLOUD_STORAGE_FILE_SYSTEM_TYPE = "Cloud storage file system type.";

    public static final String CLUSTER_TEMPLATE_NAME = "The name of the cluster template used for Data Lake creation.";

    public static final String FLOW_IDENTIFIER = "The id of the flow or flow chain that was triggered as part of the process.";

    public static final String CCM_UPGRADE_RESPONSE_TYPE = "Information about the CCM upgrade process.";

    public static final String CCM_UPGRADE_ERROR_REASON = "Reason of the error if CCM upgrade could not be started.";

    public static final String TEMPLATE_DETAILS = "Template details.";

    public static final String DATABASE_SERVER_UPGRADE_TARGET_MAJOR_VERSION = "The target major version the database server upgrade process should upgrade to.";

    public static final String OPERATION_STATUS = "Operation status.";

    public static final String OPERATION_STATUS_REASON = "Operation status reason.";

    public static final String STACK_REQUEST = "Stack request.";

    public static final String STACK_RESPONSE = "Stack response.";

    public static final String OPERATION_ID = "Operation id.";

    public static final String COMMAND_ID = "Command id.";

    public static final String RECOVERABLE_STATUS_REASON = "Description about why the cluster is recoverable or not recoverable.";

    public static final String RECOVERABLE_STATUS = "Status that indicates whether the cluster is recoverable or not recoverable.";

    public static final String INSTANCE_GROUP_NAME = "Instance group name.";

    public static final String INSTANCE_TYPE = "Instance type.";

    public static final String INSTANCE_DISK_SIZE = "Instance attached disk size.";

    public static final String IMD_UPDATE_TYPE = "Instance metadata update type.";

    public static final String IMAGE_CATALOG = "Image catalog.";

    public static final String IMAGE_ID = "Image id.";

    public static final String HOST_GROUP_NAME = "Host group name.";

    public static final String HOST_GROUP_NAMES = "Host group names.";

    public static final String NODE_IDS = "Node ids.";

    public static final String DELETE_VOLUMES = "If true, delete volumes, otherwise reattaches them to a newly created node instance.";

    public static final String DATA_LAKE_CRN = "Data Lake crn.";

    public static final String DATA_LAKE_NAME = "Data Lake name.";

    public static final String DATA_LAKE_STATUS = "Data Lake status.";

    public static final String DATA_LAKE_STATUS_REASON = "Data Lake status reason.";

    public static final String DATABASE_SERVER_CRN = "Database server crn.";

    public static final String STACK_CRN = "Stack crn.";

    public static final String CREATED = "Timestamp when the resource was created.";

    public static final String RUNTIME_VERSION = "Runtime version.";

    public static final String OS = "Operating system.";

    public static final String ARCHITECTURE = "Cpu architecture.";

    public static final String DEFAULT_RUNTIME_VERSION = "Default runtime version.";

    public static final String LOCK_COMPONENTS = "Option to lock components during the upgrade.";

    public static final String DRY_RUN = "Doesn't perform the actual operation just validates if all preconditions are met.";

    public static final String VALIDATION_ONLY = "Doesn't perform the actual operation just validates if all preconditions are met.";

    public static final String SKIP_DATAHUB_VALIDATION = "With this option, the Data Lake upgrade can be performed with running Data Hub clusters. "
            + "The usage of this option can cause problems on the running Data Hub clusters during the Data Lake upgrade.";

    public static final String ROLLING_UPGRADE_ENABLED = "Doesn't perform the actual operation just validates if all preconditions are met.";

    public static final String SKIP_BACKUP = "Option to skip the backup before the upgrade.";

    public static final String SKIP_VALIDATION = "Option to skip validation before the backup/restore.";

    public static final String SKIP_ATLAS = "Option to skip the backup/restore of Atlas data.";

    public static final String SKIP_RANGER_AUDIT = "Option to skip the backup/restore of Ranger Audit data.";

    public static final String SKIP_RANGER_METADATA = "Option to skip the backup/restore of Ranger HMS Metadata.";

    public static final String SHOW_AVAILABLE_IMAGES = "Option to show available images.";

    public static final String REPLACE_VMS = "Option to replace virtual machines  during the upgrade.";

    public static final String KEEP_VARIANT = "Value which indicates that the internal cloud platform variant should not be changed during the operation.";

    public static final String LOAD_BALANCER_SKU = "Load balancer SKU type.";

    public static final String CURRENT_IMAGE = "Information about the image that the Data Lake cluster is using currently.";

    public static final String UPGRADE_CANDIDATE_IMAGES = "Images that the Data Lake cluster can be upgraded to.";

    public static final String UPGRADE_ERROR_REASON = "Error reason why the Data Lake cluster can't be upgraded.";

    public static final String RANGER_RAZ_ENABLED = "Option to enable ranger raz.";

    public static final String RANGER_CLOUD_ACCESS_AUTHORIZER_ROLE = "AWS IAM role for Ranger authorizer";

    public static final String RANGER_RMS_ENABLED = "Option to enable ranger rms.";

    public static final String MULTI_AZ_ENABLED = "Option to enable multi availability zones.";

    public static final String CLOUD_PROVIDER_VARIANT = "cloud provider variant.";

    public static final String TAGS = "Tags.";

    public static final String DATA_LAKE_CLUSTER_SERVICE_VERSION = "Data Lake cluster service version.";

    public static final String DETACHED = "Shows whether the cluster is in detached state.";

    public static final String DATABASE_ENGINE_VERSION = "Database engine version.";

    public static final String AZURE_DATABASE_REQUEST = "Azure Database request.";

    public static final String AZURE_DATABASE_TYPE = "The type of the azure database: single server / flexible server";

    public static final String EXTERNAL_DATABASE_OPTIONS = "External database options.";

    public static final String DISABLE_DB_SSL_ENFORCEMENT = "Option to enforce disabling database SSL encryption for clusters.";

    public static final String AWS_OPTIONS = "AWS options.";

    public static final String AZURE_OPTIONS = "Azure options.";

    public static final String CUSTOM_INSTANCE_GROUP_OPTIONS = "Custom instance group options.";

    public static final String RECIPES = "Recipes.";

    public static final String RECIPE_NAME = "Recipe name.";

    public static final String SPOT_PARAMETERS = "Spot parameters.";

    public static final String IMAGE_SETTINGS = "Image settings.";

    public static final String RECENT_FLOW_OPERATIONS = "Recent flow operations.";

    public static final String RECENT_INTERNAL_FLOW_OPERATIONS = "Recent internal flow operations.";

    public static final String LAST_FLOW_OPERATION = "Last flow operations.";

    public static final String LAST_INTERNAL_FLOW_OPERATION = "Last internal flow operations.";

    public static final String CREATE_DATABASE_OPTION = "Option to create external database.";

    public static final String DATABASE_AVAILABILITY_TYPE = "Database availability type.";

    public static final String AZURE_USER_MAPPING = "Azure user mapping.";

    public static final String AZURE_GROUP_MAPPING = "Deprecated. Azure group mapping.";

    public static final String AVAILABLE_VM_TYPES = "Collection of available vm types";

    public static final String VM_TYPE_CONFIGS = "Virtual machine type configurations";

    public static final String VM_TYPE_PROPERTIES = "Virtual machine type properties";

    public static final String VM_TYPE_NAME = "Virtual machine type name";

    public static final String VM_TYPE_METADATA = "Virtual machine type meta data";

    public static final String VOLUME_PARAMETER_TYPE = "Volume parameter type";

    public static final String VOLUME_MINIMUM_SIZE = "Volume minimum size";

    public static final String VOLUME_MAXIMUM_SIZE = "Volume maximum size";

    public static final String VOLUME_MINIMUM_NUMBER = "Volume minimum number";

    public static final String VOLUME_MAXIMUM_NUMBER = "Volume maximum number";

    public static final String STOPPABLE = "Whether the cluster is stoppable or not";

    public static final String UNSTOPPABLE_REASON = "Reason why the cluster is not stoppable";

    public static final String DATAHUB_CRNS_REFRESHED = "List of Data Hubs that are refreshed with CRN";

    public static final String TARGET_MAJOR_VERSION = "The database major version to upgrade to";

    public static final String BACKUP_INCLUDED_DATA =
            "Data included in the backup (RANGER_PERMISSIONS, HMS_METADATA, RANGER_AUDITS, ATLAS_METADATA, ATLAS_INDEXES)";

    public static final String BACKUP_TIMESTAMP = "When backup was made (start of the backup procedure)";

    public static final String JAVA_VERSION = "Java version to be forced on virtual machines";

    public static final String DATABASE_BACKUP_RESTORE_MAX_DURATION = "The maximum duration allowed for a backup or restore";

    public static final String FORCED = "Force operation. It will disable some of the validation.";

    public static final String DATABASE = "Database details";

    public static final String DATABASE_INSTANCE_TYPE = "Database instance type";

    public static final String DATABASE_INSTANCE_STORAGE = "Database instance storage";

    public static final String SECURITY = "Security related objects";

    public static final String BACKUP_END_TIMESTAMP = "When backup was completed (end of the backup procedure)";

    public static final String PROVIDER_SYNC_STATES = "Contains information about provider issues. No issues found if empty or 'VALID'.";

    private ModelDescriptions() {
    }

    public static class SdxRotateRdsCertificateDescription {
        public static final String FLOW_ID = "Flow identifier for the current Rotate Database Certificate operation.";
        public static final String ROTATE_RDS_CERTIFICATE_RESPONSE_TYPE = "Information about the Rotate Database Certificate operation acceptance.";
        public static final String MIGRATE_DATABASE_TO_SSL_RESPONSE_TYPE = "Information about the Migrate Database operation acceptance.";
        public static final String ROTATE_RDS_CERTIFICATE_ERROR_REASON = "Reason of the error if Rotate Database Certificate could not be started.";
        public static final String MIGRATE_DATABASE_TO_SSL_ERROR_REASON = "Reason of the error if Migrate Database could not be started.";
        public static final String RDS_CERTIFICATE_ROTATION_BY_NAME = "rotate Database certificate of the data lake by name";
        public static final String RDS_CERTIFICATE_ROTATION_BY_CRN = "rotate Database certificate of the data lake by crn";
        public static final String ROTATE_RDS_CERTIFICATE_NOTES = "Rotate the public certificate of the Database";
        public static final String MIGRATE_DATABASE_TO_SSL_BY_NAME = "Migrate database to ssl of the data lake by name";
        public static final String MIGRATE_DATABASE_TO_SSL_BY_CRN = "Migrate database to ssl of the data lake by crn";
        public static final String MIGRATE_DATABASE_TO_SSL_NOTES = "Migrate non ssl to ssl based Database";
    }
}
