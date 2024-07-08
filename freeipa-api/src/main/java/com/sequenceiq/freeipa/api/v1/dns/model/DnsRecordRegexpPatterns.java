package com.sequenceiq.freeipa.api.v1.dns.model;

public final class DnsRecordRegexpPatterns {

    public static final String DNS_ZONE_PATTERN = "^[a-zA-Z0-9]+[a-zA-Z0-9-\\.]*[a-zA-Z0-9\\.]+$";

    public static final String DNS_ZONE_MSG = "DNS zone must be valid. It can contain alphanumeric characters, dash and dot.";

    public static final String DNS_REVERSEZONE_PATTERN = "^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){1,3}in-addr\\.arpa\\.?$";

    public static final String DNS_REVERSEZONE_MSG = "DNS reverse zone must be valid (eg. 0.10.in-addr.arpa)";

    public static final String DNS_CNAME_PATTERN = "^(\\*\\.)?[a-zA-Z0-9]+[a-zA-Z0-9-\\.]*[a-zA-Z0-9]+$";

    public static final String DNS_CNAME_MSG = "CNAME must be valid. Might start with '*.' and can contain alphanumeric characters, dash and dot.";

    public static final String DNS_HOSTNAME_PATTERN = "^(\\*\\.)?[a-zA-Z0-9]+[a-zA-Z0-9-\\.]*[a-zA-Z0-9]+$";

    public static final String DNS_HOSTNAME_MSG = "Hostname must be valid. Can contain alphanumeric characters, dash and dot.";

    public static final String DNS_FQDN_MSG = "FQDN must be valid. Can contain alphanumeric characters, dash and dot.";

    public static final String CNAME_TARGET_REGEXP = "^[a-zA-Z0-9]+[a-zA-Z0-9-\\.]*[a-zA-Z0-9]+[.]?$";

    public static final String DNS_IP_PATTERN = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    public static final String DNS_IP_MSG = "Must be a valid IPv4 format like 1.2.3.4";

    private DnsRecordRegexpPatterns() {
    }
}
