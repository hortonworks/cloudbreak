package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4TestResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapTestConnectionV4Request;
import com.sequenceiq.it.IntegrationTestContext;

public class LdapTestEntity extends AbstractCloudbreakEntity<LdapTestConnectionV4Request, LdapV4TestResponse, LdapTestEntity> {
    private static final String LDAP_TEST = "LDAP_TEST";

    LdapTestEntity(String newId) {
        super(newId);
        setRequest(new LdapTestConnectionV4Request());
    }

    LdapTestEntity() {
        this(LDAP_TEST);
    }

    private static Function<IntegrationTestContext, LdapTest> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, LdapTest.class);
    }

    public LdapTestEntity withBindPassword(String bindPassword) {
        getRequest().setBindPassword(bindPassword);
        return this;
    }

    public LdapTestEntity withBindDn(String bindDn) {
        getRequest().setBindDn(bindDn);
        return this;
    }

    public LdapTestEntity withProtocol(String protocol) {
        getRequest().setProtocol(protocol);
        return this;
    }

    public LdapTestEntity withServerPort(Integer serverPort) {
        getRequest().setServerPort(serverPort);
        return this;
    }

    public LdapTestEntity withServerHost(String serverHost) {
        getRequest().setServerHost(serverHost);
        return this;
    }
}