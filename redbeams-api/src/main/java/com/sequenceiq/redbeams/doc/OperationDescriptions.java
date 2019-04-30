package com.sequenceiq.redbeams.doc;

public final class OperationDescriptions {
    // public static class BlueprintOpDescription {
    //     public static final String GET_BY_NAME = "retrieve validation request by blueprint name";
    //     public static final String LIST_BY_WORKSPACE = "list blueprints for the given workspace";
    //     public static final String GET_BY_NAME_IN_WORKSPACE = "get blueprint by name in workspace";
    //     public static final String CREATE_IN_WORKSPACE = "create blueprint in workspace";
    //     public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete blueprint by name in workspace";
    //     public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "delete multiple blueprints by name in workspace";
    // }

    // public static class CredentialOpDescription {
    //     public static final String PUT_IN_WORKSPACE = "modify public credential resource in workspace";
    //     public static final String INTERACTIVE_LOGIN = "interactive login";
    //     public static final String INIT_CODE_GRANT_FLOW = "start a credential creation with Oauth2 Authorization Code Grant flow";
    //     public static final String INIT_CODE_GRANT_FLOW_ON_EXISTING = "Reinitialize Oauth2 Authorization Code Grant flow on an existing credential";
    //     public static final String AUTHORIZE_CODE_GRANT_FLOW = "Authorize Oauth2 Authorization Code Grant flow";
    //     public static final String LIST_BY_WORKSPACE = "list credentials for the given workspace";
    //     public static final String GET_BY_NAME_IN_WORKSPACE = "get credential by name in workspace";
    //     public static final String CREATE_IN_WORKSPACE = "create credential in workspace";
    //     public static final String DELETE_BY_NAME_IN_WORKSPACE = "delete credential by name in workspace";
    //     public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "delete multiple credentials by name in workspace";
    //     public static final String GET_PREREQUISTIES_BY_CLOUD_PROVIDER = "get credential prerequisites for cloud platform";
    // }

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
        public static final String LIST_BY_WORKSPACE = "list database servers for the given workspace";
        public static final String GET_BY_NAME_IN_WORKSPACE = "get a database server by name in workspace";
        public static final String REGISTER_IN_WORKSPACE = "register a database server in workspace";
        public static final String DELETE_BY_NAME_IN_WORKSPACE = "deregister or terminate a database server by name in workspace";
        public static final String DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE = "deregister or terminate multiple database servers by name in workspace";
        // public static final String GET_REQUEST_IN_WORKSPACE = "get request in workspace";
        // public static final String ATTACH_TO_ENVIRONMENTS = "attach RDS resource to environemnts";
        // public static final String DETACH_FROM_ENVIRONMENTS = "detach RDS resource from environemnts";

        private DatabaseServerOpDescription() {
        }
    }

    private OperationDescriptions() {
    }
}
