package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;

import java.util.Optional;

public class LdapConfigRequestDataCollector {

    private LdapConfigRequestDataCollector() {
    }

    public static LdapConfigRequest createLdapRequestWithProperties(TestParameter testParametert) {
        LdapConfigRequest request = new LdapConfigRequest();
        request.setServerHost(getParam("NN_LDAP_SERVER_HOST", testParametert));
        request.setServerPort(Integer.parseInt(getParam("NN_LDAP_SERVER_PORT", testParametert)));
        request.setBindDn(getParam("NN_LDAP_BIND_DN", testParametert));
        request.setBindPassword(getParam("NN_LDAP_BIND_PASSWORD", testParametert));
        request.setUserSearchBase(getParam("NN_LDAP_USER_SEARCH_BASE", testParametert));
        request.setUserNameAttribute(getParam("NN_LDAP_USER_NAME_ATTRIBUTE", testParametert));
        request.setUserDnPattern(getParam("NN_LDAP_USER_DN_PATTERN", testParametert));
        request.setUserObjectClass(getParam("NN_LDAP_USER_OBJECT_CLASS", testParametert));
        request.setGroupSearchBase(getParam("NN_LDAP_GROUP_SEARCH_BASE", testParametert));
        request.setGroupNameAttribute(getParam("NN_LDAP_GROUP_NAME_ATTRIBUTE", testParametert));
        request.setGroupObjectClass(getParam("NN_LDAP_GROUP_OBJECT_CLASS", testParametert));
        request.setGroupMemberAttribute(getParam("NN_LDAP_GROUP_MEMBER_ATTRIBUTE", testParametert));
        request.setName(getParam("NN_LDAP", testParametert));
        return request;
    }

    private static String getParam(String key, TestParameter testParameter) {
        return Optional.ofNullable(testParameter.get(key)).orElseThrow(() -> new MissingExpectedParameterException(key));
    }
}
