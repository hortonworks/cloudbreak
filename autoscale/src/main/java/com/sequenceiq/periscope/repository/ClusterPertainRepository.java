package com.sequenceiq.periscope.repository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.ClusterPertain;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.Optional;

@EntityType(entityClass = ClusterPertain.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ClusterPertainRepository extends CrudRepository<ClusterPertain, Long> {
    Optional<ClusterPertain> findByUserCrn(@Param("userCrn") String userCrn);
}