package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class KerberosView {

    private KerberosType type;

    private String masterKey;

    private String admin;

    private String password;

    private String url;

    private String adminUrl;

    private String realm;

    private Boolean tcpAllowed;

    private String principal;

    private String ldapUrl;

    private String containerDn;

    private String descriptor;

    private String krb5Conf;

    public KerberosView(KerberosConfig kerberosConfig) {
        this.type = kerberosConfig.getType();
        this.masterKey = kerberosConfig.getMasterKey();
        this.admin = kerberosConfig.getAdmin();
        this.password = kerberosConfig.getPassword();
        this.url = kerberosConfig.getUrl();
        this.adminUrl = kerberosConfig.getAdminUrl();
        this.realm = kerberosConfig.getRealm();
        this.tcpAllowed = kerberosConfig.getTcpAllowed();
        this.principal = kerberosConfig.getPrincipal();
        this.ldapUrl = kerberosConfig.getLdapUrl();
        this.containerDn = kerberosConfig.getContainerDn();
        this.descriptor = kerberosConfig.getDescriptor();
        this.krb5Conf = kerberosConfig.getKrb5Conf();
    }

    public KerberosType getType() {
        return type;
    }

    public String getMasterKey() {
        return masterKey;
    }

    public String getAdmin() {
        return admin;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public String getRealm() {
        return realm;
    }

    public Boolean getTcpAllowed() {
        return tcpAllowed;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public String getContainerDn() {
        return containerDn;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getKrb5Conf() {
        return krb5Conf;
    }
}
