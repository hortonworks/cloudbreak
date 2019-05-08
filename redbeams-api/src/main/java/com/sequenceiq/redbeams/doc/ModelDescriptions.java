package com.sequenceiq.redbeams.doc;

public final class ModelDescriptions {

    public static final String ID = "ID of the resource";
    public static final String DESCRIPTION = "Description of the resource";
    public static final String CREATION_DATE = "Creation date / time of the resource, in epoch milliseconds";
    public static final String ENVIRONMENT_ID = "ID of the environment of the resource";

    // public static class Database {
    //     public static final String CONNECTION_URL = "JDBC connection URL in the form of jdbc:<db-type>://<address>:<port>/<db>";
    //     public static final String CONNECTION_DRIVER = "Name of the JDBC connection driver (for example: 'org.postgresql.Driver')";
    //     public static final String DB_ENGINE = "Name of the external database engine (MYSQL, POSTGRES...)";
    //     public static final String DB_ENGINE_DISPLAYNAME = "Display name of the external database engine (Mysql, PostgreSQL...)";
    //     public static final String VERSION = "Version of the Database";
    //     public static final String USERNAME = "Username to use for the jdbc connection";
    //     public static final String PASSWORD = "Password to use for the jdbc connection";
    //     public static final String ORACLE = "Oracle specific properties";
    //     public static final String NAME = "Name of the RDS configuration resource";
    //     public static final String STACK_VERSION = "(HDP, HDF)Stack version for the RDS configuration";
    //     public static final String RDSTYPE = "Type of RDS, aka the service name that will use the RDS like HIVE, DRUID, SUPERSET, RANGER, etc.";
    //     public static final String CONNECTOR_JAR_URL = "URL that points to the jar of the connection driver(connector)";
    //     public static final String DATABASE_REQUEST_CLUSTER_NAME = "requested cluster name";
    //     public static final String DATABASE_CONNECTION_TEST_RESULT = "result of RDS connection test";
    //     public static final String DATABASE_REQUEST = "unsaved RDS config to be tested by connectivity";
    // }

    public static class DatabaseServer {
        public static final String NAME = "Name of the database server";
        public static final String HOST = "Host of the database server";
        public static final String PORT = "Port of the database server";
        public static final String DATABASE_VENDOR = "Name of the database vendor (MYSQL, POSTGRES, ...)";
        public static final String DATABASE_VENDOR_DISPLAY_NAME = "Display name of the database vendor (MySQL, PostgreSQL, ...)";
        public static final String CONNECTION_DRIVER = "Name of the JDBC connection driver (for example: 'org.postgresql.Driver')";
        public static final String CONNECTOR_JAR_URL = "URL that points to the JAR of the connection driver (JDBC connector)";
        public static final String CONNECTION_USER_NAME = "User name for the administrative user of the database server";
        public static final String CONNECTION_PASSWORD = "Password for the administrative user of the database server";
        // public static final String VERSION = "Version of the Database";
        // public static final String ORACLE = "Oracle specific properties";
        // public static final String STACK_VERSION = "(HDP, HDF)Stack version for the RDS configuration";
        // public static final String RDSTYPE = "Type of RDS, aka the service name that will use the RDS like HIVE, DRUID, SUPERSET, RANGER, etc.";
        // public static final String DATABASE_REQUEST_CLUSTER_NAME = "requested cluster name";
        // public static final String DATABASE_CONNECTION_TEST_RESULT = "result of RDS connection test";
        // public static final String DATABASE_REQUEST = "unsaved RDS config to be tested by connectivity";
    }

    private ModelDescriptions() {
    }
}
