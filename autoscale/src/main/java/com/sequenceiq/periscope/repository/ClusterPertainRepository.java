package com.sequenceiq.periscope.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.ClusterPertain;

@EntityType(entityClass = ClusterPertain.class)
public interface ClusterPertainRepository extends CrudRepository<ClusterPertain, Long> {
    Optional<ClusterPertain> findByUserCrn(@Param("userCrn") String userCrn);
}
