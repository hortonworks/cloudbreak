package com.sequenceiq.redbeams.doc;

public final class ModelDescriptions {

    public static final String ALLOCATE_DATABASE_SERVER_REQUEST = "Request for allocating a new database server in a provider";
    public static final String SSL_CONFIG_REQUEST = "Request for the SSL config of a database server";
    public static final String CREATE_DATABASE_REQUEST = "Request for creating a new database on a registered database server";
    public static final String CREATE_DATABASE_RESPONSE = "Response for creating a new database on a registered database server";
    public static final String DATABASE_IDENTIFIERS = "Identifiers that together identify a database in an environment";
    public static final String DATABASE_REQUEST = "Request containing information about a database to be registered";
    public static final String DATABASE_RESPONSE = "Response containing information about a database that was acted upon, e.g., retrieved, deleted, listed";
    public static final String DATABASE_RESPONSES = "A set of multiple database responses";
    public static final String DATABASE_SERVER_REQUEST = "Request containing information about a database server to be registered";
    public static final String DATABASE_SERVER_RESPONSE = "Response containing information about a database server that was acted upon, e.g., retrieved, "
        + "deleted, listed";
    public static final String DATABASE_SERVER_RESPONSES = "A set of multiple database server responses";
    public static final String SSL_CONFIG_RESPONSE = "Response for the SSL config of a database server";
    public static final String DATABASE_SERVER_STATUS_RESPONSE = "Response containing status information about a database server";
    public static final String DATABASE_SERVER_TEST_REQUEST = "Request for testing connectivity to a database server";
    public static final String DATABASE_SERVER_TEST_RESPONSE = "Response for testing connectivity to a database server";
    public static final String DATABASE_TEST_REQUEST = "Request for testing connectivity to a database";
    public static final String DATABASE_TEST_RESPONSE = "Response for testing connectivity to a database";

    private ModelDescriptions() {
    }

    public static class Database {
        public static final String CRN = "CRN of the database";
        public static final String DESCRIPTION = "Description of the database";
        public static final String CONNECTION_URL = "JDBC connection URL in the form of jdbc:<db-type>:<driver-specific-part>";
        public static final String CONNECTION_DRIVER = "Name of the JDBC connection driver (for example: 'org.postgresql.Driver')";
        public static final String DB_ENGINE = "Name of the database vendor (MYSQL, POSTGRES...)";
        public static final String DB_ENGINE_DISPLAYNAME = "Display name of the database vendor (MySQL, PostgreSQL, ...)";
        public static final String VERSION = "Version of the Database";
        public static final String USERNAME = "Username to use for authentication";
        public static final String PASSWORD = "Password to use for authentication";
        public static final String NAME = "Name of the database";
        public static final String TYPE = "Type of database, i.e., the service name that will use the database (HIVE, DRUID, SUPERSET, RANGER, ...)";
        public static final String CREATE_RESULT = "Result of database creation";
        public static final String ENVIRONMENT_CRN = "CRN of the environment of the database";
        public static final String CREATION_DATE = "Creation date / time of the database, in epoch milliseconds";
        public static final String RESOURCE_STATUS = "Ownership status of the database";
    }

    public static class DatabaseTest {
        public static final String EXISTING_REQUEST = "Identifiers of registered database to be tested for connectivity";
        public static final String NEW_REQUEST = "Information about a unregistered database to be tested for connectivity";
        public static final String RESULT = "Result of database connection test";
    }

    public static class DatabaseServer {
        public static final String ID = "Internal ID of the database server";
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
        public static final String CREATION_DATE = "Creation date / time of the database server, in epoch milliseconds";
        public static final String RESOURCE_STATUS = "Ownership status of the database server";
        public static final String STATUS = "Status of the database server stack";
        public static final String STATUS_REASON = "Additional status information about the database server stack";
        public static final String SSL_CERTIFICATES = "Set of relevant SSL certificates for the database server, including the active one";
        public static final String SSL_CERTIFICATE_TYPE = "SSL certificate type";
        public static final String SSL_MODE = "SSL enforcement mode for the database server";
        public static final String SSL_CONFIG = "SSL config of the database server";
        public static final String SSL_CERTIFICATE_ACTIVE_VERSION = "Version number of the SSL certificate currently active for the database server";
        public static final String SSL_CERTIFICATE_HIGHEST_AVAILABLE_VERSION =
                "Highest version number of the SSL certificate available for the database server; does not necessarily equal the active version";
        public static final String SSL_CERTIFICATE_ACTIVE_CLOUD_PROVIDER_IDENTIFIER =
                "Cloud provider specific identifier of the SSL certificate currently active for the database server";
        public static final String TAGS = "UserDefined tags for the DB";
    }

