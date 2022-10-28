package com.sequenceiq.freeipa.ldap.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.secret.SecretTestUtil;
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigConverter;
import com.sequenceiq.freeipa.ldap.LdapConfigService;

@ExtendWith(MockitoExtension.class)
public class LdapConfigV1ServiceTest {
    private static final String LDAPNAME = "ldapname";

    private static final String LDAPDESC = "ldapdesc";

    private static final String ENVID = "envid";

    private static final String LDAP_PROTOCOL = "ldap://";

    private static final String HOST = "host";

    private static final Integer PORT = 1111;

    private static final String BINDDN = "binddn";

    private static final String PWD = "pwd";

    private static final String ADMIN = "admin";

    private static final String CERT = "cert";

    private static final String DOMAIN = "domain";

    private static final String MEMBER = "member";

    private static final String GROUPOBJCLASS = "groupobjclass";

    private static final String GROUPSEARCHBASE = "groupsearchbase";

    private static final String GROUPNAMEATTR = "groupnameattr";

    private static final String USERDNPATTERN = "userdnpattern";

    private static final String USERNAMEATTR = "usernameattr";

    private static final String USEROBJCLASS = "userobjclass";

    private static final String USERSEARCHBASE = "usersearchbase";

    private static final String SECRET_BINDDN = "secret-binddn";

    private static final String SECRET_PWD = "secret-pwd";

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private LdapConfigConverter ldapConfigConverter;

    @InjectMocks
    private LdapConfigV1Service underTest;

    @Test
    public void testPost() {
        CreateLdapConfigRequest request = new CreateLdapConfigRequest();
        LdapConfig ldapConfig = new LdapConfig();
        when(ldapConfigConverter.convertCreateLdapConfigRequest(request)).thenReturn(ldapConfig);
        LdapConfig persistedLdapConfig = new LdapConfig();
        when(ldapConfigService.createLdapConfig(ldapConfig)).thenReturn(persistedLdapConfig);
        DescribeLdapConfigResponse response = new DescribeLdapConfigResponse();
        when(ldapConfigConverter.convertLdapConfigToDescribeLdapConfigResponse(persistedLdapConfig)).thenReturn(response);

        DescribeLdapConfigResponse actualResponse = underTest.post(request);

        assertEquals(response, actualResponse);
    }

    @Test
    public void testDescribe() {
        // GIVEN
        LdapConfig ldapConfig = createLdapConfig();
        when(ldapConfigService.get(ENVID)).thenReturn(ldapConfig);
        DescribeLdapConfigResponse response = new DescribeLdapConfigResponse();
        when(ldapConfigConverter.convertLdapConfigToDescribeLdapConfigResponse(ldapConfig)).thenReturn(response);
        // WHEN
        DescribeLdapConfigResponse actualResponse = underTest.describe(ENVID);
        // THEN
        assertEquals(response, actualResponse);
    }

    @Test
    public void testGetCreateRequest() {
        // GIVEN
        LdapConfig ldapConfig = createLdapConfig();
        when(ldapConfigService.get(ENVID)).thenReturn(ldapConfig);
        CreateLdapConfigRequest request = new CreateLdapConfigRequest();
        when(ldapConfigConverter.convertLdapConfigToCreateLdapConfigRequest(ldapConfig)).thenReturn(request);
        // WHEN
        CreateLdapConfigRequest actualRequest = underTest.getCreateLdapConfigRequest(ENVID);
        // THEN
        assertEquals(request, actualRequest);
    }

    private LdapConfig createLdapConfig() {
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setName(LDAPNAME);
        ldapConfig.setDescription(LDAPDESC);
        ldapConfig.setEnvironmentCrn(ENVID);
        ldapConfig.setProtocol(LDAP_PROTOCOL);
        ldapConfig.setServerHost(HOST);
        ldapConfig.setServerPort(PORT);
        SecretTestUtil.setSecretField(LdapConfig.class, "bindDn", ldapConfig, BINDDN, SECRET_BINDDN);
        SecretTestUtil.setSecretField(LdapConfig.class, "bindPassword", ldapConfig, PWD, SECRET_PWD);
        ldapConfig.setAdminGroup(ADMIN);
        ldapConfig.setCertificate(CERT);
        ldapConfig.setDirectoryType(DirectoryType.LDAP);
        ldapConfig.setDomain(DOMAIN);
        ldapConfig.setGroupMemberAttribute(MEMBER);
        ldapConfig.setGroupObjectClass(GROUPOBJCLASS);
        ldapConfig.setGroupSearchBase(GROUPSEARCHBASE);
        ldapConfig.setGroupNameAttribute(GROUPNAMEATTR);
        ldapConfig.setUserDnPattern(USERDNPATTERN);
        ldapConfig.setUserNameAttribute(USERNAMEATTR);
        ldapConfig.setUserObjectClass(USEROBJCLASS);
        ldapConfig.setUserSearchBase(USERSEARCHBASE);
        return ldapConfig;
    }
}
