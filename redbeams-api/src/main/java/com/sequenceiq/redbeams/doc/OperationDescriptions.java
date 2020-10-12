package com.sequenceiq.redbeams.doc;

public final class OperationDescriptions {

    public static final class DatabaseOpDescription {
        public static final String TEST_CONNECTION = "test database connectivity";
        public static final String LIST = "list database configs";
        public static final String GET_BY_NAME = "get a database config by name";
        public static final String GET_BY_CRN = "get a database config by CRN";
        public static final String REGISTER = "register a database config of existing database";
        public static final String DELETE_BY_CRN = "delete a database config by CRN";
        public static final String DELETE_BY_NAME = "delete a database config by name";
        public static final String DELETE_MULTIPLE_BY_CRN = "delete multiple database configs by CRN";

        private DatabaseOpDescription() {
        }
    }

    public static final class DatabaseServerOpDescription {
        public static final String TEST_CONNECTION = "test database server connectivity";
        public static final String LIST = "list database servers";
        public static final String GET_BY_NAME = "get a database server by name";
        public static final String GET_BY_CRN = "get a database server by CRN";
        public static final String CREATE = "create and register a database server in a cloud provider";
        public static final String CREATE_INTERNAL = "create and register a database server in a cloud provider with internal actor";
        public static final String RELEASE = "release management of a service-managed database server";
        public static final String REGISTER = "register a database server";
        public static final String DELETE_BY_CRN = "terminate and/or deregister a database server by CRN";
        public static final String DELETE_BY_NAME = "terminate and/or deregister a database server by name";
        public static final String DELETE_MULTIPLE_BY_CRN = "terminate and/or deregister multiple database servers by CRN";
        public static final String CREATE_DATABASE = "create a database on an existing database server";
        public static final String START = "start database server";
        public static final String STOP = "stop database server";

        private DatabaseServerOpDescription() {
        }
    }

    private OperationDescriptions() {
    }
}
