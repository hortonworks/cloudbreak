package com.sequenceiq.cloudbreak.dto;

import com.sequenceiq.cloudbreak.type.KerberosType;

public class KerberosConfig {
    private KerberosType type;

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

    private Boolean verifyKdcTrust;

    private String domain;

    private String nameServers;

    public KerberosType getType() {
        return type;
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

    public Boolean getVerifyKdcTrust() {
        return verifyKdcTrust;
    }

    public String getDomain() {
        return domain;
    }

    public String getNameServers() {
        return nameServers;
    }

    @Override
    public String toString() {
        return "KerberosConfig{" +
                "type=" + type +
                ", realm='" + realm + '\'' +
                ", domain='" + domain + '\'' +
                ", nameServers='" + nameServers + '\'' +
                '}';
    }

    public static final class KerberosConfigBuilder {
        private KerberosType type;

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

        private Boolean verifyKdcTrust;

        private String domain;

        private String nameServers;

        private KerberosConfigBuilder() {
        }

        public static KerberosConfigBuilder aKerberosConfig() {
            return new KerberosConfigBuilder();
        }

        public KerberosConfigBuilder withType(KerberosType type) {
            this.type = type;
            return this;
        }

        public KerberosConfigBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public KerberosConfigBuilder withUrl(String url) {
            this.url = url;
            return this;
        }

        public KerberosConfigBuilder withAdminUrl(String adminUrl) {
            this.adminUrl = adminUrl;
            return this;
        }

        public KerberosConfigBuilder withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        public KerberosConfigBuilder withTcpAllowed(Boolean tcpAllowed) {
            this.tcpAllowed = tcpAllowed;
            return this;
        }

        public KerberosConfigBuilder withPrincipal(String principal) {
            this.principal = principal;
            return this;
        }

        public KerberosConfigBuilder withLdapUrl(String ldapUrl) {
            this.ldapUrl = ldapUrl;
            return this;
        }

        public KerberosConfigBuilder withContainerDn(String containerDn) {
            this.containerDn = containerDn;
            return this;
        }

        public KerberosConfigBuilder withDescriptor(String descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        public KerberosConfigBuilder withKrb5Conf(String krb5Conf) {
            this.krb5Conf = krb5Conf;
            return this;
        }

        public KerberosConfigBuilder withVerifyKdcTrust(Boolean verifyKdcTrust) {
            this.verifyKdcTrust = verifyKdcTrust;
            return this;
        }

        public KerberosConfigBuilder withDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public KerberosConfigBuilder withNameServers(String nameServers) {
            this.nameServers = nameServers;
            return this;
        }

        public KerberosConfig build() {
            KerberosConfig kerberosConfig = new KerberosConfig();
            kerberosConfig.type = type;
            kerberosConfig.password = password;
            kerberosConfig.url = url;
            kerberosConfig.adminUrl = adminUrl;
            kerberosConfig.realm = realm;
            kerberosConfig.tcpAllowed = tcpAllowed;
            kerberosConfig.principal = principal;
            kerberosConfig.ldapUrl = ldapUrl;
            kerberosConfig.containerDn = containerDn;
            kerberosConfig.descriptor = descriptor;
            kerberosConfig.krb5Conf = krb5Conf;
            kerberosConfig.verifyKdcTrust = verifyKdcTrust;
            kerberosConfig.domain = domain;
            kerberosConfig.nameServers = nameServers;
            return kerberosConfig;
        }
    }
}
