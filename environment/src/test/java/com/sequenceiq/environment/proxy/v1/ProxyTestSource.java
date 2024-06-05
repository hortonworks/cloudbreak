package com.sequenceiq.environment.proxy.v1;

import java.util.List;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

public class ProxyTestSource {
    public static final String NAME = "name";

    public static final String DESCRIPTION = "desc";

    public static final String SERVER_HOST = "local";

    public static final String PROTOCOL = "http";

    public static final Integer SERVER_PORT = -1;

    public static final SecretResponse USERNAME_SECRET;

    public static final SecretResponse PASSWORD_SECRET;

    public static final String USERNAME = "usern";

    public static final String PASSWORD = "pwd";

    public static final String RESCRN = "crn:cdp:environments:us-west-1:accid:proxyConfig:crn1";

    public static final String CREATOR = "creator";

    public static final String ACCOUNT_ID = "accid";

    public static final long ID = 1L;

    public static final String NO_PROXY_HOSTS = "noproxy.com";

    public static final List<String> INBOUND_PROXY_CIDR = List.of("0.0.0.0/0", "1.1.1.1/1");

    static {
        USERNAME_SECRET = new SecretResponse();
        USERNAME_SECRET.setEnginePath("-");
        USERNAME_SECRET.setSecretPath(USERNAME);

        PASSWORD_SECRET = new SecretResponse();
        PASSWORD_SECRET.setEnginePath("-");
        PASSWORD_SECRET.setSecretPath(PASSWORD);
    }

    private ProxyTestSource() {
    }

    public static ProxyConfig getProxyConfig() {
        ProxyConfig testSource  = new ProxyConfig();
        testSource.setPassword(PASSWORD);
        testSource.setUserName(USERNAME);
        testSource.setName(NAME);
        testSource.setDescription(DESCRIPTION);
        testSource.setAccountId(ACCOUNT_ID);
        testSource.setCreator(CREATOR);
        testSource.setId(ID);
        testSource.setProtocol(PROTOCOL);
        testSource.setServerHost(SERVER_HOST);
        testSource.setResourceCrn(RESCRN);
        testSource.setServerPort(SERVER_PORT);
        testSource.setNoProxyHosts(NO_PROXY_HOSTS);
        testSource.setInboundProxyCidr(INBOUND_PROXY_CIDR);
        return testSource;
    }

    public static ProxyRequest getProxyRequest() {
        ProxyRequest result = new ProxyRequest();
        result.setDescription(DESCRIPTION);
        result.setName(NAME);
        result.setUserName(USERNAME);
        result.setPassword(PASSWORD);
        result.setHost(SERVER_HOST);
        result.setPort(SERVER_PORT);
        result.setProtocol(PROTOCOL);
        result.setNoProxyHosts(NO_PROXY_HOSTS);
        result.setInboundProxyCidr(INBOUND_PROXY_CIDR);
        return result;
    }
}
