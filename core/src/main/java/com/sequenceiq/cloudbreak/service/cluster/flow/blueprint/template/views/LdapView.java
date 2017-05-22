package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.views;

import com.sequenceiq.cloudbreak.domain.LdapConfig;

public class LdapView {

    private String connectionURL;

    private String bindDn;

    private String bindPassword;

    private String userSearchBase;

    private String userSearchFilter;

    private String userSearchAttribute;

    private String groupSearchBase;

    private String groupSearchFilter;

    private String principalRegex;

    private String domain;

    public LdapView(LdapConfig ldapConfig) {
        this.connectionURL = ldapConfig.getProtocol() + "://" + ldapConfig.getServerHost() + ":" + ldapConfig.getServerPort();
        this.bindDn = ldapConfig.getBindDn();
        this.bindPassword = ldapConfig.getBindPassword();
        this.userSearchBase = ldapConfig.getUserSearchBase();
        this.userSearchFilter = ldapConfig.getGroupSearchFilter();
        this.userSearchAttribute = ldapConfig.getUserSearchAttribute();
        this.groupSearchBase = ldapConfig.getGroupSearchBase();
        this.groupSearchFilter = ldapConfig.getGroupSearchFilter();
        this.principalRegex = ldapConfig.getPrincipalRegex();
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

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public String getUserSearchFilter() {
        return userSearchFilter;
    }

    public String getUserSearchAttribute() {
        return userSearchAttribute;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public String getGroupSearchFilter() {
        return groupSearchFilter;
    }

    public String getPrincipalRegex() {
        return principalRegex;
    }

    public String getDomain() {
        return domain;
    }
}
