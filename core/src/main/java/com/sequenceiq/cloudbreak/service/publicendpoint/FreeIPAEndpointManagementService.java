package com.sequenceiq.cloudbreak.service.publicendpoint;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsARecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;

@Service
public class FreeIPAEndpointManagementService extends BasePublicEndpointManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPAEndpointManagementService.class);

    private static final String DOMAIN_PART_DELIMITER = ".";

    @Inject
    private DnsV1Endpoint dnsV1Endpoint;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public void registerLoadBalancerDomainWithFreeIPA(StackView stack) {
        Optional<LoadBalancer> loadBalancerOptional = getLoadBalancer(stack.getId());
        if (loadBalancerOptional.isPresent()) {
            LoadBalancer loadBalancer = loadBalancerOptional.get();
            try {
                if (StringUtils.isNotEmpty(loadBalancer.getDns())) {
                    sendAddDnsCnameRecordRequest(stack, loadBalancer);
                } else if (StringUtils.isNotEmpty(loadBalancer.getIp())) {
                    sendAddDnsARecordRequest(stack, loadBalancer);
                } else {
                    LOGGER.error("Unable to find DNS or IP for load balancer. Load balancer will not be registered with FreeIPA.");
                }
            } catch (Exception e) {
                LOGGER.error("Unable to register load balancer with FreeIPA", e);
            }
        } else {
            LOGGER.debug("No load balancer found that needs to be registered with FreeIPA");
        }
    }

    private void sendAddDnsCnameRecordRequest(StackView stack, LoadBalancer loadBalancer) {
        String targetFQDN = StringUtils.appendIfMissing(loadBalancer.getDns(), DOMAIN_PART_DELIMITER);
        String endpoint = loadBalancer.getEndpoint();

        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setCname(endpoint);
        request.setTargetFqdn(targetFQDN);
        request.setEnvironmentCrn(stack.getEnvironmentCrn());
        LOGGER.debug("Registering load balancer with target FQDN {} in FreeIPA with CNAME {}", targetFQDN, endpoint);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> dnsV1Endpoint.addDnsCnameRecordInternal(accountId, request));
    }

    private void sendAddDnsARecordRequest(StackView stack, LoadBalancer loadBalancer) {
        String ip = loadBalancer.getIp();
        String endpoint = loadBalancer.getEndpoint();

        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setHostname(endpoint);
        request.setIp(ip);
        request.setEnvironmentCrn(stack.getEnvironmentCrn());
        request.setCreateReverse(true);
        LOGGER.debug("Registering load balancer with target IP {} in FreeIPA with A record {}", ip, endpoint);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> dnsV1Endpoint.addDnsARecordInternal(accountId, request));
    }

    public void deleteLoadBalancerDomainFromFreeIPA(StackDtoDelegate stack) {
        Optional<LoadBalancer> loadBalancerOptional = getLoadBalancer(stack.getId());
        if (loadBalancerOptional.isPresent()) {
            try {
                LoadBalancer loadBalancer = loadBalancerOptional.get();
                if (StringUtils.isNotEmpty(loadBalancer.getDns())) {
                    sendDeleteDnsCnameRecordRequest(stack, loadBalancer);
                } else if (StringUtils.isNotEmpty(loadBalancer.getIp())) {
                    sendDeleteDnsARecordRequest(stack, loadBalancer);
                } else {
                    LOGGER.debug("Unable to find DNS or IP for load balancer. Load balancer will not be deleted from FreeIPA.");
                }
            } catch (Exception e) {
                LOGGER.error("Unable to delete load balancer domain from FreeIPA", e);
            }
        } else {
            LOGGER.debug("No load balancer found that needs to be deleted from FreeIPA");
        }
    }

    private void sendDeleteDnsCnameRecordRequest(StackDtoDelegate stack, LoadBalancer loadBalancer) {
        String endpoint = loadBalancer.getEndpoint();
        LOGGER.debug("Deleting load balancer with CNAME  {} from FreeIPA", endpoint);
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> dnsV1Endpoint.deleteDnsCnameRecord(stack.getEnvironmentCrn(), null, endpoint));
    }

    private void sendDeleteDnsARecordRequest(StackDtoDelegate stack, LoadBalancer loadBalancer) {
        String endpoint = loadBalancer.getEndpoint();
        LOGGER.debug("Deleting load balancer with A record {} from FreeIPA", endpoint);
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> dnsV1Endpoint.deleteDnsARecord(stack.getEnvironmentCrn(), null, endpoint));
    }

    private Optional<LoadBalancer> getLoadBalancer(Long stackId) {
        Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(stackId);
        if (!loadBalancers.isEmpty()) {
            return loadBalancerConfigService.selectLoadBalancerForFrontend(loadBalancers, LoadBalancerType.PRIVATE);
        }
        return Optional.empty();
    }
}
