package com.sequenceiq.freeipa.service.freeipa.dns;

import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreEmptyModOrDuplicateException;
import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundException;
import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue;

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.NetworkService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class DnsZoneService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsZoneService.class);

    private static final String FORWARD_POLICY = "only";

    private static final String IPV4_REVERSE_LOOKUP_DOMAIN = "in-addr.arpa.";

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackService stackService;

    @Inject
    private ReverseDnsZoneCalculator reverseDnsZoneCalculator;

    @Inject
    private HybridReverseDnsZoneCalculator hybridReverseDnsZoneCalculator;

    @Inject
    private CachedEnvironmentClientService environmentClient;

    @Inject
    private NetworkService networkService;

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public AddDnsZoneForSubnetsResponse addDnsZonesForSubnets(AddDnsZoneForSubnetsRequest request, String accountId) throws FreeIpaClientException {
        FreeIpaClient client = getFreeIpaClient(request.getEnvironmentCrn(), accountId);
        AddDnsZoneForSubnetsResponse response = new AddDnsZoneForSubnetsResponse();
        if (EnvironmentType.isHybridFromEnvironmentTypeString(environmentClient.getByCrn(request.getEnvironmentCrn()).getEnvironmentType())) {
            addHybridDnsZonesForSubnets(client, request.getSubnets());
        } else {
            addDnsZonesForSubnets(request, client, response);
        }
        return response;
    }

    private void addDnsZonesForSubnets(AddDnsZoneForSubnetsRequest request, FreeIpaClient client, AddDnsZoneForSubnetsResponse response)
            throws RetryableFreeIpaClientException {
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
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
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

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void deleteDnsZoneBySubnet(String environmentCrn, String accountId, String subnet) throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = getFreeIpaClient(environmentCrn, accountId);
        String reverseDnsZone = reverseDnsZoneCalculator.reverseDnsZoneForCidr(subnet);
        LOGGER.info("Delete DNS reverse zone [{}], for subnet [{}]", reverseDnsZone, subnet);
        ignoreNotFoundException(() -> freeIpaClient.deleteDnsZone(reverseDnsZone), "DNS zone was not present on FreeIPA: {}", reverseDnsZone);
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public AddDnsZoneForSubnetsResponse addDnsZonesForSubnetIds(AddDnsZoneForSubnetIdsRequest request, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        Multimap<String, String> subnetWithCidr = networkService.getFilteredSubnetWithCidr(request.getEnvironmentCrn(), stack,
                request.getAddDnsZoneNetwork().getNetworkId(), request.getAddDnsZoneNetwork().getSubnetIds());
        FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        AddDnsZoneForSubnetsResponse response = new AddDnsZoneForSubnetsResponse();
        if (EnvironmentType.isHybridFromEnvironmentTypeString(environmentClient.getByCrn(stack.getEnvironmentCrn()).getEnvironmentType())) {
            addHybridDnsZonesForSubnets(client, subnetWithCidr.values());
        } else {
            for (Entry<String, String> subnet : subnetWithCidr.entries()) {
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
                    LOGGER.warn("Can't add subnet's [{}] reverse DNS zone with cidr [{}]", subnet, subnet.getValue(), e);
                    response.getFailed().putIfAbsent(subnet.getKey(), e.getMessage());
                }
            }
        }
        return response;
    }

    public void deleteDnsZoneBySubnetId(String environmentCrn, String accountId, String networkId, String subnetId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        Multimap<String, String> subnetWithCidr =
                networkService.getFilteredSubnetWithCidr(environmentCrn, stack, networkId, Collections.singletonList(subnetId));
        for (String cidr : subnetWithCidr.values()) {
            deleteDnsZoneBySubnet(environmentCrn, accountId, cidr);
        }
    }

    private void addHybridDnsZonesForSubnets(FreeIpaClient client, Collection<String> subnetCidrs) throws FreeIpaClientException {
        LOGGER.debug("Calculating hybrid dns zones for subnets: {}", subnetCidrs);
        Set<String> cidrs = Set.copyOf(subnetCidrs);
        Set<String> reverseDnsZoneForCidrs = hybridReverseDnsZoneCalculator.reverseDnsZoneForCidrsAsSet(cidrs);
        Set<DnsZone> allDnsZone = client.findAllDnsZone();
        Set<String> reverseZonesFromFreeIpa = allDnsZone.stream()
                .map(DnsZone::getIdnsname)
                .filter(StringUtils::isNotBlank)
                .filter(zone -> zone.endsWith(ReverseDnsZoneCalculator.IN_ADDR_ARPA))
                .collect(Collectors.toSet());
        reverseDnsZoneForCidrs.removeAll(reverseZonesFromFreeIpa);
        LOGGER.info("Adding reverse zones: {}", reverseDnsZoneForCidrs);
        for (String reverseZone : reverseDnsZoneForCidrs) {
            FreeIpaClientExceptionUtil.ignoreEmptyModOrDuplicateException(() -> client.addDnsZone(reverseZone), null);
        }
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void addDnsForwardZone(FreeIpaClientFactory ipaClientFactory, Stack stack, CrossRealmTrust crossRealmTrust) throws Exception {
        FreeIpaClient freeIpaClient = ipaClientFactory.getFreeIpaClientForStackIgnoreUnreachable(stack);

        String realm = crossRealmTrust.getKdcRealm();
        LOGGER.info("Add forward DNS zone [{}]", realm);
        addOrModifyDnsForwardZone(freeIpaClient, realm, crossRealmTrust.getDnsIp());
        LOGGER.info("Add forward DNS zone [{}]", IPV4_REVERSE_LOOKUP_DOMAIN);
        addOrModifyDnsForwardZone(freeIpaClient, IPV4_REVERSE_LOOKUP_DOMAIN, crossRealmTrust.getDnsIp());
    }

    private void addOrModifyDnsForwardZone(FreeIpaClient freeIpaClient, String forwardZone, String dnsIp) throws FreeIpaClientException {
        Optional<DnsZone> dnsZone = ignoreNotFoundExceptionWithValue(() -> freeIpaClient.showForwardDnsZone(forwardZone), null);
        if (dnsZone.isEmpty()) {
            LOGGER.debug("Forward DNS zone does not exists [{}], add it now", forwardZone);
            ignoreEmptyModOrDuplicateException(() -> freeIpaClient.addForwardDnsZone(forwardZone, dnsIp, FORWARD_POLICY), null);
            LOGGER.debug("Forward DNS zone [{}] added", forwardZone);
        } else {
            LOGGER.debug("Forward DNS zone [{}] already exists, modify it now", forwardZone);
            ignoreEmptyModOrDuplicateException(() -> freeIpaClient.modForwardDnsZone(forwardZone, dnsIp, FORWARD_POLICY), null);
            LOGGER.debug("Forward DNS zone [{}] modified", forwardZone);
        }
    }
}
