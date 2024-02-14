package com.sequenceiq.freeipa.repository;

import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.InstanceGroupAvailabilityZone;

@Transactional(Transactional.TxType.REQUIRED)
public interface InstanceGroupAvailabilityZoneRepository extends CrudRepository<InstanceGroupAvailabilityZone, Long> {
    @Query("SELECT az FROM instancegroup_availabilityzones az WHERE az.instanceGroup.id = :instanceGroupId")
    Set<InstanceGroupAvailabilityZone> findAllByInstanceGroupId(@Param("instanceGroupId") Long instanceGroupId);
}
