package com.sequenceiq.redbeams.doc;

public final class ModelDescriptions {

    public static final String ID = "ID of the resource";
    public static final String CRN = "CRN of the resource";
    public static final String DESCRIPTION = "Description of the resource";
    public static final String CREATION_DATE = "Creation date / time of the resource, in epoch milliseconds";
    public static final String ENVIRONMENT_CRN = "Crn of the environment of the resource";

    public static class Database {
        public static final String CONNECTION_URL = "JDBC connection URL in the form of JDBC:<db-type>://<address>:<port>/<db>";
        public static final String CONNECTION_DRIVER = "Name of the JDBC connection driver (for example: 'org.postgresql.Driver')";
        public static final String DB_ENGINE = "Name of the external database engine (MYSQL, POSTGRES...)";
        public static final String DB_ENGINE_DISPLAYNAME = "Display name of the external database engine (Mysql, PostgreSQL...)";
        public static final String VERSION = "Version of the Database";
        public static final String USERNAME = "Username to use for the JDBC connection";
        public static final String PASSWORD = "Password to use for the JDBC connection";
        public static final String NAME = "Name of the database configuration resource";
        public static final String TYPE = "Type of database, aka the service name that will use the database like HIVE, DRUID, SUPERSET, RANGER, etc.";
        public static final String CONNECTOR_JAR_URL = "URL that points to the jar of the connection driver(connector)";
        public static final String DATABASE_CONNECTION_TEST_RESULT = "Result of database connection test";
        public static final String DATABASE_TEST_EXISTING_REQUEST = "Identifiers of saved database config to be tested for connectivity";
        public static final String DATABASE_TEST_NEW_REQUEST = "Unsaved database config to be tested for connectivity";
        public static final String DATABASE_CREATE_RESULT = "Result of database creation";
    }

    public static class DatabaseServer {
        public static final String NAME = "Name of the database server";
        public static final String HOST = "Host of the database server";
        public static final String PORT = "Port of the database server";
        public static final String DATABASE_VENDOR = "Name of the database vendor (MYSQL, POSTGRES, ...)";
        public static final String DATABASE_VENDOR_DISPLAY_NAME = "Display name of the database vendor (MySQL, PostgreSQL, ...)";
        public static final String CONNECTION_DRIVER = "Name of the JDBC connection driver (for example: 'org.postgresql.Driver')";
        public static final String CONNECTOR_JAR_URL = "URL that points to the JAR of the connection driver (JDBC connector)";
        public static final String CONNECTION_USER_NAME = "User name of the administrative user of the database server";
        public static final String CONNECTION_PASSWORD = "Password of the administrative user of the database server";
        public static final String DATABASE_SERVER_TEST_EXISTING_REQUEST = "Identifiers of saved database server config to be tested for connectivity";
        public static final String DATABASE_SERVER_TEST_NEW_REQUEST = "Unsaved database server config to be tested for connectivity";
        public static final String DATABASE_SERVER_CONNECTION_TEST_RESULT = "Result of database server connection test";
    }

    public static class ExternalDatabaseServer {
        public static final String NAME = "Name of the external database server";
        public static final String DATABASE_SERVER = "The external database server configuration";
        public static final String STATUS = "Status of the database stack";
        public static final String STATUS_REASON = "Status message of the database stack";
    }

    public static class DBStack {
        public static final String STACK_NAME = "Name of the database stack";
        public static final String REGION = "Region of the database stack";
        public static final String NETWORK = "Network of the database stack";
        public static final String DATABASE_SERVER = "Database server of the database stack";
        public static final String AWS_PARAMETERS = "AWS-specific parameters of the database stack";
    }

    public static class NetworkModelDescription {
        public static final String AWS_PARAMETERS = "AWS-specific parameters of the specified network";
        public static final String GCP_PARAMETERS = "GCP-specific parameters of the specified network";
        public static final String AZURE_PARAMETERS = "Azure-specific parameters of the specified network";
        public static final String OPEN_STACK_PARAMETERS = "OpenStack-specific parameters of the specified network";
    }

    public static class AwsNetworkModelDescription {
        public static final String SUBNET_ID = "Subnet ID(s) of the specified AWS network";
    }

    public static class DatabaseServerModelDescription {
        public static final String INSTANCE_TYPE = "Instance type of the database server";
        public static final String DATABASE_VENDOR = "Database vendor of the database server";
        public static final String STORAGE_SIZE = "Storage size of the database server, in GB";
        public static final String ROOT_USER_NAME = "Root user name of the database server";
        public static final String ROOT_USER_PASSWORD = "Root user password of the database server";
        public static final String PORT = "Port for the database";
        public static final String AWS_PARAMETERS = "AWS-specific parameters of the specified database server";
        public static final String GCP_PARAMETERS = "GCP-specific parameters of the specified database server";
        public static final String AZURE_PARAMETERS = "Azure-specific parameters of the specified database server";
        public static final String OPEN_STACK_PARAMETERS = "OpenStack-specific parameters of the specified database server";
        public static final String SECURITY_GROUP = "Security group of the database server";
    }

    public static class SecurityGroupModelDescription {
        public static final String SECURITY_GROUP_IDS = "Exisiting security group ID(s) for the database server";
    }

    private ModelDescriptions() {
    }
}
