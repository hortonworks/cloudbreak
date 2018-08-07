package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.BaseRepository;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Topology.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface TopologyRepository extends BaseRepository<Topology, Long> {

    @Query("SELECT t FROM Topology t WHERE t.account= :account AND deleted IS NOT TRUE")
    Set<Topology> findAllInAccount(@Param("account") String account);

    @Query("SELECT t FROM Topology t WHERE t.id= :id and t.account= :account AND deleted IS NOT TRUE")
    Topology findByIdInAccount(@Param("id") Long id, @Param("account") String account);
}
