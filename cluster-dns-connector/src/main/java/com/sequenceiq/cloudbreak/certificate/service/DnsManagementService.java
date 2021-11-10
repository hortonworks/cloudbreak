package com.sequenceiq.cloudbreak.certificate.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse;
import com.sequenceiq.cloudbreak.client.GrpcClusterDnsClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class DnsManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsManagementService.class);

    private static final String DOMAIN_NAME_PART_DELIMITER = ".";

    @Inject
    private GrpcClusterDnsClient grpcClusterDnsClient;

    public boolean createOrUpdateDnsEntryWithIp(String accountId, String endpoint, String environment, boolean wildcard, List<String> ips) {
        String ipsAsString = String.join(",", ips);
        try {
            LOGGER.info("Creating DNS entry with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString);
            Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getOrGenerateRequestId());
            grpcClusterDnsClient.createOrUpdateDnsEntryWithIp(accountId, endpoint, environment, wildcard, ips, requestIdOptional);
            LOGGER.info("DNS entry has been created with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString);
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to create DNS entry with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString, e);
        }
        return false;
    }

    public boolean deleteDnsEntryWithIp(String accountId, String endpoint, String environment, boolean wildcard, List<String> ips) {
        String ipsAsString = String.join(",", ips);
        try {
            LOGGER.info("Deleting DNS entry with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString);
            Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getOrGenerateRequestId());
            grpcClusterDnsClient.deleteDnsEntryWithIp(accountId, endpoint, environment, wildcard, ips, requestIdOptional);
            LOGGER.info("DNS entry has been deleted with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString);
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to delete DNS entry with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString, e);
        }
        return false;
    }

    public boolean createOrUpdateDnsEntryWithCloudDns(String accountId, String endpoint, String environment, String cloudDns,
            String hostedZoneId) {
        try {
            LOGGER.info("Creating DNS entry with endpoint name: '{}', environment name: '{}' and cloud DNS: '{}'", endpoint, environment, cloudDns);
            Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getOrGenerateRequestId());
            grpcClusterDnsClient.createOrUpdateDnsEntryWithCloudDns(accountId, endpoint, environment, cloudDns, hostedZoneId, requestIdOptional);
            LOGGER.info("DNS entry has been created with endpoint name: '{}', environment name: '{}' and cloud DNS: '{}'", endpoint, environment, cloudDns);
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to create DNS entry with endpoint name: '{}', environment name: '{}' and cloud DNS: '{}'", endpoint, environment, cloudDns, e);
        }
        return false;
    }

    public boolean deleteDnsEntryWithCloudDns(String accountId, String endpoint, String environment, String cloudDns, String hostedZoneId) {
        try {
            LOGGER.info("Deleting DNS entry with endpoint name: '{}', environment name: '{}' and cloud DNS: '{}'", endpoint, environment, cloudDns);
            Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getOrGenerateRequestId());
            grpcClusterDnsClient.deleteDnsEntryWithCloudDns(accountId, endpoint, environment, cloudDns, hostedZoneId, requestIdOptional);
            LOGGER.info("DNS entry has been deleted with endpoint name: '{}', environment name: '{}' and cloud DNS: '{}'", endpoint, environment, cloudDns);
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to delete DNS entry with endpoint name: '{}', environment name: '{}' and cloud DNS: '{}'", endpoint, environment, cloudDns, e);
        }
        return false;
    }

    public String generateManagedDomain(String accountId, String environmentName) {
        String wildCardSubDomain = "*";
        List<String> subDomains = List.of(wildCardSubDomain);
        LOGGER.debug("Generating managed domain names for environment: '{}', subdomain: '{}'", environmentName, String.join(",", subDomains));
        Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getOrGenerateRequestId());
        GenerateManagedDomainNamesResponse response = grpcClusterDnsClient.generateManagedDomain(environmentName, subDomains, accountId, requestIdOptional);
        Map<String, String> domainsMap = Optional.ofNullable(response)
                .map(GenerateManagedDomainNamesResponse::getDomainsMap)
                .orElse(new HashMap<>());
        LOGGER.info("Domain names has been generated: '{}'", domainsMap);
        String environmentDomain = domainsMap.getOrDefault(wildCardSubDomain, null);
        if (StringUtils.isNotEmpty(environmentDomain) && environmentDomain.startsWith(wildCardSubDomain)) {
            environmentDomain = environmentDomain.replace(wildCardSubDomain + DOMAIN_NAME_PART_DELIMITER, "");
        }
        return environmentDomain;
    }
}
