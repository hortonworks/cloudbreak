package com.sequenceiq.cloudbreak.service.publicendpoint;

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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class KafkaBrokerPublicDnsEntryService extends BasePublicEndpointManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaBrokerPublicDnsEntryService.class);

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private ComponentLocatorService componentLocatorService;

    public Map<String, String> register(Stack stack) {
        return doActionOnStack(stack, null, this::doRegister);
    }

    public Map<String, String> deregister(Stack stack) {
        return doActionOnStack(stack, null, this::doDeregister);
    }

    public Map<String, String> register(Stack stack, Map<String, String> candidateAddressesByFqdn) {
        return doActionOnStack(stack, candidateAddressesByFqdn, this::doRegister);
    }

    public Map<String, String> deregister(Stack stack, Map<String, String> candidateAddressesByFqdn) {
        return doActionOnStack(stack, candidateAddressesByFqdn, this::doDeregister);
    }

    private Map<String, String> doActionOnStack(
            Stack stack, Map<String,
            String> candidateAddressesByFqdn,
            BiFunction<Map<String, String>, String, Map<String, String>> action) {

        Map<String, String> result = new HashMap<>();
        //TODO include indicator flag in the condition
        if (stack.getCluster() != null && isCertGenerationEnabled()) {
            LOGGER.info("Modifying DNS entries for Kafka Brokers on stack '{}'", stack.getName());
            Cluster cluster = stack.getCluster();
            Map<String, String> ipsByFqdn = getBrokerIpsByFqdn(stack, cluster);
            if (!CollectionUtils.isEmpty(candidateAddressesByFqdn)) {
                LOGGER.info("Modifying DNS entries for Kafka Brokers on stack '{}', whitelist of candidates for the update: '{}'", stack.getName(),
                        String.join(",", candidateAddressesByFqdn.keySet()));
                ipsByFqdn = ipsByFqdn.entrySet().stream()
                        .filter(entry -> candidateAddressesByFqdn.containsKey(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            result.putAll(action.apply(ipsByFqdn, stack.getEnvironmentCrn()));
        }
        return result;
    }

    private Map<String, String> getBrokerIpsByFqdn(Stack stack, Cluster cluster) {
        Map<String, List<String>> componentLocation = componentLocatorService.getComponentLocation(cluster, List.of(KafkaRoles.KAFKA_BROKER));
        final Set<InstanceMetaData> runningInstanceMetaData = stack.getNotDeletedInstanceMetaDataSet();
        final InstanceMetaData primaryGatewayInstanceMetadata = stack.getPrimaryGatewayInstance();
        return componentLocation
                .values()
                .stream()
                .flatMap(fqdns -> runningInstanceMetaData.stream().filter(im -> fqdns.contains(im.getDiscoveryFQDN())))
                .filter(im -> primaryGatewayInstanceMetadata != null && !primaryGatewayInstanceMetadata.getPrivateId().equals(im.getPrivateId()))
                .collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, InstanceMetaData::getPublicIpWrapper));
    }

    private Map<String, String> doRegister(Map<String, String> ipsByFqdn, String environmentCrn) {
        LOGGER.info("Register DNS entries for Kafka brokers for FQDNs: '{}'", String.join(",", ipsByFqdn.keySet()));
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        String accountId = threadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(environmentCrn);

        return ipsByFqdn
                .entrySet()
                .stream()
                .filter(entry -> getDnsManagementService().createDnsEntryWithIp(userCrn, accountId,
                        getShortHostname(entry.getKey()), environment.getName(), false, List.of(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, String> doDeregister(Map<String, String> ipsByFqdn, String environmentCrn) {
        LOGGER.info("Deregister DNS entries for Kafka brokers for FQDNs: '{}'", String.join(",", ipsByFqdn.keySet()));
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        String accountId = threadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(environmentCrn);

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
}
