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

    public void createDnsEntryWithIp(String actorCrn, String accountId, String endpoint, String environment, boolean wildcard, List<String> ips) {
        try {
            Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
            grpcClusterDnsClient.createDnsEntryWithIp(actorCrn, accountId, endpoint, environment, wildcard, ips, requestIdOptional);
            LOGGER.info("Dns entry is created with ips: {}", ips);
        } catch (Exception e) {
            LOGGER.info("Failed to create the dns entry with ips: {}", ips, e);
        }
    }

    public void deleteDnsEntryWithIp(String actorCrn, String accountId, String endpoint, String environment, boolean wildcard, List<String> ips) {
        try {
            Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
            grpcClusterDnsClient.deleteDnsEntryWithIp(actorCrn, accountId, endpoint, environment, wildcard, ips, requestIdOptional);
            LOGGER.info("Dns entry is deleted with ips: {}", ips);
        } catch (Exception e) {
            LOGGER.info("Failed to delete the dns entry with ips: {}", ips, e);
        }
    }
}
