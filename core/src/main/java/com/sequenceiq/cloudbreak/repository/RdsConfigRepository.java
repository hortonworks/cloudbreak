package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.RDSConfig;

@EntityType(entityClass = RDSConfig.class)
public interface RdsConfigRepository extends CrudRepository<RDSConfig, Long> {
    Set<RDSConfig> findForUser(@Param("user") String user);

    Set<RDSConfig> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    Set<RDSConfig> findAllInAccount(@Param("account") String account);

    RDSConfig findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    RDSConfig findOneByName(@Param("name") String name, @Param("account") String account);

    RDSConfig findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    RDSConfig findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    RDSConfig findById(@Param("id") Long id);
}
