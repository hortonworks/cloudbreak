package com.sequenceiq.freeipa.service.freeipa.trust;

public class TrustSaltStateParamsConstants {
    public static final String FREEIPA = "freeipa";

    public static final String TRUST_SETUP_PILLAR = "trust_setup";

    public static final String KDC_DOMAIN = "kdc_domain";

    public static final String KDC_IP = "kdc_ip";

    public static final String KDC_REALM = "kdc_realm";

    public static final String DNS_IP = "dns_ip";

    public static final int MAX_RETRY = 5;

    public static final int MAX_RETRY_ON_ERROR = 3;

    private TrustSaltStateParamsConstants() {
    }
}
