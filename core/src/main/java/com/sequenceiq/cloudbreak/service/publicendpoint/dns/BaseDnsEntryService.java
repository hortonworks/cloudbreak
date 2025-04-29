package com.sequenceiq.cloudbreak.service.publicendpoint.dns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.publicendpoint.BasePublicEndpointManagementService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public abstract class BaseDnsEntryService extends BasePublicEndpointManagementService {

    private static final String CONSOLE_CDP_APPS = "console-cdp.apps";

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDnsEntryService.class);

    @Inject
    private EnvironmentClientService environmentClientService;

    protected abstract Map<String, List<String>> getComponentLocation(StackDtoDelegate stack);

    protected abstract String logName();

    public Map<String, String> createOrUpdate(StackDtoDelegate stack) {
        return doActionOnStack(stack, null, this::createOrUpdateDnsEntries);
    }

    public Map<String, String> deregister(StackDtoDelegate stack) {
        return doActionOnStack(stack, null, this::doDeregister);
    }

    public Map<String, String> createOrUpdateCandidates(StackDtoDelegate stack, Map<String, String> candidateAddressesByHostname) {
        return doActionOnStack(stack, candidateAddressesByHostname, this::createOrUpdateDnsEntries);
    }

    public Map<String, String> deregister(StackDtoDelegate stack, Map<String, String> candidateAddressesByHostname) {
        return doActionOnStack(stack, candidateAddressesByHostname, this::doDeregister);
    }

    private Map<String, String> doActionOnStack(
            StackDtoDelegate stack,
            Map<String, String> candidateAddressesByHostname,
            BiFunction<Map<String, String>, String, Map<String, String>> action) {

        Map<String, String> result = new HashMap<>();
        if (stack.getCluster() != null && manageCertificateAndDnsInPem(stack.getStack())) {
            LOGGER.info("Modifying DNS entries for {} on stack '{}'", logName(), stack.getName());
            Map<String, String> ipsByHostname = getCandidateIpsByHostname(stack);
            if (!CollectionUtils.isEmpty(candidateAddressesByHostname)) {
                LOGGER.info("Modifying DNS entries for {} on stack '{}', whitelist of candidates for the update: '{}'", logName(), stack.getName(),
                        String.join(",", candidateAddressesByHostname.keySet()));
                ipsByHostname = ipsByHostname.entrySet().stream()
                        .filter(entry -> candidateAddressesByHostname.containsKey(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            result.putAll(action.apply(ipsByHostname, stack.getEnvironmentCrn()));
        }
        return result;
    }

    private Map<String, String> getCandidateIpsByHostname(StackDtoDelegate stack) {
        Map<String, List<String>> componentLocation = getComponentLocation(stack);
        final List<? extends InstanceMetadataView> runningInstanceMetaData = stack.getAllAvailableInstances();
        final InstanceMetadataView primaryGatewayInstanceMetadata = stack.getPrimaryGatewayInstance();
        Map<String, String> candidateIpsByHostname = componentLocation
                .values()
                .stream()
                .flatMap(fqdns -> runningInstanceMetaData.stream().filter(im -> fqdns.contains(im.getDiscoveryFQDN())))
                .filter(im -> primaryGatewayInstanceMetadata != null && !primaryGatewayInstanceMetadata.getPrivateId().equals(im.getPrivateId()))
                .collect(Collectors.toMap(InstanceMetadataView::getShortHostname, InstanceMetadataView::getPublicIpWrapper));

        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        candidateIpsByHostname.putAll(createCdpConsoleDomainEntry(candidateIpsByHostname, componentLocation, environment.getEnvironmentType()));
        return candidateIpsByHostname;
    }

    private Map<String, String> createCdpConsoleDomainEntry(
            Map<String, String> candidateIpsByHostname, Map<String, List<String>> componentLocation, String environmentType) {
        Map<String, String> ipsByHostname = new HashMap<>();
        if (EnvironmentType.HYBRID_BASE.toString().equals(environmentType)) {
            List<String> ecsMasterHostnames = componentLocation.get("ecs_master");
            if (ecsMasterHostnames != null && !ecsMasterHostnames.isEmpty()) {
                String firstEcsMasterHostname = ecsMasterHostnames.getFirst();
                String firstEcsMasterIp = candidateIpsByHostname.get(firstEcsMasterHostname);
                ipsByHostname.put(CONSOLE_CDP_APPS, firstEcsMasterIp);
            }
        }
        return ipsByHostname;
    }

    private Map<String, String> createOrUpdateDnsEntries(Map<String, String> ipsByHostname, String environmentCrn) {
        LOGGER.info("Register DNS entries for {} for Hostnames: '{}'", logName(), String.join(",", ipsByHostname.keySet()));
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(environmentCrn);
        Map<String, String> finishedIpsByHostnames = new HashMap<>();
        try {
            for (Map.Entry<String, String> ipsByHostnameEntry : ipsByHostname.entrySet()) {
                String hostname = ipsByHostnameEntry.getKey();
                String ip = ipsByHostnameEntry.getValue();
                getDnsManagementService().createOrUpdateDnsEntryWithIp(accountId, hostname,
                        environment.getName(), false, List.of(ip));
                finishedIpsByHostnames.put(hostname, ip);
            }
            return ipsByHostname;
        } catch (PemDnsEntryCreateOrUpdateException e) {
            String message = String.format("Failed to create DNS entry: '%s'", e.getMessage());
            LOGGER.warn(message, e);
            rollBackDnsEntries(finishedIpsByHostnames, accountId, environment);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private void rollBackDnsEntries(Map<String, String> finishedIpsByHostnames, String accountId, DetailedEnvironmentResponse environment) {
        LOGGER.info("Rolling back, de-registering previously created or updated DNS entries for Hostnames: '{}'",
                String.join(",", finishedIpsByHostnames.keySet()));
        finishedIpsByHostnames.forEach((key, value) -> getDnsManagementService().deleteDnsEntryWithIp(accountId, key, environment.getName(),
                false, List.of(value)));
    }

    private Map<String, String> doDeregister(Map<String, String> ipsByHostname, String environmentCrn) {
        LOGGER.info("Deregister DNS entries for {} for Hostnames: '{}'", logName(), String.join(",", ipsByHostname.keySet()));
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(environmentCrn);

        return ipsByHostname
                .entrySet()
                .stream()
                .filter(entry -> getDnsManagementService().deleteDnsEntryWithIp(accountId,
                        entry.getKey(), environment.getName(), false, List.of(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected void setCertGenerationEnabled(boolean enabled) {
        super.setCertGenerationEnabled(enabled);
    }
}
