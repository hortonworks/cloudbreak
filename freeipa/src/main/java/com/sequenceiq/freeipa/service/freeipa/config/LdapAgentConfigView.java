package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.Map;

public class LdapAgentConfigView {
    private static final int PORT = 6080;

    private static final String HOST = "localhost";

    private static final String LDAP_HOST = "localhost";

    private final String baseDn;

    private final boolean useTls;

    public LdapAgentConfigView(String baseDn, boolean useTls) {
        this.baseDn = baseDn;
        this.useTls = useTls;
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "port", PORT,
                "host", HOST,
                "ldapHost", LDAP_HOST,
                "baseDn", baseDn,
                "useTls", useTls
        );
    }

    @Override
    public String toString() {
        return "LdapAgentConfigView{" +
                "port=" + PORT +
                ", host='" + HOST + '\'' +
                ", ldapHost='" + LDAP_HOST + '\'' +
                ", baseDn='" + baseDn + '\'' +
                ", useTls='" + useTls + '\'' +
                '}';
    }
}
