package com.sequenceiq.freeipa.service.freeipa.dns;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.NetworkService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class DnsZoneService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsZoneService.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackService stackService;

    @Inject
    private ReverseDnsZoneCalculator reverseDnsZoneCalculator;

    @Inject
    private NetworkService networkService;

    public AddDnsZoneForSubnetsResponse addDnsZonesForSubnets(AddDnsZoneForSubnetsRequest request, String accountId) throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(request.getEnvironmentCrn(), accountId);
        AddDnsZoneForSubnetsResponse response = new AddDnsZoneForSubnetsResponse();
        for (String subnet : request.getSubnets()) {
            try {
                LOGGER.info("Add subnet's [{}] reverse DNS zone", subnet);
                client.addReverseDnsZone(subnet);
                response.getSuccess().add(subnet);
                LOGGER.debug("Subnet [{}] added", subnet);
            } catch (RetryableFreeIpaClientException e) {
                throw e;
            } catch (FreeIpaClientException e) {
                LOGGER.warn("Can't add subnet's [{}] reverse DNS zone", subnet, e);
                response.getFailed().put(subnet, e.getMessage());
            }
        }
        return response;
    }

    public Set<String> listDnsZones(String environmentCrn, String accountId) throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = getFreeIpaClient(environmentCrn, accountId);
        Set<DnsZone> allDnsZone = freeIpaClient.findAllDnsZone();
        return allDnsZone.stream().map(DnsZone::getIdnsname).collect(Collectors.toSet());

    }

    private FreeIpaClient getFreeIpaClient(String environmentCrn, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return freeIpaClientFactory.getFreeIpaClientForStack(stack);
    }

    public void deleteDnsZoneBySubnet(String environmentCrn, String accountId, String subnet) throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = getFreeIpaClient(environmentCrn, accountId);
        String reverseDnsZone = reverseDnsZoneCalculator.reverseDnsZoneForCidr(subnet);
        LOGGER.info("Delete DNS reverse zone [{}], for subnet [{}]", reverseDnsZone, subnet);
        try {
            freeIpaClient.deleteDnsZone(reverseDnsZone);
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                LOGGER.info("DNS zone was not present on FreeIPA: {}", reverseDnsZone);
            } else {
                throw e;
            }
        }
    }

    public AddDnsZoneForSubnetsResponse addDnsZonesForSubnetIds(AddDnsZoneForSubnetIdsRequest request, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        Map<String, String> subnetWithCidr = networkService.getFilteredSubnetWithCidr(request.getEnvironmentCrn(), stack,
                request.getAddDnsZoneNetwork().getNetworkId(), request.getAddDnsZoneNetwork().getSubnetIds());
        FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        AddDnsZoneForSubnetsResponse response = new AddDnsZoneForSubnetsResponse();
        for (Entry<String, String> subnet : subnetWithCidr.entrySet()) {
            try {
                LOGGER.info("Add subnet's [{}] reverse DNS zone", subnet);
                String subnetCidr = subnet.getValue();
                Set<DnsZone> dnsZones = client.findDnsZone(subnetCidr);
                if (dnsZones.isEmpty()) {
                    LOGGER.debug("Subnet reverse DNS zone does not exists [{}], add it now", subnet);
                    client.addReverseDnsZone(subnetCidr);
                    response.getSuccess().add(subnet.getKey());
                    LOGGER.debug("Subnet [{}] added", subnet);
                }
            } catch (RetryableFreeIpaClientException e) {
                throw e;
            } catch (FreeIpaClientException e) {
                LOGGER.warn("Can't add subnet's [{}] reverse DNS zone", subnet, e);
                response.getFailed().put(subnet.getKey(), e.getMessage());
            }
        }
        return response;
    }

    public void deleteDnsZoneBySubnetId(String environmentCrn, String accountId, String networkId, String subnetId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        Map<String, String> subnetWithCidr = networkService.getFilteredSubnetWithCidr(environmentCrn, stack, networkId, Collections.singletonList(subnetId));
        for (String cidr : subnetWithCidr.values()) {
            deleteDnsZoneBySubnet(environmentCrn, accountId, cidr);
        }
    }
}
