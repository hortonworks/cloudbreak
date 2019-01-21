package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.LdapTest;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class LdapConfigTests extends CloudbreakTest {
    private static final String VALID_LDAP_CONFIG = "e2e-ldap";

    private static final Integer VALID_SERVER_PORT = 389;

    private static final String SPECIAL_LDAP_NAME = "a-@#$%|:&*;";

    private static final String VALID_LDAP_DESC = "Valid ldap config description";

    private static final String LDAP = "LDAP";

    private static final String BIND_DN = "CN=Administrator,CN=Users,DC=ad,DC=hwx,DC=com";

    private static final String SEARCH_BASE = "CN=Users,DC=ad,DC=hwx,DC=com";

    private static final String USER_NAME_ATTRIBUTE = "sAMAccountName";

    private static final String USER_OBJECT_CLASS = "person";

    private static final String GROUP_MEMBER_ATTRIBUTE = "member";

    private static final String GROUP_NAME_ATTRIBUTE = "cn";

    private static final String GROUP_OBJECT_CLASS  = "group";

    private final List<String> ldapConfigsToDelete = new ArrayList<>();

    private String ldapServerHost;

    private String bindPassword;

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @BeforeTest
    public void setup() throws Exception {
        given(CloudbreakClient.created());
        ldapServerHost = getTestParameter().get("integrationtest.ldapconfig.ldapServerHost");
        bindPassword = getTestParameter().get("integrationtest.ldapconfig.bindPassword");
    }

    @Test
    public void testCreateValidLdap() throws Exception {
        given(LdapConfig.request()
                .withName(VALID_LDAP_CONFIG)
                .withDirectoryType(DirectoryType.LDAP)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE)
                .withDescription(VALID_LDAP_DESC), "create ldap config with LDAP directory type"

        );
        when(LdapConfig.post(), "post the request");
        then(LdapConfig.assertThis(
                (ldapconfig, t) -> Assert.assertNotNull(ldapconfig.getResponse().getId(), "ldap config id must not be null"))
        );
        ldapConfigsToDelete.add(VALID_LDAP_CONFIG);
    }

    @Test
    public void testCreateValidAd() throws Exception {
        given(LdapConfig.request()
                .withName(VALID_LDAP_CONFIG + "-ad")
                .withDirectoryType(DirectoryType.ACTIVE_DIRECTORY)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE), "create ldap config with ad directory type"

        );
        when(LdapConfig.post(), "post the request");
        then(LdapConfig.assertThis(
                (ldapconfig, t) -> Assert.assertNotNull(ldapconfig.getResponse().getId(), "ldap config id must not be null"))
        );
        ldapConfigsToDelete.add(VALID_LDAP_CONFIG + "-ad");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateLdapWithShortName() throws Exception {
        given(LdapConfig.request()
                .withName("")
                .withDirectoryType(DirectoryType.LDAP)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE), "create invalid ldap config with short name"

        );
        when(LdapConfig.post(), "post the request");
        then(LdapConfig.assertThis(
                (ldapconfig, t) -> Assert.assertNull(ldapconfig.getResponse().getId(), "ldap config id must be null"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class, enabled = false)
    //Existing bug: BUG-99596
    public void testCreateLdapWithSpecialName() throws Exception {
        given(LdapConfig.request()
                .withName(SPECIAL_LDAP_NAME)
                .withDirectoryType(DirectoryType.LDAP)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE), "create invalid ldap config with special name"
        );
        when(LdapConfig.post(), "post the request");
        then(LdapConfig.assertThis(
                (ldapconfig, t) -> Assert.assertNull(ldapconfig.getResponse().getId(), "ldap config id must be null"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateLdapWithLongName() throws Exception {
        given(LdapConfig.request()
                .withName(longStringGeneratorUtil.stringGenerator(101))
                .withDirectoryType(DirectoryType.LDAP)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE), "create invalid ldap config with long name"

        );
        when(LdapConfig.post(), "post the request");
        then(LdapConfig.assertThis(
                (ldapconfig, t) -> Assert.assertNull(ldapconfig.getResponse().getId(), "ldap config id must be null"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateLdapWithLongDesc() throws Exception {
        given(LdapConfig.request()
                .withName(VALID_LDAP_CONFIG + "longdesc")
                .withDirectoryType(DirectoryType.LDAP)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE)
                .withDescription(longStringGeneratorUtil.stringGenerator(1001)), "create invalid ldap config with long description"

        );
        when(LdapConfig.post(), "post the request");
        then(LdapConfig.assertThis(
                (ldapconfig, t) -> Assert.assertNull(ldapconfig.getResponse().getId(), "ldap config id must be null"))
        );
    }

    @Test
    public void testCreateDeleteCreateAgain() throws Exception {
        given(LdapConfig.isCreatedDeleted()
                .withName(VALID_LDAP_CONFIG + "-again")
                .withDirectoryType(DirectoryType.LDAP)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE), "create ldap config, then delete"
        );

        given(LdapConfig.request()
                .withName(VALID_LDAP_CONFIG + "-again")
                .withDirectoryType(DirectoryType.LDAP)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE), "create ldap config with same name again"
        );
        when(LdapConfig.post(), "post the request");
        then(LdapConfig.assertThis(
                (ldapconfig, t) -> Assert.assertNotNull(ldapconfig.getResponse().getId(), "ldap config id must not be null"))
        );
        ldapConfigsToDelete.add(VALID_LDAP_CONFIG + "-again");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateLdapWithSameName() throws Exception {
        given(LdapConfig.isCreated()
                .withName(VALID_LDAP_CONFIG + "-same")
                .withDirectoryType(DirectoryType.LDAP)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE), "create ldap config"
        );

        given(LdapConfig.request()
                .withName(VALID_LDAP_CONFIG + "-same")
                .withDirectoryType(DirectoryType.LDAP)
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword)
                .withUserSearchBase(SEARCH_BASE)
                .withUserNameAttribute(USER_NAME_ATTRIBUTE)
                .withUserObjectClass(USER_OBJECT_CLASS)
                .withGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE)
                .withGroupNameAttribute(GROUP_NAME_ATTRIBUTE)
                .withGroupObjectClass(GROUP_OBJECT_CLASS)
                .withGroupSearchBase(SEARCH_BASE), "create ldap with name already exists"
        );
        try {
            when(LdapConfig.post(), "post the request");
            then(LdapConfig.assertThis(
                    (ldapconfig, t) -> Assert.assertNull(ldapconfig.getResponse().getId(), "ldap config id must be null"))
            );
        } finally {
            ldapConfigsToDelete.add(VALID_LDAP_CONFIG + "-same");
        }
    }

    @Test
    public void testLdapConnectOk() throws Exception {
        given(LdapTest.request()
                .withProtocol(LDAP)
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword), "create valid ldap connection test"

        );
        when(LdapTest.testConnect(), "post the request");
        then(LdapTest.assertThis(
                (ldapconfig, t) -> Assert.assertTrue(ldapconfig.getResponse().getResult().contains("connected"),
                        "ldap should be connected"))
        );
    }

    @Test
    public void testLdapConnectWithInvalidProtocol() throws Exception {
        given(LdapTest.request()
                .withProtocol("invalid")
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword), "create invalid ldap connection test with invalid protocol"

        );
        when(LdapTest.testConnect(), "post the request");
        then(LdapTest.assertThis(
                (ldapconfig, t) -> Assert.assertFalse(ldapconfig.getResponse().getResult().contains("connected"),
                        "ldap should not be connected"))
        );
    }

    @Test
    public void testLdapConnectWithInvalidServer() throws Exception {
        given(LdapTest.request()
                .withProtocol(LDAP)
                .withServerHost("www.google.com")
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword), "create invalid ldap connection test with invalid server host"

        );
        when(LdapTest.testConnect(), "post the request");
        then(LdapTest.assertThis(
                (ldapconfig, t) -> Assert.assertFalse(ldapconfig.getResponse().getResult().contains("connected"),
                        "ldap should not be connected"))
        );
    }

    @Test
    public void testLdapConnectWithInvalidPort() throws Exception {
        given(LdapTest.request()
                .withProtocol("Ldap")
                .withServerHost(ldapServerHost)
                .withServerPort(999)
                .withBindDn(BIND_DN)
                .withBindPassword(bindPassword), "create invalid ldap connection test with invalid port"

        );
        when(LdapTest.testConnect(), "post the request");
        then(LdapTest.assertThis(
                (ldapconfig, t) -> Assert.assertFalse(ldapconfig.getResponse().getResult().contains("connected"),
                        "ldap should not be connected"))
        );
    }

    @Test
    public void testLdapConnectWithInvalidBindDn() throws Exception {
        given(LdapTest.request()
                .withProtocol("Ldap")
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn("invalid=invalid")
                .withBindPassword(bindPassword), "create invalid ldap connection test with invalid bind dn"

        );
        when(LdapTest.testConnect(), "post the request");
        then(LdapTest.assertThis(
                (ldapconfig, t) -> Assert.assertFalse(ldapconfig.getResponse().getResult().contains("connected"),
                        "ldap should not be connected"))
        );
    }

    @Test
    public void testLdapConnectWithInvalidBindPass() throws Exception {
        given(LdapTest.request()
                .withProtocol("Ldap")
                .withServerHost(ldapServerHost)
                .withServerPort(VALID_SERVER_PORT)
                .withBindDn(BIND_DN)
                .withBindPassword("inavlid"), "create invalid ldap connection test with invalid bind password"

        );
        when(LdapTest.testConnect(), "post the request");
        then(LdapTest.assertThis(
                (ldapconfig, t) -> Assert.assertFalse(ldapconfig.getResponse().getResult().contains("connected"),
                        "ldap should not be connected"))
        );
    }

    @AfterSuite
    public void cleanAll() throws Exception {
        for (String ldapConfig : ldapConfigsToDelete) {
            given(LdapConfig.request()
                    .withName(ldapConfig)
            );
            when(LdapConfig.delete());
        }
    }
}