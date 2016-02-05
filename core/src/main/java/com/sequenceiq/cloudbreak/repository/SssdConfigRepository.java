package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.SssdConfig;

@EntityType(entityClass = SssdConfig.class)
public interface SssdConfigRepository extends CrudRepository<SssdConfig, Long> {

    SssdConfig findByNameInAccount(@Param("name") String name, @Param("account") String account);

    Set<SssdConfig> findPublicInAccountForUser(@Param("owner") String userId, @Param("account") String account);

    Set<SssdConfig> findAllInAccount(@Param("account") String account);

    Set<SssdConfig> findForUser(@Param("owner") String userId);

    SssdConfig findByNameForUser(@Param("name") String name, @Param("owner") String userId);
}
