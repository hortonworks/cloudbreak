package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.InstanceGroup;

public interface InstanceGroupRepository extends CrudRepository<InstanceGroup, Long> {

    InstanceGroup findOne(@Param("id") Long id);

    InstanceGroup findOneByGroupNameInStack(@Param("stackId") Long stackId, @Param("groupName") String groupName);

}