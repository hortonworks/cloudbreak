package com.sequenceiq.freeipa.ldap.v1;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.secret.SecretTestUtil;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;

@RunWith(MockitoJUnitRunner.class)
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
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @InjectMocks
    private LdapConfigV1Service underTest;

    @Test
    public void testPost() {
        // GIVEN
        CreateLdapConfigRequest createLdapConfigRequest = createCreateLdapConfigRequest();
        LdapConfig ldapConfig = createLdapConfig();
        ArgumentCaptor<LdapConfig> ldapConfigParam = ArgumentCaptor.forClass(LdapConfig.class);
        Mockito.when(stringToSecretResponseConverter.convert(SECRET_BINDDN)).thenReturn(new SecretResponse("enginepath", "binddn-secretpath"));
        Mockito.when(stringToSecretResponseConverter.convert(SECRET_PWD)).thenReturn(new SecretResponse("enginepath", "pwd-secretpath"));
        Mockito.when(ldapConfigService.createLdapConfig(ldapConfigParam.capture())).thenReturn(ldapConfig);
        // WHEN
        DescribeLdapConfigResponse actualResponse = underTest.post(createLdapConfigRequest);
        // THEN
        checkInnerLdapConfig(ldapConfigParam.getValue());
        checkResponse(actualResponse);
    }

    @Test
    public void testDescribe() {
        // GIVEN
        LdapConfig ldapConfig = createLdapConfig();
        Mockito.when(ldapConfigService.get(ENVID)).thenReturn(ldapConfig);
        Mockito.when(stringToSecretResponseConverter.convert(SECRET_BINDDN)).thenReturn(new SecretResponse("enginepath", "binddn-secretpath"));
        Mockito.when(stringToSecretResponseConverter.convert(SECRET_PWD)).thenReturn(new SecretResponse("enginepath", "pwd-secretpath"));
        // WHEN
        DescribeLdapConfigResponse actualResponse = underTest.describe(ENVID);
        // THEN
        checkResponse(actualResponse);
    }

    @Test
    public void testGetCreateRequest() {
        // GIVEN
        LdapConfig ldapConfig = createLdapConfig();
        Mockito.when(ldapConfigService.get(ENVID)).thenReturn(ldapConfig);
        // WHEN
        CreateLdapConfigRequest actualRequest = underTest.getCreateLdapConfigRequest(ENVID);
        // THEN
        checkCreateRequest(actualRequest);
    }

    private LdapConfig createLdapConfig() {
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setName(LDAPNAME);
        ldapConfig.setDescription(LDAPDESC);
        ldapConfig.setEnvironmentId(ENVID);
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
        createLdapConfigRequest.setEnvironmentId(ENVID);
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
        Assert.assertEquals(LDAPNAME, response.getName());
        Assert.assertEquals(LDAPDESC, response.getDescription());
        Assert.assertEquals(ENVID, response.getEnvironmentId());
        Assert.assertEquals(LDAP_PROTOCOL, response.getProtocol());
        Assert.assertEquals(HOST, response.getHost());
        Assert.assertEquals(PORT, response.getPort());
        Assert.assertEquals(ADMIN, response.getAdminGroup());
        Assert.assertEquals(CERT, response.getCertificate());
        Assert.assertEquals(DirectoryType.LDAP, response.getDirectoryType());
        Assert.assertEquals(DOMAIN, response.getDomain());
        Assert.assertEquals(MEMBER, response.getGroupMemberAttribute());
        Assert.assertEquals(GROUPOBJCLASS, response.getGroupObjectClass());
        Assert.assertEquals(GROUPSEARCHBASE, response.getGroupSearchBase());
        Assert.assertEquals(GROUPNAMEATTR, response.getGroupNameAttribute());
        Assert.assertEquals(USERDNPATTERN, response.getUserDnPattern());
        Assert.assertEquals(USERNAMEATTR, response.getUserNameAttribute());
        Assert.assertEquals(USEROBJCLASS, response.getUserObjectClass());
        Assert.assertEquals(USERSEARCHBASE, response.getUserSearchBase());
        Assert.assertEquals(response.getBindDn().getSecretPath(), "binddn-secretpath");
        Assert.assertEquals(response.getBindPassword().getSecretPath(), "pwd-secretpath");
    }

    private void checkInnerLdapConfig(LdapConfig innerLdapConfig) {
        Assert.assertEquals(LDAPNAME, innerLdapConfig.getName());
        Assert.assertEquals(LDAPDESC, innerLdapConfig.getDescription());
        Assert.assertEquals(ENVID, innerLdapConfig.getEnvironmentId());
        Assert.assertEquals(LDAP_PROTOCOL, innerLdapConfig.getProtocol());
        Assert.assertEquals(HOST, innerLdapConfig.getServerHost());
        Assert.assertEquals(PORT, innerLdapConfig.getServerPort());
        Assert.assertEquals(ADMIN, innerLdapConfig.getAdminGroup());
        Assert.assertEquals(CERT, innerLdapConfig.getCertificate());
        Assert.assertEquals(DirectoryType.LDAP, innerLdapConfig.getDirectoryType());
        Assert.assertEquals(DOMAIN, innerLdapConfig.getDomain());
        Assert.assertEquals(MEMBER, innerLdapConfig.getGroupMemberAttribute());
        Assert.assertEquals(GROUPOBJCLASS, innerLdapConfig.getGroupObjectClass());
        Assert.assertEquals(GROUPSEARCHBASE, innerLdapConfig.getGroupSearchBase());
        Assert.assertEquals(GROUPNAMEATTR, innerLdapConfig.getGroupNameAttribute());
        Assert.assertEquals(USERDNPATTERN, innerLdapConfig.getUserDnPattern());
        Assert.assertEquals(USERNAMEATTR, innerLdapConfig.getUserNameAttribute());
        Assert.assertEquals(USEROBJCLASS, innerLdapConfig.getUserObjectClass());
        Assert.assertEquals(USERSEARCHBASE, innerLdapConfig.getUserSearchBase());
        Assert.assertEquals(BINDDN, innerLdapConfig.getBindDn());
        Assert.assertEquals(PWD, innerLdapConfig.getBindPassword());
    }

    private void checkCreateRequest(CreateLdapConfigRequest createLdapConfigRequest) {
        Assert.assertEquals(LDAPNAME, createLdapConfigRequest.getName());
        Assert.assertEquals(LDAPDESC, createLdapConfigRequest.getDescription());
        Assert.assertEquals(ENVID, createLdapConfigRequest.getEnvironmentId());
        Assert.assertEquals(LDAP_PROTOCOL, createLdapConfigRequest.getProtocol());
        Assert.assertEquals(HOST, createLdapConfigRequest.getHost());
        Assert.assertEquals(PORT, createLdapConfigRequest.getPort());
        Assert.assertEquals(ADMIN, createLdapConfigRequest.getAdminGroup());
        Assert.assertEquals(CERT, createLdapConfigRequest.getCertificate());
        Assert.assertEquals(DirectoryType.LDAP, createLdapConfigRequest.getDirectoryType());
        Assert.assertEquals(DOMAIN, createLdapConfigRequest.getDomain());
        Assert.assertEquals(MEMBER, createLdapConfigRequest.getGroupMemberAttribute());
        Assert.assertEquals(GROUPOBJCLASS, createLdapConfigRequest.getGroupObjectClass());
        Assert.assertEquals(GROUPSEARCHBASE, createLdapConfigRequest.getGroupSearchBase());
        Assert.assertEquals(GROUPNAMEATTR, createLdapConfigRequest.getGroupNameAttribute());
        Assert.assertEquals(USERDNPATTERN, createLdapConfigRequest.getUserDnPattern());
        Assert.assertEquals(USERNAMEATTR, createLdapConfigRequest.getUserNameAttribute());
        Assert.assertEquals(USEROBJCLASS, createLdapConfigRequest.getUserObjectClass());
        Assert.assertEquals(USERSEARCHBASE, createLdapConfigRequest.getUserSearchBase());
        Assert.assertEquals("fake-user", createLdapConfigRequest.getBindDn());
        Assert.assertEquals("fake-password", createLdapConfigRequest.getBindPassword());
    }
}
