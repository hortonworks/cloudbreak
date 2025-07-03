package com.sequenceiq.freeipa.service.config;

public class FreeIpaDomainUtils {

    private static final String IPA_CA_HOST = "ipa-ca";

    private static final String KDC_HOST = "kdc";

    private static final String KERBEROS_HOST = "kerberos";

    private static final String LDAP_HOST = "ldap";

    private static final String FREEIPA_HOST = "freeipa";

    private static final String SEPARATOR = ".";

    private FreeIpaDomainUtils() {
    }

    public static String getKerberosFqdn(String domain) {
        return buildFqdn(KERBEROS_HOST, domain);
    }

    public static String getKerberosHost() {
        return KERBEROS_HOST;
    }

    public static String getKdcFqdn(String domain) {
        return buildFqdn(KDC_HOST, domain);
    }

    public static String getKdcHost() {
        return KDC_HOST;
    }

    public static String getLdapFqdn(String domain) {
        return buildFqdn(LDAP_HOST, domain);
    }

    public static String getLdapHost() {
        return LDAP_HOST;
    }

    public static String getFreeIpaFqdn(String domain) {
        return buildFqdn(FREEIPA_HOST, domain);
    }

    public static String getFreeIpaHost() {
        return FREEIPA_HOST;
    }

    public static String getBuiltInFreeIpaDnsLoadBalancedName(String domain) {
        return buildFqdn(IPA_CA_HOST, domain);
    }

    public static String buildFqdn(String host, String domain) {
        return host + SEPARATOR + domain;
    }

}
