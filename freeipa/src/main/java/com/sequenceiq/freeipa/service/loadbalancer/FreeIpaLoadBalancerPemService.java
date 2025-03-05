package com.sequenceiq.freeipa.service.loadbalancer;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.freeipa.entity.LoadBalancer;

@Service
public class FreeIpaLoadBalancerPemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerPemService.class);

    @Inject
    private DnsManagementService dnsManagementService;

    public void createOrUpdateDnsEntry(LoadBalancer loadBalancer, String environmentName, String accountId)
            throws PemDnsEntryCreateOrUpdateException {
        String endpoint = loadBalancer.getEndpoint();
        List<String> loadBalancerIps = getLbIpListOrThrow(loadBalancer, environmentName);
        LOGGER.info("Creating FreeIpa load balancer DNS entry with endpoint name: '{}', environment name: '{}' and IP: '{}'",
                endpoint, environmentName, loadBalancer.getIp());
        dnsManagementService.createOrUpdateDnsEntryWithIp(accountId, endpoint,
                environmentName, false, loadBalancerIps);
    }

    private ArrayList<String> getLbIpListOrThrow(LoadBalancer loadBalancer, String environmentName) throws PemDnsEntryCreateOrUpdateException {
        if (loadBalancer.getIp() != null && !loadBalancer.getIp().isEmpty()) {
            return new ArrayList<>(loadBalancer.getIp());
        } else {
            String message = String.format("Could not find IP info for load balancer with endpoint '%s'" +
                    " and environment name: '%s'. DNS registration could not be executed.", loadBalancer.getEndpoint(), environmentName);
            LOGGER.warn(message);
            throw new PemDnsEntryCreateOrUpdateException(message);
        }
    }

    public void deleteDnsEntry(LoadBalancer loadBalancer, String environmentName) throws PemDnsEntryCreateOrUpdateException {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        String endpoint = loadBalancer.getEndpoint();
        List<String> loadBalancerIp = getLbIpListOrThrow(loadBalancer, environmentName);
        LOGGER.info("Deleting load balancer DNS entry with endpoint name: '{}', environment name: '{}' and IP: '{}'",
                endpoint, environmentName, loadBalancerIp);
        dnsManagementService.deleteDnsEntryWithIp(accountId, endpoint, environmentName, false, loadBalancerIp);
    }
}
