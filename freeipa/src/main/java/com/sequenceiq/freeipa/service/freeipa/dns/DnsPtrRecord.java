package com.sequenceiq.freeipa.service.freeipa.dns;

import java.util.List;

record DnsPtrRecord(String ip, List<String> ipParts, String revereseDnsZone, List<String> zoneParts, String fqdn) {
}
