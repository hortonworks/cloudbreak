package com.sequenceiq.freeipa.service.freeipa.dns;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class DnsSoaRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsSoaRecordService.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void updateSoaRecords(Long stackId, Set<String> fqdnsToUpdate) throws FreeIpaClientException {
        LOGGER.info("Updating SOA records for FQDNs: {}", fqdnsToUpdate);
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackId(stackId);
        String newFqdn = freeIpaClient.findAllServers().stream()
                .map(IpaServer::getFqdn)
                .filter(Objects::nonNull)
                .map(fqdn -> StringUtils.appendIfMissing(fqdn, "."))
                .filter(fqdn -> !fqdnsToUpdate.contains(fqdn))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A alternate FreeIPA server was not found for the SOA records"));
        Set<DnsZone> dnsZonesToUpdate = freeIpaClient.findAllDnsZone().stream()
                .filter(dnsZone -> Objects.nonNull(dnsZone.getIdnssoamname()))
                .filter(dnsZone -> fqdnsToUpdate.contains(dnsZone.getIdnssoamname()))
                .collect(Collectors.toSet());
        LOGGER.info("New FQDN [{}] and DNS zones to update: {}", newFqdn, dnsZonesToUpdate);
        for (DnsZone dnsZone : dnsZonesToUpdate) {
            freeIpaClient.setDnsZoneAuthoritativeNameserver(dnsZone.getIdnsname(), newFqdn);
        }
    }
}
