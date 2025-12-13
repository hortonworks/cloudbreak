package com.sequenceiq.freeipa.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.secret.SecretTestUtil;
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;

@ExtendWith(MockitoExtension.class)
public class LdapConfigValidatorTest {

    @InjectMocks
    private LdapConfigValidator underTest;

    @Test
    public void testInvalidLdapConnection() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setProtocol("ldap://");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            underTest.validateLdapConnection(ldapConfig);
        });
        assertEquals("Failed to connect to LDAP server: Invalid name: /localhost:389", ex.getMessage());
    }

    private LdapConfig ldapConfig() {
        LdapConfig config = new LdapConfig();
        config.setId(1L);
        config.setName("dummyName");
        config.setDescription("dummyDescription");
        config.setUserSearchBase("cn=users,dc=example,dc=org");
        config.setUserDnPattern("cn={0},cn=users,dc=example,dc=org");
        config.setGroupSearchBase("cn=groups,dc=example,dc=org");
        SecretTestUtil.setSecretField(LdapConfig.class, "bindDn", config, "cn=admin,dc=example,dc=org", "secret/path");
        SecretTestUtil.setSecretField(LdapConfig.class, "bindPassword", config, "admin", "secret/path");
        config.setServerHost("localhost");
        config.setUserNameAttribute("cn=admin,dc=example,dc=org");
        config.setDomain("ad.hdc.com");
        config.setServerPort(389);
        config.setProtocol("ldap");
        config.setDirectoryType(DirectoryType.LDAP);
        config.setUserObjectClass("person");
        config.setGroupObjectClass("groupOfNames");
        config.setGroupNameAttribute("cn");
        config.setGroupMemberAttribute("member");
        config.setAdminGroup("ambariadmins");
        config.setCertificate("-----BEGIN CERTIFICATE-----certificate-----END CERTIFICATE-----");
        return config;
    }

}
