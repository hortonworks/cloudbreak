package com.sequenceiq.freeipa.api.v1.dns.model;

public final class DnsRecordRegexpPatterns {

    public static final String DNS_ZONE_PATTERN = "^[a-zA-Z0-9]+[a-zA-Z0-9-\\.]*[a-zA-Z0-9\\.]+$";

    public static final String DNS_ZONE_MSG = "DNS zone must be valid. It can contain alphanumeric characters, dash and dot.";

    public static final String DNS_CNAME_PATTERN = "^(\\*\\.)?[a-zA-Z0-9]+[a-zA-Z0-9-\\.]*[a-zA-Z0-9]+$";

    public static final String DNS_CNAME_MSG = "CNAME must be valid. Might start with '*.' and can contain alphanumeric characters, dash and dot.";

    public static final String DNS_HOSTNAME_PATTERN = "^[a-zA-Z0-9]+[a-zA-Z0-9-\\.]*[a-zA-Z0-9]+$";

    public static final String DNS_HOSTNAME_MSG = "Hostname must be valid. Can contain alphanumeric characters, dash and dot.";

    public static final String CNAME_TARGET_REGEXP = "^[a-zA-Z0-9]+[a-zA-Z0-9-\\.]*[a-zA-Z0-9]+$";

    private DnsRecordRegexpPatterns() {
    }
}
