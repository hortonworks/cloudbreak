package com.sequenceiq.mock.service;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.mock.salt.SaltStoreService;

@Service
public class HostNameService {

    @Inject
    private SaltStoreService saltStoreService;

    public String getHostName(String mockUuid, String privateIp) {
        List<Minion> minions = saltStoreService.getMinions(mockUuid);
        Optional<Minion> minion = minions.stream().filter(m -> m.getAddress().equals(privateIp)).findFirst();
        return minion.map(m -> m.getHostName() + "." + m.getDomain()).orElse(null);
    }
}
