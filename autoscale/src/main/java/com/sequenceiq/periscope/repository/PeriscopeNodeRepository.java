package com.sequenceiq.periscope.repository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.PeriscopeNode;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;

@EntityType(entityClass = PeriscopeNode.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface PeriscopeNodeRepository extends CrudRepository<PeriscopeNode, String> {

    long countByLeaderIsTrueAndLastUpdatedIsGreaterThan(long than);

    List<PeriscopeNode> findAllByLastUpdatedIsGreaterThan(long than);

    @Modifying
    @Query("UPDATE PeriscopeNode pn SET pn.leader = false WHERE pn.leader = true")
    void deallocateLeader();

    @Modifying
    @Query("DELETE PeriscopeNode pn WHERE pn NOT IN :activeNodes")
    void deleteAllOtherNodes(@Param("activeNodes") List<PeriscopeNode> activeNodes);
}
