package com.sequenceiq.freeipa.service.freeipa.dns;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.freeipa.api.v1.freeipa.dns.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.dns.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.dns.AddDnsZoneForSubnetsResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsZoneList;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
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
    private CloudParameterService cloudParameterService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private CredentialService credentialService;

    public AddDnsZoneForSubnetsResponse addDnsZonesForSubnets(AddDnsZoneForSubnetsRequest request, String accountId) throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(request.getEnvironmentCrn(), accountId);
        AddDnsZoneForSubnetsResponse response = new AddDnsZoneForSubnetsResponse();
        for (String subnet : request.getSubnets()) {
            try {
                LOGGER.info("Add subnet's [{}] reverse DNS zone", subnet);
                client.addReverseDnsZone(subnet);
                response.getSuccess().add(subnet);
                LOGGER.debug("Subnet [{}] added", subnet);
            } catch (FreeIpaClientException e) {
                LOGGER.warn("Can't add subnet's [{}] reverse DNS zone", subnet, e);
                response.getFailed().put(subnet, e.getMessage());
            }
        }
        return response;
    }

    public Set<String> listDnsZones(String environmentCrn, String accountId) throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = getFreeIpaClient(environmentCrn, accountId);
        Set<DnsZoneList> allDnsZone = freeIpaClient.findAllDnsZone();
        return allDnsZone.stream().map(DnsZoneList::getIdnsname).collect(Collectors.toSet());

    }

    private FreeIpaClient getFreeIpaClient(String environmentCrn, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        return freeIpaClientFactory.getFreeIpaClientForStack(stack);
    }

    public void deleteDnsZoneBySubnet(String environmentCrn, String accountId, String subnet) throws FreeIpaClientException {
        String reverseDnsZone = reverseDnsZoneCalculator.reverseDnsZoneForCidr(subnet);
        LOGGER.info("Delete DNS reverse zone [{}], for subnet [{}]", reverseDnsZone, subnet);
        FreeIpaClient freeIpaClient = getFreeIpaClient(environmentCrn, accountId);
        freeIpaClient.deleteDnsZone(reverseDnsZone);
    }

    public AddDnsZoneForSubnetsResponse addDnsZonesForSubnetIds(AddDnsZoneForSubnetIdsRequest request, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(request.getEnvironmentCrn(), accountId);
        Map<String, String> subnetWithCidr = getFilteredSubnetWithCidr(request.getEnvironmentCrn(), stack, request.getAddDnsZoneNetwork().getNetworkId(),
                request.getAddDnsZoneNetwork().getSubnetIds());
        FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        AddDnsZoneForSubnetsResponse response = new AddDnsZoneForSubnetsResponse();
        for (Entry<String, String> subnet : subnetWithCidr.entrySet()) {
            try {
                LOGGER.info("Add subnet's [{}] reverse DNS zone", subnet);
                String subnetCidr = subnet.getValue();
                Set<DnsZoneList> dnsZone = client.findDnsZone(subnetCidr);
                if (dnsZone.isEmpty()) {
                    LOGGER.debug("Subnet reverse DNS zone does not exists [{}], add it now", subnet);
                    client.addReverseDnsZone(subnetCidr);
                    response.getSuccess().add(subnet.getKey());
                    LOGGER.debug("Subnet [{}] added", subnet);
                }
            } catch (FreeIpaClientException e) {
                LOGGER.warn("Can't add subnet's [{}] reverse DNS zone", subnet, e);
                response.getFailed().put(subnet.getKey(), e.getMessage());
            }
        }
        return response;
    }

    private Map<String, String> getFilteredSubnetWithCidr(String environmentCrn, Stack stack, String networkId, Collection<String> subnetIds) {
        Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
        ExtendedCloudCredential cloudCredential = extendedCloudCredentialConverter.convert(credential);
        CloudNetworks cloudNetworks =
                cloudParameterService.getCloudNetworks(cloudCredential, stack.getRegion(), stack.getPlatformvariant(), Collections.emptyMap());
        LOGGER.debug("Received Cloud networks for region [{}]: {}", stack.getRegion(), cloudNetworks.getCloudNetworkResponses().get(stack.getRegion()));
        return cloudNetworks.getCloudNetworkResponses().getOrDefault(stack.getRegion(), Collections.emptySet()).stream()
                .filter(cloudNetwork -> {
                    // support for azure
                    String[] splittedNetworkId = cloudNetwork.getId().split("/");
                    String cloudNetworkId = splittedNetworkId[splittedNetworkId.length - 1];
                    return networkId.equals(cloudNetworkId);
                })
                .flatMap(cloudNetwork -> cloudNetwork.getSubnetsMeta().stream())
                .filter(cloudSubnet -> StringUtils.isNoneBlank(cloudSubnet.getId(), cloudSubnet.getCidr()))
                .filter(cloudSubnet -> subnetIds.contains(cloudSubnet.getId()))
                .collect(Collectors.toMap(CloudSubnet::getId, CloudSubnet::getCidr));
    }

    public void deleteDnsZoneBySubnetId(String environmentCrn, String accountId, String networkId, String subnetId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        Map<String, String> subnetWithCidr = getFilteredSubnetWithCidr(environmentCrn, stack, networkId, Collections.singletonList(subnetId));
        for (String cidr : subnetWithCidr.values()) {
            deleteDnsZoneBySubnet(environmentCrn, accountId, cidr);
        }
    }
}
