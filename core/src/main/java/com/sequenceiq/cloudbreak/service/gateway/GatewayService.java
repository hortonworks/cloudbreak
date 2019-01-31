package com.sequenceiq.cloudbreak.service.gateway;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.repository.GatewayRepository;

@Service
public class GatewayService {

    @Inject
    private GatewayRepository gatewayRepository;

    public Gateway save(Gateway gateway) {
        return gatewayRepository.save(gateway);
    }

}
