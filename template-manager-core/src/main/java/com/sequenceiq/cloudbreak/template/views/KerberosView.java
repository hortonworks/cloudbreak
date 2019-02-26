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

    public KerberosView(KerberosConfig kerberosConfig) {
        type = kerberosConfig.getType();
        masterKey = kerberosConfig.getMasterKey();
        admin = kerberosConfig.getAdmin();
        password = kerberosConfig.getPassword();
        url = kerberosConfig.getUrl();
        adminUrl = kerberosConfig.getAdminUrl();
        realm = kerberosConfig.getRealm();
        tcpAllowed = kerberosConfig.getTcpAllowed();
        principal = kerberosConfig.getPrincipal();
        ldapUrl = kerberosConfig.getLdapUrl();
        containerDn = kerberosConfig.getContainerDn();
        descriptor = kerberosConfig.getDescriptor();
        krb5Conf = kerberosConfig.getKrb5Conf();
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
