package com.sequenceiq.freeipa.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.secret.SecretTestUtil;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;

@ExtendWith(MockitoExtension.class)
class LdapConfigConverterTest {

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
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @InjectMocks
    private LdapConfigConverter underTest;

    @Test
    public void testCreateLdapConfigRequestToLdapConfig() {
        CreateLdapConfigRequest createLdapConfigRequest = createCreateLdapConfigRequest();

        LdapConfig result = underTest.convertCreateLdapConfigRequest(createLdapConfigRequest);

        checkLdapConfig(result);
    }

    @Test
    public void testLdapConfigToDescribeLdapConfigResponse() {
        LdapConfig ldapConfig = createLdapConfig();
        when(stringToSecretResponseConverter.convert(SECRET_BINDDN)).thenReturn(new SecretResponse("enginepath", "binddn-secretpath", 1));
        when(stringToSecretResponseConverter.convert(SECRET_PWD)).thenReturn(new SecretResponse("enginepath", "pwd-secretpath", 1));

        DescribeLdapConfigResponse response = underTest.convertLdapConfigToDescribeLdapConfigResponse(ldapConfig);

        checkResponse(response);
    }

    @Test
    public void testLdapConfigToCreateLdapConfigRequest() {
        LdapConfig ldapConfig = createLdapConfig();

        CreateLdapConfigRequest request = underTest.convertLdapConfigToCreateLdapConfigRequest(ldapConfig);

        checkCreateRequest(request);
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

    private CreateLdapConfigRequest createCreateLdapConfigRequest() {
        CreateLdapConfigRequest createLdapConfigRequest = new CreateLdapConfigRequest();
        createLdapConfigRequest.setName(LDAPNAME);
        createLdapConfigRequest.setDescription(LDAPDESC);
        createLdapConfigRequest.setEnvironmentCrn(ENVID);
        createLdapConfigRequest.setProtocol(LDAP_PROTOCOL);
        createLdapConfigRequest.setHost(HOST);
        createLdapConfigRequest.setPort(PORT);
        createLdapConfigRequest.setBindDn(BINDDN);
        createLdapConfigRequest.setBindPassword(PWD);
        createLdapConfigRequest.setAdminGroup(ADMIN);
        createLdapConfigRequest.setCertificate(CERT);
        createLdapConfigRequest.setDirectoryType(DirectoryType.LDAP);
        createLdapConfigRequest.setDomain(DOMAIN);
        createLdapConfigRequest.setGroupMemberAttribute(MEMBER);
        createLdapConfigRequest.setGroupObjectClass(GROUPOBJCLASS);
        createLdapConfigRequest.setGroupSearchBase(GROUPSEARCHBASE);
        createLdapConfigRequest.setGroupNameAttribute(GROUPNAMEATTR);
        createLdapConfigRequest.setUserDnPattern(USERDNPATTERN);
        createLdapConfigRequest.setUserNameAttribute(USERNAMEATTR);
        createLdapConfigRequest.setUserObjectClass(USEROBJCLASS);
        createLdapConfigRequest.setUserSearchBase(USERSEARCHBASE);
        return createLdapConfigRequest;
    }

    private void checkResponse(DescribeLdapConfigResponse response) {
        assertEquals(LDAPNAME, response.getName());
        assertEquals(LDAPDESC, response.getDescription());
        assertEquals(ENVID, response.getEnvironmentCrn());
        assertEquals(LDAP_PROTOCOL, response.getProtocol());
        assertEquals(HOST, response.getHost());
        assertEquals(PORT, response.getPort());
        assertEquals(ADMIN, response.getAdminGroup());
        assertEquals(CERT, response.getCertificate());
        assertEquals(DirectoryType.LDAP, response.getDirectoryType());
        assertEquals(DOMAIN, response.getDomain());
        assertEquals(MEMBER, response.getGroupMemberAttribute());
        assertEquals(GROUPOBJCLASS, response.getGroupObjectClass());
        assertEquals(GROUPSEARCHBASE, response.getGroupSearchBase());
        assertEquals(GROUPNAMEATTR, response.getGroupNameAttribute());
        assertEquals(USERDNPATTERN, response.getUserDnPattern());
        assertEquals(USERNAMEATTR, response.getUserNameAttribute());
        assertEquals(USEROBJCLASS, response.getUserObjectClass());
        assertEquals(USERSEARCHBASE, response.getUserSearchBase());
        assertEquals(response.getBindDn().getSecretPath(), "binddn-secretpath");
        assertEquals(response.getBindPassword().getSecretPath(), "pwd-secretpath");
    }

    private void checkLdapConfig(LdapConfig innerLdapConfig) {
        assertEquals(LDAPNAME, innerLdapConfig.getName());
        assertEquals(LDAPDESC, innerLdapConfig.getDescription());
        assertEquals(ENVID, innerLdapConfig.getEnvironmentCrn());
        assertEquals(LDAP_PROTOCOL, innerLdapConfig.getProtocol());
        assertEquals(HOST, innerLdapConfig.getServerHost());
        assertEquals(PORT, innerLdapConfig.getServerPort());
        assertEquals(ADMIN, innerLdapConfig.getAdminGroup());
        assertEquals(CERT, innerLdapConfig.getCertificate());
        assertEquals(DirectoryType.LDAP, innerLdapConfig.getDirectoryType());
        assertEquals(DOMAIN, innerLdapConfig.getDomain());
        assertEquals(MEMBER, innerLdapConfig.getGroupMemberAttribute());
        assertEquals(GROUPOBJCLASS, innerLdapConfig.getGroupObjectClass());
        assertEquals(GROUPSEARCHBASE, innerLdapConfig.getGroupSearchBase());
        assertEquals(GROUPNAMEATTR, innerLdapConfig.getGroupNameAttribute());
        assertEquals(USERDNPATTERN, innerLdapConfig.getUserDnPattern());
        assertEquals(USERNAMEATTR, innerLdapConfig.getUserNameAttribute());
        assertEquals(USEROBJCLASS, innerLdapConfig.getUserObjectClass());
        assertEquals(USERSEARCHBASE, innerLdapConfig.getUserSearchBase());
        assertEquals(BINDDN, innerLdapConfig.getBindDn());
        assertEquals(PWD, innerLdapConfig.getBindPassword());
    }

    private void checkCreateRequest(CreateLdapConfigRequest createLdapConfigRequest) {
        assertEquals(LDAPNAME, createLdapConfigRequest.getName());
        assertEquals(LDAPDESC, createLdapConfigRequest.getDescription());
        assertEquals(ENVID, createLdapConfigRequest.getEnvironmentCrn());
        assertEquals(LDAP_PROTOCOL, createLdapConfigRequest.getProtocol());
        assertEquals(HOST, createLdapConfigRequest.getHost());
        assertEquals(PORT, createLdapConfigRequest.getPort());
        assertEquals(ADMIN, createLdapConfigRequest.getAdminGroup());
        assertEquals(CERT, createLdapConfigRequest.getCertificate());
        assertEquals(DirectoryType.LDAP, createLdapConfigRequest.getDirectoryType());
        assertEquals(DOMAIN, createLdapConfigRequest.getDomain());
        assertEquals(MEMBER, createLdapConfigRequest.getGroupMemberAttribute());
        assertEquals(GROUPOBJCLASS, createLdapConfigRequest.getGroupObjectClass());
        assertEquals(GROUPSEARCHBASE, createLdapConfigRequest.getGroupSearchBase());
        assertEquals(GROUPNAMEATTR, createLdapConfigRequest.getGroupNameAttribute());
        assertEquals(USERDNPATTERN, createLdapConfigRequest.getUserDnPattern());
        assertEquals(USERNAMEATTR, createLdapConfigRequest.getUserNameAttribute());
        assertEquals(USEROBJCLASS, createLdapConfigRequest.getUserObjectClass());
        assertEquals(USERSEARCHBASE, createLdapConfigRequest.getUserSearchBase());
        assertEquals("fake-user", createLdapConfigRequest.getBindDn());
        assertEquals("fake-password", createLdapConfigRequest.getBindPassword());
    }

}