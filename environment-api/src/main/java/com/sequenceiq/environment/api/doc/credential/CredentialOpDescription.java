package com.sequenceiq.environment.api.doc.credential;

public class CredentialOpDescription {
    public static final String PUT = "modify public credential resource";
    public static final String INTERACTIVE_LOGIN = "interactive login";
    public static final String INIT_CODE_GRANT_FLOW = "start a credential creation with Oauth2 Authorization Code Grant flow";
    public static final String INIT_CODE_GRANT_FLOW_ON_EXISTING = "Reinitialize Oauth2 Authorization Code Grant flow on an existing credential";
    public static final String AUTHORIZE_CODE_GRANT_FLOW = "Authorize Oauth2 Authorization Code Grant flow";
    public static final String LIST = "list credentials";
    public static final String GET_BY_NAME = "get credential by name";
    public static final String GET_BY_CRN = "get credential by crn";
    public static final String GET_BY_ENVIRONMENT_CRN = "get credential by environment crn";
    public static final String GET_BY_ENVIRONMENT_NAME = "get credential by environment name";
    public static final String CREATE = "create credential";
    public static final String DELETE_BY_NAME = "delete credential by name";
    public static final String DELETE_BY_CRN = "delete credential by crn";
    public static final String DELETE_MULTIPLE_BY_NAME = "delete multiple credentials by name";
    public static final String GET_PREREQUISTIES_BY_CLOUD_PROVIDER = "get credential prerequisites for cloud platform";

    private CredentialOpDescription() {
    }
}
