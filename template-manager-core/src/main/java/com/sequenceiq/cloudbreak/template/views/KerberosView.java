package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class KerberosView {

    private final KerberosType type;

    private final String masterKey;

    private final String admin;

    private final String password;

    private final String url;

    private final String adminUrl;

    private final String realm;

    private final Boolean tcpAllowed;

    private final String principal;

    private final String ldapUrl;

    private final String containerDn;

    private final String descriptor;

    private final String krb5Conf;

    public KerberosView(KerberosConfig kerberosConfig, String masterKey, String admin, String password, String principal, String descriptor, String krb5Conf) {
        this.masterKey = masterKey;
        this.admin = admin;
        this.password = password;
        this.descriptor = descriptor;
        this.krb5Conf = krb5Conf;
        this.principal = principal;
        type = kerberosConfig.getType();
        url = kerberosConfig.getUrl();
        adminUrl = kerberosConfig.getAdminUrl();
        realm = kerberosConfig.getRealm();
        tcpAllowed = kerberosConfig.isTcpAllowed();
        ldapUrl = kerberosConfig.getLdapUrl();
        containerDn = kerberosConfig.getContainerDn();
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

    public Boolean isTcpAllowed() {
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
