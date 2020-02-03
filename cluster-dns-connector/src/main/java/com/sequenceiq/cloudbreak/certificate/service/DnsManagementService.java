package com.sequenceiq.cloudbreak.certificate.service;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.GrpcClusterDnsClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class DnsManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsManagementService.class);

    @Inject
    private GrpcClusterDnsClient grpcClusterDnsClient;

    public boolean createDnsEntryWithIp(String actorCrn, String accountId, String endpoint, String environment, boolean wildcard, List<String> ips) {
        String ipsAsString = String.join(",", ips);
        try {
            LOGGER.info("Creating DNS entry with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString);
            Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
            grpcClusterDnsClient.createDnsEntryWithIp(actorCrn, accountId, endpoint, environment, wildcard, ips, requestIdOptional);
            LOGGER.info("DNS entry has been created with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString);
            return true;
        } catch (Exception e) {
            LOGGER.info("Failed to create DNS entry with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString);
        }
        return false;
    }

    public boolean deleteDnsEntryWithIp(String actorCrn, String accountId, String endpoint, String environment, boolean wildcard, List<String> ips) {
        String ipsAsString = String.join(",", ips);
        try {
            LOGGER.info("Deleting DNS entry with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString);
            Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
            grpcClusterDnsClient.deleteDnsEntryWithIp(actorCrn, accountId, endpoint, environment, wildcard, ips, requestIdOptional);
            LOGGER.info("DNS entry has been deleted with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString);
            return true;
        } catch (Exception e) {
            LOGGER.info("Failed to delete DNS entry with endpoint name: '{}', environment name: '{}' and IPs: '{}'", endpoint, environment, ipsAsString);
        }
        return false;
    }
}
