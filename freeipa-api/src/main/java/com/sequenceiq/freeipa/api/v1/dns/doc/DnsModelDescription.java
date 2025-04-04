package com.sequenceiq.freeipa.api.v1.dns.doc;

public final class DnsModelDescription {
    public static final String CNAME = "DNS name without the domain. eg. 'ipaserver' from 'ipaserver.cloudera.site'";

    public static final String HOSTNAME = "Hostname name without the domain. eg. 'ipaserver' from 'ipaserver.cloudera.site'";

    public static final String FQDN = "Fully qualified domain name eg. 'ipaserver.cloudera.site'";

    public static final String IP = "The IP address of the host the A record should point to. Only IPv4";

    public static final String DNS_ZONE = "It's the domain. Eg if your FQDN is ipaserver.cloudera.site, it's 'cloudera.site'. "
            + "'168.192.in-addr.arpa' for a reverse record like '5.1.168.192.in-addr.arpa'";

    public static final String CNAME_TARGET_FQDN = "The fully qualified domain name of the host the CNAME should point to.";

    public static final String CREATE_REVERSE = "Tries to create a reverse pointer for the record (PTR). Only if reverse zone already exists";

    public static final String FORCE = "Replaces the current value regardless of whether it already exists";

    private DnsModelDescription() {
    }
}
