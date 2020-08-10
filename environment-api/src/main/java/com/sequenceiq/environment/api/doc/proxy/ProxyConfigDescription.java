package com.sequenceiq.environment.api.doc.proxy;

public class ProxyConfigDescription {
    public static final String LIST = "list proxy configurations";
    public static final String GET_BY_NAME = "get proxy configuration ";
    public static final String GET_BY_CRN = "get proxy configuration ";
    public static final String CREATE = "create proxy configuration";
    public static final String DELETE_BY_NAME = "delete proxy configuration by name";
    public static final String DELETE_BY_CRN = "delete proxy configuration by crn";
    public static final String DELETE_MULTIPLE_BY_NAME = "delete multiple proxy configurations by name";
    public static final String GET_REQUEST_BY_NAME = "get request by name";

    public static final String PROXY_CONFIG_NOTES = "An proxy Configuration describe a connection to an external proxy server which provides internet access "
            + "cluster members. It's applied for package manager and Ambari too";
    public static final String PROXY_CONFIG_ID = "proxy configuration id for the cluster";

    public static final String DESCRIPTION = "Cloudbreak allows you to save your existing proxy configuration "
            + "information as an external source so that you can provide the proxy information to multiple "
            + "clusters that you create with Cloudbreak";
    public static final String SERVER_HOST = "host or IP address of proxy server";
    public static final String SERVER_PORT = "port of proxy server (typically: 3128 or 8080)";
    public static final String PROTOCOL = "determines the protocol (http or https)";
    public static final String NAME = "Name of the proxy configuration resource";
    public static final String USERNAME = "Username to use for basic authentication";
    public static final String PASSWORD = "Password to use for basic authentication";
    public static final String CREATOR = "crn of the creator";
    public static final String GET_BY_ENVIRONMENT_CRN = "get proxyconfig by environment crn";

    private ProxyConfigDescription() {
    }
}
