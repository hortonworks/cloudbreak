package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.ClusterPertain;

@EntityType(entityClass = ClusterPertain.class)
public interface ClusterPertainRepository extends CrudRepository<ClusterPertain, Long> {
    List<ClusterPertain> findByUserCrn(@Param("userCrn") String userCrn, Pageable page);

    default List<ClusterPertain> findByUserCrn(String userCrn) {
        return findByUserCrn(userCrn, PageRequest.of(0, 1));
    }
}
