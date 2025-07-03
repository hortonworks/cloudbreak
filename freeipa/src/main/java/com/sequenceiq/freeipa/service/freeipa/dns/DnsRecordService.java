package com.sequenceiq.freeipa.service.freeipa.dns;

import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreEmptyModExceptionWithValue;
import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundException;
import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsARecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
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

    @Inject
    private CleanupService cleanupService;

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    @Measure(DnsRecordService.class)
    public void deleteDnsRecordByFqdn(String environmentCrn, String accountId, List<String> fqdns) throws FreeIpaClientException {
        FreeIpaAndClient freeIpaAndClient = createFreeIpaAndClient(environmentCrn, accountId);
        for (DnsZone dnsZone : freeIpaAndClient.getClient().findAllDnsZone()) {
            LOGGER.debug("Looking for records in zone [{}]", dnsZone.getIdnsname());
            Set<DnsRecord> allDnsRecordsInZone = freeIpaAndClient.getClient().findAllDnsRecordInZone(dnsZone.getIdnsname());
            deleteRegularRecords(freeIpaAndClient.getClient(), dnsZone, allDnsRecordsInZone, fqdns, freeIpaAndClient.getFreeIpa().getDomain());
            deleteSrvRecords(freeIpaAndClient.getClient(), dnsZone, allDnsRecordsInZone, fqdns);
        }
    }

    private void deleteRegularRecords(FreeIpaClient freeIpaClient, DnsZone dnsZone, Set<DnsRecord> allDnsRecordsInZone, List<String> fqdns, String domain)
            throws FreeIpaClientException {
        Set<DnsRecord> recordsToDelete = allDnsRecordsInZone.stream()
                .filter(record -> fqdns.stream().anyMatch(fqdn -> record.isHostRelatedRecord(fqdn, domain)))
                .collect(Collectors.toSet());
        for (DnsRecord dnsRecord : recordsToDelete) {
            LOGGER.info("Delete DNS record [{}] in zone [{}]", dnsRecord, dnsZone);
            ignoreNotFoundException(() -> freeIpaClient.deleteDnsRecord(dnsRecord.getIdnsname(), dnsZone.getIdnsname()),
                    "DNS record [{}] not found in zone [{}]", dnsRecord, dnsZone);
        }
    }

    private void deleteSrvRecords(FreeIpaClient freeIpaClient, DnsZone dnsZone, Set<DnsRecord> allDnsRecordsInZone, List<String> fqdns)
            throws FreeIpaClientException {
        Set<DnsRecord> srvRecordsToDelete = allDnsRecordsInZone.stream()
                .filter(record -> fqdns.stream().anyMatch(record::isHostRelatedSrvRecord))
                .collect(Collectors.toSet());
        for (DnsRecord dnsRecord : srvRecordsToDelete) {
            for (String fqdn : fqdns) {
                List<String> srvRecords = dnsRecord.getHostRelatedSrvRecords(fqdn);
                if (!srvRecords.isEmpty()) {
                    LOGGER.info("Delete DNS SRV record [{}] for [{}] in zone [{}]", dnsRecord.getIdnsname(), fqdn, dnsZone);
                    ignoreNotFoundException(() -> freeIpaClient.deleteDnsSrvRecord(dnsRecord.getIdnsname(), dnsZone.getIdnsname(), srvRecords),
                            "DNS SRV record [{}] for [{}] not found in zone [{}]", dnsRecord.getIdnsname(), fqdn, dnsZone);
                }
            }
        }
    }

    private FreeIpaAndClient createFreeIpaAndClient(String environmentCrn, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        return createFreeIpaAndClient(stack);
    }

    private FreeIpaAndClient createFreeIpaAndClient(Stack stack) throws FreeIpaClientException {
        MDCBuilder.buildMdcContext(stack);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        return new FreeIpaAndClient(freeIpa, freeIpaClient);
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    @Measure(DnsRecordService.class)
    public void addDnsARecord(String accountId, @Valid AddDnsARecordRequest request) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(request.getEnvironmentCrn(), accountId);
        FreeIpaAndClient freeIpaAndClient = createFreeIpaAndClient(stack);
        LOGGER.info("Processing AddDnsARecordRequest: {}", request);
        String zone = calculateZone(request.getDnsZone(), freeIpaAndClient);
        Optional<DnsRecord> dnsRecord = ignoreNotFoundExceptionWithValue(() -> freeIpaAndClient.getClient().showDnsRecord(zone, request.getHostname()), null);
        if (dnsRecord.isEmpty() && !request.isForce()) {
            createDnsARecord(freeIpaAndClient.getClient(), zone, request.getHostname(), request.getIp(), request.isCreateReverse());
        } else if (request.isForce() && (dnsRecord.isEmpty() || !dnsRecord.get().getArecord().contains(request.getIp()))) {
            LOGGER.debug("Force updating DNS record {} based on the requested data: {}", dnsRecord, request);
            cleanupOldRecords(freeIpaAndClient.getClient(), request, stack);
            createDnsARecord(freeIpaAndClient.getClient(), zone, request.getHostname(), request.getIp(), request.isCreateReverse());
        } else {
            dnsRecord.ifPresent(record -> validateExistingARecordMatchesRequested(request.getIp(), record));
        }
    }

    private void cleanupOldRecords(FreeIpaClient freeIpaClient, AddDnsARecordRequest request, Stack stack) throws FreeIpaClientException {
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        String domain = freeIpa.getDomain();
        cleanupService.removeDnsEntries(freeIpaClient, Set.of(FreeIpaDomainUtils.buildFqdn(request.getHostname(), domain)), Set.of(request.getIp()), domain);
    }

    private String calculateZone(String zoneFromRequest, FreeIpaAndClient freeIpaAndClient) throws FreeIpaClientException {
        Optional<String> optionalZone = Optional.ofNullable(zoneFromRequest);
        String zone = optionalZone.orElse(freeIpaAndClient.getFreeIpa().getDomain());
        boolean providedZoneDifferentThanDefault = !freeIpaAndClient.getFreeIpa().getDomain().equals(StringUtils.removeEnd(zone, "."));
        if (optionalZone.isPresent() && providedZoneDifferentThanDefault) {
            LOGGER.debug("Zone is provided in the request [{}] and different from the default one [{}]", zone, freeIpaAndClient.getFreeIpa().getDomain());
            validateZoneExists(optionalZone.get(), freeIpaAndClient.getClient());
        }
        return zone;
    }

    private void validateZoneExists(String zone, FreeIpaClient client) throws FreeIpaClientException {
        Set<DnsZone> allDnsZone = client.findAllDnsZone();
        String zoneInFreeIpaFormat = StringUtils.appendIfMissing(zone, ".");
        boolean zoneMissing = allDnsZone.stream()
                .map(DnsZone::getIdnsname)
                .noneMatch(zoneFromIpa -> zoneFromIpa.equals(zoneInFreeIpaFormat));
        if (zoneMissing) {
            String msg = String.format("Zone [%s] doesn't exists", zone);
            LOGGER.info(msg);
            throw new BadRequestException(msg);
        }
    }

    private void validateExistingARecordMatchesRequested(String ip, DnsRecord record) {
        LOGGER.debug("Validating already existing record: {}", record);
        if (!record.isARecord()) {
            LOGGER.info("Record already exists and it's not an A record");
            throw new DnsRecordConflictException("Record already exists and it's not an A record");
        } else if (!record.getArecord().contains(ip)) {
            LOGGER.info("Record already exists and the IP doesn't match");
            throw new DnsRecordConflictException("Record already exists and the IP doesn't match");
        } else {
            LOGGER.info("A record already exists and matches with requested. Nothing to do");
        }
    }

    private void createDnsARecord(FreeIpaClient client, String zone, String hostname, String ip, boolean createReverse) throws FreeIpaClientException {
        LOGGER.info("Creating A record in zone [{}] with hostname [{}] with IP [{}]. Create reverse set to [{}]",
                zone, hostname, ip, createReverse);
        try {
            Optional<DnsRecord> record = ignoreEmptyModExceptionWithValue(() -> client.addDnsARecord(zone, hostname, ip, createReverse),
                    "Record [{}] pointing to [{}] is already exists, nothing to do.", hostname, ip);
            LOGGER.info("A record [{}] pointing to [{}] is created successfully. Created record: {}", hostname, ip, record);
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                LOGGER.warn("Duplicate entry, usually reverse entry already exists.", e);
                throw new DnsRecordConflictException(e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    @Measure(DnsRecordService.class)
    public void addDnsCnameRecord(String accountId, AddDnsCnameRecordRequest request) throws FreeIpaClientException {
        FreeIpaAndClient freeIpaAndClient = createFreeIpaAndClient(request.getEnvironmentCrn(), accountId);
        String zone = calculateZone(request.getDnsZone(), freeIpaAndClient);
        Optional<DnsRecord> dnsRecord = ignoreNotFoundExceptionWithValue(() -> freeIpaAndClient.getClient().showDnsRecord(zone, request.getCname()), null);
        String targetFqdn = StringUtils.appendIfMissing(request.getTargetFqdn(), ".");
        if (dnsRecord.isPresent()) {
            handleExistingCname(freeIpaAndClient.getClient(), accountId, request, dnsRecord.get(), targetFqdn, zone);
        } else {
            createDnsCnameRecord(freeIpaAndClient.getClient(), zone, request.getCname(), targetFqdn);
        }
    }

    private void handleExistingCname(FreeIpaClient freeIpaClient, String accountId, AddDnsCnameRecordRequest request, DnsRecord dnsRecord, String targetFqdn,
            String zone) throws FreeIpaClientException {
        if (request.isForce() && !dnsRecord.getCnamerecord().contains(targetFqdn)) {
            LOGGER.info("Record already exists and the target doesn't match. CNAME {}, FQDN: {}. Deleting record", dnsRecord.getCnamerecord(), targetFqdn);
            deleteDnsRecord(accountId, request.getEnvironmentCrn(), null, request.getCname());
            createDnsCnameRecord(freeIpaClient, zone, request.getCname(), targetFqdn);
        } else {
            validateExistingCnameRecordMatches(dnsRecord, targetFqdn);
        }
    }

    private void validateExistingCnameRecordMatches(DnsRecord record, String targetFqdn) {
        LOGGER.debug("Validating already existing record: {}", record);
        if (!record.isCnameRecord()) {
            LOGGER.info("Record already exists and it's not a CNAME record");
            throw new DnsRecordConflictException("Record already exists and it's not a CNAME record");
        } else if (!record.getCnamerecord().contains(targetFqdn)) {
            LOGGER.info("Record already exists and the target doesn't match");
            throw new DnsRecordConflictException("Record already exists and the target doesn't match");
        } else {
            LOGGER.info("A record already exists and matches with requested. Nothing to do");
        }
    }

    private void createDnsCnameRecord(FreeIpaClient client, String zone, String cname, String targetFqdn) throws FreeIpaClientException {
        LOGGER.info("Creating CNAME record in zone [{}] with name [{}] with target [{}].", zone, cname, targetFqdn);
        Optional<DnsRecord> record = ignoreEmptyModExceptionWithValue(() -> client.addDnsCnameRecord(zone, cname, targetFqdn),
                "CNAME record created with name [{}] with target [{}] is already exists, nothing to do.", cname, targetFqdn);
        LOGGER.info("CNAME record created with name [{}] with target [{}]. Record: {}", cname, targetFqdn, record);
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    @Measure(DnsRecordService.class)
    public void deleteDnsRecord(String accountId, String environmentCrn, String dnsZone, String record) throws FreeIpaClientException {
        FreeIpaAndClient freeIpaAndClient = createFreeIpaAndClient(environmentCrn, accountId);
        String zone = calculateZone(dnsZone, freeIpaAndClient);
        LOGGER.info("Trying to delete record with name [{}] in zone [{}]", record, dnsZone);
        ignoreNotFoundException(() -> freeIpaAndClient.getClient().deleteDnsRecord(record, zone),
                "A record in zone [{}] with name [{}] not found", zone, record);
        LOGGER.info("Deleted record or record missing with name [{}] in zone [{}]", record, zone);
    }
}
