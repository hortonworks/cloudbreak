package com.sequenceiq.cloudbreak.service.publicendpoint.dns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.publicendpoint.BasePublicEndpointManagementService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public abstract class BaseDnsEntryService extends BasePublicEndpointManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDnsEntryService.class);

    @Inject
    private EnvironmentClientService environmentClientService;

    protected abstract Map<String, List<String>> getComponentLocation(Stack stack);

    protected abstract String logName();

    public Map<String, String> createOrUpdate(Stack stack) {
        return doActionOnStack(stack, null, this::createOrUpdateDnsEntries);
    }

    public Map<String, String> deregister(Stack stack) {
        return doActionOnStack(stack, null, this::doDeregister);
    }

    public Map<String, String> createOrUpdateCandidates(Stack stack, Map<String, String> candidateAddressesByFqdn) {
        return doActionOnStack(stack, candidateAddressesByFqdn, this::createOrUpdateDnsEntries);
    }

    public Map<String, String> deregister(Stack stack, Map<String, String> candidateAddressesByFqdn) {
        return doActionOnStack(stack, candidateAddressesByFqdn, this::doDeregister);
    }

    private Map<String, String> doActionOnStack(
            Stack stack, Map<String,
            String> candidateAddressesByFqdn,
            BiFunction<Map<String, String>, String, Map<String, String>> action) {

        Map<String, String> result = new HashMap<>();
        if (stack.getCluster() != null && manageCertificateAndDnsInPem()) {
            LOGGER.info("Modifying DNS entries for {} on stack '{}'", logName(), stack.getName());
            Map<String, String> ipsByFqdn = getCandidateIpsByFqdn(stack);
            if (!CollectionUtils.isEmpty(candidateAddressesByFqdn)) {
                LOGGER.info("Modifying DNS entries for {} on stack '{}', whitelist of candidates for the update: '{}'", logName(), stack.getName(),
                        String.join(",", candidateAddressesByFqdn.keySet()));
                ipsByFqdn = ipsByFqdn.entrySet().stream()
                        .filter(entry -> candidateAddressesByFqdn.containsKey(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            result.putAll(action.apply(ipsByFqdn, stack.getEnvironmentCrn()));
        }
        return result;
    }

    private Map<String, String> getCandidateIpsByFqdn(Stack stack) {
        Map<String, List<String>> componentLocation = getComponentLocation(stack);
        final Set<InstanceMetaData> runningInstanceMetaData = stack.getNotDeletedInstanceMetaDataSet();
        final InstanceMetaData primaryGatewayInstanceMetadata = stack.getPrimaryGatewayInstance();
        return componentLocation
                .values()
                .stream()
                .flatMap(fqdns -> runningInstanceMetaData.stream().filter(im -> fqdns.contains(im.getDiscoveryFQDN())))
                .filter(im -> primaryGatewayInstanceMetadata != null && !primaryGatewayInstanceMetadata.getPrivateId().equals(im.getPrivateId()))
                .collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, InstanceMetaData::getPublicIpWrapper));
    }

    private Map<String, String> createOrUpdateDnsEntries(Map<String, String> ipsByFqdn, String environmentCrn) {
        LOGGER.info("Register DNS entries for {} for FQDNs: '{}'", logName(), String.join(",", ipsByFqdn.keySet()));
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(environmentCrn);

        return ipsByFqdn
                .entrySet()
                .stream()
                .filter(entry -> getDnsManagementService().createOrUpdateDnsEntryWithIp(userCrn, accountId,
                        getShortHostname(entry.getKey()), environment.getName(), false, List.of(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, String> doDeregister(Map<String, String> ipsByFqdn, String environmentCrn) {
        LOGGER.info("Deregister DNS entries for {} for FQDNs: '{}'", logName(), String.join(",", ipsByFqdn.keySet()));
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(environmentCrn);

        // TODO do we need to deregister the load balancer DNS here as well?
        return ipsByFqdn
                .entrySet()
                .stream()
                .filter(entry -> getDnsManagementService().deleteDnsEntryWithIp(userCrn, accountId,
                        getShortHostname(entry.getKey()), environment.getName(), false, List.of(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String getShortHostname(String fqdn) {
        if (StringUtils.isEmpty(fqdn)) {
            return null;
        }
        return fqdn.split("\\.")[0];
    }

    protected void setCertGenerationEnabled(boolean enabled) {
        super.setCertGenerationEnabled(enabled);
    }
}
