package com.sequenceiq.cloudbreak.blueprint.template.views;

import com.sequenceiq.cloudbreak.api.model.DirectoryType;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

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

    private String connectionURL;

    public LdapView(LdapConfig ldapConfig) {
        connectionURL = ldapConfig.getProtocol() + "://" + ldapConfig.getServerHost();
        if (ldapConfig.getServerPort() != null) {
            connectionURL = connectionURL + ':' + ldapConfig.getServerPort();
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
}
