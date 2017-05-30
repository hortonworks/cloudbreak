package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.views;

import com.sequenceiq.cloudbreak.common.type.DirectoryType;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

public class LdapView {

    private String connectionURL;

    private String bindDn;

    private String bindPassword;

    private DirectoryType directoryType;

    private String userSearchBase;

    private String userNameAttribute;

    private String userObjectClass;

    private String groupSearchBase;

    private String groupNameAttribute;

    private String groupObjectClass;

    private String groupMemberAttribute;

    private String domain;

    public LdapView(LdapConfig ldapConfig) {
        this.connectionURL = ldapConfig.getProtocol() + "://" + ldapConfig.getServerHost() + ":" + ldapConfig.getServerPort();
        this.bindDn = ldapConfig.getBindDn();
        this.bindPassword = ldapConfig.getBindPassword();
        this.directoryType = ldapConfig.getDirectoryType();
        this.userSearchBase = ldapConfig.getUserSearchBase();
        this.userNameAttribute = ldapConfig.getUserNameAttribute();
        this.userObjectClass = ldapConfig.getUserObjectClass();
        this.groupSearchBase = ldapConfig.getGroupSearchBase();
        this.groupNameAttribute = ldapConfig.getGroupNameAttribute();
        this.groupObjectClass = ldapConfig.getGroupObjectClass();
        this.groupMemberAttribute = ldapConfig.getGroupMemberAttribute();
        this.domain = ldapConfig.getDomain();
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
