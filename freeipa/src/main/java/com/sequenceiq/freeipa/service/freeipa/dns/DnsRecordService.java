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
            Set<DnsRecord> allDnsRecordsInZone = freeIpaClient.findAllDnsRecordInZone(dnsZone.getIdnsname());
            deleteRegularRecords(freeIpaClient, dnsZone, allDnsRecordsInZone, fqdns, freeIpa.getDomain());
            deleteSrvRecords(freeIpaClient, dnsZone, allDnsRecordsInZone, fqdns);
        }
    }

    private void deleteRegularRecords(FreeIpaClient freeIpaClient, DnsZoneList dnsZone, Set<DnsRecord> allDnsRecordsInZone, List<String> fqdns, String domain)
            throws FreeIpaClientException {
        Set<DnsRecord> recordsToDelete = allDnsRecordsInZone.stream()
                .filter(record -> fqdns.stream().anyMatch(fqdn -> record.isHostRelatedRecord(fqdn, domain)))
                .collect(Collectors.toSet());
        for (DnsRecord dnsRecord : recordsToDelete) {
            LOGGER.info("Delete DNS record [{}] in zone [{}]", dnsRecord, dnsZone);
            freeIpaClient.deleteDnsRecord(dnsRecord.getIdnsname(), dnsZone.getIdnsname());
        }
    }

    private void deleteSrvRecords(FreeIpaClient freeIpaClient, DnsZoneList dnsZone, Set<DnsRecord> allDnsRecordsInZone, List<String> fqdns)
            throws FreeIpaClientException {
        Set<DnsRecord> srvRecordsToDelete = allDnsRecordsInZone.stream()
                .filter(record -> fqdns.stream().anyMatch(fqdn -> record.isHostRelatedSrvRecord(fqdn)))
                .collect(Collectors.toSet());
        for (DnsRecord dnsRecord : srvRecordsToDelete) {
            for (String fqdn : fqdns) {
                List<String> srvRecords = dnsRecord.getHostRelatedSrvRecords(fqdn);
                if (!srvRecords.isEmpty()) {
                    LOGGER.info("Delete DNS SRV record [{}] for [{}] in zone [{}]", dnsRecord.getIdnsname(), fqdn, dnsZone);
                    freeIpaClient.deleteDnsSrvRecord(dnsRecord.getIdnsname(), dnsZone.getIdnsname(), srvRecords);
                }
            }
        }
    }
}
