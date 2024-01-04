package com.sequenceiq.periscope.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.ClusterPertain;

@EntityType(entityClass = ClusterPertain.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ClusterPertainRepository extends CrudRepository<ClusterPertain, Long> {
    Optional<ClusterPertain> findFirstByUserCrn(@Param("userCrn") String userCrn);
}
