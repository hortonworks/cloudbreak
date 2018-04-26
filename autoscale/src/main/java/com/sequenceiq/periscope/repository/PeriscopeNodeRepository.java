package com.sequenceiq.periscope.repository;


import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.domain.PeriscopeNode;

public interface PeriscopeNodeRepository extends CrudRepository<PeriscopeNode, String> {

    long countByLeaderIsTrueAndLastUpdatedIsGreaterThan(long than);

    List<PeriscopeNode> findAllByLastUpdatedIsGreaterThan(long than);

    @Modifying
    @Query("UPDATE PeriscopeNode pn SET pn.leader = false WHERE pn.leader = true")
    void deallocateLeader();
}
