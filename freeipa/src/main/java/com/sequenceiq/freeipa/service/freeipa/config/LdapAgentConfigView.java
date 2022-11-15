package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.Map;

public class LdapAgentConfigView {
    private static final int PORT = 6080;

    private static final String HOST = "localhost";

    private static final String LDAP_HOST = "localhost";

    private final String baseDn;

    public LdapAgentConfigView(String baseDn) {
        this.baseDn = baseDn;
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "port", PORT,
                "host", HOST,
                "ldapHost", LDAP_HOST,
                "baseDn", baseDn
        );
    }

    @Override
    public String toString() {
        return "LdapAgentConfigView{" +
                "port=" + PORT +
                ", host='" + HOST + '\'' +
                ", ldapHost='" + LDAP_HOST + '\'' +
                ", baseDn='" + baseDn + '\'' +
                '}';
    }
}
