package com.sequenceiq.freeipa.service.crossrealm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.repository.CrossRealmTrustRepository;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustProvider;

@Component
public class CrossRealmTrustService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrossRealmTrustService.class);

    @Inject
    private CrossRealmTrustRepository crossRealmTrustRepository;

    @Inject
    private List<TrustProvider> trustProviderList;

    private final Map<KdcType, TrustProvider> trustSetupStepsMap = new HashMap<>();

    @PostConstruct
    public void init() {
        trustProviderList.forEach(steps -> trustSetupStepsMap.put(steps.kdcType(), steps));
    }

    public CrossRealmTrust getByStackId(Long stackId) {
        return crossRealmTrustRepository.findByStackId(stackId)
                .orElseThrow(() -> {
                    LOGGER.warn("Cross-realm trust config not found by FreeIPA stack id: {}", stackId);
                    return new NotFoundException("Cross-realm trust config not found.");
                });
    }

    public Optional<CrossRealmTrust> getByStackIdIfExists(Long stackId) {
        return crossRealmTrustRepository.findByStackId(stackId);
    }

    public void updateTrustStateByStackId(Long stackId, TrustStatus trustStatus) {
        crossRealmTrustRepository.updateTrustStatusByStackId(stackId, trustStatus);
    }

    public void updateOperationIdByStackId(Long stackId, String operationId) {
        crossRealmTrustRepository.updateOperationIdByStackId(stackId, operationId);
    }

    public void deleteByStackIdIfExists(Long stackId) {
        crossRealmTrustRepository.deleteByStackId(stackId);
    }

    public CrossRealmTrust save(CrossRealmTrust crossRealmTrust) {
        return crossRealmTrustRepository.save(crossRealmTrust);
    }

    public TrustProvider getTrustProvider(Long stackId) {
        CrossRealmTrust crossRealmTrust = getByStackId(stackId);
        return trustSetupStepsMap.computeIfAbsent(crossRealmTrust.getKdcType(), kdcType -> {
            throw new IllegalArgumentException("Unsupported KDC type: " + kdcType);
        });
    }
}
