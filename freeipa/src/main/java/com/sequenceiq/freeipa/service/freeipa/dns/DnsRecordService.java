package com.sequenceiq.freeipa.service.freeipa.dns;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZoneList;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class DnsRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsRecordService.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private StackService stackService;

    public void deleteDnsRecordByFqdn(String environmentCrn, String accountId,  List<String> fqdns) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientByAccountAndEnvironment(environmentCrn, accountId);
        for (DnsZoneList dnsZone : freeIpaClient.findAllDnsZone()) {
            LOGGER.debug("Looking for records in zone [{}]", dnsZone.getIdnsname());
            Set<DnsRecord> allDnsRecordInZone = freeIpaClient.findAllDnsRecordInZone(dnsZone.getIdnsname());
            Set<DnsRecord> recordsToDelete = allDnsRecordInZone.stream()
                    .filter(record -> fqdns.stream().anyMatch(fqdn -> record.isHostRelatedRecord(fqdn, freeIpa.getDomain())))
                    .collect(Collectors.toSet());
            for (DnsRecord dnsRecord : recordsToDelete) {
                LOGGER.info("Delete DNS record [{}] in zone [{}]", dnsRecord, dnsZone);
                freeIpaClient.deleteDnsRecord(dnsRecord.getIdnsname(), dnsZone.getIdnsname());
            }
        }
    }
}
