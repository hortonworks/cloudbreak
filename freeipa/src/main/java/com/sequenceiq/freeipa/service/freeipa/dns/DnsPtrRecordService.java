package com.sequenceiq.freeipa.service.freeipa.dns;

import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreEmptyModExceptionWithValue;
import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundException;
import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsPtrRecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.DeleteDnsPtrRecordRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class DnsPtrRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DnsPtrRecordService.class);

    private static final String IN_ADDR_ARPA = ".in-addr.arpa.";

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private StackService stackService;

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    @Measure(DnsPtrRecordService.class)
    public void addDnsPtrRecord(AddDnsPtrRecordRequest addDnsPtrRecordRequest, String accountId) throws FreeIpaClientException {
        DnsPtrRecord dnsPtrRecord = createPtrRecord(addDnsPtrRecordRequest);
        FreeIpaAndClient freeIpaAndClient = createFreeIpaAndClient(addDnsPtrRecordRequest.getEnvironmentCrn(), accountId);
        FreeIpaClient freeIpaClient = freeIpaAndClient.getClient();
        String reverseDnsZone = calculateReverseDnsZone(dnsPtrRecord, freeIpaClient);
        if (StringUtils.isNotEmpty(reverseDnsZone)) {
            String ptrRecordName = createPtrRecordName(createZoneParts(reverseDnsZone), dnsPtrRecord.ipParts());
            Optional<DnsRecord> dnsRecord = ignoreNotFoundExceptionWithValue(() -> freeIpaClient.showDnsRecord(reverseDnsZone, ptrRecordName), null);
            String targetFqdn = StringUtils.appendIfMissing(addDnsPtrRecordRequest.getFqdn(), ".");
            if (dnsRecord.isEmpty()) {
                createDnsPtrRecord(freeIpaClient, reverseDnsZone, ptrRecordName, targetFqdn);
            } else {
                validateExistingPtrRecordMatches(dnsRecord.get(), targetFqdn);
            }
        } else {
            throw new FreeIpaClientException(String.format("Reverse Dns Zone %s is missing.", addDnsPtrRecordRequest.getReverseDnsZone()));
        }
    }

    private DnsPtrRecord createPtrRecord(AddDnsPtrRecordRequest addDnsPtrRecordRequest) {
        String fixedZone = StringUtils.appendIfMissing(addDnsPtrRecordRequest.getReverseDnsZone(), ".");
        return new DnsPtrRecord(addDnsPtrRecordRequest.getIp(),
                Arrays.asList(addDnsPtrRecordRequest.getIp().split("\\.")),
                fixedZone,
                fixedZone != null ? Arrays.asList(StringUtils.removeEnd(fixedZone, IN_ADDR_ARPA).split("\\.")).reversed() : null,
                StringUtils.appendIfMissing(addDnsPtrRecordRequest.getFqdn(), "."));
    }

    private DnsPtrRecord createPtrRecord(DeleteDnsPtrRecordRequest deleteDnsPtrRecordRequest) {
        String fixedZone = StringUtils.appendIfMissing(deleteDnsPtrRecordRequest.getReverseDnsZone(), ".");
        return new DnsPtrRecord(deleteDnsPtrRecordRequest.getIp(),
                Arrays.asList(deleteDnsPtrRecordRequest.getIp().split("\\.")),
                fixedZone,
                fixedZone != null ? Arrays.asList(StringUtils.removeEnd(fixedZone, IN_ADDR_ARPA).split("\\.")).reversed() : null,
                "");
    }

    private String calculateReverseDnsZone(DnsPtrRecord dnsPtrRecord, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        if (StringUtils.isNotEmpty(dnsPtrRecord.revereseDnsZone())) {
            if (isIpInZoneRange(dnsPtrRecord.ipParts(), dnsPtrRecord.zoneParts())) {
                return dnsPtrRecord.revereseDnsZone();
            } else {
                throw new FreeIpaClientException(
                        String.format("Reverse dns zone %s is not matching with ip %s", dnsPtrRecord.revereseDnsZone(), dnsPtrRecord.ip()));
            }
        } else {
            String reverseDnsZone = filterReverseDnsZonesForIp(getReverseDnsZones(freeIpaClient), dnsPtrRecord.ipParts()).stream()
                    .max(Comparator.comparingInt(String::length))
                    .map(zone -> StringUtils.appendIfMissing(zone, "."))
                    .orElseThrow(() -> {
                        LOGGER.info("No matching reverse dns zone found for {} ip", dnsPtrRecord.ip());
                        return new FreeIpaClientException(String.format("No matching reverse dns zone found for %s ip", dnsPtrRecord.ip()));
                    });
            LOGGER.info("Matched reverse dns zone for ip {} is {}", dnsPtrRecord.ipParts(), reverseDnsZone);
            return reverseDnsZone;
        }
    }

    private void createDnsPtrRecord(FreeIpaClient client, String zone, String ptrRecordName, String targetFqdn) throws FreeIpaClientException {
        LOGGER.info("Creating PTR record in zone [{}] with name [{}] with target [{}].", zone, ptrRecordName, targetFqdn);
        Optional<DnsRecord> record = ignoreEmptyModExceptionWithValue(() -> client.addDnsPtrRecord(zone, ptrRecordName, targetFqdn),
                "PTR record created with name [{}] with target [{}] is already exists, nothing to do.", ptrRecordName, targetFqdn);
        LOGGER.info("PTR record created with name [{}] with target [{}]. Record: {}", ptrRecordName, targetFqdn, record);
    }

    private void validateExistingPtrRecordMatches(DnsRecord record, String targetFqdn) {
        LOGGER.debug("Validating already existing PTR record: {}", record);
        if (!record.isPtrRecord()) {
            LOGGER.info("Record already exists and it's not a PTR record");
            throw new DnsRecordConflictException("Record already exists and it's not a PTR record");
        } else if (!record.getPtrrecord().contains(targetFqdn)) {
            LOGGER.info("PTR record already exists and the target doesn't match");
            throw new DnsRecordConflictException("PTR record already exists and the target doesn't match");
        } else {
            LOGGER.info("PTR record already exists and matches with requested. Nothing to do");
        }
    }

    private List<String> getReverseDnsZones(FreeIpaClient client) throws FreeIpaClientException {
        Set<DnsZone> allDnsZone = client.findAllDnsZone();
        return allDnsZone.stream()
                .map(DnsZone::getIdnsname)
                .filter(dnsZone -> dnsZone.endsWith(IN_ADDR_ARPA))
                .toList();
    }

    private List<String> filterReverseDnsZonesForIp(List<String> zones, List<String> ipParts) {
        return zones.stream()
                .filter(zone -> isIpInZoneRange(ipParts, createZoneParts(zone)))
                .toList();
    }

    private boolean isIpInZoneRange(List<String> ipParts, List<String> zoneParts) {
        return ipParts.subList(0, Math.min(zoneParts.size(), ipParts.size())).equals(zoneParts);
    }

    private List<String> createZoneParts(String zone) {
        return Arrays.asList(StringUtils.removeEnd(zone, IN_ADDR_ARPA).split("\\.")).reversed();
    }

    private String createPtrRecordName(List<String> zoneParts, List<String> ipParts) {
        return zoneParts.size() >= ipParts.size() ? "" : String.join(".", ipParts.subList(zoneParts.size(), ipParts.size()).reversed());
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    @Measure(DnsPtrRecordService.class)
    public void deleteDnsPtrRecord(DeleteDnsPtrRecordRequest deleteDnsPtrRecordRequest, String accountId) throws FreeIpaClientException {
        DnsPtrRecord dnsPtrRecord = createPtrRecord(deleteDnsPtrRecordRequest);
        FreeIpaAndClient freeIpaAndClient = createFreeIpaAndClient(deleteDnsPtrRecordRequest.getEnvironmentCrn(), accountId);
        FreeIpaClient freeIpaClient = freeIpaAndClient.getClient();
        List<String> reverseDnsZones;
        if (StringUtils.isNotEmpty(deleteDnsPtrRecordRequest.getReverseDnsZone())) {
            if (isIpInZoneRange(dnsPtrRecord.ipParts(), dnsPtrRecord.zoneParts())) {
                reverseDnsZones = List.of(dnsPtrRecord.revereseDnsZone());
            } else {
                throw new FreeIpaClientException(
                        String.format("Reverse dns zone %s is not matching with ip %s", dnsPtrRecord.revereseDnsZone(), dnsPtrRecord.ip()));
            }
        } else {
            reverseDnsZones = filterReverseDnsZonesForIp(getReverseDnsZones(freeIpaClient), dnsPtrRecord.ipParts());
        }
        for (String reverseDnsZone : reverseDnsZones) {
            List<String> zoneParts = createZoneParts(reverseDnsZone);
            String ptrRecordName = createPtrRecordName(zoneParts, dnsPtrRecord.ipParts());
            ignoreNotFoundException(() -> freeIpaAndClient.getClient().deleteDnsRecord(ptrRecordName, reverseDnsZone),
                    "PTR record in zone [{}] with name [{}] not found", reverseDnsZones, ptrRecordName);
        }
    }

    private FreeIpaAndClient createFreeIpaAndClient(String environmentCrn, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        return new FreeIpaAndClient(freeIpa, freeIpaClient);
    }
}
