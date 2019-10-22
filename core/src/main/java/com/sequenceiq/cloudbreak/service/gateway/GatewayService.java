package com.sequenceiq.cloudbreak.service.gateway;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.repository.GatewayRepository;

@Service
public class GatewayService {

    @Value("${cb.https.port}")
    private String httpsPort;

    @Inject
    private GatewayRepository repository;

    public Gateway save(Gateway gateway) {
        gateway.setGatewayPort(Integer.valueOf(httpsPort));
        return repository.save(gateway);
    }

}
