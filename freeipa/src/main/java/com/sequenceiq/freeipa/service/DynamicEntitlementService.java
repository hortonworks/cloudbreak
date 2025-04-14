package com.sequenceiq.freeipa.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.DynamicEntitlement;
import com.sequenceiq.freeipa.repository.DynamicEntitlementRepository;

@Service
public class DynamicEntitlementService {

    @Inject
    private DynamicEntitlementRepository repository;

    public Set<DynamicEntitlement> findByStackId(Long stackId) {
        return repository.findByStackId(stackId);
    }

    public DynamicEntitlement save(DynamicEntitlement dynamicEntitlement) {
        return repository.save(dynamicEntitlement);
    }

    public Iterable<DynamicEntitlement> saveNew(Long stackId, Iterable<DynamicEntitlement> dynamicEntitlements) {
        Set<DynamicEntitlement> changed = new HashSet<>();
        Set<DynamicEntitlement> entitlementsInDB = repository.findByStackId(stackId);
        for (DynamicEntitlement dynamicEntitlement : dynamicEntitlements) {
            if (!entitlementsInDB.contains(dynamicEntitlement)) {
                changed.add(dynamicEntitlement);
            } else {
                Optional<DynamicEntitlement> dbValueOptional = entitlementsInDB.stream().filter(inDb -> inDb.equals(dynamicEntitlement)).findFirst();
                dbValueOptional.ifPresent(dbValue -> {
                    if (dbValue.getEntitlementValue() != dynamicEntitlement.getEntitlementValue()) {
                        dbValue.setEntitlementValue(dynamicEntitlement.getEntitlementValue());
                        changed.add(dbValue);
                    }
                });
            }
        }
        return repository.saveAll(changed);
    }

}
