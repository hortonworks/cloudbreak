package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.periscope.domain.PeriscopeNode;

@DisableHasPermission
@EntityType(entityClass = PeriscopeNode.class)
public interface PeriscopeNodeRepository extends DisabledBaseRepository<PeriscopeNode, String> {

    long countByLeaderIsTrueAndLastUpdatedIsGreaterThan(long than);

    List<PeriscopeNode> findAllByLastUpdatedIsGreaterThan(long than);

    @Modifying
    @Query("UPDATE PeriscopeNode pn SET pn.leader = false WHERE pn.leader = true")
    void deallocateLeader();

    @Modifying
    @Query("DELETE PeriscopeNode pn WHERE pn NOT IN :activeNodes")
    void deleteAllOtherNodes(@Param("activeNodes") List<PeriscopeNode> activeNodes);
}
