package com.sequenceiq.redbeams.doc;

public final class OperationDescriptions {

    public static final class DatabaseOpDescription {
        public static final String POST_CONNECTION_TEST = "test database connectivity";
        public static final String LIST = "list database configs";
        public static final String GET_BY_NAME = "get database config by name";
        public static final String CREATE = "create database config";
        public static final String REGISTER = "register database config of existing database";
        public static final String DELETE_BY_NAME = "delete database config by name";
        public static final String DELETE_MULTIPLE_BY_NAME = "delete multiple database configs by name";
        public static final String GET_REQUEST = "get the database request creating this database config";

        private DatabaseOpDescription() {
        }
    }

    public static final class DatabaseServerOpDescription {
        public static final String TEST_CONNECTION = "test database server connectivity";
        public static final String LIST = "list database servers";
        public static final String GET_BY_NAME = "get a database server by name";
        public static final String REGISTER = "register a database server";
        public static final String DELETE_BY_NAME = "deregister or terminate a database server by name";
        public static final String DELETE_MULTIPLE_BY_NAME = "deregister or terminate multiple database servers by name";
        // public static final String GET_REQUEST_IN_WORKSPACE = "get request in workspace";

        private DatabaseServerOpDescription() {
        }
    }

    private OperationDescriptions() {
    }
}