    public static class DatabaseServerTest {
        public static final String EXISTING_CRN = "CRN of registered database server to be tested for connectivity";
        public static final String NEW_REQUEST = "Information about an unregistered database server to be tested for connectivity";
        public static final String RESULT = "Result of database server connection test";
    }

    public static class DBStack {
        public static final String STACK_NAME = "Name of the database stack";
        public static final String NETWORK = "Network information for the database stack";
        public static final String DATABASE_SERVER = "Database server information for the database stack";
        public static final String AWS_PARAMETERS = "AWS-specific parameters for the database stack";
        public static final String AZURE_PARAMETERS = "Azure-specific parameters for the database stack";
    }

    public static class NetworkModelDescriptions {
        public static final String AWS_PARAMETERS = "AWS-specific parameters for the network";
        public static final String GCP_PARAMETERS = "GCP-specific parameters for the network";
        public static final String AZURE_PARAMETERS = "Azure-specific parameters for the network";
    }

    public static class AwsNetworkModelDescriptions {
        public static final String SUBNET_ID = "Subnet ID(s) of the specified AWS network";
    }

    public static class DatabaseServerModelDescriptions {
        public static final String INSTANCE_TYPE = "Instance type of the database server";
        public static final String DATABASE_VENDOR = DatabaseServer.DATABASE_VENDOR;
        public static final String CONNECTION_DRIVER = DatabaseServer.CONNECTION_DRIVER;
        public static final String STORAGE_SIZE = "Storage size of the database server, in GB";
        public static final String ROOT_USER_NAME = DatabaseServer.USERNAME;
        public static final String ROOT_USER_PASSWORD = DatabaseServer.PASSWORD;
        public static final String PORT = DatabaseServer.PORT;
        public static final String AWS_PARAMETERS = "AWS-specific parameters for the database server";
        public static final String GCP_PARAMETERS = "GCP-specific parameters for the database server";
        public static final String AZURE_PARAMETERS = "Azure-specific parameters for the database server";
        public static final String SECURITY_GROUP = "Security group of the database server";
    }

    public static class AzureNetworkModelDescription {
        public static final String SUBNETS = "Comma-separated list of fully-qualified subnets with connectivity to the database server";
    }

    public static class GcpNetworkModelDescription {
        public static final String SUBNETS = "Comma-separated list of fully-qualified subnets with connectivity to the database server";
    }

    public static class AwsDatabaseServerModelDescriptions {
        public static final String BACKUP_RETENTION_PERIOD = "Time to retain backups, in days";
        public static final String ENGINE_VERSION = "Version of the database engine (vendor)";
        public static final String MULTI_AZ = "Whether to use a multi-AZ deployment";
        public static final String STORAGE_TYPE = "Storage type";
    }

    public static class AzureDatabaseServerModelDescriptions {
        public static final String BACKUP_RETENTION_DAYS = "Time to retain backups, in days";
        public static final String GEO_REDUNDANT_BACKUPS = "Whether backups are geographically redundant";
        public static final String SKU_CAPACITY = "The number of vCPUs assigned to the database server";
        public static final String SKU_FAMILY = "The family of hardware used for the database server";
        public static final String SKU_TIER = "The tier of SKU for the database server";
        public static final String STORAGE_AUTO_GROW = "Whether the database server will automatically grow storage when necessary";
        public static final String DB_VERSION = "The version of the database software to use";
    }

    public static class GcpDatabaseServerModelDescriptions {
        public static final String BACKUP_RETENTION_DAYS = "Time to retain backups, in days";
        public static final String GEO_REDUNDANT_BACKUPS = "Whether backups are geographically redundant";
        public static final String SKU_CAPACITY = "The number of vCPUs assigned to the database server";
        public static final String SKU_FAMILY = "The family of hardware used for the database server";
        public static final String SKU_TIER = "The tier of SKU for the database server";
        public static final String STORAGE_AUTO_GROW = "Whether the database server will automatically grow storage when necessary";
        public static final String DB_VERSION = "The version of the database software to use";
    }

    public static class SecurityGroupModelDescriptions {
        public static final String SECURITY_GROUP_IDS = "Exisiting security group ID(s) for the database server";
    }

}
