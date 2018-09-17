package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.ldap.LdapTestResult;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapValidationRequest;
import com.sequenceiq.it.IntegrationTestContext;

public class LdapTestEntity extends AbstractCloudbreakEntity<LdapValidationRequest, LdapTestResult, LdapTestEntity> {
    private static final String LDAP_TEST = "LDAP_TEST";

    LdapTestEntity(String newId) {
        super(newId);
        setRequest(new LdapValidationRequest());
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