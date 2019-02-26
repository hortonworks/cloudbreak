package com.sequenceiq.cloudbreak.template.views;

import javax.annotation.Nonnull;

import com.google.common.xml.XmlEscapers;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
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

    private final String protocol;

    private final String adminGroup;

    private final String userDnPattern;

    private final String serverHost;

    private String connectionURL;

    private int serverPort;

    public LdapView(@Nonnull LdapConfig ldapConfig, @Nonnull String bindDn, @Nonnull String bindPassword) {
        protocol = ldapConfig.getProtocol().toLowerCase();
        serverHost = ldapConfig.getServerHost();
        connectionURL = protocol + "://" + ldapConfig.getServerHost();
        if (ldapConfig.getServerPort() != null) {
            serverPort = ldapConfig.getServerPort();
            connectionURL = connectionURL.toLowerCase() + ':' + ldapConfig.getServerPort();
        }
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
        this.bindDn = bindDn;
        this.bindPassword = bindPassword;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public boolean isSecure() {
        return "ldaps".equalsIgnoreCase(protocol.toLowerCase());
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

    public String getBindPasswordEscaped() {
        return XmlEscapers.xmlAttributeEscaper().escape(bindPassword);
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

    public boolean isLdap() {
        return directoryType != DirectoryType.ACTIVE_DIRECTORY;
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