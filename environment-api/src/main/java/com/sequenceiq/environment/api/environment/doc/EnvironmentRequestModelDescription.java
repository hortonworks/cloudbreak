package com.sequenceiq.environment.api.environment.doc;

public class EnvironmentRequestModelDescription {

    public static final String CREDENTIAL_NAME = "Name of the credential of the environment. If the name is given, "
            + "the detailed credential is ignored in the request.";
    public static final String CREDENTIAL = "If credentialName is not specified, the credential is used to create the new credential for the environment.";
    public static final String INTERACTIVE_LOGIN_CREDENTIAL_VERIFICATION_URL = "The url provided by Azure where the user have to use the given user code "
            + "to sign in";
    public static final String INTERACTIVE_LOGIN_CREDENTIAL_USER_CODE = "The user code what has to be used for the sign-in process on the Azure portal";
    public static final String CREDENTIAL_DESCRIPTION = "Credential request related data";
    public static final String PROXY_CONFIGS = "Name of the proxy configurations to be attached to the environment.";
    public static final String RDS_CONFIGS = "Name of the RDS configurations to be attached to the environment.";
    public static final String KUBERNETES_CONFIGS = "Name of the Kubernetes configurations to be attached to the environment.";
    public static final String LDAP_CONFIGS = "Name of the LDAP configurations to be attached to the environment.";
    public static final String REGIONS = "Regions of the environment.";
    public static final String LOCATION = "Location of the environment.";
    public static final String LONGITUDE = "Location longitude of the environment.";
    public static final String LATITUDE = "Location latitude of the environment.";
    public static final String KERBEROS_CONFIGS = "Name of Kerberos configs to be attached to the environment.";
    public static final String LOCATION_DISPLAY_NAME = "Display name of the location of the environment.";
    public static final String NETWORK = "Network related specifics of the environment.";
}
