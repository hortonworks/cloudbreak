package com.sequenceiq.periscope.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.SecurityConfig;

import javax.transaction.Transactional;

@EntityType(entityClass = SecurityConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface SecurityConfigRepository extends CrudRepository<SecurityConfig, Long> {

    @Query("SELECT sc FROM SecurityConfig sc WHERE sc.cluster.id = :clusterId")
    SecurityConfig findByClusterId(@Param("clusterId") Long clusterId);

}
