package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@EntityType(entityClass = InstanceGroup.class)
public interface InstanceGroupRepository extends CrudRepository<InstanceGroup, Long> {

    @Override
    InstanceGroup findOne(@Param("id") Long id);

    @Query("SELECT i from InstanceGroup i WHERE i.stack.id = :stackId AND i.groupName = :groupName")
    InstanceGroup findOneByGroupNameInStack(@Param("stackId") Long stackId, @Param("groupName") String groupName);

    Long countBySecurityGroup(SecurityGroup securityGroup);
}