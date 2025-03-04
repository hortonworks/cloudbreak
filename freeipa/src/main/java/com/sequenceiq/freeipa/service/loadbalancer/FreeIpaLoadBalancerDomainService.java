package com.sequenceiq.freeipa.service.loadbalancer;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

@Service
public class FreeIpaLoadBalancerDomainService {

    @Inject
    private FreeIpaLoadBalancerService loadBalancerService;

    @Inject
    private FreeIpaService freeIpaService;

    public void registerLbDomain(Long stackId, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        Optional<LoadBalancer> loadBalancer = loadBalancerService.findByStackId(stackId);
        if (loadBalancer.isPresent()) {
            LoadBalancer lb = loadBalancer.get();
            FreeIpa freeIpa = freeIpaService.findByStackId(stackId);
            for (String ip : lb.getIp()) {
                FreeIpaClientExceptionUtil.ignoreEmptyModOrDuplicateException(() -> freeIpaClient.addDnsARecord(freeIpa.getDomain(), lb.getEndpoint(), ip, true),
                        "LB A record with endpoint [{}], IP [{}], domain [{}] already exists, nothing to do", lb.getEndpoint(), lb.getIp(), freeIpa.getDomain());
            }
            // TODO PEM REG
        }
    }
}
