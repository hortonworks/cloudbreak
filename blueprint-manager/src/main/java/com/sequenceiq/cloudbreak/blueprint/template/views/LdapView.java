package com.sequenceiq.cloudbreak.blueprint.template.views;

import com.sequenceiq.cloudbreak.api.model.DirectoryType;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

import javax.annotation.Nonnull;

public class LdapView {

    private final String bindDn;

    private final String bindPassword;

    private final DirectoryType directoryType;

    private final String userSearchBase;

    private final String userNameAttribute;

    private final String userObjectClass;

    private final String groupSearchBase;

    private final String groupNameAttribute;

    private final String groupObjectClass;

    private final String groupMemberAttribute;

    private final String domain;

    private final String protocol;

    private final String adminGroup;

    private final String userDnPattern;

    private String connectionURL;

    public LdapView(@Nonnull LdapConfig ldapConfig) {
        protocol = ldapConfig.getProtocol().toLowerCase();
        connectionURL = protocol + "://" + ldapConfig.getServerHost();
        if (ldapConfig.getServerPort() != null) {
            connectionURL = connectionURL.toLowerCase() + ':' + ldapConfig.getServerPort();
        }
        bindDn = ldapConfig.getBindDn();
        bindPassword = ldapConfig.getBindPassword();
        directoryType = ldapConfig.getDirectoryType();
        userSearchBase = ldapConfig.getUserSearchBase();
        userNameAttribute = ldapConfig.getUserNameAttribute();
        userObjectClass = ldapConfig.getUserObjectClass();
        groupSearchBase = ldapConfig.getGroupSearchBase();
        groupNameAttribute = ldapConfig.getGroupNameAttribute();
        groupObjectClass = ldapConfig.getGroupObjectClass();
        groupMemberAttribute = ldapConfig.getGroupMemberAttribute();
        domain = ldapConfig.getDomain();
        userDnPattern = ldapConfig.getUserDnPattern();
        adminGroup = ldapConfig.getAdminGroup();
    }

    public String getProtocol() {
        return protocol;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public String getBindDn() {
        return bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public DirectoryType getDirectoryType() {
        return directoryType;
    }

    public String getDirectoryTypeShort() {
        if (directoryType == DirectoryType.ACTIVE_DIRECTORY) {
            return "ad";
        }
        return "ldap";
    }

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public String getUserObjectClass() {
        return userObjectClass;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    public String getGroupObjectClass() {
        return groupObjectClass;
    }

    public String getGroupMemberAttribute() {
        return groupMemberAttribute;
    }

    public String getDomain() {
        return domain;
    }

    public String getAdminGroup() {
        return adminGroup;
    }

    public String getUserDnPattern() {
        return userDnPattern;
    }
}