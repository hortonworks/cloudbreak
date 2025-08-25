package com.sequenceiq.redbeams.doc;

public final class OperationDescriptions {

    private OperationDescriptions() {
    }

    public static final class DatabaseOpDescription {
        public static final String TEST_CONNECTION = "test database connectivity";
        public static final String LIST = "list database configs";
        public static final String GET_BY_NAME = "get a database config by name";
        public static final String GET_BY_CRN = "get a database config by CRN";
        public static final String REGISTER = "register a database config of existing database";
        public static final String DELETE_BY_CRN = "delete a database config by CRN";
        public static final String DELETE_BY_NAME = "delete a database config by name";
        public static final String DELETE_MULTIPLE_BY_CRN = "delete multiple database configs by CRN";
        public static final String GET_USED_SUBNETS_BY_ENVIRONMENT_CRN = "list the used subnets by the given Environment resource CRN";

        private DatabaseOpDescription() {
        }
    }

    public static final class DatabaseServerOpDescription {
        public static final String TEST_CONNECTION = "test database server connectivity";
        public static final String LIST = "list database servers";
        public static final String LIST_CERTIFICATE_STATUS = "list certificate status for database servers";
        public static final String GET_BY_NAME = "get a database server by name";
        public static final String GET_BY_CRN = "get a database server by CRN";
        public static final String GET_BY_CLUSTER_CRN = "get a database server by cluster CRN";
        public static final String LIST_BY_CLUSTER_CRN = "list database servers by cluster CRN";

        public static final String CREATE = "create and register a database server in a cloud provider";

        public static final String UPGRADE = "upgrade a database server in a cloud provider to a higher major version";
        public static final String ROTATE = "rotate database server secrets";
        public static final String VALIDATE_UPGRADE = "validate if upgrade is possible on the database server in a cloud provider to a higher major version";
        public static final String VALIDATE_UPGRADE_CLEANUP = "cleans up the validation related cloud resources of the database server";
        public static final String CREATE_INTERNAL = "create and register a database server in a cloud provider with internal actor";
        public static final String CREATE_INTERNAL_NON_UNIQUE =
                "create and register multiple database servers for a cluster in a cloud provider with internal actor";
        public static final String UPDATE_CLUSTER_CRN = "Update the cluster crn associated with the database";
        public static final String RELEASE = "release management of a service-managed database server";
        public static final String REGISTER = "register a database server";
        public static final String DELETE_BY_CRN = "terminate and/or deregister a database server by CRN";
        public static final String DELETE_BY_NAME = "terminate and/or deregister a database server by name";
        public static final String DELETE_MULTIPLE_BY_CRN = "terminate and/or deregister multiple database servers by CRN";
        public static final String CREATE_DATABASE = "create a database on an existing database server";
        public static final String START = "start database server";
        public static final String ROTATE_SSL_CERT = "rotate database server cert";
        public static final String TURN_ON_DB_SSL = "Migrate database to use SSL/TLS connection.";
        public static final String UPDATE_SSL_CERT = "update database server cert";
        public static final String LATEST_CERTIFICATE_LIST = "query latest certificate for a specific provider and region";
        public static final String STOP = "stop database server";
        public static final String CERT_SWAP = "change certificate on mock provider";
        public static final String MIGRATE_DATABASE_TO_SSL =
                "Migrate database to ssl.";
        public static final String ENFORCE_SSL_ON_DATABASE =
                "Enforce ssl on database.";
        private DatabaseServerOpDescription() {
        }
    }

    public static final class ProgressOperationDescriptions {

        public static final String NOTES = "Flow operation progress";
        public static final String GET_LAST_FLOW_PROGRESS = "Get last flow operation progress details for resource by resource crn";
        public static final String LIST_FLOW_PROGRESS = "List recent flow operations progress details for resource by resource crn";

        private ProgressOperationDescriptions() {
        }
    }

    public static final class OperationOpDescriptions {

        public static final String NOTES = "Flow operation details";
        public static final String GET_OPERATIONS = "Get flow operation progress details for resource by resource crn";
        public static final String GET_OPERATION_STATUS = "Get flow operation status for resource by resource crn and operation id";

        private OperationOpDescriptions() {
        }
    }

}