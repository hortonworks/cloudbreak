package com.sequenceiq.freeipa.service.loadbalancer;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.FreeIpaClientRunnable;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaLoadBalancerDomainService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerDomainService.class);

    @Inject
    private FreeIpaLoadBalancerService loadBalancerService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private StackService stackService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private FreeIpaLoadBalancerPemService freeIpaLoadBalancerPemService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    public void registerLbDomain(Long stackId, FreeIpaClient freeIpaClient) throws FreeIpaClientException, PemDnsEntryCreateOrUpdateException {
        Optional<LoadBalancer> loadBalancer = loadBalancerService.findByStackId(stackId);
        if (loadBalancer.isPresent()) {
            LoadBalancer lb = loadBalancer.get();
            FreeIpa freeIpa = freeIpaService.findByStackId(stackId);
            for (String ip : lb.getIp()) {
                String msg = "LB A record with endpoint [{}], IP [{}], domain [{}] already exists, nothing to do";
                FreeIpaClientRunnable ipaClientRunnable = () -> freeIpaClient.addDnsARecord(freeIpa.getDomain(), lb.getEndpoint(), ip, true);
                FreeIpaClientExceptionUtil.ignoreEmptyModOrDuplicateException(ipaClientRunnable, msg, lb.getEndpoint(), lb.getIp(), freeIpa.getDomain());
            }
            Stack stack = freeIpa.getStack();
            if (manageLbDomainInPem(stack.getCloudPlatform())) {
                performLoadBalancerDNSUpdateOnPEM(lb, stack.getEnvironmentCrn(), stack.getAccountId());
            }
        }
    }

    @Retryable(retryFor = FreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void registerLbDomain(Long stackId) throws FreeIpaClientException, PemDnsEntryCreateOrUpdateException {
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackId(stackId);
        registerLbDomain(stackId, freeIpaClient);
    }

    public void deregisterLbDomain(Long stackId) throws PemDnsEntryCreateOrUpdateException {
        Stack stack = stackService.getStackById(stackId);
        Optional<LoadBalancer> loadBalancer = loadBalancerService.findByStackId(stackId);
        if (manageLbDomainInPem(stack.getCloudPlatform()) && loadBalancer.isPresent()) {
            String environmentCrn = stack.getEnvironmentCrn();
            DetailedEnvironmentResponse environmentResponse = environmentEndpoint.getByCrn(environmentCrn);
            String environmentName = environmentResponse.getName();
            freeIpaLoadBalancerPemService.deleteDnsEntry(loadBalancer.get(), environmentName);
        } else {
            LOGGER.info("Load balancer domain was not managed in PEM for this platform, or load balancer does not exist");
        }
    }

    private void performLoadBalancerDNSUpdateOnPEM(LoadBalancer loadBalancer, String environmentCrn, String accountId)
            throws PemDnsEntryCreateOrUpdateException {
        DetailedEnvironmentResponse environmentResponse = environmentEndpoint.getByCrn(environmentCrn);
        String environmentName = environmentResponse.getName();

        try {
            freeIpaLoadBalancerPemService.createOrUpdateDnsEntry(loadBalancer, environmentName, accountId);
        } catch (PemDnsEntryCreateOrUpdateException e) {
            String message = String.format("Failed to register FreeIPA load balancer endpoint in Public Endpoint Management service. " +
                    "Endpoint: %s, environment name: %s", loadBalancer.getEndpoint(), environmentName);
            LOGGER.warn(message, e);
            throw new PemDnsEntryCreateOrUpdateException(message, e);
        }
    }

    private boolean manageLbDomainInPem(String cloudPlatform) {
        if (!CloudPlatform.MOCK.name().equalsIgnoreCase(cloudPlatform)) {
            return true;
        } else {
            LOGGER.info("Load balancer domain is not managed in PEM for this platform.");
            return false;
        }
    }
}
