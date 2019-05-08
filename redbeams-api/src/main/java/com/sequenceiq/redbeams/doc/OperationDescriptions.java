package com.sequenceiq.redbeams.doc;

public final class OperationDescriptions {

    public static final class DatabaseOpDescription {
        public static final String POST_CONNECTION_TEST = "test RDS connectivity";
        public static final String LIST_BY_WORKSPACE = "list RDS configs for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get RDS config by name in workspace";
        public static final String CREATE_IN_WORKSPACE = "create RDS config in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete RDS config by name in workspace";
        public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "delete multiple RDS configs by name in workspace";
        public static final String GET_REQUEST_IN_WORKSPACE = "get request in workspace";
        public static final String ATTACH_TO_ENVIRONMENTS = "attach RDS resource to environemnts";
        public static final String DETACH_FROM_ENVIRONMENTS = "detach RDS resource from environemnts";

        private DatabaseOpDescription() {
        }
    }

    public static final class DatabaseServerOpDescription {
        // public static final String POST_CONNECTION_TEST = "test RDS connectivity";
        public static final String LIST = "list database servers";
        public static final String GET_BY_NAME = "get a database server by name";
        public static final String REGISTER = "register a database server";
        public static final String DELETE_BY_NAME = "deregister or terminate a database server by name";
        public static final String DELETE_MULTIPLE_BY_NAME = "deregister or terminate multiple database servers by name";
        // public static final String GET_REQUEST_IN_WORKSPACE = "get request in workspace";
        // public static final String ATTACH_TO_ENVIRONMENTS = "attach RDS resource to environemnts";
        // public static final String DETACH_FROM_ENVIRONMENTS = "detach RDS resource from environemnts";

        private DatabaseServerOpDescription() {
        }
    }

    private OperationDescriptions() {
    }
}
