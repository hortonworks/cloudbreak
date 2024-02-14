package com.sequenceiq.freeipa.service.stack.instance;

import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.InstanceGroupAvailabilityZone;
import com.sequenceiq.freeipa.repository.InstanceGroupAvailabilityZoneRepository;

@Service
public class InstanceGroupAvailabilityZoneService {

    @Inject
    private InstanceGroupAvailabilityZoneRepository repository;

    public Set<InstanceGroupAvailabilityZone> findAllByInstanceGroupId(Long instanceGroupId) {
        return repository.findAllByInstanceGroupId(instanceGroupId);
    }

}
