package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapMinimalV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapTestV4Response;
import com.sequenceiq.it.IntegrationTestContext;

public class LdapTestEntity extends AbstractCloudbreakEntity<LdapMinimalV4Request, LdapTestV4Response, LdapTestEntity> {
    private static final String LDAP_TEST = "LDAP_TEST";

    LdapTestEntity(String newId) {
        super(newId);
        setRequest(new LdapMinimalV4Request());
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
        getRequest().setPort(serverPort);
        return this;
    }

    public LdapTestEntity withServerHost(String serverHost) {
        getRequest().setHost(serverHost);
        return this;
    }
}