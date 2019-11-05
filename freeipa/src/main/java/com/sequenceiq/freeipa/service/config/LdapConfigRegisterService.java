package com.sequenceiq.freeipa.service.config;

import java.util.StringJoiner;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;

@Service
public class LdapConfigRegisterService extends AbstractConfigRegister {
    public static final String USER_GROUP = "ipausers";

    public static final String BIND_DN = "uid=admin,cn=users,cn=accounts";

    public static final String USER_SEARCH_BASE = "cn=users,cn=accounts";

    public static final String GROUP_SEARCH_BASE = "cn=groups,cn=accounts";

    public static final String USER_DN_PATTERN = "uid={0},cn=users,cn=accounts";

    public static final String PROTOCOL = "ldaps";

    public static final String USER_NAME_ATTRIBUTE = "uid";

    public static final String USER_OBJECT_CLASS = "posixAccount";

    public static final String GROUP_MEMBER_ATTRIBUTE = "member";

    public static final String GROUP_NAME_ATTRIBUTE = "cn";

    public static final String GROUP_OBJECT_CLASS = "posixGroup";

    public static final Integer SERVER_PORT = 636;

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigRegisterService.class);

    @Inject
    private LdapConfigService ldapConfigService;

    @Override
    public void register(Long stackId) {
        createLdapConfig(stackId, null, null, null);
    }

    public LdapConfig createLdapConfig(Long stackId, String bindDn, String bindPassword, String clusterName) {
        Stack stack = getStackWithInstanceMetadata(stackId);
        FreeIpa freeIpa = getFreeIpaService().findByStackId(stackId);
        String adminGroupName = StringUtils.isNotEmpty(freeIpa.getAdminGroupName()) ? freeIpa.getAdminGroupName() : "";
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setName(stack.getName());
        ldapConfig.setEnvironmentCrn(stack.getEnvironmentCrn());
        ldapConfig.setAdminGroup(adminGroupName);
        ldapConfig.setUserGroup(USER_GROUP);
        String domainComponent = generateDomainComponent(freeIpa);
        ldapConfig.setBindDn(StringUtils.isBlank(bindDn) ? BIND_DN + domainComponent : bindDn);
        ldapConfig.setUserSearchBase(USER_SEARCH_BASE + domainComponent);
        ldapConfig.setGroupSearchBase(GROUP_SEARCH_BASE + domainComponent);
        ldapConfig.setUserDnPattern(USER_DN_PATTERN + domainComponent);
        ldapConfig.setServerHost(getMasterInstance(stack).getDiscoveryFQDN());
        ldapConfig.setProtocol(PROTOCOL);
        ldapConfig.setServerPort(SERVER_PORT);
        ldapConfig.setDomain(freeIpa.getDomain());
        ldapConfig.setBindPassword(StringUtils.isBlank(bindPassword) ? freeIpa.getAdminPassword() : bindPassword);
        ldapConfig.setDirectoryType(DirectoryType.LDAP);
        ldapConfig.setUserNameAttribute(USER_NAME_ATTRIBUTE);
        ldapConfig.setUserObjectClass(USER_OBJECT_CLASS);
        ldapConfig.setGroupMemberAttribute(GROUP_MEMBER_ATTRIBUTE);
        ldapConfig.setGroupNameAttribute(GROUP_NAME_ATTRIBUTE);
        ldapConfig.setGroupObjectClass(GROUP_OBJECT_CLASS);
        ldapConfig.setClusterName(clusterName);
        return ldapConfigService.createLdapConfig(ldapConfig, stack.getAccountId());
    }

    private String generateDomainComponent(FreeIpa freeIpa) {
        StringJoiner dcJoiner = new StringJoiner(",dc=", ",dc=", "");
        for (String domain : freeIpa.getDomain().split("\\.")) {
            dcJoiner.add(domain);
        }
        return dcJoiner.toString();
    }

    @Override
    public void delete(Stack stack) {
        ldapConfigService.deleteAllInEnvironment(stack.getEnvironmentCrn(), stack.getAccountId());
    }
}
