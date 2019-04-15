package com.sequenceiq.it.cloudbreak.newway.dto.ldap;

import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.ADMIN_GROUP;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.BIND_DN;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.BIND_PASSWORD;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.DIRECTORY_TYPE;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.GROUP_MEMBER_ATTRIBUTE;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.GROUP_NAME_ATTRIBUTE;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.GROUP_OBJECT_CLASS;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.GROUP_SEARCH_BASE;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.LDAP_CONFIG_NAME;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.LDAP_DOMAIN;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.SERVER_HOST;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.SERVER_PORT;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.SERVER_PROTOCOL;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.USER_DN_PATTERN;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.USER_NAME_ATTRIBUTE;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.USER_OBJECT_CLASS;
import static com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Ldap.USER_SEARCH_BASE;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.it.cloudbreak.newway.MissingExpectedParameterException;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public class LdapConfigRequestDataCollector {

    private LdapConfigRequestDataCollector() {
    }

    public static LdapV4Request createLdapRequestWithProperties(TestParameter testParameter) {
        return createLdapRequestWithPropertiesAndName(testParameter, getParam(LDAP_CONFIG_NAME, testParameter));
    }

    public static LdapV4Request createLdapRequestWithPropertiesAndName(TestParameter testParameter, @Nonnull @NotEmpty String name) {
        LdapV4Request request = new LdapV4Request();
        request.setHost(getParam(SERVER_HOST, testParameter));
        request.setPort(Integer.parseInt(getParam(SERVER_PORT, testParameter)));
        request.setBindDn(getParam(BIND_DN, testParameter));
        request.setBindPassword(getParam(BIND_PASSWORD, testParameter));
        request.setUserSearchBase(getParam(USER_SEARCH_BASE, testParameter));
        request.setUserNameAttribute(getParam(USER_NAME_ATTRIBUTE, testParameter));
        request.setUserDnPattern(getParam(USER_DN_PATTERN, testParameter));
        request.setUserObjectClass(getParam(USER_OBJECT_CLASS, testParameter));
        request.setGroupSearchBase(getParam(GROUP_SEARCH_BASE, testParameter));
        request.setGroupNameAttribute(getParam(GROUP_NAME_ATTRIBUTE, testParameter));
        request.setGroupObjectClass(getParam(GROUP_OBJECT_CLASS, testParameter));
        request.setGroupMemberAttribute(getParam(GROUP_MEMBER_ATTRIBUTE, testParameter));
        request.setName(name);
        request.setAdminGroup(getParam(ADMIN_GROUP, testParameter));
        request.setDirectoryType(DirectoryType.valueOf(getParam(DIRECTORY_TYPE, testParameter)));
        request.setProtocol(getParam(SERVER_PROTOCOL, testParameter));
        request.setDomain(getParam(LDAP_DOMAIN, testParameter));
        return request;
    }

    private static String getParam(String key, TestParameter testParameter) {
        return Optional.ofNullable(testParameter.get(key)).orElseThrow(() -> new MissingExpectedParameterException(key));
    }
}
