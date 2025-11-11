package com.sequenceiq.freeipa.service.freeipa.trust;

public class TrustSaltStateParamsConstants {
    public static final String FREEIPA = "freeipa";

    public static final String TRUST_SETUP_PILLAR = "trust_setup";

    public static final String TRUSTSETUP_DNS_STATE = "trustsetup.dns";

    public static final String TRUSTSETUP_ADD_TRUST = "trustsetup.add_mit_trust";

    public static final String TRUSTSETUP_ADTRUST_INSTALL = "trustsetup.adtrust_install";

    public static final String FREEIPA_REALM = "freeipa_realm";

    public static final String KDC_FQDN = "kdc_fqdn";

    public static final String KDC_IP = "kdc_ip";

    public static final String KDC_REALM = "kdc_realm";

    public static final String TRUST_SECRET = "trust_secret";

    public static final String DNS_IP = "dns_ip";

    public static final int MAX_RETRY = 5;

    public static final int MAX_RETRY_ON_ERROR = 3;

    private TrustSaltStateParamsConstants() {
    }
}
